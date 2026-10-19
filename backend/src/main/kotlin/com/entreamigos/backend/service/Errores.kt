package com.entreamigos.backend.service

/** Se traduce a HTTP 404 en `plugins/StatusPages.kt`. */
class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)

/**
 * Se traduce a HTTP 400. Las validaciones usan `validar(...)` en lugar de `require`
 * para no confundir un error de datos del cliente con un IllegalArgumentException
 * lanzado por alguna librería (que sí sería un 500).
 */
class SolicitudInvalidaException(mensaje: String) : RuntimeException(mensaje)

inline fun validar(condicion: Boolean, mensaje: () -> String) {
    if (!condicion) throw SolicitudInvalidaException(mensaje())
}
