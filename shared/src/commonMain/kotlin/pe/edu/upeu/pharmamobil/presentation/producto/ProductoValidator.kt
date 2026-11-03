package pe.edu.upeu.pharmamobil.presentation.producto

/** Mensajes de error por campo; null significa que el campo es valido. */
data class ErroresProducto(
    val nombre: String? = null,
    val precio: String? = null,
    val stock: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null || precio != null || stock != null
}

/**
 * Reglas del formulario de productos (Sesion 3). La conversion usa
 * toDoubleOrNull()/toIntOrNull() para que una entrada como "abc" produzca
 * un mensaje en lugar de una excepcion.
 */
object ProductoValidator {

    fun validar(nombre: String, precio: String, stock: String): ErroresProducto {
        val precioNumero = precio.trim().toDoubleOrNull()
        val stockNumero = stock.trim().toIntOrNull()
        return ErroresProducto(
            nombre = when {
                nombre.isBlank() -> "El nombre es obligatorio."
                else -> null
            },
            precio = when {
                precioNumero == null -> "Ingrese un precio numérico."
                precioNumero <= 0.0 -> "El precio debe ser mayor que cero."
                else -> null
            },
            stock = when {
                stockNumero == null -> "Ingrese un stock entero."
                stockNumero < 0 -> "El stock no puede ser negativo."
                else -> null
            }
        )
    }
}
