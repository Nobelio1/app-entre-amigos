package com.entreamigos.backend.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.entreamigos.backend.db.DatabaseFactory.consulta
import com.entreamigos.backend.db.Usuarios
import com.entreamigos.backend.db.esViolacionDeUnicidad
import com.entreamigos.backend.model.LoginGoogleRequest
import com.entreamigos.backend.model.LoginRequest
import com.entreamigos.backend.model.RegistroRequest
import com.entreamigos.backend.model.SesionDto
import com.entreamigos.backend.model.UsuarioDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.mindrot.jbcrypt.BCrypt
import java.util.Date
import java.util.concurrent.TimeUnit

data class ConfigJwt(val secreto: String, val emisor: String, val audiencia: String, val validezDias: Long)

class ServicioTokens(private val config: ConfigJwt) {

    private val algoritmo = Algorithm.HMAC256(config.secreto)

    val verificador: JWTVerifier = JWT.require(algoritmo)
        .withIssuer(config.emisor)
        .withAudience(config.audiencia)
        .build()

    fun emitir(usuarioId: Long): Pair<String, Long> {
        val expira = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(config.validezDias)
        val token = JWT.create()
            .withIssuer(config.emisor)
            .withAudience(config.audiencia)
            .withSubject(usuarioId.toString())
            .withIssuedAt(Date())
            .withExpiresAt(Date(expira))
            .sign(algoritmo)
        return token to expira
    }
}

