package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
    val sincronizado: Boolean = false,
    /** Código para que otros se unan al grupo; lo asigna el servidor al sincronizar. */
    val codigoInvitacion: String? = null,
    /** Se envía como Idempotency-Key: reintentar la subida nunca duplica el grupo. */
    val idempotencyKey: String = UUID.randomUUID().toString()
)
