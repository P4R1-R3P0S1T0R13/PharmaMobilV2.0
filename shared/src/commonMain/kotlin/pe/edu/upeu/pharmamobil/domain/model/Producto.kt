package pe.edu.upeu.pharmamobil.domain.model

/**
 * Producto del inventario. Es inmutable: para cambiar el stock se usa
 * copy(stock = ...) y se obtiene un objeto nuevo.
 */
data class Producto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int
) {
    init {
        require(nombre.isNotBlank()) { "El nombre del producto es obligatorio" }
        require(precio > 0.0) { "El precio debe ser mayor que cero" }
        require(stock >= 0) { "El stock no puede ser negativo" }
    }

    val disponible: Boolean
        get() = stock > 0
}
