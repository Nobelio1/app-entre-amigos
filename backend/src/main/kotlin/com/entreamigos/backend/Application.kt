package com.entreamigos.backend

import com.entreamigos.backend.db.ConfigBaseDatos
import com.entreamigos.backend.db.DatabaseFactory
import com.entreamigos.backend.plugins.configurarErrores
import com.entreamigos.backend.plugins.configurarLogs
import com.entreamigos.backend.plugins.configurarSerializacion
import com.entreamigos.backend.routes.rutasGrupos
import com.entreamigos.backend.service.GastoService
import com.entreamigos.backend.service.GrupoService
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.netty.EngineMain
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = environment.config
    DatabaseFactory.inicializar(
        ConfigBaseDatos(
            url = config.property("database.url").getString(),
            usuario = config.property("database.user").getString(),
            password = config.property("database.password").getString(),
            maxPoolSize = config.property("database.maxPoolSize").getString().toInt()
        )
    )
    moduloApi(GrupoService())
}

fun Application.moduloApi(grupoService: GrupoService) {
    configurarSerializacion()
    configurarLogs()
    configurarErrores()

    val gastoService = GastoService(grupoService)
    routing {
        get("/health") { call.respond(mapOf("estado" to "ok")) }
        rutasGrupos(grupoService, gastoService)
    }
}
