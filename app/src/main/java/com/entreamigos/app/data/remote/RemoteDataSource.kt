package com.entreamigos.app.data.remote

import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.TipoError
import com.entreamigos.app.data.remote.dto.ErrorDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

class RemoteDataSource(private val api: EntreAmigosApi) {

    suspend fun <T> llamar(bloque: suspend EntreAmigosApi.() -> T): Resource<T> =
        try {
            Resource.Success(api.bloque())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val mensaje = extraerMensaje(e) ?: "El servidor respondió con el código ${e.code()}"
            val tipo = if (e.code() in 400..499) TipoError.SOLICITUD_INVALIDA else TipoError.SERVIDOR
            Resource.Error(mensaje, tipo)
        } catch (e: IOException) {
            Resource.Error("No se pudo conectar con el servidor", TipoError.SIN_CONEXION)
        } catch (e: SerializationException) {
            Resource.Error("Respuesta inesperada del servidor", TipoError.SERVIDOR)
        }

    private fun extraerMensaje(e: HttpException): String? =
        runCatching {
            val cuerpo = e.response()?.errorBody()?.string().orEmpty()
            NetworkModule.json.decodeFromString(ErrorDto.serializer(), cuerpo).error
        }.getOrNull()?.takeIf { it.isNotBlank() }
}
