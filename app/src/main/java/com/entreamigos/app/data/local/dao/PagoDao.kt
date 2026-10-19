package com.entreamigos.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Query
import com.entreamigos.app.data.local.entity.PagoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoDao {

    @Query("SELECT * FROM pagos WHERE grupoId = :grupoId ORDER BY fecha DESC")
    fun observarPagos(grupoId: Long): Flow<List<PagoEntity>>

    @Query("SELECT * FROM pagos")
    fun observarTodosLosPagos(): Flow<List<PagoEntity>>

    @Insert
    suspend fun insertar(pago: PagoEntity): Long

    @Update
    suspend fun actualizar(pago: PagoEntity)

    @Query("SELECT * FROM pagos WHERE remoteId = :remoteId")
    suspend fun buscarPorRemoteId(remoteId: Long): PagoEntity?

    @Query("SELECT * FROM pagos WHERE sincronizado = 0 ORDER BY id ASC")
    suspend fun obtenerPendientes(): List<PagoEntity>

    @Query("UPDATE pagos SET remoteId = :remoteId, sincronizado = 1 WHERE id = :pagoId")
    suspend fun marcarSincronizado(pagoId: Long, remoteId: Long)

    @Query(
        "SELECT (SELECT COUNT(*) FROM grupos WHERE sincronizado = 0) + " +
            "(SELECT COUNT(*) FROM miembros WHERE sincronizado = 0) + " +
            "(SELECT COUNT(*) FROM gastos WHERE sincronizado = 0) + " +
            "(SELECT COUNT(*) FROM pagos WHERE sincronizado = 0)"
    )
    fun observarCambiosPendientes(): Flow<Int>
}
