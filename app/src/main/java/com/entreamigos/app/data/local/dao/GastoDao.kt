package com.entreamigos.app.data.local.dao

import androidx.room.*
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

    /**
     * Gastos registrados por otros miembros después de [desde]: alimenta el badge de la
     * campana. Los gastos que registró el propio usuario no cuentan como "nuevos".
     */
    @Query(
        "SELECT COUNT(*) FROM gastos g INNER JOIN miembros m ON m.id = g.pagadoPorId " +
            "WHERE g.fecha > :desde AND m.esUsuarioActual = 0"
    )
    fun observarGastosDeOtrosDesde(desde: Long): Flow<Int>

    // ---- Sincronización con el backend ----

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
}
