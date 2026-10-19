package com.entreamigos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.entreamigos.app.data.model.EstadoConexion
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.ResumenGeneral
import com.entreamigos.app.data.model.ResumenGrupo
import com.entreamigos.app.data.model.comoResource
import com.entreamigos.app.data.repository.GrupoRepository
import com.entreamigos.app.data.repository.SincronizacionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    grupoRepository: GrupoRepository,
    private val sincronizacion: SincronizacionRepository
) : ViewModel() {

    val grupos: StateFlow<Resource<List<ResumenGrupo>>> = grupoRepository.observarResumenGrupos()
        .comoResource("No se pudieron cargar tus grupos")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    val resumenGeneral: StateFlow<ResumenGeneral> = grupoRepository.observarResumenGeneral()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ResumenGeneral(0.0, 0.0, 0.0)
        )

    val conexion: StateFlow<EstadoConexion> = sincronizacion.estadoConexion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoConexion())

    /** Pull-to-refresh y botón "Reintentar". */
    fun refrescar() = sincronizacion.sincronizarEnSegundoPlano()

    class Factory(
        private val grupoRepository: GrupoRepository,
        private val sincronizacion: SincronizacionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(grupoRepository, sincronizacion) as T
        }
    }
}
