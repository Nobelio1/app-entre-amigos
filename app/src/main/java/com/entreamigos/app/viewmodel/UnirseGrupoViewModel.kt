package com.entreamigos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.TipoError
import com.entreamigos.app.data.remote.dto.InvitacionDto
import com.entreamigos.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnirseGrupoUiState(
    val codigo: String = "",
    /** Paso 2: grupo encontrado; el usuario elige con qué integrante unirse. */
    val invitacion: InvitacionDto? = null,
    /** null = unirse como integrante nuevo. */
    val miembroElegido: Long? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val grupoUnidoId: Long? = null
)

class UnirseGrupoViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(UnirseGrupoUiState())
    val uiState: StateFlow<UnirseGrupoUiState> = _uiState.asStateFlow()

    fun actualizarCodigo(valor: String) = _uiState.update {
        it.copy(codigo = valor.uppercase().filter(Char::isLetterOrDigit).take(12), error = null)
    }

    fun elegirMiembro(miembroId: Long?) = _uiState.update { it.copy(miembroElegido = miembroId) }

    fun volver() = _uiState.update { it.copy(invitacion = null, miembroElegido = null, error = null) }

    fun buscar() {
        val codigo = _uiState.value.codigo
        if (codigo.length < 4) {
            _uiState.update { it.copy(error = "Ingresa el código que te compartieron") }
            return
        }
        ejecutar {
            when (val r = authRepository.verInvitacion(codigo)) {
                is Resource.Success -> _uiState.update { it.copy(cargando = false, invitacion = r.data, miembroElegido = null) }
                is Resource.Error -> falla(r)
                Resource.Loading -> Unit
            }
        }
    }

    fun unirse() {
        val estado = _uiState.value
        ejecutar {
            when (val r = authRepository.unirseAGrupo(estado.codigo, estado.miembroElegido)) {
                is Resource.Success -> _uiState.update { it.copy(cargando = false, grupoUnidoId = r.data) }
                is Resource.Error -> falla(r)
                Resource.Loading -> Unit
            }
        }
    }

    private fun ejecutar(bloque: suspend () -> Unit) {
        if (_uiState.value.cargando) return
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch { bloque() }
    }

    private fun falla(error: Resource.Error) = _uiState.update {
        it.copy(
            cargando = false,
            error = if (error.tipo == TipoError.SIN_CONEXION) "Necesitas conexión para unirte a un grupo" else error.mensaje
        )
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = UnirseGrupoViewModel(authRepository) as T
    }
}
