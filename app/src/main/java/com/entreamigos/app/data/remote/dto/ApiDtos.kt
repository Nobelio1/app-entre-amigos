package com.entreamigos.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorDto(val error: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegistroRequest(val nombre: String, val email: String, val password: String)

@Serializable
data class UsuarioDto(
    val id: Long,
    val nombre: String,
    val email: String,
    val colorAvatarHex: String = "#7E57C2",
    val fotoUrl: String? = null,
    val conGoogle: Boolean = false
)

@Serializable
data class LoginGoogleRequest(val idToken: String)

@Serializable
data class SesionDto(val token: String, val expiraEn: Long, val usuario: UsuarioDto)

@Serializable
data class UnirseGrupoRequest(val codigo: String, val miembroId: Long? = null)

@Serializable
data class MiembroInvitacionDto(val id: Long, val nombre: String, val colorAvatarHex: String, val vinculado: Boolean)

@Serializable
data class InvitacionDto(
    val grupoId: Long,
    val nombre: String,
    val icono: String = "GENERAL",
    val colorHex: String = "#1B5E4F",
    val codigo: String,
    val miembros: List<MiembroInvitacionDto> = emptyList()
)

@Serializable
data class CrearMiembroRequest(
    val nombre: String,
    val esUsuarioActual: Boolean = false,
    val colorAvatarHex: String? = null
)

@Serializable
data class CrearGrupoRequest(
    val nombre: String,
    val icono: String,
    val colorHex: String,
    val miembros: List<CrearMiembroRequest>,
    val fechaCreacion: Long? = null
)

@Serializable
data class CrearGastoRequest(
    val descripcion: String,
    val monto: Double,
    val pagadoPorId: Long,
    val categoria: String,
    val participantes: List<Long>,
    val fecha: Long? = null
)

@Serializable
data class CrearPagoRequest(
    val deudorId: Long,
    val acreedorId: Long,
    val monto: Double,
    val fecha: Long? = null
)

@Serializable
data class MiembroDto(
    val id: Long,
    val grupoId: Long,
    val nombre: String,
    val esUsuarioActual: Boolean = false,
    val colorAvatarHex: String = "#7E57C2",
    val usuarioId: Long? = null
)

@Serializable
data class GrupoDto(
    val id: Long,
    val nombre: String,
    val icono: String = "GENERAL",
    val colorHex: String = "#1B5E4F",
    val fechaCreacion: Long,
    val codigoInvitacion: String? = null,
    val miembros: List<MiembroDto> = emptyList()
)

@Serializable
data class ParticipanteDto(val miembroId: Long, val montoAsignado: Double)

@Serializable
data class GastoDto(
    val id: Long,
    val grupoId: Long,
    val descripcion: String,
    val monto: Double,
    val pagadoPorId: Long,
    val categoria: String = "OTROS",
    val fecha: Long,
    val participantes: List<ParticipanteDto> = emptyList()
)

@Serializable
data class PagoDto(
    val id: Long,
    val grupoId: Long,
    val deudorId: Long,
    val acreedorId: Long,
    val monto: Double,
    val fecha: Long
)

@Serializable
data class DeudaDto(
    val deudorId: Long,
    val deudorNombre: String,
    val acreedorId: Long,
    val acreedorNombre: String,
    val monto: Double
)

@Serializable
data class BalanceMiembroDto(val miembroId: Long, val nombre: String, val balance: Double)

@Serializable
data class SaldosDto(val balances: List<BalanceMiembroDto>, val deudas: List<DeudaDto>)

@Serializable
data class EstadisticaCategoriaDto(val categoria: String, val monto: Double, val porcentaje: Double)

@Serializable
data class EstadisticasDto(val total: Double, val categorias: List<EstadisticaCategoriaDto>)

@Serializable
data class ActividadDto(
    val tipo: String,
    val id: Long,
    val fecha: Long,
    val monto: Double,
    val descripcion: String,
    val miembroId: Long,
    val miembroNombre: String,
    val acreedorId: Long? = null,
    val acreedorNombre: String? = null,
    val categoria: String? = null,
    val numeroParticipantes: Int? = null
)
