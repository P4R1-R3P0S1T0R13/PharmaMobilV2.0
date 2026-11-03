package pe.edu.upeu.pharmamobil.platform

/**
 * Capacidades nativas (Sesion 9). commonMain solo declara QUE se necesita;
 * cada plataforma resuelve COMO en su source set.
 */

/** Precio en soles con el formato regional de Peru (es-PE). */
expect fun formatearSoles(valor: Double): String

/** Datos del equipo en el que corre la app (tercera capacidad nativa). */
data class InfoDispositivo(
    val sistema: String,
    val version: String,
    val modelo: String,
    val idioma: String
)

expect fun infoDispositivo(): InfoDispositivo

/**
 * Compartir necesita el Context de Android o el ViewController de iOS, que
 * no existen en commonMain: por eso es una interfaz cuya implementacion
 * nativa entrega Koin desde platformModule.
 */
interface Compartidor {
    fun compartir(texto: String)
}
