package pe.edu.upeu.pharmamobil.domain.port

/**
 * Notificaciones del sistema. Si el usuario no concedio el permiso, la
 * implementacion no hace nada: nunca debe fallar por eso.
 */
interface Notificador {
    fun notificar(titulo: String, mensaje: String)
}

object NotificadorNulo : Notificador {
    override fun notificar(titulo: String, mensaje: String) = Unit
}
