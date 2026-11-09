package com.entreamigos.backend.service

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VerificadorFirebaseTest {

    private val proyecto = "entreamigos-test"
    private val par = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publica = par.public as RSAPublicKey
    private val privada = par.private as RSAPrivateKey

    /** Hace de endpoint de claves públicas de Google con una sola clave "kid-1". */
    private val claves = JwkProvider { kid ->
        if (kid != "kid-1") throw SigningKeyNotFoundException("kid desconocido", null)
        val b64 = Base64.getUrlEncoder().withoutPadding()
        Jwk.fromValues(
            mapOf(
                "kid" to "kid-1", "kty" to "RSA", "alg" to "RS256", "use" to "sig",
                "n" to b64.encodeToString(publica.modulus.toByteArray().dropWhile { it == 0.toByte() }.toByteArray()),
                "e" to b64.encodeToString(publica.publicExponent.toByteArray())
            )
        )
    }

    private val verificador = VerificadorFirebase(proyecto, claves)

    private fun token(
        aud: String = proyecto,
        iss: String = "https://securetoken.google.com/$proyecto",
        expiraEnSeg: Long = 3600,
        kid: String = "kid-1",
        sub: String = "uid-123",
        clave: RSAPrivateKey = privada
    ): String {
        val ahora = System.currentTimeMillis()
        return JWT.create()
            .withKeyId(kid)
            .withAudience(aud)
            .withIssuer(iss)
            .withSubject(sub)
            .withIssuedAt(Date(ahora - 1000))
            .withExpiresAt(Date(ahora + expiraEnSeg * 1000))
            .withClaim("auth_time", ahora / 1000 - 5)
            .withClaim("email", "Ana@Gmail.com")
            .withClaim("email_verified", true)
            .withClaim("name", "Ana Pérez")
            .withClaim("picture", "https://lh3.googleusercontent.com/foto")
            .sign(Algorithm.RSA256(null, clave))
    }

    @Test
    fun `token valido devuelve la identidad de Google`() {
        val identidad = verificador.verificar(token())
        assertEquals("uid-123", identidad.uid)
        assertEquals("Ana@Gmail.com", identidad.email)
        assertTrue(identidad.emailVerificado)
        assertEquals("Ana Pérez", identidad.nombre)
    }

    @Test
    fun `token de otro proyecto se rechaza`() {
        assertFailsWith<NoAutenticadoException> { verificador.verificar(token(aud = "otro-proyecto")) }
        assertFailsWith<NoAutenticadoException> {
            verificador.verificar(token(iss = "https://securetoken.google.com/otro-proyecto"))
        }
    }

    @Test
    fun `token expirado se rechaza`() {
        assertFailsWith<NoAutenticadoException> { verificador.verificar(token(expiraEnSeg = -600)) }
    }

    @Test
    fun `token firmado con otra clave o kid desconocido se rechaza`() {
        val otraClave = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().private as RSAPrivateKey
        assertFailsWith<NoAutenticadoException> { verificador.verificar(token(clave = otraClave)) }
        assertFailsWith<NoAutenticadoException> { verificador.verificar(token(kid = "kid-falso")) }
    }

    @Test
    fun `basura no es un token`() {
        assertFailsWith<NoAutenticadoException> { verificador.verificar("no-es-un-jwt") }
    }
}
