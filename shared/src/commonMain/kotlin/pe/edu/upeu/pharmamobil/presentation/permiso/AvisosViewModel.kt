package pe.edu.upeu.pharmamobil.presentation.permiso

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso

data class AvisosUiState(
    /** null mientras se consulta al sistema. */
    val estado: EstadoPermiso? = null,
    /** La interfaz explica para que se usan los avisos ANTES de mostrar el dialogo del sistema. */
    val mostrarExplicacion: Boolean = false
)

/** Permiso real de la app (Sesion 13): notificaciones de sincronizacion y bajo stock. */
class AvisosViewModel(private val gestor: GestorPermisos) : ViewModel() {

    private val _uiState = MutableStateFlow(AvisosUiState())
    val uiState: StateFlow<AvisosUiState> = _uiState.asStateFlow()

    /** Se llama al entrar y al volver de Ajustes (el usuario pudo cambiarlo alli). */
    fun refrescar() {
        viewModelScope.launch {
            val estado = gestor.estado(Permiso.NOTIFICACIONES)
            _uiState.update { it.copy(estado = estado) }
        }
    }

    /** Boton principal de la tarjeta: su accion depende del estado. */
    fun accionPrincipal() {
        when (_uiState.value.estado) {
            EstadoPermiso.Concedido -> Unit
            EstadoPermiso.DenegadoParaSiempre -> gestor.abrirAjustes()
            EstadoPermiso.Denegado, null -> _uiState.update { it.copy(mostrarExplicacion = true) }
        }
    }

    fun aceptarExplicacion() {
        _uiState.update { it.copy(mostrarExplicacion = false) }
        viewModelScope.launch {
            val estado = gestor.solicitar(Permiso.NOTIFICACIONES)
            _uiState.update { it.copy(estado = estado) }
        }
    }

    fun cancelarExplicacion() = _uiState.update { it.copy(mostrarExplicacion = false) }
}

/** Textos de la tarjeta para cada uno de los tres estados. */
fun textosAvisos(estado: EstadoPermiso?): Pair<String, String?> = when (estado) {
    EstadoPermiso.Concedido ->
        "Activados: te avisamos cuando tus cambios se envian o fallan y cuando hay bajo stock." to null
    EstadoPermiso.Denegado ->
        "Desactivados. Activalos para enterarte de lo que pasa con tus cambios sin abrir la app." to "Activar avisos"
    EstadoPermiso.DenegadoParaSiempre ->
        "Bloqueados. El sistema ya no muestra el permiso: actívalos desde Ajustes." to "Abrir Ajustes"
    null -> "Consultando el permiso..." to null
}
