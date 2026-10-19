package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pagos",
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
            childColumns = ["deudorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MiembroEntity::class,
            parentColumns = ["id"],
            childColumns = ["acreedorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("grupoId"), Index("deudorId"), Index("acreedorId"), Index(value = ["remoteId"], unique = true)]
)
data class PagoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val grupoId: Long,
    val deudorId: Long,
    val acreedorId: Long,
    val monto: Double,
    val fecha: Long = System.currentTimeMillis(),
    val remoteId: Long? = null,
    val sincronizado: Boolean = false
)
