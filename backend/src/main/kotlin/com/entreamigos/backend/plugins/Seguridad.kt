package com.entreamigos.backend.plugins

import com.entreamigos.backend.model.ErrorResponse
import com.entreamigos.backend.service.NoAutenticadoException
import com.entreamigos.backend.service.ServicioTokens
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond

const val AUTH_JWT = "auth-jwt"

data class UsuarioPrincipal(val id: Long) : Principal

fun Application.configurarSeguridad(tokens: ServicioTokens) {
    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = "entreamigos"
            verifier(tokens.verificador)
            validate { credencial ->
                credencial.payload.subject?.toLongOrNull()?.takeIf { it > 0 }?.let(::UsuarioPrincipal)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Inicia sesión para continuar (token ausente, inválido o expirado)"))
            }
        }
    }
}

fun ApplicationCall.usuarioId(): Long =
    principal<UsuarioPrincipal>()?.id ?: throw NoAutenticadoException("Inicia sesión para continuar")
