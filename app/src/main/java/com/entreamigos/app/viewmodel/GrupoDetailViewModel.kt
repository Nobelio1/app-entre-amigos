package com.entreamigos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.entreamigos.app.data.local.entity.GrupoEntity
import com.entreamigos.app.data.local.entity.MiembroEntity
import com.entreamigos.app.data.model.Deuda
import com.entreamigos.app.data.model.EstadoConexion
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.EstadisticaCategoria
import com.entreamigos.app.data.model.Gasto
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.data.repository.SincronizacionRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GrupoDetailUiState(
    val grupo: GrupoEntity? = null,
    val miembros: List<MiembroEntity> = emptyList(),
    val gastos: List<Gasto> = emptyList(),
    val deudas: List<Deuda> = emptyList(),
    val estadisticas: List<EstadisticaCategoria> = emptyList()
) {
    val balanceUsuarioActual: Double
        get() {
            val usuario = miembros.firstOrNull { it.esUsuarioActual } ?: return 0.0
            val leDeben = deudas.filter { it.acreedor.id == usuario.id }.sumOf { it.monto }
            val debe = deudas.filter { it.deudor.id == usuario.id }.sumOf { it.monto }
            return leDeben - debe
        }

    val deudasQueLeDebenAlUsuario: List<Deuda>
        get() {
            val usuarioId = miembros.firstOrNull { it.esUsuarioActual }?.id ?: return emptyList()
            return deudas.filter { it.acreedor.id == usuarioId }
        }

    val deudasQueElUsuarioDebe: List<Deuda>
        get() {
            val usuarioId = miembros.firstOrNull { it.esUsuarioActual }?.id ?: return emptyList()
            return deudas.filter { it.deudor.id == usuarioId }
        }
}

class GrupoDetailViewModel(
    private val grupoId: Long,
    private val grupoRepository: GrupoRepository,
    private val gastoRepository: GastoRepository,
    private val sincronizacion: SincronizacionRepository
) : ViewModel() {

    val uiState: StateFlow<Resource<GrupoDetailUiState>> = combine(
        grupoRepository.observarGrupo(grupoId),
        grupoRepository.observarMiembros(grupoId),
        gastoRepository.observarGastosDeGrupo(grupoId),
        gastoRepository.observarDeudasDeGrupo(grupoId),
        gastoRepository.observarEstadisticasDeGrupo(grupoId)
    ) { grupo, miembros, gastos, deudas, estadisticas ->
        if (grupo == null) {
            Resource.Error("Este grupo ya no existe")
        } else {
            Resource.Success(
                GrupoDetailUiState(
                    grupo = grupo,
                    miembros = miembros,
                    gastos = gastos,
                    deudas = deudas,
                    estadisticas = estadisticas
                )
            )
        }
    }
        .catch { emit(Resource.Error("No se pudo cargar el grupo")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    val conexion: StateFlow<EstadoConexion> = sincronizacion.estadoConexion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoConexion())

    fun refrescar() = sincronizacion.sincronizarEnSegundoPlano()

    fun liquidarDeuda(deuda: Deuda) {
        viewModelScope.launch {
            gastoRepository.liquidarDeuda(
                grupoId = grupoId,
                deudorId = deuda.deudor.id,
                acreedorId = deuda.acreedor.id,
                monto = deuda.monto
            )
        }
    }

    class Factory(
        private val grupoId: Long,
        private val grupoRepository: GrupoRepository,
        private val gastoRepository: GastoRepository,
        private val sincronizacion: SincronizacionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GrupoDetailViewModel(grupoId, grupoRepository, gastoRepository, sincronizacion) as T
        }
    }
}
