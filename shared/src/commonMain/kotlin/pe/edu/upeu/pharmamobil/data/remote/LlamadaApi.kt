package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import pe.edu.upeu.pharmamobil.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException

/**
 * Unico punto donde las excepciones de Ktor se traducen a ErrorApi.
 * Todas las llamadas de las *Api pasan por aqui.
 */
suspend fun <T> llamadaApi(bloque: suspend () -> T): T = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (e: ErrorApiException) {
    throw e
} catch (e: ClientRequestException) {
    throw ErrorApiException(errorDeCliente(e.response))
} catch (e: ServerResponseException) {
    throw ErrorApiException(ErrorApi.Servidor(e.response.status.value))
} catch (e: HttpRequestTimeoutException) {
    throw ErrorApiException(ErrorApi.TiempoAgotado)
} catch (e: SerializationException) {
    throw ErrorApiException(ErrorApi.RespuestaInvalida)
} catch (e: ContentConvertException) {
    // Ktor envuelve los errores de kotlinx.serialization en JsonConvertException.
    throw ErrorApiException(ErrorApi.RespuestaInvalida)
} catch (e: Exception) {
    // IOException, UnknownHost, conexion rechazada, modo avion...
    throw ErrorApiException(ErrorApi.SinConexion)
}

private suspend fun errorDeCliente(respuesta: HttpResponse): ErrorApi {
    val cuerpo = runCatching { respuesta.body<ErrorResponseDto>() }.getOrNull()
    val mensaje = cuerpo?.message?.takeIf { it.isNotBlank() }
    return when (respuesta.status.value) {
        400 -> ErrorApi.Validacion(mensaje ?: "Datos invalidos", cuerpo?.validationErrors.orEmpty())
        401 -> ErrorApi.NoAutorizado(mensaje ?: "Debe iniciar sesion")
        403 -> ErrorApi.Prohibido(mensaje ?: "No tiene permisos para realizar esta operacion")
        404 -> ErrorApi.NoEncontrado(mensaje ?: "Recurso no encontrado")
        409 -> ErrorApi.Conflicto(mensaje ?: "La operacion entra en conflicto con los datos existentes")
        else -> ErrorApi.Validacion(mensaje ?: "Solicitud rechazada (${respuesta.status.value})")
    }
}
