package pe.edu.upeu.pharmamobil.platform

import java.text.NumberFormat
import java.util.Locale

actual fun formatearSoles(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "PE"))
        .format(valor)
        // CLDR (JDK 16+) separa "S/" del numero con un espacio no separable
        // (U+00A0 o U+202F); se normaliza a espacio simple para que el texto
        // sea identico en Android, iOS y en los tests.
        .replace(' ', ' ')
        .replace(' ', ' ')
