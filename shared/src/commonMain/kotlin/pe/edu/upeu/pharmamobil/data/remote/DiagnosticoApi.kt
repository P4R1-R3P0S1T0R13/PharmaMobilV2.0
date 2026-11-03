package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.util.AttributeKey
import pe.edu.upeu.pharmamobil.data.auth.GestorSesion
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.repository.DiagnosticoSesion

/**
 * Marca una peticion para que el plugin AutenticacionJwt no le agregue el
 * token ni cierre la sesion si recibe 401. Solo para las pruebas de la Sesion 10.
 */
val OmitirAutenticacion = AttributeKey<Boolean>("OmitirAutenticacion")

/**
 * Provoca desde la app los escenarios de la bitacora de tokens y devuelve
 * el codigo HTTP y el mensaje literal del servidor.
 */
class DiagnosticoApi(
    private val client: HttpClient,
    private val gestor: GestorSesion
) : DiagnosticoSesion {

    override suspend fun consultarMe(): String = probar(gestor.tokenVigente()?.let { "Bearer $it" })

    override suspend fun peticionSinCabecera(): String = probar(cabecera = null)

    override suspend fun peticionPrefijoMinuscula(): String =
        probar(gestor.tokenVigente()?.let { "bearer $it" })

    override fun alterarToken() = gestor.alterarTokenParaPrueba()

    private suspend fun probar(cabecera: String?): String = llamadaApi {
        val respuesta = client.get("api/v1/auth/me") {
            attributes.put(OmitirAutenticacion, true)
            expectSuccess = false
            cabecera?.let { header(HttpHeaders.Authorization, it) }
        }
        val mensaje = runCatching { respuesta.body<ErrorResponseDto>().message }.getOrNull()
        "HTTP ${respuesta.status.value}" + (mensaje?.let { " · «$it»" } ?: " · OK")
    }
}
