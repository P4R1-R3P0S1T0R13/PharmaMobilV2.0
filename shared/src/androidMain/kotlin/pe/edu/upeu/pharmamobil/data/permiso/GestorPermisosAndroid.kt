package pe.edu.upeu.pharmamobil.data.permiso

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso

/**
 * shouldShowRequestPermissionRationale devuelve false en dos casos distintos:
 * antes de la primera solicitud y despues de una negacion definitiva. Para
 * distinguirlos se recuerda si el permiso ya se pidio alguna vez.
 *
 * | Situacion               | Concedido | Ya se pidio | Explicar | Estado              |
 * | Primera vez             | No        | No          | No       | Denegado            |
 * | Nego una vez            | No        | Si          | Si       | Denegado            |
 * | Nego definitivamente    | No        | Si          | No       | DenegadoParaSiempre |
 * | Concedido               | Si        | -           | -        | Concedido           |
 */
class GestorPermisosAndroid(
    private val contexto: Context,
    private val preferencias: SharedPreferences
) : GestorPermisos {

    /** La Activity registra aqui su lanzador en onCreate. */
    var puente: PuentePermisos? = null

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        val nombre = nombreDeSistema(permiso) ?: return EstadoPermiso.Concedido
        if (contexto.checkSelfPermission(nombre) == PackageManager.PERMISSION_GRANTED) return EstadoPermiso.Concedido
        val actividad = puente?.actividad ?: return EstadoPermiso.Denegado
        val yaSePidio = preferencias.getBoolean(CLAVE + nombre, false)
        val procedeExplicar = actividad.shouldShowRequestPermissionRationale(nombre)
        return when {
            !yaSePidio -> EstadoPermiso.Denegado
            procedeExplicar -> EstadoPermiso.Denegado
            else -> EstadoPermiso.DenegadoParaSiempre
        }
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        val nombre = nombreDeSistema(permiso) ?: return EstadoPermiso.Concedido
        preferencias.edit().putBoolean(CLAVE + nombre, true).apply()
        val concedido = puente?.pedir(nombre) ?: false
        return if (concedido) EstadoPermiso.Concedido else estado(permiso)
    }

    override fun abrirAjustes() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + contexto.packageName))
        }
        contexto.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** null = el permiso no requiere solicitud en esta version de Android. */
    private fun nombreDeSistema(permiso: Permiso): String? = when (permiso) {
        // POST_NOTIFICATIONS existe desde Android 13; antes las notificaciones no se pedian.
        Permiso.NOTIFICACIONES ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        Permiso.CAMARA -> Manifest.permission.CAMERA
        Permiso.UBICACION -> Manifest.permission.ACCESS_COARSE_LOCATION
    }

    private companion object {
        const val CLAVE = "pedido_"
    }
}
