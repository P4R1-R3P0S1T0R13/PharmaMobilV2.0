package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementacion temporal del inventario (Unidad 1). El delay hace visible
 * el estado de carga en la pantalla.
 */
class ProductoRepositorioEnMemoria(
    private val latenciaMs: Long = 500
) : ProductoRepository {

    private val productos = mutableListOf(
        Producto(1, "Paracetamol", 15.50, 100),
        Producto(2, "Ibuprofeno", 18.90, 50),
        Producto(3, "Amoxicilina", 25.00, 5),
        Producto(4, "Loratadina", 12.50, 0, activo = false),
        Producto(5, "Diclofenaco", 20.00, 3)
    )

    private var siguienteId = 6L

    override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto {
        delay(latenciaMs)
        val producto = Producto(siguienteId++, nombre, precio, stock)
        productos += producto
        return producto
    }

    override suspend fun listar(): List<Producto> {
        delay(latenciaMs)
        return productos.toList()
    }
}
