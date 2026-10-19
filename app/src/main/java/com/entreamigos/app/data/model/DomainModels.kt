package com.entreamigos.app.data.model

import com.entreamigos.app.data.local.entity.CategoriaGasto
import com.entreamigos.app.data.local.entity.GrupoIcono

data class Miembro(
    val id: Long,
    val nombre: String,
    val esUsuarioActual: Boolean,
    val colorAvatarHex: String
)

data class Gasto(
    val id: Long,
    val descripcion: String,
    val monto: Double,
    val pagadoPor: Miembro,
    val categoria: CategoriaGasto,
    val fecha: Long,
    val participantes: List<Miembro>
)

data class Deuda(
    val deudor: Miembro,
    val acreedor: Miembro,
    val monto: Double
)

data class ResumenGrupo(
    val grupoId: Long,
    val nombre: String,
    val icono: GrupoIcono,
    val colorHex: String,
    val numeroMiembros: Int,
    val balanceUsuarioActual: Double
)

data class ResumenGeneral(
    val totalGastado: Double,
    val totalTeDeben: Double,
    val totalDebes: Double
)

data class EstadisticaCategoria(
    val categoria: CategoriaGasto,
    val monto: Double,
    val porcentaje: Float
)
