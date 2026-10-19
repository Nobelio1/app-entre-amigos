package com.entreamigos.app.data.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Estado de una operación que la UI debe reflejar: cargando, con datos o con error.
 *
 * El error distingue [TipoError.SIN_CONEXION] del resto porque la app es offline-first:
 * no poder llegar al backend no es un fallo para el usuario (sigue viendo y editando
 * sus datos locales), solo cambia el indicador a "modo offline". Un 4xx/5xx, en cambio,
 * sí es un error real que se muestra en un banner con opción de reintentar.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val mensaje: String, val tipo: TipoError = TipoError.DESCONOCIDO) : Resource<Nothing>
}

enum class TipoError {
    /** No hubo respuesta: sin red, servidor apagado, timeout o DNS. */
    SIN_CONEXION,
    /** El backend respondió 4xx: el cliente envió algo inválido. */
    SOLICITUD_INVALIDA,
    /** El backend respondió 5xx o algo que no se pudo interpretar. */
    SERVIDOR,
    DESCONOCIDO
}

/**
 * Envuelve un Flow de Room en Resource: `Loading` hasta la primera emisión, luego
 * `Success`, y `Error` si la consulta falla (p. ej. base corrupta) en vez de crashear.
 */
fun <T> Flow<T>.comoResource(mensajeError: String): Flow<Resource<T>> =
    map<T, Resource<T>> { Resource.Success(it) }
        .onStart { emit(Resource.Loading) }
        .catch { emit(Resource.Error(mensajeError)) }
