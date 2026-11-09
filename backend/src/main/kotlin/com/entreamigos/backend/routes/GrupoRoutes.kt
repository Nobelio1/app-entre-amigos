package com.entreamigos.backend.routes

import com.entreamigos.backend.model.CrearGastoRequest
import com.entreamigos.backend.model.CrearGrupoRequest
import com.entreamigos.backend.model.CrearMiembroRequest
import com.entreamigos.backend.model.CrearPagoRequest
import com.entreamigos.backend.model.UnirseGrupoRequest
import com.entreamigos.backend.plugins.usuarioId
import com.entreamigos.backend.service.Creado
import com.entreamigos.backend.service.GastoService
import com.entreamigos.backend.service.GrupoService
import com.entreamigos.backend.service.SolicitudInvalidaException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/** Rutas de grupos; deben registrarse dentro de un bloque `authenticate`. */
fun Route.rutasGrupos(grupoService: GrupoService, gastoService: GastoService) {
    route("/grupos") {
        get { call.respond(grupoService.listar(call.usuarioId())) }

        post {
            val request = call.receive<CrearGrupoRequest>()
            call.responderCreado(grupoService.crear(request, call.usuarioId(), call.claveIdempotencia()))
        }

        get("/invitacion/{codigo}") {
            val codigo = call.parameters["codigo"].orEmpty()
            call.respond(grupoService.verInvitacion(codigo))
        }

        post("/unirse") {
            val request = call.receive<UnirseGrupoRequest>()
            call.responderCreado(grupoService.unirse(request, call.usuarioId()))
        }

        route("/{id}") {
            get { call.respond(grupoService.obtener(call.grupoId(), call.usuarioId())) }

            post("/miembros") {
                val request = call.receive<CrearMiembroRequest>()
                call.responderCreado(
                    grupoService.agregarMiembro(call.grupoId(), request, call.usuarioId(), call.claveIdempotencia())
                )
            }

            route("/gastos") {
                get { call.respond(gastoService.listarGastos(call.grupoId(), call.usuarioId())) }

                post {
                    val request = call.receive<CrearGastoRequest>()
                    call.responderCreado(
                        gastoService.registrarGasto(call.grupoId(), request, call.usuarioId(), call.claveIdempotencia())
                    )
                }

                put("/{gastoId}") {
                    val request = call.receive<CrearGastoRequest>()
                    call.respond(gastoService.actualizarGasto(call.grupoId(), call.gastoId(), request, call.usuarioId()))
                }

                delete("/{gastoId}") {
                    gastoService.eliminarGasto(call.grupoId(), call.gastoId(), call.usuarioId())
                    call.respond(HttpStatusCode.NoContent)
                }
            }

            get("/pagos") { call.respond(gastoService.listarPagos(call.grupoId(), call.usuarioId())) }

            post("/pagos") {
                val request = call.receive<CrearPagoRequest>()
                call.responderCreado(
                    gastoService.registrarPago(call.grupoId(), request, call.usuarioId(), call.claveIdempotencia())
                )
            }

            get("/saldos") { call.respond(gastoService.calcularSaldos(call.grupoId(), call.usuarioId())) }

            get("/estadisticas") { call.respond(gastoService.calcularEstadisticas(call.grupoId(), call.usuarioId())) }

            get("/actividad") {
                val limite = call.request.queryParameters["limite"]?.let {
                    it.toIntOrNull()?.takeIf { n -> n in 1..500 }
                        ?: throw SolicitudInvalidaException("limite debe ser un entero entre 1 y 500")
                } ?: 50
                call.respond(gastoService.obtenerActividad(call.grupoId(), call.usuarioId(), limite))
            }
        }
    }
}

/** 201 si se creó el recurso; 200 + `Idempotent-Replayed: true` si es un reintento. */
private suspend inline fun <reified T : Any> ApplicationCall.responderCreado(resultado: Creado<T>) {
    if (resultado.nuevo) {
        respond(HttpStatusCode.Created, resultado.valor)
    } else {
        response.header("Idempotent-Replayed", "true")
        respond(HttpStatusCode.OK, resultado.valor)
    }
}

private fun ApplicationCall.claveIdempotencia(): String? {
    val clave = request.headers["Idempotency-Key"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (clave.length > 64) throw SolicitudInvalidaException("Idempotency-Key no puede superar 64 caracteres")
    return clave
}

private fun ApplicationCall.grupoId(): Long =
    parameters["id"]?.toLongOrNull()?.takeIf { it > 0 }
        ?: throw SolicitudInvalidaException("El id del grupo debe ser un número entero positivo")

private fun ApplicationCall.gastoId(): Long =
    parameters["gastoId"]?.toLongOrNull()?.takeIf { it > 0 }
        ?: throw SolicitudInvalidaException("El id del gasto debe ser un número entero positivo")
