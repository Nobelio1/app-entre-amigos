package com.entreamigos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.entreamigos.app.data.local.entity.GrupoIcono
import com.entreamigos.app.data.repository.GrupoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val ICONOS_GRUPO = GrupoIcono.entries.filter { it != GrupoIcono.GENERAL }

data class CrearGrupoUiState(
    val nombreGrupo: String = "",
    val iconoSeleccionado: GrupoIcono = ICONOS_GRUPO.first(),
    val nombresMiembros: List<String> = listOf("Tú", ""),
    val errorNombreGrupo: String? = null,
    val errorMiembros: String? = null,
    val guardando: Boolean = false,
    val grupoCreadoId: Long? = null
) {
    val esValido: Boolean
        get() = nombreGrupo.isNotBlank() && nombresMiembros.count { it.isNotBlank() } >= 2
}

class CrearGrupoViewModel(private val grupoRepository: GrupoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CrearGrupoUiState())
    val uiState: StateFlow<CrearGrupoUiState> = _uiState.asStateFlow()

    fun actualizarNombreGrupo(nombre: String) {
        _uiState.value = _uiState.value.copy(nombreGrupo = nombre, errorNombreGrupo = null)
    }

    fun seleccionarIcono(icono: GrupoIcono) {
        _uiState.value = _uiState.value.copy(iconoSeleccionado = icono)
    }

    fun actualizarMiembro(index: Int, nombre: String) {
        val lista = _uiState.value.nombresMiembros.toMutableList()
        if (index in lista.indices) lista[index] = nombre
        _uiState.value = _uiState.value.copy(nombresMiembros = lista, errorMiembros = null)
    }

    fun agregarCampoMiembro() {
        val lista = _uiState.value.nombresMiembros + ""
        _uiState.value = _uiState.value.copy(nombresMiembros = lista)
    }

    fun eliminarCampoMiembro(index: Int) {
        if (_uiState.value.nombresMiembros.size <= 2) return
        val lista = _uiState.value.nombresMiembros.toMutableList().apply { removeAt(index) }
        _uiState.value = _uiState.value.copy(nombresMiembros = lista)
    }

    fun crearGrupo() {
        val estado = _uiState.value
        val nombresValidos = estado.nombresMiembros.map { it.trim() }.filter { it.isNotBlank() }

        var errorNombre: String? = null
        var errorMiembros: String? = null
        if (estado.nombreGrupo.isBlank()) errorNombre = "Ponle un nombre al grupo"
        if (nombresValidos.size < 2) errorMiembros = "Agrega al menos 2 integrantes (tú incluido)"

        if (errorNombre != null || errorMiembros != null) {
            _uiState.value = estado.copy(errorNombreGrupo = errorNombre, errorMiembros = errorMiembros)
            return
        }

        _uiState.value = estado.copy(guardando = true)
        viewModelScope.launch {
            val id = grupoRepository.crearGrupo(
                nombre = estado.nombreGrupo.trim(),
                icono = estado.iconoSeleccionado,
                colorHex = "#1B5E4F",
                nombresMiembros = nombresValidos
            )
            _uiState.value = _uiState.value.copy(guardando = false, grupoCreadoId = id)
        }
    }

    class Factory(private val grupoRepository: GrupoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CrearGrupoViewModel(grupoRepository) as T
        }
    }
}