class AuthService(
    private val tokens: ServicioTokens,
    /** null si FIREBASE_PROJECT_ID no está configurado: el login con Google queda deshabilitado. */
    private val verificadorGoogle: VerificadorTokenGoogle? = null
) {

    suspend fun registrar(request: RegistroRequest): SesionDto {
        val nombre = request.nombre.trim()
        val email = request.email.trim().lowercase()
        validar(nombre.isNotEmpty()) { "El nombre es obligatorio" }
        validar(nombre.length <= 80) { "El nombre no puede superar 80 caracteres" }
        validar(EMAIL.matches(email) && email.length <= 160) { "El email no es válido" }
        validar(request.password.length >= PASSWORD_MINIMO) {
            "La contraseña debe tener al menos $PASSWORD_MINIMO caracteres"
        }
        validar(request.password.length <= 72) { "La contraseña no puede superar 72 caracteres" }

        val hash = BCrypt.hashpw(request.password, BCrypt.gensalt(10))
        val usuario = try {
            consulta {
                if (buscarPorEmail(email) != null) throw ConflictoException(EMAIL_REPETIDO)
                val id = Usuarios.insert {
                    it[Usuarios.nombre] = nombre
                    it[Usuarios.email] = email
                    it[passwordHash] = hash
                    it[colorAvatarHex] = PALETA.random()
                    it[fechaCreacion] = System.currentTimeMillis()
                } get Usuarios.id
                Usuarios.selectAll().where { Usuarios.id eq id }.single().aUsuarioDto()
            }
        } catch (e: Exception) {
            // Dos registros simultáneos con el mismo email: lo resuelve el índice único
            if (e.esViolacionDeUnicidad()) throw ConflictoException(EMAIL_REPETIDO)
            throw e
        }
        return sesionPara(usuario)
    }

    suspend fun login(request: LoginRequest): SesionDto {
        val email = request.email.trim().lowercase()
        validar(email.isNotEmpty() && request.password.isNotEmpty()) { "Ingresa tu email y contraseña" }
        val fila = consulta { buscarPorEmail(email) }
        val hash = fila?.get(Usuarios.passwordHash)
        if (fila != null && hash == null) {
            throw NoAutenticadoException("Esta cuenta se creó con Google. Usa «Continuar con Google».")
        }
        val valido = fila != null && runCatching { BCrypt.checkpw(request.password, hash) }.getOrDefault(false)
        if (!valido) throw NoAutenticadoException("Email o contraseña incorrectos")
        return sesionPara(fila!!.aUsuarioDto())
    }

    /**
     * Inicia sesión con el ID token que Firebase Authentication entregó a la app.
     * Busca al usuario por su UID de Firebase; si no existe y Google confirma el email, se
     * vincula con la cuenta que ya tenga ese email; si no, se crea una cuenta nueva.
     */
    suspend fun loginConGoogle(request: LoginGoogleRequest): SesionDto {
        val verificador = verificadorGoogle
            ?: throw ServicioNoDisponibleException("El inicio de sesión con Google no está configurado en el servidor (falta FIREBASE_PROJECT_ID)")
        validar(request.idToken.isNotBlank()) { "Falta el idToken de Google" }
        val identidad = verificador.verificar(request.idToken.trim())

        val usuario = try {
            consulta { vincularOCrear(identidad) }
        } catch (e: Exception) {
            // Primer login simultáneo desde dos dispositivos: el otro ya creó la cuenta
            if (!e.esViolacionDeUnicidad()) throw e
            consulta { vincularOCrear(identidad) }
        }
        return sesionPara(usuario)
    }

    private fun vincularOCrear(identidad: IdentidadGoogle): UsuarioDto {
        Usuarios.selectAll().where { Usuarios.firebaseUid eq identidad.uid }.singleOrNull()?.let { fila ->
            if (fila[Usuarios.fotoUrl] != identidad.fotoUrl) {
                Usuarios.update({ Usuarios.id eq fila[Usuarios.id] }) { it[fotoUrl] = identidad.fotoUrl?.take(500) }
            }
            return Usuarios.selectAll().where { Usuarios.id eq fila[Usuarios.id] }.single().aUsuarioDto()
        }

        val email = identidad.email?.trim()?.lowercase()
            ?: throw SolicitudInvalidaException("Tu cuenta de Google no compartió un email")
        val existente = buscarPorEmail(email)
        if (existente != null) {
            if (!identidad.emailVerificado) {
                throw ConflictoException("Ya existe una cuenta con $email. Inicia sesión con tu contraseña.")
            }
            if (existente[Usuarios.firebaseUid] != null) {
                throw ConflictoException("Esa cuenta ya está vinculada a otro usuario de Google")
            }
            Usuarios.update({ Usuarios.id eq existente[Usuarios.id] }) {
                it[firebaseUid] = identidad.uid
                it[fotoUrl] = identidad.fotoUrl?.take(500)
            }
            return Usuarios.selectAll().where { Usuarios.id eq existente[Usuarios.id] }.single().aUsuarioDto()
        }

        val nombre = identidad.nombre?.trim()?.takeIf { it.isNotEmpty() }?.take(80)
            ?: email.substringBefore('@').take(80)
        val id = Usuarios.insert {
            it[Usuarios.nombre] = nombre
            it[Usuarios.email] = email
            it[passwordHash] = null
            it[colorAvatarHex] = PALETA.random()
            it[fechaCreacion] = System.currentTimeMillis()
            it[firebaseUid] = identidad.uid
            it[fotoUrl] = identidad.fotoUrl?.take(500)
        } get Usuarios.id
        return Usuarios.selectAll().where { Usuarios.id eq id }.single().aUsuarioDto()
    }

    suspend fun obtener(usuarioId: Long): UsuarioDto = consulta {
        Usuarios.selectAll().where { Usuarios.id eq usuarioId }.singleOrNull()?.aUsuarioDto()
            ?: throw NoAutenticadoException("La cuenta ya no existe")
    }

    private fun sesionPara(usuario: UsuarioDto): SesionDto {
        val (token, expira) = tokens.emitir(usuario.id)
        return SesionDto(token = token, expiraEn = expira, usuario = usuario)
    }

    private fun buscarPorEmail(email: String): ResultRow? =
        Usuarios.selectAll().where { Usuarios.email.lowerCase() eq email }.singleOrNull()

    companion object {
        private const val PASSWORD_MINIMO = 8
        private const val EMAIL_REPETIDO = "Ya existe una cuenta con ese email"
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private val PALETA = GrupoService.PALETA_AVATARES
    }
}

fun ResultRow.aUsuarioDto() = UsuarioDto(
    id = this[Usuarios.id],
    nombre = this[Usuarios.nombre],
    email = this[Usuarios.email],
    colorAvatarHex = this[Usuarios.colorAvatarHex],
    fotoUrl = this[Usuarios.fotoUrl],
    conGoogle = this[Usuarios.firebaseUid] != null
)
