package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * `remoteId` y `sincronizado` (presentes en todas las entidades) implementan el
 * patrón offline-first: el `id` local lo genera Room y nunca cambia, así las FK
 * locales siguen siendo válidas aunque no haya red. `remoteId` es el id asignado
 * por el backend (null mientras no se haya subido) y `sincronizado` indica si el
 * registro ya existe en el servidor o queda pendiente de reintento.
 */
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
