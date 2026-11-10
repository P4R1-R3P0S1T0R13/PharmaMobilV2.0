package pe.edu.upeu.pharmamobil.domain.model

/** Resultado de enviar UNA fila pendiente al servidor (Sesion 13). */
sealed interface ResultadoEnvio {
    data class Exito(val idServidor: Long) : ResultadoEnvio

    /**
     * Puede funcionar mas tarde: tiempo agotado, 5xx, respuesta ilegible.
     * sinConexion = true corta la sincronizacion y NO consume intentos.
     */
    data class Transitorio(val motivo: String, val sinConexion: Boolean = false) : ResultadoEnvio

    /** No va a funcionar reintentando: validacion, conflicto, recurso inexistente o falta de privilegio. */
    data class Permanente(val motivo: String) : ResultadoEnvio
}

/** Resumen de una sincronizacion para la interfaz y las notificaciones. */
data class ResumenSync(
    val enviadas: Int = 0,
    val fallidas: Int = 0,
    val pendientes: Int = 0,
    val sinConexion: Boolean = false,
    /** id local negativo -> id real asignado por PharmaSoft. */
    val confirmados: Map<Long, Long> = emptyMap(),
    /** Productos que bajaron del servidor; null si no se llego a bajar (sin red). */
    val descargados: Int? = null
)

/** Inventario que baja del servidor. */
data class Inventario(
    val productos: List<Producto>,
    val categorias: List<Categoria>
)

/** Limite y memoria del reintento. */
object PoliticaReintentos {
    const val MAX_INTENTOS = 5

    /** Espera creciente entre intentos: 1 s, 2 s, 4 s, 8 s y 16 s como maximo. */
    fun espera(intentos: Int): Long = 1_000L shl intentos.coerceIn(0, 4)
}
