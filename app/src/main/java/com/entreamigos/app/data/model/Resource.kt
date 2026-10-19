package com.entreamigos.app.data.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val mensaje: String, val tipo: TipoError = TipoError.DESCONOCIDO) : Resource<Nothing>
}

enum class TipoError {
    SIN_CONEXION,
    SOLICITUD_INVALIDA,
    SERVIDOR,
    DESCONOCIDO
}

fun <T> Flow<T>.comoResource(mensajeError: String): Flow<Resource<T>> =
    map<T, Resource<T>> { Resource.Success(it) }
        .onStart { emit(Resource.Loading) }
        .catch { emit(Resource.Error(mensajeError)) }
