package com.entreamigos.backend.service

class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)

class SolicitudInvalidaException(mensaje: String) : RuntimeException(mensaje)

inline fun validar(condicion: Boolean, mensaje: () -> String) {
    if (!condicion) throw SolicitudInvalidaException(mensaje())
}
