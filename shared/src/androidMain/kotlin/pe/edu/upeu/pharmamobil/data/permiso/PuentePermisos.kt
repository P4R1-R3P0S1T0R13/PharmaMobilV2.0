package pe.edu.upeu.pharmamobil.data.permiso

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

/**
 * Conecta la corrutina con el lanzador de la Activity. Se crea en onCreate
 * (registerForActivityResult debe llamarse antes de onStart) y convierte la
 * respuesta del dialogo en el valor que la funcion suspend estaba esperando.
 */
class PuentePermisos(val actividad: ComponentActivity) {

    private var pendiente: CompletableDeferred<Boolean>? = null

    private val lanzador = actividad.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> pendiente?.complete(concedido) }

    suspend fun pedir(nombre: String): Boolean {
        val espera = CompletableDeferred<Boolean>()
        pendiente = espera
        lanzador.launch(nombre)
        return espera.await()
    }
}
