package pe.edu.upeu.pharmamobil.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.comoErrorApi
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.usecase.runCatchingSuspend

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsuario(valor: String) = _uiState.update { it.copy(usuario = valor, error = null) }
    fun onClave(valor: String) = _uiState.update { it.copy(clave = valor, error = null) }

    fun ingresar() {
        val estado = _uiState.value
        if (estado.cargando) return
        if (estado.usuario.isBlank() || estado.clave.isBlank()) {
            _uiState.update { it.copy(error = "Ingrese usuario y contrasena.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            runCatchingSuspend { auth.iniciarSesion(estado.usuario, estado.clave) }.fold(
                // Al iniciar sesion, App cambia de pantalla observando auth.estado.
                onSuccess = { _uiState.value = LoginUiState() },
                onFailure = { fallo ->
                    _uiState.update { it.copy(cargando = false, clave = "", error = fallo.comoErrorApi().mensaje) }
                }
            )
        }
    }
}
