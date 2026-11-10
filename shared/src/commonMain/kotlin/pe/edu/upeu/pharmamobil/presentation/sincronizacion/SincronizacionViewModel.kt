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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.comoErrorApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.runCatchingSuspend
import pe.edu.upeu.pharmamobil.presentation.producto.textoAntiguedad

/** Lo que el usuario debe poder responder sin abrir ningun menu (Sesion 13). */
data class SincronizacionUiState(
    val enCurso: Boolean = false,
    val pendientes: Int = 0,
    val fallidas: Int = 0,
    /** epoch en milisegundos; null si nunca se sincronizo. */
    val ultimaSync: Long? = null,
    val sinConexion: Boolean = false,
    /** Error no relacionado con la red (5xx, respuesta ilegible...). */
    val error: String? = null,
    /** Filas que bajaron en la ultima sincronizacion correcta de esta sesion. */
    val descargados: Int? = null
) {
    val cambiosSinEnviar: Int get() = pendientes + fallidas
}

/**
 * Arma el estado a partir de consultas OBSERVABLES de la base local (nunca
 * de la red) y ejecuta la sincronizacion en segundo plano.
 */
class SincronizacionViewModel(
    private val repositorio: ProductoRepository,
    private val telemetria: Telemetria,
    private val notificador: Notificador
) : ViewModel() {

    /** Para avisar de bajo stock solo cuando aumenta, no en cada sincronizacion. */
    private var bajoStockAvisado = 0

    private val enCurso = MutableStateFlow(false)
    private data class UltimoResultado(
        val sinConexion: Boolean = false,
        val error: String? = null,
        val descargados: Int? = null
    )

    private val ultimoResultado = MutableStateFlow(UltimoResultado())

    private val _resultados = MutableSharedFlow<ResumenSync>(extraBufferCapacity = 1)

    /** Cada sincronizacion terminada (para notificaciones). */
    val resultados: SharedFlow<ResumenSync> = _resultados.asSharedFlow()

    val estado: StateFlow<SincronizacionUiState> = combine(
        repositorio.observarPendientes(),
        repositorio.observarFallidas(),
        repositorio.observarUltimaSincronizacion(),
        enCurso,
        ultimoResultado
    ) { pendientes, fallidas, ultima, curso, r ->
        SincronizacionUiState(curso, pendientes, fallidas, ultima, r.sinConexion, r.error, r.descargados)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SincronizacionUiState())

    init {
        sincronizar()
    }

    fun sincronizar() = ejecutar { repositorio.sincronizar() }

    /** "Reintentar ahora" sobre las filas que agotaron sus intentos. */
    fun reintentarFallidas() = ejecutar { repositorio.reintentarFallidas() }

    private suspend fun avisar(resumen: ResumenSync) {
        bajoStockAvisado = avisarInterno(resumen, repositorio, notificador, bajoStockAvisado)
    }

    private fun ejecutar(operacion: suspend () -> ResumenSync) {
        if (enCurso.value) return
        viewModelScope.launch {
            enCurso.value = true
            val resultado = runCatchingSuspend { operacion() }
            resultado.onSuccess { resumen ->
                ultimoResultado.value = UltimoResultado(
                    sinConexion = resumen.sinConexion,
                    descargados = resumen.descargados ?: ultimoResultado.value.descargados
                )
                _resultados.tryEmit(resumen)
                avisar(resumen)
            }.onFailure { fallo ->
                val error = fallo.comoErrorApi()
                val sinRed = error == ErrorApi.SinConexion || error == ErrorApi.TiempoAgotado
                if (!sinRed && error !is ErrorApi.NoAutorizado) {
                    telemetria.registrarError(fallo, mapOf("pantalla" to "Productos", "operacion" to "sincronizar"))
                }
                ultimoResultado.value = ultimoResultado.value.copy(
                    sinConexion = sinRed,
                    error = error.mensaje.takeUnless { sinRed }
                )
            }
            enCurso.value = false
        }
    }
}

/** Notificaciones del sistema (si el permiso no esta concedido, el notificador no hace nada). */
private suspend fun avisarInterno(
    resumen: ResumenSync,
    repositorio: ProductoRepository,
    notificador: Notificador,
    bajoStockPrevio: Int
): Int {
    if (resumen.enviadas > 0) {
        notificador.notificar("Cambios enviados", "${resumen.enviadas} cambio(s) guardados sin conexion ya estan en PharmaSoft.")
    }
    if (resumen.fallidas > 0) {
        notificador.notificar("Cambios sin enviar", "${resumen.fallidas} cambio(s) no se pudieron enviar. Revisalos en Productos.")
    }
    if (resumen.sinConexion) return bajoStockPrevio
    val bajoStock = repositorio.observar().first().count { it.requiereReposicion }
    if (bajoStock > bajoStockPrevio) {
        notificador.notificar("Bajo stock", "$bajoStock producto(s) tienen stock de ${Producto.STOCK_MINIMO} unidades o menos.")
    }
    return bajoStock
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

/** «Actualizado hace 2 min (14:32) · 5 productos descargados». */
fun textoDetalleSync(estado: SincronizacionUiState, ahoraMs: Long, formatearHora: (Long) -> String): String {
    val hora = estado.ultimaSync?.let { " (${formatearHora(it)})" }.orEmpty()
    val filas = estado.descargados?.let { " · $it producto${if (it == 1) "" else "s"} descargado${if (it == 1) "" else "s"}" }.orEmpty()
    return textoAntiguedad(estado.ultimaSync, ahoraMs) + hora + filas
}

/** Mensaje de las filas fallidas, con la accion que se ofrece. */
fun textoFallidas(fallidas: Int): String? = when (fallidas) {
    0 -> null
    1 -> "1 cambio no se pudo enviar"
    else -> "$fallidas cambios no se pudieron enviar"
}
