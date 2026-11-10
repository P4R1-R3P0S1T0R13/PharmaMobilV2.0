package pe.edu.upeu.pharmamobil.platform

/**
 * Capacidades nativas (Sesion 9). commonMain solo declara QUE se necesita;
 * cada plataforma resuelve COMO en su source set.
 */

/** Precio en soles con el formato regional de Peru (es-PE). */
expect fun formatearSoles(valor: Double): String

/**
 * Datos de la build (Sesion 14). Android los toma de BuildConfig en
 * MainApplication; iOS, del binario y de su Info.plist.
 */
data class InfoApp(
    /** false en la version de entrega: oculta las herramientas de prueba. */
    val depuracion: Boolean,
    val version: String
)

/** Datos del equipo en el que corre la app (tercera capacidad nativa). */
data class InfoDispositivo(
    val sistema: String,
    val version: String,
    val modelo: String,
    val idioma: String
)

expect fun infoDispositivo(): InfoDispositivo

/** Hora actual en milisegundos desde 1970 (para la expiracion del token). */
expect fun ahoraMs(): Long

/** Hora local "HH:mm" de un instante epoch en milisegundos. */
expect fun formatearHora(epochMs: Long): String

/**
 * Compartir necesita el Context de Android o el ViewController de iOS, que
 * no existen en commonMain: por eso es una interfaz cuya implementacion
 * nativa entrega Koin desde platformModule.
 */
interface Compartidor {
    fun compartir(texto: String)
}
