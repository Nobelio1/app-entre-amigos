package com.entreamigos.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Borrado pendiente de enviar al servidor. El gasto local se elimina en el acto (la UI no
 * tiene que filtrar nada) y esta fila recuerda que falta el `DELETE` remoto.
 */
@Entity(tableName = "gastos_eliminados")
data class GastoEliminadoEntity(
    @PrimaryKey val remoteId: Long,
    val grupoRemoteId: Long
)
