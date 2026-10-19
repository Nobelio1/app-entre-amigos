package com.entreamigos.app.data.repository

import androidx.room.withTransaction
import com.entreamigos.app.data.local.AppDatabase
import com.entreamigos.app.data.local.dao.GastoDao
import com.entreamigos.app.data.local.dao.GrupoDao
import com.entreamigos.app.data.local.dao.MiembroDao
import com.entreamigos.app.data.local.dao.PagoDao
import com.entreamigos.app.data.local.entity.GastoEntity
import com.entreamigos.app.data.local.entity.GastoParticipanteEntity
import com.entreamigos.app.data.local.entity.GrupoEntity
import com.entreamigos.app.data.local.entity.GrupoIcono
import com.entreamigos.app.data.local.entity.MiembroEntity
import com.entreamigos.app.data.local.entity.PagoEntity
import com.entreamigos.app.data.model.ResumenGeneral
import com.entreamigos.app.data.model.ResumenGrupo
import com.entreamigos.app.util.BalanceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Las lecturas salen siempre de Room. Las escrituras se guardan primero en local y
 * luego se suben al backend en segundo plano (write-through vía [sincronizacion]).
 */
class GrupoRepository(
    private val database: AppDatabase,
    private val grupoDao: GrupoDao,
    private val miembroDao: MiembroDao,
    private val gastoDao: GastoDao,
    private val pagoDao: PagoDao,
    private val sincronizacion: SincronizacionRepository
) {

    fun observarGrupos(): Flow<List<GrupoEntity>> = grupoDao.observarGrupos()

    fun observarGrupo(grupoId: Long): Flow<GrupoEntity?> = grupoDao.observarGrupo(grupoId)

    fun observarMiembros(grupoId: Long): Flow<List<MiembroEntity>> = miembroDao.observarMiembros(grupoId)

    suspend fun crearGrupo(nombre: String, icono: GrupoIcono, colorHex: String, nombresMiembros: List<String>): Long {
        // Transacción: la sincronización nunca debe ver un grupo todavía sin miembros
        val grupoId = database.withTransaction {
            val id = grupoDao.insertar(GrupoEntity(nombre = nombre, icono = icono, colorHex = colorHex))
            val miembros = nombresMiembros.mapIndexed { index, nombreMiembro ->
                MiembroEntity(
                    grupoId = id,
                    nombre = nombreMiembro,
                    esUsuarioActual = index == 0,
                    colorAvatarHex = PALETA_AVATARES[index % PALETA_AVATARES.size]
                )
            }
            miembroDao.insertarTodos(miembros)
            id
        }
        sincronizacion.subirPendientesEnSegundoPlano()
        return grupoId
    }

    fun observarResumenGrupos(): Flow<List<ResumenGrupo>> =
        combine(
            grupoDao.observarGrupos(),
            miembroDao.observarTodosLosMiembros(),
            gastoDao.observarTodosLosGastos(),
            gastoDao.observarTodosLosParticipantes(),
            pagoDao.observarTodosLosPagos()
        ) { grupos: List<GrupoEntity>,
            todosMiembros: List<MiembroEntity>,
            todosGastos: List<GastoEntity>,
            todasParticipaciones: List<GastoParticipanteEntity>,
            todosPagos: List<PagoEntity> ->

            grupos.map { grupo ->
                val miembrosDelGrupo = todosMiembros.filter { it.grupoId == grupo.id }
                val gastosDelGrupo = todosGastos.filter { it.grupoId == grupo.id }
                val idsGastos = gastosDelGrupo.map { it.id }.toSet()
                val participacionesDelGrupo = todasParticipaciones.filter { it.gastoId in idsGastos }
                val pagosDelGrupo = todosPagos.filter { it.grupoId == grupo.id }

                val balances = BalanceCalculator.calcularBalancesNetos(
                    miembrosDelGrupo, gastosDelGrupo, participacionesDelGrupo, pagosDelGrupo
                )
                val usuarioActual = miembrosDelGrupo.firstOrNull { it.esUsuarioActual }
                val balanceUsuario = usuarioActual?.let { balances[it.id] } ?: 0.0

                ResumenGrupo(
                    grupoId = grupo.id,
                    nombre = grupo.nombre,
                    icono = grupo.icono,
                    colorHex = grupo.colorHex,
                    numeroMiembros = miembrosDelGrupo.size,
                    balanceUsuarioActual = BalanceCalculator.redondear(balanceUsuario)
                )
            }
        }

    fun observarResumenGeneral(): Flow<ResumenGeneral> =
        combine(
            gastoDao.observarTodosLosGastos(),
            observarResumenGrupos()
        ) { gastos, resumenes ->
            val totalGastado = gastos.sumOf { it.monto }
            val totalTeDeben = resumenes.filter { it.balanceUsuarioActual > 0 }
                .sumOf { it.balanceUsuarioActual }
            val totalDebes = resumenes.filter { it.balanceUsuarioActual < 0 }
                .sumOf { -it.balanceUsuarioActual }
            ResumenGeneral(
                totalGastado = BalanceCalculator.redondear(totalGastado),
                totalTeDeben = BalanceCalculator.redondear(totalTeDeben),
                totalDebes = BalanceCalculator.redondear(totalDebes)
            )
        }

    /**
     * Nombre real del usuario para el saludo de Home. "Tú" es el valor por defecto del
     * formulario de crear grupo, así que no sirve como nombre: si no hay otro, null.
     */
    fun observarNombreUsuarioActual(): Flow<String?> =
        miembroDao.observarNombresUsuarioActual().map { nombres ->
            nombres.map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.equals(NOMBRE_POR_DEFECTO, ignoreCase = true) }
        }

    companion object {
        const val NOMBRE_POR_DEFECTO = "Tú"
        val PALETA_AVATARES = listOf("#EF5350", "#42A5F5", "#66BB6A", "#AB47BC", "#FFA726", "#26C6DA")
    }
}
