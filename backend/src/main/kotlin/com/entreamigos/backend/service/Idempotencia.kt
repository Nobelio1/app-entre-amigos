package com.entreamigos.backend.service

import com.entreamigos.backend.db.DatabaseFactory.consulta
import com.entreamigos.backend.db.esViolacionDeUnicidad

/**
 * Crea un recurso respetando el encabezado `Idempotency-Key`.
 *
 * - Sin clave: siempre crea.
 * - Con clave: si ya existe un recurso con esa clave lo devuelve (`nuevo = false`)
 *   en lugar de duplicarlo. Si dos reintentos llegan a la vez, el índice único de
 *   la base rechaza al segundo y este termina devolviendo el recurso del primero.
 *
 * [buscar] y [crear] corren cada uno en su propia transacción; [buscar] debe
 * lanzar [ConflictoException] si la clave pertenece a un recurso de otro contexto.
 */
suspend fun <T> crearIdempotente(clave: String?, buscar: (String) -> T?, crear: () -> T): Creado<T> {
    if (clave == null) return Creado(consulta { crear() }, nuevo = true)
    consulta { buscar(clave) }?.let { return Creado(it, nuevo = false) }
    return try {
        Creado(consulta { crear() }, nuevo = true)
    } catch (e: Exception) {
        if (!e.esViolacionDeUnicidad()) throw e
        consulta { buscar(clave) }?.let { Creado(it, nuevo = false) } ?: throw e
    }
}
