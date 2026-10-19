package com.entreamigos.app.data.repository

import androidx.room.withTransaction
import com.entreamigos.app.data.local.AppDatabase
import com.entreamigos.app.data.local.PreferenciasUsuario
import com.entreamigos.app.data.local.dao.GastoDao
import com.entreamigos.app.data.local.dao.GrupoDao
import com.entreamigos.app.data.local.dao.MiembroDao
import com.entreamigos.app.data.local.dao.PagoDao
import com.entreamigos.app.data.local.entity.CategoriaGasto
import com.entreamigos.app.data.local.entity.GastoEntity
import com.entreamigos.app.data.local.entity.GastoParticipanteEntity
import com.entreamigos.app.data.local.entity.MiembroEntity
import com.entreamigos.app.data.local.entity.PagoEntity
import com.entreamigos.app.data.model.ActividadItem
import com.entreamigos.app.data.model.Deuda
import com.entreamigos.app.data.model.EstadisticaCategoria
import com.entreamigos.app.data.model.Gasto
import com.entreamigos.app.data.model.Miembro
import com.entreamigos.app.util.BalanceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/** Igual que GrupoRepository: lectura desde Room, escritura local + write-through. */
class GastoRepository(
    private val database: AppDatabase,
    private val gastoDao: GastoDao,
    private val miembroDao: MiembroDao,
    private val pagoDao: PagoDao,
    private val grupoDao: GrupoDao,
    private val sincronizacion: SincronizacionRepository,
    private val preferencias: PreferenciasUsuario
) {

    suspend fun registrarGasto(
        grupoId: Long,
        descripcion: String,
        monto: Double,
        pagadoPorId: Long,
        categoria: CategoriaGasto,
        miembrosParticipantesIds: List<Long>
    ): Long {
        require(descripcion.isNotBlank()) { "La descripción no puede estar vacía" }
        require(monto > 0.0) { "El monto debe ser mayor a cero" }
        require(miembrosParticipantesIds.isNotEmpty()) { "Selecciona al menos un participante" }

        val gastoId = database.withTransaction {
            val id = gastoDao.insertarGasto(
                GastoEntity(
                    grupoId = grupoId,
                    descripcion = descripcion.trim(),
                    monto = monto,
                    pagadoPorId = pagadoPorId,
                    categoria = categoria
                )
            )
            val participantes = BalanceCalculator.dividirEnPartesIguales(monto, miembrosParticipantesIds)
                .map { (miembroId, montoAsignado) ->
                    GastoParticipanteEntity(gastoId = id, miembroId = miembroId, montoAsignado = montoAsignado)
                }
            gastoDao.insertarParticipantes(participantes)
            id
        }
        sincronizacion.subirPendientesEnSegundoPlano()
        return gastoId
    }

    fun observarGastosDeGrupo(grupoId: Long): Flow<List<Gasto>> =
        combine(
            gastoDao.observarGastos(grupoId),
            gastoDao.observarParticipantesDeGrupo(grupoId),
            miembroDao.observarMiembros(grupoId)
        ) { gastos, participaciones, miembros ->
            val miembroPorId = miembros.associateBy { it.id }
            gastos.map { gasto ->
                val idsParticipantes = participaciones.filter { it.gastoId == gasto.id }.map { it.miembroId }
                Gasto(
                    id = gasto.id,
                    descripcion = gasto.descripcion,
                    monto = gasto.monto,
                    pagadoPor = miembroPorId[gasto.pagadoPorId]?.toModel()
                        ?: Miembro(0, "—", false, "#9E9E9E"),
                    categoria = gasto.categoria,
                    fecha = gasto.fecha,
                    participantes = idsParticipantes.mapNotNull { miembroPorId[it]?.toModel() }
                )
            }
        }

    fun observarDeudasDeGrupo(grupoId: Long): Flow<List<Deuda>> =
        combine(
            miembroDao.observarMiembros(grupoId),
            gastoDao.observarGastos(grupoId),
            gastoDao.observarParticipantesDeGrupo(grupoId),
            pagoDao.observarPagos(grupoId)
        ) { miembros, gastos, participaciones, pagos ->
            val balances = BalanceCalculator.calcularBalancesNetos(miembros, gastos, participaciones, pagos)
            BalanceCalculator.simplificarDeudas(miembros, balances)
        }

    fun observarEstadisticasDeGrupo(grupoId: Long): Flow<List<EstadisticaCategoria>> =
        combine(gastoDao.observarGastos(grupoId), miembroDao.observarMiembros(grupoId)) { gastos, _ ->
            val total = gastos.sumOf { it.monto }
            CategoriaGasto.values()
                .map { categoria ->
                    val montoCategoria = gastos.filter { it.categoria == categoria }.sumOf { it.monto }
                    EstadisticaCategoria(
                        categoria = categoria,
                        monto = BalanceCalculator.redondear(montoCategoria),
                        porcentaje = if (total > 0) (montoCategoria / total * 100).toFloat() else 0f
                    )
                }
                .filter { it.monto > 0.0 }
                .sortedByDescending { it.monto }
        }

    suspend fun liquidarDeuda(grupoId: Long, deudorId: Long, acreedorId: Long, monto: Double) {
        pagoDao.insertar(
            PagoEntity(grupoId = grupoId, deudorId = deudorId, acreedorId = acreedorId, monto = monto)
        )
        sincronizacion.subirPendientesEnSegundoPlano()
    }

    fun observarHistorialPagos(grupoId: Long): Flow<List<PagoEntity>> = pagoDao.observarPagos(grupoId)

    fun observarActividadGlobal(): Flow<List<ActividadItem>> =
        combine(
            gastoDao.observarTodosLosGastos(),
            gastoDao.observarTodosLosParticipantes(),
            miembroDao.observarTodosLosMiembros(),
            grupoDao.observarGrupos()
        ) { gastos, participaciones, miembros, grupos ->
            val miembroPorId = miembros.associateBy { it.id }
            val grupoPorId = grupos.associateBy { it.id }
            val participantesPorGasto = participaciones.groupBy({ it.gastoId }, { it.miembroId })
            gastos.mapNotNull { gasto ->
                val grupo = grupoPorId[gasto.grupoId] ?: return@mapNotNull null
                val pagador = miembroPorId[gasto.pagadoPorId]?.toModel()
                    ?: Miembro(0, "—", false, "#9E9E9E")
                ActividadItem(
                    gasto = Gasto(
                        id = gasto.id,
                        descripcion = gasto.descripcion,
                        monto = gasto.monto,
                        pagadoPor = pagador,
                        categoria = gasto.categoria,
                        fecha = gasto.fecha,
                        participantes = participantesPorGasto[gasto.id].orEmpty()
                            .mapNotNull { miembroPorId[it]?.toModel() }
                    ),
                    nombreGrupo = grupo.nombre,
                    iconoGrupo = grupo.icono
                )
            }
        }

    /** Badge de la campana: gastos de otros miembros desde la última vez que se miró. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observarGastosNoVistos(): Flow<Int> =
        preferencias.ultimaVistaNotificaciones.flatMapLatest { gastoDao.observarGastosDeOtrosDesde(it) }

    fun marcarNotificacionesVistas() = preferencias.marcarNotificacionesVistas()

    private fun MiembroEntity.toModel() = Miembro(
        id = id,
        nombre = nombre,
        esUsuarioActual = esUsuarioActual,
        colorAvatarHex = colorAvatarHex
    )
}
