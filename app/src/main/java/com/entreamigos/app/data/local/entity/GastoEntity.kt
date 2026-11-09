package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class CategoriaGasto {
    COMIDA, TRANSPORTE, HOSPEDAJE, ENTRETENIMIENTO, SERVICIOS, OTROS
}

@Entity(
    tableName = "gastos",
    foreignKeys = [
        ForeignKey(
            entity = GrupoEntity::class,
            parentColumns = ["id"],
            childColumns = ["grupoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MiembroEntity::class,
            parentColumns = ["id"],
            childColumns = ["pagadoPorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("grupoId"), Index("pagadoPorId"), Index(value = ["remoteId"], unique = true)]
)
data class GastoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val grupoId: Long,
    val descripcion: String,
    val monto: Double,
    val pagadoPorId: Long,
    val categoria: CategoriaGasto = CategoriaGasto.OTROS,
    val fecha: Long = System.currentTimeMillis(),
    /** null = aún no existe en el servidor (se crea con POST); con valor y sin sincronizar = edición pendiente (PUT). */
    val remoteId: Long? = null,
    val sincronizado: Boolean = false,
    /** Se incrementa en cada edición local; detecta ediciones hechas mientras la subida estaba en vuelo. */
    val version: Int = 0,
    val idempotencyKey: String = UUID.randomUUID().toString()
)
