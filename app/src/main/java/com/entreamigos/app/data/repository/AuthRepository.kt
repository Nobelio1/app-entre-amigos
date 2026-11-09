package com.entreamigos.app.data.repository

import android.content.Context
import com.entreamigos.app.data.local.AppDatabase
import com.entreamigos.app.data.local.PreferenciasUsuario
import com.entreamigos.app.data.local.Sesion
import com.entreamigos.app.data.local.SesionManager
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.TipoError
import com.entreamigos.app.data.remote.GoogleAuthCliente
import com.entreamigos.app.data.remote.ResultadoGoogle
import com.entreamigos.app.data.remote.dto.LoginGoogleRequest
import com.entreamigos.app.data.remote.RemoteDataSource
import com.entreamigos.app.data.remote.dto.InvitacionDto
import com.entreamigos.app.data.remote.dto.LoginRequest
import com.entreamigos.app.data.remote.dto.RegistroRequest
import com.entreamigos.app.data.remote.dto.SesionDto
import com.entreamigos.app.data.remote.dto.UnirseGrupoRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class AuthRepository(
    private val database: AppDatabase,
    private val remoto: RemoteDataSource,
    private val sesionManager: SesionManager,
    private val sincronizacion: SincronizacionRepository,
    private val preferencias: PreferenciasUsuario,
    private val googleAuth: GoogleAuthCliente
) {

    val sesion: StateFlow<Sesion?> = sesionManager.sesion

    /** false si la app se compiló sin google-services.json. */
    val googleDisponible: Boolean get() = googleAuth.disponible

    /**
     * "Continuar con Google". Devuelve null si la persona cerró el selector de cuentas.
     * [contextoActividad] es la Activity actual (Credential Manager muestra su UI encima).
     */
    suspend fun iniciarSesionConGoogle(contextoActividad: Context): Resource<Unit>? {
        val idToken = when (val r = googleAuth.obtenerToken(contextoActividad)) {
            is ResultadoGoogle.Token -> r.idTokenFirebase
            ResultadoGoogle.Cancelado -> return null
            is ResultadoGoogle.Error ->
                return Resource.Error(r.mensaje, if (r.sinConexion) TipoError.SIN_CONEXION else TipoError.DESCONOCIDO)
        }
        val resultado = abrirSesion(remoto.llamar { loginConGoogle(LoginGoogleRequest(idToken)) })
        // Si nuestro backend lo rechaza, no dejamos una sesión de Firebase colgando
        if (resultado is Resource.Error) googleAuth.cerrarSesion()
        return resultado
    }

    suspend fun iniciarSesion(email: String, password: String): Resource<Unit> =
        abrirSesion(remoto.llamar { login(LoginRequest(email.trim(), password)) })

    suspend fun registrarse(nombre: String, email: String, password: String): Resource<Unit> =
        abrirSesion(remoto.llamar { registrar(RegistroRequest(nombre.trim(), email.trim(), password)) })

    /** Cierra la sesión y borra los datos locales de esta cuenta (incluidos cambios sin subir). */
    suspend fun cerrarSesion() {
        googleAuth.cerrarSesion()
        sincronizacion.enExclusiva {
            borrarDatosLocales()
            sesionManager.propietarioDatos = null
            // Al final: quitar la sesión saca de composición la UI que lanzó esta corrutina
            sesionManager.borrar()
        }
    }

    suspend fun verInvitacion(codigo: String): Resource<InvitacionDto> =
        remoto.llamar { verInvitacion(codigo.trim().uppercase()) }

    /** Une al usuario al grupo y lo descarga. Devuelve el id local del grupo. */
    suspend fun unirseAGrupo(codigo: String, miembroId: Long?): Resource<Long> {
        val grupo = when (val r = remoto.llamar { unirseAGrupo(UnirseGrupoRequest(codigo.trim().uppercase(), miembroId)) }) {
            is Resource.Success -> r.data
            is Resource.Error -> return r
            Resource.Loading -> return Resource.Loading
        }
        sincronizacion.sincronizar()
        val local = database.grupoDao().buscarPorRemoteId(grupo.id)
            ?: return Resource.Error("Te uniste al grupo, pero aún no se pudo descargar. Desliza para actualizar.")
        return Resource.Success(local.id)
    }

    private suspend fun abrirSesion(respuesta: Resource<SesionDto>): Resource<Unit> {
        val dto = when (respuesta) {
            is Resource.Success -> respuesta.data
            is Resource.Error -> return respuesta
            Resource.Loading -> return Resource.Loading
        }
        sincronizacion.enExclusiva {
            // Los datos locales de otra cuenta no deben mezclarse con los de esta
            if (sesionManager.propietarioDatos != dto.usuario.id) {
                borrarDatosLocales()
                sesionManager.propietarioDatos = dto.usuario.id
            }
            sesionManager.guardar(
                Sesion(
                    token = dto.token,
                    expiraEn = dto.expiraEn,
                    usuarioId = dto.usuario.id,
                    nombre = dto.usuario.nombre,
                    email = dto.usuario.email,
                    colorAvatarHex = dto.usuario.colorAvatarHex
                )
            )
        }
        sincronizacion.sincronizarEnSegundoPlano()
        return Resource.Success(Unit)
    }

    private suspend fun borrarDatosLocales() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        preferencias.marcarNotificacionesVistas(0L)
    }
}
