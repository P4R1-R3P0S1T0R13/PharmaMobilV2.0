package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Mensajes de error por campo; null significa que el campo es valido. */
data class ErroresProducto(
    val nombre: String? = null,
    val precio: String? = null,
    val stock: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null || precio != null || stock != null
}

class ProductoInvalidoException(val errores: ErroresProducto) :
    IllegalArgumentException("Datos del producto invalidos")

/**
 * Caso de uso que concentra las reglas de registro (antes en
 * ProductoValidator, en presentacion). Si los datos no cumplen, el
 * repositorio no se llama.
 */
class RegistrarProductoUseCase(
    private val repositorio: ProductoRepository
) {
    suspend operator fun invoke(nombre: String, precio: String, stock: String): Result<Producto> {
        val errores = validar(nombre, precio, stock)
        if (errores.hayErrores) return Result.failure(ProductoInvalidoException(errores))
        return runCatching {
            repositorio.registrar(nombre.trim(), precio.trim().toDouble(), stock.trim().toInt())
        }
    }

    companion object {
        fun validar(nombre: String, precio: String, stock: String): ErroresProducto {
            val precioNumero = precio.trim().toDoubleOrNull()
            val stockNumero = stock.trim().toIntOrNull()
            return ErroresProducto(
                nombre = if (nombre.isBlank()) "El nombre es obligatorio." else null,
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
}
