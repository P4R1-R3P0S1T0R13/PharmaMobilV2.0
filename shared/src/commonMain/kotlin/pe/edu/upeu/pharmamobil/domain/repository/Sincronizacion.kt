package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Inventario
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio

/** Lado local de la sincronizacion: la cola de filas pendientes. */
interface ColaSincronizacion {
    fun pendientesPorEnviar(maxIntentos: Int): List<Producto>

    /** La fila ya existe en el servidor: se retira y vuelve con su id real al conciliar. */
    fun confirmar(idLocal: Long)

    fun sumarIntento(idLocal: Long, motivo: String)

    fun marcarFallida(idLocal: Long, motivo: String, maxIntentos: Int)

    /** Reemplaza lo sincronizado sin tocar lo pendiente y guarda la hora. */
    fun conciliar(inventario: Inventario)

    fun contarPendientes(maxIntentos: Int): Int
}

/** Lado remoto de la sincronizacion. */
interface EnvioRemoto {
    /**
     * Traduce cualquier error de la API a una rama de ResultadoEnvio.
     * Solo lanza excepcion si la sesion ya no es valida (401).
     */
    suspend fun enviar(producto: Producto): ResultadoEnvio

    /** Lanza ErrorApiException si no se puede bajar. */
    suspend fun descargar(): Inventario
}

/** Borra los datos del dispositivo (cierre de sesion o cambio de usuario). */
interface DatosLocales {
    fun borrarTodo()
}
