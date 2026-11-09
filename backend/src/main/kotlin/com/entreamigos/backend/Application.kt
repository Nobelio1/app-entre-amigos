package com.entreamigos.backend

import com.entreamigos.backend.db.ConfigBaseDatos
import com.entreamigos.backend.db.DatabaseFactory
import com.entreamigos.backend.plugins.AUTH_JWT
import com.entreamigos.backend.plugins.configurarErrores
import com.entreamigos.backend.plugins.configurarLogs
import com.entreamigos.backend.plugins.configurarSeguridad
import com.entreamigos.backend.plugins.configurarSerializacion
import com.entreamigos.backend.routes.rutasAuth
import com.entreamigos.backend.routes.rutasGrupos
import com.entreamigos.backend.service.AuthService
import com.entreamigos.backend.service.ConfigJwt
import com.entreamigos.backend.service.GastoService
import com.entreamigos.backend.service.GrupoService
import com.entreamigos.backend.service.ServicioTokens
import com.entreamigos.backend.service.VerificadorFirebase
import com.entreamigos.backend.service.VerificadorTokenGoogle
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.netty.EngineMain
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>) = EngineMain.main(args)

private const val SECRETO_DESARROLLO = "entreamigos-dev-secret-cambiar-en-produccion"

fun Application.module() {
    val config = environment.config
    DatabaseFactory.inicializar(
        ConfigBaseDatos(
            url = config.property("database.url").getString(),
            usuario = config.property("database.user").getString(),
            password = config.property("database.password").getString(),
            maxPoolSize = config.property("database.maxPoolSize").getString().toInt(),
            cargarSeed = config.property("database.seed").getString().toBoolean()
        )
    )
    val configJwt = ConfigJwt(
        secreto = config.property("jwt.secret").getString(),
        emisor = config.property("jwt.issuer").getString(),
        audiencia = config.property("jwt.audience").getString(),
        validezDias = config.property("jwt.validezDias").getString().toLong()
    )
    if (configJwt.secreto == SECRETO_DESARROLLO) {
        environment.log.warn("JWT_SECRET no está definido: se usa el secreto de desarrollo. No lo uses en producción.")
    }
    val proyectoFirebase = config.propertyOrNull("firebase.projectId")?.getString()?.trim().orEmpty()
    val verificadorGoogle = if (proyectoFirebase.isEmpty()) {
        environment.log.warn("FIREBASE_PROJECT_ID no está definido: el inicio de sesión con Google está deshabilitado.")
        null
    } else {
        environment.log.info("Inicio de sesión con Google habilitado para el proyecto Firebase '{}'", proyectoFirebase)
        VerificadorFirebase(proyectoFirebase)
    }
    moduloApi(ServicioTokens(configJwt), verificadorGoogle)
}

fun Application.moduloApi(tokens: ServicioTokens, verificadorGoogle: VerificadorTokenGoogle? = null) {
    configurarSerializacion()
    configurarLogs()
    configurarErrores()
    configurarSeguridad(tokens)

    val grupoService = GrupoService()
    val gastoService = GastoService(grupoService)
    val authService = AuthService(tokens, verificadorGoogle)
    routing {
        get("/health") { call.respond(mapOf("estado" to "ok")) }
        rutasAuth(authService)
        authenticate(AUTH_JWT) {
            rutasGrupos(grupoService, gastoService)
        }
    }
}
