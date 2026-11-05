package pe.edu.upeu.pharmamobil.domain.model

/**
 * Producto del inventario. Es inmutable: para cambiar el stock se usa
 * copy(stock = ...) y se obtiene un objeto nuevo.
 *
 * categoriaId es obligatorio en PharmaSoft (productos.categoria_id NOT NULL).
 */
data class Producto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean = true,
    val categoriaId: Long = 0,
    val categoriaNombre: String? = null,
    /** true: creado sin conexion, aun no existe en PharmaSoft (id local negativo). */
    val pendiente: Boolean = false,
    /** Sesion 13: intentos de envio y ultimo motivo de fallo de una fila pendiente. */
    val intentos: Int = 0,
    val ultimoError: String? = null
) {
    init {
        require(nombre.isNotBlank()) { "El nombre del producto es obligatorio" }
        require(precio > 0.0) { "El precio debe ser mayor que cero" }
        require(stock >= 0) { "El stock no puede ser negativo" }
    }

    /** Pendiente que agoto sus intentos: ya no se envia sola, el usuario decide. */
    val fallida: Boolean
        get() = pendiente && intentos >= PoliticaReintentos.MAX_INTENTOS

    val disponible: Boolean
        get() = stock > 0

    /**
     * Regla de negocio: un producto activo con stock <= STOCK_MINIMO necesita
     * reposicion. Un producto inactivo nunca se marca, aunque tenga stock 0,
     * porque ya no se vende.
     */
    val requiereReposicion: Boolean
        get() = activo && stock <= STOCK_MINIMO

    companion object {
        const val STOCK_MINIMO = 5
    }
}
