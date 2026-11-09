package com.entreamigos.app.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Sesion(
    val token: String,
    val expiraEn: Long,
    val usuarioId: Long,
    val nombre: String,
    val email: String,
    val colorAvatarHex: String
) {
    val primerNombre: String get() = nombre.trim().substringBefore(' ').ifEmpty { nombre }
}

/**
 * Guarda el token JWT y los datos del usuario autenticado en SharedPreferences privadas
 * de la app. [sesion] es `null` cuando no hay nadie logueado (o el token expiró).
 */
class SesionManager(context: Context) {

    private val prefs = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    private val _sesion = MutableStateFlow(leer())
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    /** Token vigente para el encabezado Authorization (lo usa el interceptor de OkHttp). */
    val token: String? get() = _sesion.value?.token

    fun guardar(sesion: Sesion) {
        prefs.edit()
            .putString(CLAVE_TOKEN, sesion.token)
            .putLong(CLAVE_EXPIRA, sesion.expiraEn)
            .putLong(CLAVE_ID, sesion.usuarioId)
            .putString(CLAVE_NOMBRE, sesion.nombre)
            .putString(CLAVE_EMAIL, sesion.email)
            .putString(CLAVE_COLOR, sesion.colorAvatarHex)
            .apply()
        _sesion.value = sesion
    }

    fun actualizarUsuario(nombre: String, email: String, colorAvatarHex: String) {
        val actual = _sesion.value ?: return
        guardar(actual.copy(nombre = nombre, email = email, colorAvatarHex = colorAvatarHex))
    }

    /** Quita el token (logout o sesión expirada). Los datos locales se conservan hasta saber quién entra. */
    fun borrar() {
        prefs.edit()
            .remove(CLAVE_TOKEN).remove(CLAVE_EXPIRA).remove(CLAVE_ID)
            .remove(CLAVE_NOMBRE).remove(CLAVE_EMAIL).remove(CLAVE_COLOR)
            .apply()
        _sesion.value = null
    }

    /**
     * Usuario al que pertenecen los datos guardados en Room. Sobrevive a [borrar] para que,
     * si la sesión expira y vuelve a entrar la misma persona, no se pierdan cambios offline.
     */
    var propietarioDatos: Long?
        get() = prefs.getLong(CLAVE_PROPIETARIO, -1L).takeIf { it > 0 }
        set(valor) {
            prefs.edit().apply { if (valor == null) remove(CLAVE_PROPIETARIO) else putLong(CLAVE_PROPIETARIO, valor) }.apply()
        }

    private fun leer(): Sesion? {
        val token = prefs.getString(CLAVE_TOKEN, null) ?: return null
        val expira = prefs.getLong(CLAVE_EXPIRA, 0L)
        if (expira <= System.currentTimeMillis()) return null
        return Sesion(
            token = token,
            expiraEn = expira,
            usuarioId = prefs.getLong(CLAVE_ID, 0L),
            nombre = prefs.getString(CLAVE_NOMBRE, null).orEmpty(),
            email = prefs.getString(CLAVE_EMAIL, null).orEmpty(),
            colorAvatarHex = prefs.getString(CLAVE_COLOR, null) ?: "#7E57C2"
        )
    }

    private companion object {
        const val ARCHIVO = "entreamigos_sesion"
        const val CLAVE_TOKEN = "token"
        const val CLAVE_EXPIRA = "expira_en"
        const val CLAVE_ID = "usuario_id"
        const val CLAVE_NOMBRE = "nombre"
        const val CLAVE_EMAIL = "email"
        const val CLAVE_COLOR = "color_avatar"
        const val CLAVE_PROPIETARIO = "propietario_datos"
    }
}
