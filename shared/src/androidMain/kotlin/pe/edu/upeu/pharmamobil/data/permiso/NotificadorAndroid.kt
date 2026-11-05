package pe.edu.upeu.pharmamobil.data.permiso

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import pe.edu.upeu.pharmamobil.domain.port.Notificador

/** Notificaciones con el canal "Sincronizacion e inventario". */
class NotificadorAndroid(private val contexto: Context) : Notificador {

    override fun notificar(titulo: String, mensaje: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            contexto.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return // sin permiso no se notifica, y no es un error

        val gestor = contexto.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            gestor.createNotificationChannel(
                NotificationChannel(CANAL, "Sincronizacion e inventario", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val abrirApp = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: return
        val alTocar = PendingIntent.getActivity(
            contexto, 0, abrirApp, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val constructor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(contexto, CANAL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(contexto)
        }
        val notificacion = constructor
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(Notification.BigTextStyle().bigText(mensaje))
            .setContentIntent(alTocar)
            .setAutoCancel(true)
            .build()
        gestor.notify(titulo.hashCode(), notificacion)
    }

    private companion object {
        const val CANAL = "pharmamobil_sync"
    }
}
