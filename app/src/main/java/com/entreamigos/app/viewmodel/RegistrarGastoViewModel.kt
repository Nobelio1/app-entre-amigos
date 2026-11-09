package com.entreamigos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.entreamigos.app.data.local.entity.CategoriaGasto
import com.entreamigos.app.data.local.entity.MiembroEntity
import com.entreamigos.app.data.repository.GastoRepository
import com.entreamigos.app.data.repository.GrupoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

data class RegistrarGastoUiState(
    val descripcion: String = "",
    val monto: String = "",
    val categoria: CategoriaGasto = CategoriaGasto.OTROS,
    val pagadoPorId: Long? = null,
    val participantesSeleccionados: Set<Long> = emptySet(),
    val errorDescripcion: String? = null,
    val errorMonto: String? = null,
    val errorParticipantes: String? = null,
    val guardando: Boolean = false,
    val gastoRegistrado: Boolean = false,
    val modoEdicion: Boolean = false,
    val gastoNoEncontrado: Boolean = false,
    val gastoEliminado: Boolean = false
)

class RegistrarGastoViewModel(
    private val grupoId: Long,
    /** null = registrar un gasto nuevo; con valor = editar ese gasto. */
    private val gastoId: Long?,
    grupoRepository: GrupoRepository,
    private val gastoRepository: GastoRepository
) : ViewModel() {

    val miembros: StateFlow<List<MiembroEntity>> = grupoRepository.observarMiembros(grupoId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(RegistrarGastoUiState(modoEdicion = gastoId != null))
    val uiState: StateFlow<RegistrarGastoUiState> = _uiState.asStateFlow()

    init {
        if (gastoId != null) cargarGasto(gastoId)
    }

    private fun cargarGasto(id: Long) {
        viewModelScope.launch {
            val (gasto, participantes) = gastoRepository.obtenerGastoParaEditar(id) ?: run {
                _uiState.value = _uiState.value.copy(gastoNoEncontrado = true)
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                descripcion = gasto.descripcion,
                monto = BigDecimal.valueOf(gasto.monto).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                categoria = gasto.categoria,
                pagadoPorId = gasto.pagadoPorId,
                participantesSeleccionados = participantes.toSet()
            )
        }
    }

    fun eliminarGasto() {
        val id = gastoId ?: return
        if (_uiState.value.guardando) return
        _uiState.value = _uiState.value.copy(guardando = true)
        viewModelScope.launch {
            gastoRepository.eliminarGasto(id)
            _uiState.value = _uiState.value.copy(guardando = false, gastoEliminado = true)
        }
    }

    fun inicializarConMiembros(miembros: List<MiembroEntity>) {
        if (_uiState.value.pagadoPorId != null || miembros.isEmpty()) return
        val usuarioActual = miembros.firstOrNull { it.esUsuarioActual } ?: miembros.first()
        _uiState.value = _uiState.value.copy(
            pagadoPorId = usuarioActual.id,
            participantesSeleccionados = miembros.map { it.id }.toSet()
        )
    }

    fun actualizarDescripcion(valor: String) {
        _uiState.value = _uiState.value.copy(descripcion = valor, errorDescripcion = null)
    }

    fun actualizarMonto(valor: String) {
        if (valor.isEmpty() || valor.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
            _uiState.value = _uiState.value.copy(monto = valor, errorMonto = null)
        }
    }

    fun actualizarCategoria(categoria: CategoriaGasto) {
        _uiState.value = _uiState.value.copy(categoria = categoria)
    }

    fun actualizarPagadoPor(miembroId: Long) {
        _uiState.value = _uiState.value.copy(pagadoPorId = miembroId)
    }

    fun alternarParticipante(miembroId: Long) {
        val seleccionados = _uiState.value.participantesSeleccionados.toMutableSet()
        if (!seleccionados.remove(miembroId)) seleccionados.add(miembroId)
        _uiState.value = _uiState.value.copy(participantesSeleccionados = seleccionados, errorParticipantes = null)
    }

    fun guardarGasto() {
        val estado = _uiState.value
        val montoDouble = estado.monto.toDoubleOrNull()

        var errorDescripcion: String? = null
        var errorMonto: String? = null
        var errorParticipantes: String? = null
        if (estado.descripcion.isBlank()) errorDescripcion = "Describe el gasto"
        if (montoDouble == null || montoDouble <= 0.0) errorMonto = "Ingresa un monto válido"
        if (estado.participantesSeleccionados.isEmpty()) errorParticipantes = "Selecciona al menos un participante"

        if (errorDescripcion != null || errorMonto != null || errorParticipantes != null) {
            _uiState.value = estado.copy(
                errorDescripcion = errorDescripcion,
                errorMonto = errorMonto,
                errorParticipantes = errorParticipantes
            )
            return
        }

        val pagadoPorId = estado.pagadoPorId ?: return
        _uiState.value = estado.copy(guardando = true)
        viewModelScope.launch {
            if (gastoId != null) {
                gastoRepository.actualizarGasto(
                    gastoId = gastoId,
                    descripcion = estado.descripcion,
                    monto = montoDouble!!,
                    pagadoPorId = pagadoPorId,
                    categoria = estado.categoria,
                    miembrosParticipantesIds = estado.participantesSeleccionados.toList()
                )
            } else {
                gastoRepository.registrarGasto(
                    grupoId = grupoId,
                    descripcion = estado.descripcion,
                    monto = montoDouble!!,
                    pagadoPorId = pagadoPorId,
                    categoria = estado.categoria,
                    miembrosParticipantesIds = estado.participantesSeleccionados.toList()
                )
            }
            _uiState.value = _uiState.value.copy(guardando = false, gastoRegistrado = true)
        }
    }

    class Factory(
        private val grupoId: Long,
        private val gastoId: Long?,
        private val grupoRepository: GrupoRepository,
        private val gastoRepository: GastoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RegistrarGastoViewModel(grupoId, gastoId, grupoRepository, gastoRepository) as T
        }
    }
}
