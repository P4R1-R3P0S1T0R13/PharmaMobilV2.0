package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.SerializationException

/** Traduce una excepcion de red a un mensaje para el usuario (Sesion 7). */
fun mensajeDeRed(error: Throwable): String = when (error) {
    is HttpRequestTimeoutException -> "El servidor tardo demasiado en responder."
    is ResponseException -> "El servidor respondio con el codigo ${error.response.status.value}."
    is SerializationException -> "La respuesta del servidor no tiene el formato esperado."
    else -> "Sin conexion con el servidor. Verifica tu red e intenta nuevamente."
}
