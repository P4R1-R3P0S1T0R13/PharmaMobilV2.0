package pe.edu.upeu.pharmamobil.platform

import android.content.Context
import android.content.Intent
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor

/**
 * @param contexto resuelto de forma perezosa: el [Context] solo hace falta al
 * compartir, no al ensamblar el grafo de Koin. Asi el grafo puede montarse
 * en pruebas JVM (sin Context de Android registrado) siguiendo construyendo
 * esta clase real.
 */
class CompartidorAndroid(
    private val contexto: Lazy<Context>
) : Compartidor {

    override fun compartir(texto: String) {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        val selector = Intent.createChooser(envio, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contexto.value.startActivity(selector)
    }
}
