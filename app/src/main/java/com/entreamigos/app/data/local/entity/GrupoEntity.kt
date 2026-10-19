package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grupos",
    indices = [Index(value = ["remoteId"], unique = true)]
)
data class GrupoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val nombre: String,
    val icono: GrupoIcono = GrupoIcono.GENERAL,
    val colorHex: String = "#1B5E4F",
    val fechaCreacion: Long = System.currentTimeMillis(),
    val remoteId: Long? = null,
    val sincronizado: Boolean = false
)
