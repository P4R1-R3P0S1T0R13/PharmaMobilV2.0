@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pe.edu.upeu.pharmamobil.data.permiso

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import kotlin.coroutines.resume

/**
 * iOS: en notificaciones no hay "volver a pedir". Si el usuario las nego,
 * el sistema ya no muestra el dialogo y solo queda Ajustes. No requiere
 * clave en Info.plist (camara o ubicacion si la requeririan).
 * Codigo escrito sin compilar en macOS.
 */
class GestorPermisosIos : GestorPermisos {

    private val centro = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        if (permiso != Permiso.NOTIFICACIONES) return EstadoPermiso.Denegado
        return suspendCancellableCoroutine { continuacion ->
            centro.getNotificationSettingsWithCompletionHandler { ajustes ->
                val estado = when (ajustes?.authorizationStatus) {
                    UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional,
                    UNAuthorizationStatusEphemeral -> EstadoPermiso.Concedido
                    UNAuthorizationStatusDenied -> EstadoPermiso.DenegadoParaSiempre
                    else -> EstadoPermiso.Denegado
                }
                continuacion.resume(estado)
            }
        }
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        if (permiso != Permiso.NOTIFICACIONES) return EstadoPermiso.Denegado
        return suspendCancellableCoroutine { continuacion ->
            centro.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { concedido, _ ->
                continuacion.resume(if (concedido) EstadoPermiso.Concedido else EstadoPermiso.DenegadoParaSiempre)
            }
        }
    }

    override fun abrirAjustes() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
    }
}

class NotificadorIos : Notificador {
    override fun notificar(titulo: String, mensaje: String) {
        val contenido = UNMutableNotificationContent()
        contenido.title = titulo
        contenido.body = mensaje
        val solicitud = UNNotificationRequest.requestWithIdentifier(titulo, contenido, null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(solicitud, null)
    }
}
