package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gasto_participantes",
    foreignKeys = [
        ForeignKey(
            entity = GastoEntity::class,
            parentColumns = ["id"],
            childColumns = ["gastoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MiembroEntity::class,
            parentColumns = ["id"],
            childColumns = ["miembroId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("gastoId"), Index("miembroId")]
)
data class GastoParticipanteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val gastoId: Long,
    val miembroId: Long,
    val montoAsignado: Double
)
