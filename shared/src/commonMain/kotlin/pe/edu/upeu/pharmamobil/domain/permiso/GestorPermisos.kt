package pe.edu.upeu.pharmamobil.domain.permiso

/** Permisos del sistema que la app puede necesitar. Hoy se usa NOTIFICACIONES. */
enum class Permiso { CAMARA, UBICACION, NOTIFICACIONES }

sealed interface EstadoPermiso {
    data object Concedido : EstadoPermiso

    /** Se puede volver a pedir (primera vez, o lo nego una vez). */
    data object Denegado : EstadoPermiso

    /** El sistema ya no muestra el dialogo: solo queda ir a Ajustes. */
    data object DenegadoParaSiempre : EstadoPermiso
}

/**
 * Se declara en el codigo comun como interfaz porque pedir un permiso
 * necesita la Activity (Android) o UNUserNotificationCenter (iOS), que el
 * codigo comun no puede nombrar. Cada plataforma registra la suya en Koin.
 */
interface GestorPermisos {
    suspend fun estado(permiso: Permiso): EstadoPermiso
    suspend fun solicitar(permiso: Permiso): EstadoPermiso
    fun abrirAjustes()
}
