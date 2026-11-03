package pe.edu.upeu.pharmamobil.domain.usecase

import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Mensajes de error por campo; null significa que el campo es valido. */
data class ErroresProducto(
    val nombre: String? = null,
    val precio: String? = null,
    val stock: String? = null,
    val categoria: String? = null
) {
    val hayErrores: Boolean
        get() = nombre != null || precio != null || stock != null || categoria != null
}

class ProductoInvalidoException(val errores: ErroresProducto) :
    IllegalArgumentException("Datos del producto invalidos")

/**
 * Crea (id == null) o actualiza (id != null) un producto. Concentra las
 * reglas del formulario: si no se cumplen, el repositorio no se llama.
 */
class RegistrarProductoUseCase(
    private val repositorio: ProductoRepository
) {
    suspend operator fun invoke(
        id: Long?,
        nombre: String,
        precio: String,
        stock: String,
        activo: Boolean,
        categoriaId: Long?
    ): Result<Producto> {
        val errores = validar(nombre, precio, stock, categoriaId)
        if (errores.hayErrores) return Result.failure(ProductoInvalidoException(errores))
        val datos = DatosProducto(
            nombre = nombre.trim(),
            precio = precio.trim().toDouble(),
            stock = stock.trim().toInt(),
            activo = activo,
            categoriaId = categoriaId!!
        )
        return runCatchingSuspend {
            if (id == null) repositorio.registrar(datos) else repositorio.actualizar(id, datos)
        }
    }

    companion object {
        /** Mismas reglas que valida PharmaSoft (ProductoRequestDTO). */
        fun validar(nombre: String, precio: String, stock: String, categoriaId: Long?): ErroresProducto {
            val precioNumero = precio.trim().toDoubleOrNull()
            val stockNumero = stock.trim().toIntOrNull()
            return ErroresProducto(
                nombre = when {
                    nombre.isBlank() -> "El nombre es obligatorio."
                    nombre.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres."
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
                },
                categoria = if (categoriaId == null) "Seleccione una categoría." else null
            )
        }
    }
}

/** runCatching que no se traga la cancelacion de corrutinas. */
suspend fun <T> runCatchingSuspend(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
