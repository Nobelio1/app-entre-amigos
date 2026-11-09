package com.entreamigos.backend.service

class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)

class SolicitudInvalidaException(mensaje: String) : RuntimeException(mensaje)

/** 401: credenciales incorrectas o token ausente/inválido. */
class NoAutenticadoException(mensaje: String) : RuntimeException(mensaje)

/** 403: el usuario está autenticado pero no pertenece al grupo. */
class SinPermisoException(mensaje: String) : RuntimeException(mensaje)

/** 503: la funcionalidad depende de configuración que el servidor no tiene. */
class ServicioNoDisponibleException(mensaje: String) : RuntimeException(mensaje)

/** 409: choca con un recurso existente (email repetido, Idempotency-Key reutilizada…). */
class ConflictoException(mensaje: String) : RuntimeException(mensaje)

inline fun validar(condicion: Boolean, mensaje: () -> String) {
    if (!condicion) throw SolicitudInvalidaException(mensaje())
}

/** Resultado de un POST idempotente: [nuevo] es false cuando se devolvió un recurso ya creado. */
data class Creado<T>(val valor: T, val nuevo: Boolean)
