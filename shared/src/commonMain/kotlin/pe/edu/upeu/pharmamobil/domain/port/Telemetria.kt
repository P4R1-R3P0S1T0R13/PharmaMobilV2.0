package pe.edu.upeu.pharmamobil.domain.port

/**
 * Lo que el dominio necesita del proveedor de telemetria (Firebase Crashlytics
 * hoy). Solo contexto tecnico: nunca datos personales ni el token.
 */
interface Telemetria {
    fun registrarError(e: Throwable, contexto: Map<String, String> = emptyMap())
    fun registrarEvento(nombre: String, datos: Map<String, String> = emptyMap())
}

/** Implementacion vacia para pruebas o si el proveedor no esta configurado. */
object TelemetriaNula : Telemetria {
    override fun registrarError(e: Throwable, contexto: Map<String, String>) = Unit
    override fun registrarEvento(nombre: String, datos: Map<String, String>) = Unit
}
