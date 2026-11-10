package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.NSDate
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

actual fun formatearSoles(valor: Double): String {
    val formateador = NSNumberFormatter()
    formateador.numberStyle = NSNumberFormatterCurrencyStyle
    formateador.locale = NSLocale(localeIdentifier = "es_PE")
    return formateador.stringFromNumber(NSNumber(double = valor))
        ?.replace('\u00A0', ' ')
        ?.replace('\u202F', ' ')
        ?: "S/ $valor"
}

actual fun infoDispositivo(): InfoDispositivo = InfoDispositivo(
    sistema = UIDevice.currentDevice.systemName(),
    version = UIDevice.currentDevice.systemVersion,
    modelo = UIDevice.currentDevice.model,
    idioma = NSLocale.currentLocale.languageCode
)

actual fun ahoraMs(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun formatearHora(epochMs: Long): String {
    val formateador = platform.Foundation.NSDateFormatter()
    formateador.dateFormat = "HH:mm"
    // initWithTimeIntervalSinceReferenceDate cuenta desde 2001-01-01 (978307200 s despues de 1970).
    return formateador.stringFromDate(NSDate(timeIntervalSinceReferenceDate = epochMs / 1000.0 - 978_307_200.0))
}

/** iOS: UIActivityViewController (hoja de compartir) sobre la ventana actual. */
class CompartidorIos : Compartidor {
    override fun compartir(texto: String) {
        val hoja = UIActivityViewController(activityItems = listOf(texto), applicationActivities = null)
        UIApplication.sharedApplication.keyWindow?.rootViewController
            ?.presentViewController(hoja, animated = true, completion = null)
    }
}
