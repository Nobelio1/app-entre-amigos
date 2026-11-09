package com.entreamigos.app.data.local.dao

import androidx.room.*
import com.entreamigos.app.data.local.entity.GastoEliminadoEntity
import com.entreamigos.app.data.local.entity.GastoEntity
import com.entreamigos.app.data.local.entity.GastoParticipanteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoDao {

    @Query("SELECT * FROM gastos WHERE grupoId = :grupoId ORDER BY fecha DESC")
    fun observarGastos(grupoId: Long): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gastos ORDER BY fecha DESC")
    fun observarTodosLosGastos(): Flow<List<GastoEntity>>

    @Insert
    suspend fun insertarGasto(gasto: GastoEntity): Long

    @Insert
    suspend fun insertarParticipantes(participantes: List<GastoParticipanteEntity>)

    @Query("SELECT * FROM gasto_participantes WHERE gastoId = :gastoId")
    fun observarParticipantes(gastoId: Long): Flow<List<GastoParticipanteEntity>>

    @Query("SELECT * FROM gasto_participantes WHERE gastoId IN (SELECT id FROM gastos WHERE grupoId = :grupoId)")
    fun observarParticipantesDeGrupo(grupoId: Long): Flow<List<GastoParticipanteEntity>>

    @Query("SELECT * FROM gasto_participantes")
    fun observarTodosLosParticipantes(): Flow<List<GastoParticipanteEntity>>

    @Delete
    suspend fun eliminarGasto(gasto: GastoEntity)

    @Query(
        "SELECT COUNT(*) FROM gastos g INNER JOIN miembros m ON m.id = g.pagadoPorId " +
            "WHERE g.fecha > :desde AND m.esUsuarioActual = 0"
    )
    fun observarGastosDeOtrosDesde(desde: Long): Flow<Int>

    @Update
    suspend fun actualizarGasto(gasto: GastoEntity)

    @Query("SELECT * FROM gastos WHERE remoteId = :remoteId")
    suspend fun buscarPorRemoteId(remoteId: Long): GastoEntity?

    @Query("SELECT * FROM gastos WHERE sincronizado = 0 ORDER BY id ASC")
    suspend fun obtenerPendientes(): List<GastoEntity>

    @Query("SELECT * FROM gasto_participantes WHERE gastoId = :gastoId ORDER BY id ASC")
    suspend fun obtenerParticipantes(gastoId: Long): List<GastoParticipanteEntity>

    @Query("DELETE FROM gasto_participantes WHERE gastoId = :gastoId")
    suspend fun eliminarParticipantes(gastoId: Long)

    @Query("UPDATE gastos SET remoteId = :remoteId, sincronizado = 1 WHERE id = :gastoId")
    suspend fun marcarSincronizado(gastoId: Long, remoteId: Long)

    @Query("SELECT * FROM gastos WHERE id = :gastoId")
    suspend fun obtener(gastoId: Long): GastoEntity?

    /** El servidor ya creó el gasto pero hubo otra edición local después: sigue pendiente (PUT). */
    @Query("UPDATE gastos SET remoteId = :remoteId WHERE id = :gastoId")
    suspend fun asignarRemoteId(gastoId: Long, remoteId: Long)

    /** Quita los gastos ya sincronizados que el servidor dejó de devolver (otro dispositivo los borró). */
    @Query("DELETE FROM gastos WHERE grupoId = :grupoId AND sincronizado = 1 AND remoteId IS NOT NULL AND remoteId NOT IN (:remoteIdsVigentes)")
    suspend fun eliminarAusentes(grupoId: Long, remoteIdsVigentes: List<Long>)

    @Query("DELETE FROM gastos WHERE grupoId = :grupoId AND sincronizado = 1 AND remoteId IS NOT NULL")
    suspend fun eliminarSincronizadosDeGrupo(grupoId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarEliminacion(eliminacion: GastoEliminadoEntity)

    @Query("SELECT * FROM gastos_eliminados")
    suspend fun obtenerEliminacionesPendientes(): List<GastoEliminadoEntity>

    @Query("SELECT remoteId FROM gastos_eliminados")
    suspend fun obtenerRemoteIdsEliminados(): List<Long>

    @Query("DELETE FROM gastos_eliminados WHERE remoteId = :remoteId")
    suspend fun confirmarEliminacion(remoteId: Long)
}
