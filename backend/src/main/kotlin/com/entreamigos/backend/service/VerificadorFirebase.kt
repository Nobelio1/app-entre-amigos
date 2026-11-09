package com.entreamigos.backend.service

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.RSAKeyProvider
import java.net.URL
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

/** Datos de la persona que inició sesión con Google, extraídos del ID token de Firebase. */
data class IdentidadGoogle(
    val uid: String,
    val email: String?,
    val emailVerificado: Boolean,
    val nombre: String?,
    val fotoUrl: String?
)

fun interface VerificadorTokenGoogle {
    /** Lanza [NoAutenticadoException] si el token no es válido. */
    fun verificar(idToken: String): IdentidadGoogle
}

/**
 * Verifica ID tokens de Firebase Authentication según la documentación oficial
 * ("Verify ID tokens using a third-party JWT library"):
 *  - firma RS256 con una de las claves públicas de `securetoken@system.gserviceaccount.com`
 *  - `aud` = id del proyecto Firebase, `iss` = `https://securetoken.google.com/<proyecto>`
 *  - no expirado, `sub` no vacío y `auth_time` en el pasado
 *
 * No necesita la clave privada del service account: solo el id del proyecto.
 */
class VerificadorFirebase(
    private val proyectoId: String,
    private val claves: JwkProvider = JwkProviderBuilder(URL(URL_CLAVES))
        .cached(10, 6, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
) : VerificadorTokenGoogle {

    private val verificador = JWT.require(
        Algorithm.RSA256(object : RSAKeyProvider {
            override fun getPublicKeyById(keyId: String?): RSAPublicKey =
                claves.get(keyId).publicKey as RSAPublicKey

            override fun getPrivateKey(): RSAPrivateKey? = null
            override fun getPrivateKeyId(): String? = null
        })
    )
        .withIssuer("https://securetoken.google.com/$proyectoId")
        .withAudience(proyectoId)
        .acceptLeeway(MARGEN_RELOJ_SEGUNDOS)
        .build()

    override fun verificar(idToken: String): IdentidadGoogle {
        val token = try {
            verificador.verify(idToken)
        } catch (e: JWTVerificationException) {
            throw NoAutenticadoException("El token de Google no es válido o expiró")
        } catch (e: Exception) {
            // Clave desconocida, sin red para descargar las claves, token mal formado…
            throw NoAutenticadoException("No se pudo verificar el token de Google")
        }
        val uid = token.subject?.takeIf { it.isNotBlank() && it.length <= 128 }
            ?: throw NoAutenticadoException("El token de Google no identifica a un usuario")
        val authTime = token.getClaim("auth_time").asLong()
        if (authTime == null || authTime > System.currentTimeMillis() / 1000 + MARGEN_RELOJ_SEGUNDOS) {
            throw NoAutenticadoException("El token de Google no es válido")
        }
        return IdentidadGoogle(
            uid = uid,
            email = token.getClaim("email").asString(),
            emailVerificado = token.getClaim("email_verified").asBoolean() == true,
            nombre = token.getClaim("name").asString(),
            fotoUrl = token.getClaim("picture").asString()
        )
    }

    companion object {
        const val URL_CLAVES =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
        private const val MARGEN_RELOJ_SEGUNDOS = 60L
    }
}
