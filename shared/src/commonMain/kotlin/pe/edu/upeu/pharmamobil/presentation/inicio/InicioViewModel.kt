package pe.edu.upeu.pharmamobil.presentation.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.comoErrorApi
import pe.edu.upeu.pharmamobil.domain.repository.ReporteRepository
import pe.edu.upeu.pharmamobil.domain.usecase.runCatchingSuspend

/**
 * Comprueba el acceso a /api/v1/reportes (solo ADMIN). Con VENDEDOR o
 * ALMACENERO el servidor responde 403 y se muestra su mensaje.
 */
class InicioViewModel(private val reportes: ReporteRepository) : ViewModel() {

    private val _resultado = MutableStateFlow<String?>(null)
    val resultado: StateFlow<String?> = _resultado.asStateFlow()

    fun probarAccesoReportes() {
        viewModelScope.launch {
            _resultado.value = "Consultando..."
            _resultado.value = runCatchingSuspend { reportes.productosMasVendidos() }.fold(
                onSuccess = { "Acceso permitido: el reporte tiene $it producto(s)." },
                onFailure = { "Acceso denegado: ${it.comoErrorApi().mensaje}" }
            )
        }
    }
}
