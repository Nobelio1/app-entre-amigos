package com.entreamigos.backend.routes

import com.entreamigos.backend.model.LoginGoogleRequest
import com.entreamigos.backend.model.LoginRequest
import com.entreamigos.backend.model.RegistroRequest
import com.entreamigos.backend.plugins.AUTH_JWT
import com.entreamigos.backend.plugins.usuarioId
import com.entreamigos.backend.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.rutasAuth(authService: AuthService) {
    route("/auth") {
        post("/registro") {
            val request = call.receive<RegistroRequest>()
            call.respond(HttpStatusCode.Created, authService.registrar(request))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            call.respond(authService.login(request))
        }

        post("/google") {
            val request = call.receive<LoginGoogleRequest>()
            call.respond(authService.loginConGoogle(request))
        }

        authenticate(AUTH_JWT) {
            get("/yo") { call.respond(authService.obtener(call.usuarioId())) }
        }
    }
}
