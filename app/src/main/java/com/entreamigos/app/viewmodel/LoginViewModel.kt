package com.entreamigos.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.entreamigos.app.data.model.Resource
import com.entreamigos.app.data.model.TipoError
import com.entreamigos.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val modoRegistro: Boolean = false,
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val errorNombre: String? = null,
    val errorEmail: String? = null,
    val errorPassword: String? = null,
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
    val cargandoGoogle: Boolean = false
) {
    val ocupado: Boolean get() = cargando || cargandoGoogle
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val googleDisponible: Boolean get() = authRepository.googleDisponible

    /** [contextoActividad] solo se usa durante la llamada (no se guarda en el ViewModel). */
    fun continuarConGoogle(contextoActividad: Context) {
        if (_uiState.value.ocupado) return
        _uiState.update { it.copy(cargandoGoogle = true, errorGeneral = null) }
        viewModelScope.launch {
            val resultado = authRepository.iniciarSesionConGoogle(contextoActividad)
            val error = (resultado as? Resource.Error)?.let { e ->
                if (e.tipo == TipoError.SIN_CONEXION) "Sin conexión. Necesitas internet para iniciar sesión." else e.mensaje
            }
            _uiState.update { it.copy(cargandoGoogle = false, errorGeneral = error) }
        }
    }

    fun alternarModo() = _uiState.update {
        it.copy(modoRegistro = !it.modoRegistro, errorNombre = null, errorEmail = null, errorPassword = null, errorGeneral = null)
    }

    fun actualizarNombre(valor: String) = _uiState.update { it.copy(nombre = valor, errorNombre = null, errorGeneral = null) }

    fun actualizarEmail(valor: String) = _uiState.update { it.copy(email = valor.trim(), errorEmail = null, errorGeneral = null) }

    fun actualizarPassword(valor: String) = _uiState.update { it.copy(password = valor, errorPassword = null, errorGeneral = null) }

    fun enviar() {
        val estado = _uiState.value
        if (estado.ocupado) return
        val errorNombre = if (estado.modoRegistro && estado.nombre.isBlank()) "Escribe tu nombre" else null
        val errorEmail = when {
            estado.email.isBlank() -> "Escribe tu email"
            !EMAIL.matches(estado.email) -> "El email no es válido"
            else -> null
        }
        val errorPassword = when {
            estado.password.isEmpty() -> "Escribe tu contraseña"
            estado.modoRegistro && estado.password.length < PASSWORD_MINIMO ->
                "Debe tener al menos $PASSWORD_MINIMO caracteres"
            else -> null
        }
        if (errorNombre != null || errorEmail != null || errorPassword != null) {
            _uiState.update { it.copy(errorNombre = errorNombre, errorEmail = errorEmail, errorPassword = errorPassword) }
            return
        }

        _uiState.update { it.copy(cargando = true, errorGeneral = null) }
        viewModelScope.launch {
            val resultado = if (estado.modoRegistro) {
                authRepository.registrarse(estado.nombre, estado.email, estado.password)
            } else {
                authRepository.iniciarSesion(estado.email, estado.password)
            }
            // En caso de éxito la sesión cambia y la raíz de la app navega sola al inicio
            val error = (resultado as? Resource.Error)?.let { e ->
                if (e.tipo == TipoError.SIN_CONEXION) "Sin conexión con el servidor. Necesitas internet para iniciar sesión."
                else e.mensaje
            }
            _uiState.update { it.copy(cargando = false, errorGeneral = error, password = if (error != null) it.password else "") }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LoginViewModel(authRepository) as T
    }

    private companion object {
        const val PASSWORD_MINIMO = 8
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
