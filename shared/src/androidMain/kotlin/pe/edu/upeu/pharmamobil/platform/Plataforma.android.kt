package pe.edu.upeu.pharmamobil.platform

import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.NumberFormat
import java.util.Locale

actual fun formatearSoles(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE")).format(valor)
        // CLDR separa "S/" del numero con un espacio no separable (U+00A0 o U+202F);
        // se normaliza para que el texto sea identico en Android, iOS y en las pruebas.
        .replace('\u00A0', ' ')
        .replace('\u202F', ' ')

actual fun infoDispositivo(): InfoDispositivo = InfoDispositivo(
    sistema = "Android",
    version = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    modelo = "${Build.MANUFACTURER} ${Build.MODEL}",
    idioma = Locale.getDefault().toLanguageTag()
)

actual fun ahoraMs(): Long = System.currentTimeMillis()

/** Android: Intent ACTION_SEND con el selector del sistema. */
class CompartidorAndroid(private val context: Context) : Compartidor {
    override fun compartir(texto: String) {
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        val selector = Intent.createChooser(envio, "Compartir producto").apply {
            // Se lanza desde el Context de la aplicacion, fuera de una Activity.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(selector)
    }
}
