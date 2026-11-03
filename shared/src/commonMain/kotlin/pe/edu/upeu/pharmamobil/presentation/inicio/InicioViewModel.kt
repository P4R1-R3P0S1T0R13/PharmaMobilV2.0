package pe.edu.upeu.pharmamobil.presentation.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.comoErrorApi
import pe.edu.upeu.pharmamobil.domain.repository.DiagnosticoSesion
import pe.edu.upeu.pharmamobil.domain.repository.ReporteRepository
import pe.edu.upeu.pharmamobil.domain.usecase.runCatchingSuspend

/**
 * Comprueba el acceso a /api/v1/reportes (solo ADMIN). Con VENDEDOR o
 * ALMACENERO el servidor responde 403 y se muestra su mensaje.
 */
class InicioViewModel(
    private val reportes: ReporteRepository,
    private val diagnostico: DiagnosticoSesion
) : ViewModel() {

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

    private val _diagnostico = MutableStateFlow<String?>(null)
    val resultadoDiagnostico: StateFlow<String?> = _diagnostico.asStateFlow()

    fun consultarMe() = diagnosticar("GET /auth/me con token") { diagnostico.consultarMe() }
    fun sinCabecera() = diagnosticar("Sin cabecera Authorization") { diagnostico.peticionSinCabecera() }
    fun prefijoMinuscula() = diagnosticar("Prefijo 'bearer' en minuscula") { diagnostico.peticionPrefijoMinuscula() }

    fun alterarToken() {
        diagnostico.alterarToken()
        _diagnostico.value = "Token alterado. La siguiente operacion protegida recibira 401 y cerrara la sesion."
    }

    private fun diagnosticar(nombre: String, prueba: suspend () -> String) {
        viewModelScope.launch {
            _diagnostico.value = "$nombre: consultando..."
            _diagnostico.value = "$nombre → " + runCatchingSuspend { prueba() }
                .getOrElse { it.comoErrorApi().mensaje }
        }
    }
}
