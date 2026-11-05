package pe.edu.upeu.pharmamobil.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.FirebaseCrashlytics
import dev.gitlive.firebase.crashlytics.crashlytics
import pe.edu.upeu.pharmamobil.domain.port.Telemetria

/**
 * Unico lugar del proyecto que conoce el SDK de Firebase (GitLive,
 * multiplataforma). El dominio solo ve la interfaz Telemetria.
 *
 * Si la app se compilo sin google-services.json, Firebase no se inicializa:
 * en ese caso la telemetria se degrada al log y nunca rompe la app.
 */
class TelemetriaFirebase(
    private val respaldo: (String) -> Unit = { println("[Telemetria] $it") }
) : Telemetria {

    private val crashlytics: FirebaseCrashlytics? by lazy { runCatching { Firebase.crashlytics }.getOrNull() }

    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        val datos = contexto.sinDatosSensibles()
        val c = crashlytics
        if (c == null) {
            respaldo("error ${e::class.simpleName}: ${e.message} $datos")
            return
        }
        runCatching {
            datos.forEach { (clave, valor) -> c.setCustomKey(clave, valor) }
            c.recordException(e)
        }.onFailure { respaldo("error ${e::class.simpleName} (Crashlytics no disponible)") }
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        val linea = "$nombre ${datos.sinDatosSensibles()}"
        val c = crashlytics
        if (c == null) {
            respaldo(linea)
            return
        }
        runCatching { c.log(linea) }.onFailure { respaldo(linea) }
    }
}

/**
 * Un registro de telemetria termina en un servicio de terceros: nunca el
 * token ni datos personales, solo contexto tecnico.
 */
internal fun Map<String, String>.sinDatosSensibles(): Map<String, String> =
    filterKeys { clave -> CLAVES_PROHIBIDAS.none { clave.contains(it, ignoreCase = true) } }
        .mapValues { (_, valor) -> if (valor.startsWith("eyJ")) "***" else valor.take(200) }

private val CLAVES_PROHIBIDAS = listOf("token", "password", "clave", "authorization", "correo", "email", "dni")
