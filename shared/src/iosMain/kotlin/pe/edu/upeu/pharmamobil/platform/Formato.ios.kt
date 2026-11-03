package pe.edu.upeu.pharmamobil.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle

actual fun formatearSoles(valor: Double): String {
    val formateador = NSNumberFormatter()
    formateador.numberStyle = NSNumberFormatterCurrencyStyle
    formateador.locale = NSLocale("es_PE")
    return formateador.stringFromNumber(NSNumber(valor))
        // NSNumberFormatter (ICU) puede usar espacio no separable (U+00A0)
        // entre "S/" y el numero; se normaliza a espacio simple para que el
        // texto sea identico en Android, iOS y en los tests.
        ?.replace(' ', ' ')
        ?.replace(' ', ' ')
        ?: "S/ $valor"
}
