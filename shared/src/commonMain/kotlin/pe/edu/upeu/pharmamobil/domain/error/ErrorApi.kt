package pe.edu.upeu.pharmamobil.domain.error

/**
 * Todo lo que puede salir mal al hablar con el servidor, en terminos del
 * dominio. Es sealed: un when sobre ErrorApi obliga a tratar cada caso.
 */
sealed class ErrorApi(open val mensaje: String) {
    data object SinConexion : ErrorApi("Sin conexion con el servidor. Verifica tu red.")
    data object TiempoAgotado : ErrorApi("El servidor tardo demasiado en responder.")

    /** 400: el servidor rechazo los datos; campos trae el mensaje por cada campo. */
    data class Validacion(
        override val mensaje: String,
        val campos: Map<String, String> = emptyMap()
    ) : ErrorApi(mensaje)

    /** 401: sin token, token vencido o alterado. */
    data class NoAutorizado(override val mensaje: String) : ErrorApi(mensaje)

    /** 403: el usuario no tiene el permiso necesario. */
    data class Prohibido(override val mensaje: String) : ErrorApi(mensaje)

    data class NoEncontrado(override val mensaje: String) : ErrorApi(mensaje)

    /** 409: regla de negocio o integridad (nombre repetido, producto con ventas...). */
    data class Conflicto(override val mensaje: String) : ErrorApi(mensaje)

    data class Servidor(val codigo: Int) : ErrorApi("Error interno del servidor ($codigo). Intenta mas tarde.")
    data object RespuestaInvalida : ErrorApi("La respuesta del servidor no tiene el formato esperado.")
}

/** Excepcion que transporta un ErrorApi a traves de Result/runCatching. */
class ErrorApiException(val error: ErrorApi) : Exception(error.mensaje)

/** Extrae el ErrorApi de cualquier fallo; lo no tipificado cuenta como sin conexion. */
fun Throwable.comoErrorApi(): ErrorApi = (this as? ErrorApiException)?.error ?: ErrorApi.SinConexion
