package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "miembros",
    foreignKeys = [
        ForeignKey(
            entity = GrupoEntity::class,
            parentColumns = ["id"],
            childColumns = ["grupoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("grupoId"), Index(value = ["remoteId"], unique = true)]
)
data class MiembroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val grupoId: Long,
    val nombre: String,
    val esUsuarioActual: Boolean = false,
    val colorAvatarHex: String = "#7E57C2",
    val remoteId: Long? = null,
    val sincronizado: Boolean = false
)
