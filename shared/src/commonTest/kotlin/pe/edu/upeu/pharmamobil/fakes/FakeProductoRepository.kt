package pe.edu.upeu.pharmamobil.fakes

import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Implementacion falsa del contrato: permite forzar cada estado de la pantalla. */
class FakeProductoRepository(
    productosIniciales: List<Producto> = emptyList(),
    private val fallo: Exception? = null
) : ProductoRepository {

    val productos = productosIniciales.toMutableList()
    var llamadasRegistrar = 0
        private set

    override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto {
        llamadasRegistrar++
        fallo?.let { throw it }
        val producto = Producto((productos.maxOfOrNull { it.id } ?: 0) + 1, nombre, precio, stock)
        productos += producto
        return producto
    }

    override suspend fun listar(): List<Producto> {
        fallo?.let { throw it }
        return productos.toList()
    }

    companion object {
        val tresProductos = listOf(
            Producto(1, "Paracetamol", 15.50, 100),
            Producto(2, "Ibuprofeno", 18.90, 50),
            Producto(3, "Amoxicilina", 25.00, 5)
        )
    }
}
