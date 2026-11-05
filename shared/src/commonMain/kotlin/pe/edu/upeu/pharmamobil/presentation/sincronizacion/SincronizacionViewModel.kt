package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.comoErrorApi
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.runCatchingSuspend

/** Lo que el usuario debe poder responder sin abrir ningun menu (Sesion 13). */
data class SincronizacionUiState(
    val enCurso: Boolean = false,
    val pendientes: Int = 0,
    val fallidas: Int = 0,
    /** epoch en milisegundos; null si nunca se sincronizo. */
    val ultimaSync: Long? = null,
    val sinConexion: Boolean = false,
    /** Error no relacionado con la red (5xx, respuesta ilegible...). */
    val error: String? = null
) {
    val cambiosSinEnviar: Int get() = pendientes + fallidas
}

/**
 * Arma el estado a partir de consultas OBSERVABLES de la base local (nunca
 * de la red) y ejecuta la sincronizacion en segundo plano.
 */
class SincronizacionViewModel(
    private val repositorio: ProductoRepository,
    private val telemetria: Telemetria
) : ViewModel() {

    private val enCurso = MutableStateFlow(false)
    private val ultimoResultado = MutableStateFlow(Pair(false, null as String?)) // sinConexion, error

    private val _resultados = MutableSharedFlow<ResumenSync>(extraBufferCapacity = 1)

    /** Cada sincronizacion terminada (para notificaciones). */
    val resultados: SharedFlow<ResumenSync> = _resultados.asSharedFlow()

    val estado: StateFlow<SincronizacionUiState> = combine(
        repositorio.observarPendientes(),
        repositorio.observarFallidas(),
        repositorio.observarUltimaSincronizacion(),
        enCurso,
        ultimoResultado
    ) { pendientes, fallidas, ultima, curso, (sinConexion, error) ->
        SincronizacionUiState(curso, pendientes, fallidas, ultima, sinConexion, error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SincronizacionUiState())

    init {
        sincronizar()
    }

    fun sincronizar() = ejecutar { repositorio.sincronizar() }

    /** "Reintentar ahora" sobre las filas que agotaron sus intentos. */
    fun reintentarFallidas() = ejecutar { repositorio.reintentarFallidas() }

    private fun ejecutar(operacion: suspend () -> ResumenSync) {
        if (enCurso.value) return
        viewModelScope.launch {
            enCurso.value = true
            val resultado = runCatchingSuspend { operacion() }
            resultado.onSuccess { resumen ->
                ultimoResultado.value = resumen.sinConexion to null
                _resultados.tryEmit(resumen)
            }.onFailure { fallo ->
                val error = fallo.comoErrorApi()
                val sinRed = error == ErrorApi.SinConexion || error == ErrorApi.TiempoAgotado
                if (!sinRed && error !is ErrorApi.NoAutorizado) {
                    telemetria.registrarError(fallo, mapOf("pantalla" to "Productos", "operacion" to "sincronizar"))
                }
                ultimoResultado.value = sinRed to error.mensaje.takeUnless { sinRed }
            }
            enCurso.value = false
        }
    }
}

/** Linea principal de la barra: «Sin conexion · 3 cambios por enviar». */
fun textoEstadoSync(estado: SincronizacionUiState): String {
    val cambios = when (val n = estado.pendientes) {
        0 -> null
        1 -> "1 cambio por enviar"
        else -> "$n cambios por enviar"
    }
    return when {
        estado.enCurso -> "Sincronizando…"
        estado.sinConexion -> listOfNotNull("Sin conexion", cambios).joinToString(" · ")
        cambios != null -> cambios
        else -> "Todo enviado"
    }
}

/** Mensaje de las filas fallidas, con la accion que se ofrece. */
fun textoFallidas(fallidas: Int): String? = when (fallidas) {
    0 -> null
    1 -> "1 cambio no se pudo enviar"
    else -> "$fallidas cambios no se pudieron enviar"
}
