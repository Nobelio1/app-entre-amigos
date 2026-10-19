package com.entreamigos.backend.model

import kotlinx.serialization.Serializable

/*
 * Contrato JSON de la API. Los nombres de campo coinciden con los de las entidades
 * Room de la app para que el mapeo DTO ↔ entidad en Android sea directo.
 * Los montos viajan como número decimal en soles (ej. 120.5) y las fechas como
 * epoch en milisegundos.
 */

/** Categorías válidas; mismo enum que `CategoriaGasto` en la app. */
enum class CategoriaGasto { COMIDA, TRANSPORTE, HOSPEDAJE, ENTRETENIMIENTO, SERVICIOS, OTROS }

@Serializable
data class ErrorResponse(val error: String)

// ---------- Entrada ----------

@Serializable
data class CrearMiembroRequest(
    val nombre: String,
    val esUsuarioActual: Boolean = false,
    val colorAvatarHex: String? = null
)

@Serializable
data class CrearGrupoRequest(
    val nombre: String,
    val icono: String = "GENERAL",
    val colorHex: String = "#1B5E4F",
    val miembros: List<CrearMiembroRequest> = emptyList(),
    /** Opcional: permite que un grupo creado offline conserve su fecha original. */
    val fechaCreacion: Long? = null
)

@Serializable
data class CrearGastoRequest(
    val descripcion: String,
    val monto: Double,
    val pagadoPorId: Long,
    val categoria: String = CategoriaGasto.OTROS.name,
    /** Ids de los miembros entre los que se divide el gasto en partes iguales. */
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

// ---------- Salida ----------

@Serializable
data class MiembroDto(
    val id: Long,
    val grupoId: Long,
    val nombre: String,
    val esUsuarioActual: Boolean,
    val colorAvatarHex: String
)

@Serializable
data class GrupoDto(
    val id: Long,
    val nombre: String,
    val icono: String,
    val colorHex: String,
    val fechaCreacion: Long,
    val miembros: List<MiembroDto>
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
    val categoria: String,
    val fecha: Long,
    val participantes: List<ParticipanteDto>
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
data class BalanceMiembroDto(val miembroId: Long, val nombre: String, val balance: Double)

@Serializable
data class DeudaDto(
    val deudorId: Long,
    val deudorNombre: String,
    val acreedorId: Long,
    val acreedorNombre: String,
    val monto: Double
)

@Serializable
data class SaldosDto(val balances: List<BalanceMiembroDto>, val deudas: List<DeudaDto>)

@Serializable
data class EstadisticaCategoriaDto(val categoria: String, val monto: Double, val porcentaje: Double)

@Serializable
data class EstadisticasDto(val total: Double, val categorias: List<EstadisticaCategoriaDto>)

/** Entrada del historial combinado; `tipo` es "GASTO" o "PAGO". */
@Serializable
data class ActividadDto(
    val tipo: String,
    val id: Long,
    val fecha: Long,
    val monto: Double,
    val descripcion: String,
    /** Gasto: quien pagó. Pago: el deudor que entregó el dinero. */
    val miembroId: Long,
    val miembroNombre: String,
    /** Solo en pagos: quien recibió el dinero. */
    val acreedorId: Long? = null,
    val acreedorNombre: String? = null,
    /** Solo en gastos. */
    val categoria: String? = null,
    val numeroParticipantes: Int? = null
)
