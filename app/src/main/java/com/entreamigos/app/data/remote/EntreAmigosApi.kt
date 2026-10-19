package com.entreamigos.app.data.remote

import com.entreamigos.app.data.remote.dto.ActividadDto
import com.entreamigos.app.data.remote.dto.CrearGastoRequest
import com.entreamigos.app.data.remote.dto.CrearGrupoRequest
import com.entreamigos.app.data.remote.dto.CrearMiembroRequest
import com.entreamigos.app.data.remote.dto.CrearPagoRequest
import com.entreamigos.app.data.remote.dto.EstadisticasDto
import com.entreamigos.app.data.remote.dto.GastoDto
import com.entreamigos.app.data.remote.dto.GrupoDto
import com.entreamigos.app.data.remote.dto.MiembroDto
import com.entreamigos.app.data.remote.dto.PagoDto
import com.entreamigos.app.data.remote.dto.SaldosDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Contrato REST del backend (ver backend/API.md). Los ids son del servidor. */
interface EntreAmigosApi {

    @GET("grupos")
    suspend fun listarGrupos(): List<GrupoDto>

    @POST("grupos")
    suspend fun crearGrupo(@Body request: CrearGrupoRequest): GrupoDto

    @GET("grupos/{id}")
    suspend fun obtenerGrupo(@Path("id") grupoId: Long): GrupoDto

    @POST("grupos/{id}/miembros")
    suspend fun agregarMiembro(@Path("id") grupoId: Long, @Body request: CrearMiembroRequest): MiembroDto

    @GET("grupos/{id}/gastos")
    suspend fun listarGastos(@Path("id") grupoId: Long): List<GastoDto>

    @POST("grupos/{id}/gastos")
    suspend fun registrarGasto(@Path("id") grupoId: Long, @Body request: CrearGastoRequest): GastoDto

    @GET("grupos/{id}/pagos")
    suspend fun listarPagos(@Path("id") grupoId: Long): List<PagoDto>

    @POST("grupos/{id}/pagos")
    suspend fun registrarPago(@Path("id") grupoId: Long, @Body request: CrearPagoRequest): PagoDto

    @GET("grupos/{id}/saldos")
    suspend fun obtenerSaldos(@Path("id") grupoId: Long): SaldosDto

    @GET("grupos/{id}/estadisticas")
    suspend fun obtenerEstadisticas(@Path("id") grupoId: Long): EstadisticasDto

    @GET("grupos/{id}/actividad")
    suspend fun obtenerActividad(@Path("id") grupoId: Long, @Query("limite") limite: Int = 50): List<ActividadDto>
}
