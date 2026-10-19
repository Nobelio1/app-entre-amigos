package com.entreamigos.backend.routes

import com.entreamigos.backend.model.CrearGastoRequest
import com.entreamigos.backend.model.CrearGrupoRequest
import com.entreamigos.backend.model.CrearMiembroRequest
import com.entreamigos.backend.model.CrearPagoRequest
import com.entreamigos.backend.service.GastoService
import com.entreamigos.backend.service.GrupoService
import com.entreamigos.backend.service.SolicitudInvalidaException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.rutasGrupos(grupoService: GrupoService, gastoService: GastoService) {
    route("/grupos") {
        get { call.respond(grupoService.listar()) }

        post {
            val request = call.receive<CrearGrupoRequest>()
            call.respond(HttpStatusCode.Created, grupoService.crear(request))
        }

        route("/{id}") {
            get { call.respond(grupoService.obtener(call.grupoId())) }

            post("/miembros") {
                val request = call.receive<CrearMiembroRequest>()
                call.respond(HttpStatusCode.Created, grupoService.agregarMiembro(call.grupoId(), request))
            }

            get("/gastos") { call.respond(gastoService.listarGastos(call.grupoId())) }

            post("/gastos") {
                val request = call.receive<CrearGastoRequest>()
                call.respond(HttpStatusCode.Created, gastoService.registrarGasto(call.grupoId(), request))
            }

            get("/pagos") { call.respond(gastoService.listarPagos(call.grupoId())) }

            post("/pagos") {
                val request = call.receive<CrearPagoRequest>()
                call.respond(HttpStatusCode.Created, gastoService.registrarPago(call.grupoId(), request))
            }

            get("/saldos") { call.respond(gastoService.calcularSaldos(call.grupoId())) }

            get("/estadisticas") { call.respond(gastoService.calcularEstadisticas(call.grupoId())) }

            get("/actividad") {
                val limite = call.request.queryParameters["limite"]?.let {
                    it.toIntOrNull()?.takeIf { n -> n in 1..500 }
                        ?: throw SolicitudInvalidaException("limite debe ser un entero entre 1 y 500")
                } ?: 50
                call.respond(gastoService.obtenerActividad(call.grupoId(), limite))
            }
        }
    }
}

private fun ApplicationCall.grupoId(): Long =
    parameters["id"]?.toLongOrNull()?.takeIf { it > 0 }
        ?: throw SolicitudInvalidaException("El id del grupo debe ser un número entero positivo")
