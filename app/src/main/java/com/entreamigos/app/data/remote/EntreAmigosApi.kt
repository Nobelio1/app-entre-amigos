package com.entreamigos.app.data.remote

import com.entreamigos.app.data.remote.dto.ActividadDto
import com.entreamigos.app.data.remote.dto.CrearGastoRequest
import com.entreamigos.app.data.remote.dto.CrearGrupoRequest
import com.entreamigos.app.data.remote.dto.CrearMiembroRequest
import com.entreamigos.app.data.remote.dto.CrearPagoRequest
import com.entreamigos.app.data.remote.dto.EstadisticasDto
import com.entreamigos.app.data.remote.dto.GastoDto
import com.entreamigos.app.data.remote.dto.GrupoDto
import com.entreamigos.app.data.remote.dto.InvitacionDto
import com.entreamigos.app.data.remote.dto.LoginGoogleRequest
import com.entreamigos.app.data.remote.dto.LoginRequest
import com.entreamigos.app.data.remote.dto.MiembroDto
import com.entreamigos.app.data.remote.dto.PagoDto
import com.entreamigos.app.data.remote.dto.RegistroRequest
import com.entreamigos.app.data.remote.dto.SaldosDto
import com.entreamigos.app.data.remote.dto.SesionDto
import com.entreamigos.app.data.remote.dto.UnirseGrupoRequest
import com.entreamigos.app.data.remote.dto.UsuarioDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato del backend (ver backend/API.md). El token JWT lo agrega el interceptor de
 * [NetworkModule]; los POST que crean recursos llevan `Idempotency-Key` para que un
 * reintento tras perder la respuesta no duplique datos.
 */
interface EntreAmigosApi {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): SesionDto

    /** Canjea el ID token de Firebase (login con Google) por el JWT propio de la API. */
    @POST("auth/google")
    suspend fun loginConGoogle(@Body request: LoginGoogleRequest): SesionDto

    @POST("auth/registro")
    suspend fun registrar(@Body request: RegistroRequest): SesionDto

    @GET("auth/yo")
    suspend fun obtenerUsuarioActual(): UsuarioDto

    @GET("grupos")
    suspend fun listarGrupos(): List<GrupoDto>

    @POST("grupos")
    suspend fun crearGrupo(
        @Body request: CrearGrupoRequest,
        @Header(IDEMPOTENCY_KEY) claveIdempotencia: String? = null
    ): GrupoDto

    @GET("grupos/{id}")
    suspend fun obtenerGrupo(@Path("id") grupoId: Long): GrupoDto

    @GET("grupos/invitacion/{codigo}")
    suspend fun verInvitacion(@Path("codigo") codigo: String): InvitacionDto

    @POST("grupos/unirse")
    suspend fun unirseAGrupo(@Body request: UnirseGrupoRequest): GrupoDto

    @POST("grupos/{id}/miembros")
    suspend fun agregarMiembro(
        @Path("id") grupoId: Long,
        @Body request: CrearMiembroRequest,
        @Header(IDEMPOTENCY_KEY) claveIdempotencia: String? = null
    ): MiembroDto

    @GET("grupos/{id}/gastos")
    suspend fun listarGastos(@Path("id") grupoId: Long): List<GastoDto>

    @POST("grupos/{id}/gastos")
    suspend fun registrarGasto(
        @Path("id") grupoId: Long,
        @Body request: CrearGastoRequest,
        @Header(IDEMPOTENCY_KEY) claveIdempotencia: String? = null
    ): GastoDto

    @PUT("grupos/{id}/gastos/{gastoId}")
    suspend fun actualizarGasto(
        @Path("id") grupoId: Long,
        @Path("gastoId") gastoId: Long,
        @Body request: CrearGastoRequest
    ): GastoDto

    @DELETE("grupos/{id}/gastos/{gastoId}")
    suspend fun eliminarGasto(@Path("id") grupoId: Long, @Path("gastoId") gastoId: Long)

    @GET("grupos/{id}/pagos")
    suspend fun listarPagos(@Path("id") grupoId: Long): List<PagoDto>

    @POST("grupos/{id}/pagos")
    suspend fun registrarPago(
        @Path("id") grupoId: Long,
        @Body request: CrearPagoRequest,
        @Header(IDEMPOTENCY_KEY) claveIdempotencia: String? = null
    ): PagoDto

    @GET("grupos/{id}/saldos")
    suspend fun obtenerSaldos(@Path("id") grupoId: Long): SaldosDto

    @GET("grupos/{id}/estadisticas")
    suspend fun obtenerEstadisticas(@Path("id") grupoId: Long): EstadisticasDto

    @GET("grupos/{id}/actividad")
    suspend fun obtenerActividad(@Path("id") grupoId: Long, @Query("limite") limite: Int = 50): List<ActividadDto>

    companion object {
        const val IDEMPOTENCY_KEY = "Idempotency-Key"
    }
}
