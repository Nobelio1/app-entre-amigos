package com.entreamigos.backend.plugins

import com.entreamigos.backend.model.ErrorResponse
import com.entreamigos.backend.service.RecursoNoEncontradoException
import com.entreamigos.backend.service.SolicitudInvalidaException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun Application.configurarSerializacion() {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        })
    }
}

fun Application.configurarLogs() {
    install(CallLogging) {
        level = Level.INFO
        format { call -> "${call.request.httpMethod.value} ${call.request.path()} -> ${call.response.status()}" }
    }
}

fun Application.configurarErrores() {
    install(StatusPages) {
        exception<SolicitudInvalidaException> { call, causa ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(causa.message ?: "Solicitud inválida"))
        }
        exception<BadRequestException> { call, causa ->
            val detalle = generateSequence(causa.cause) { it.cause }
                .firstNotNullOfOrNull { it.message }
                ?.lineSequence()?.first()
                ?.removePrefix("Illegal input: ")
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Cuerpo de la solicitud inválido: ${detalle ?: causa.message}"))
        }
        exception<RecursoNoEncontradoException> { call, causa ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(causa.message ?: "Recurso no encontrado"))
        }
        exception<Throwable> { call, causa ->
            call.application.environment.log.error("Error no controlado en ${call.request.path()}", causa)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Error interno del servidor"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("Ruta no encontrada: ${call.request.httpMethod.value} ${call.request.path()}"))
        }
    }
}
