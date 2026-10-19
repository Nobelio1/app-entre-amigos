package com.entreamigos.app.data.local.dao

import androidx.room.*
import com.entreamigos.app.data.local.entity.MiembroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MiembroDao {

    @Query("SELECT * FROM miembros WHERE grupoId = :grupoId ORDER BY id ASC")
    fun observarMiembros(grupoId: Long): Flow<List<MiembroEntity>>

    @Query("SELECT * FROM miembros WHERE grupoId = :grupoId ORDER BY id ASC")
    suspend fun obtenerMiembros(grupoId: Long): List<MiembroEntity>

    @Query("SELECT * FROM miembros")
    fun observarTodosLosMiembros(): Flow<List<MiembroEntity>>

    @Insert
    suspend fun insertar(miembro: MiembroEntity): Long

    @Insert
    suspend fun insertarTodos(miembros: List<MiembroEntity>): List<Long>

    @Delete
    suspend fun eliminar(miembro: MiembroEntity)

    // ---- Sincronización con el backend ----

    @Update
    suspend fun actualizar(miembro: MiembroEntity)

    @Query("SELECT * FROM miembros WHERE remoteId = :remoteId")
    suspend fun buscarPorRemoteId(remoteId: Long): MiembroEntity?

    @Query("SELECT * FROM miembros WHERE sincronizado = 0 ORDER BY id ASC")
    suspend fun obtenerPendientes(): List<MiembroEntity>

    @Query("UPDATE miembros SET remoteId = :remoteId, sincronizado = 1 WHERE id = :miembroId")
    suspend fun marcarSincronizado(miembroId: Long, remoteId: Long)
}
