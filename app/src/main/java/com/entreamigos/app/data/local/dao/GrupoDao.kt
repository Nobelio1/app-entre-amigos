package com.entreamigos.app.data.local.dao

import androidx.room.*
import com.entreamigos.app.data.local.entity.GrupoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrupoDao {

    @Query("SELECT * FROM grupos ORDER BY fechaCreacion DESC")
    fun observarGrupos(): Flow<List<GrupoEntity>>

    @Query("SELECT * FROM grupos WHERE id = :grupoId")
    fun observarGrupo(grupoId: Long): Flow<GrupoEntity?>

    @Insert
    suspend fun insertar(grupo: GrupoEntity): Long

    @Update
    suspend fun actualizar(grupo: GrupoEntity)

    @Delete
    suspend fun eliminar(grupo: GrupoEntity)

    @Query("SELECT * FROM grupos WHERE id = :grupoId")
    suspend fun obtener(grupoId: Long): GrupoEntity?

    @Query("SELECT * FROM grupos WHERE remoteId = :remoteId")
    suspend fun buscarPorRemoteId(remoteId: Long): GrupoEntity?

    @Query("SELECT * FROM grupos WHERE sincronizado = 0 ORDER BY id ASC")
    suspend fun obtenerPendientes(): List<GrupoEntity>

    @Query("SELECT * FROM grupos WHERE remoteId IS NOT NULL")
    suspend fun obtenerSincronizados(): List<GrupoEntity>

    @Query("UPDATE grupos SET remoteId = :remoteId, sincronizado = 1 WHERE id = :grupoId")
    suspend fun marcarSincronizado(grupoId: Long, remoteId: Long)
}
