package pe.edu.upeu.pharmamobil.fakes

import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Implementacion falsa del contrato: permite forzar cada estado y cada error. */
class FakeProductoRepository(
    productosIniciales: List<Producto> = emptyList(),
    var errorAlListar: ErrorApi? = null,
    var errorAlGuardar: ErrorApi? = null,
    var errorAlEliminar: ErrorApi? = null
) : ProductoRepository {

    val productos = productosIniciales.toMutableList()
    var llamadasGuardar = 0
        private set

    override suspend fun listar(): List<Producto> {
        errorAlListar?.let { throw ErrorApiException(it) }
        return productos.toList()
    }

    override suspend fun obtener(id: Long): Producto =
        productos.firstOrNull { it.id == id } ?: throw ErrorApiException(ErrorApi.NoEncontrado("No existe $id"))

    override suspend fun registrar(datos: DatosProducto): Producto {
        llamadasGuardar++
        errorAlGuardar?.let { throw ErrorApiException(it) }
        val producto = datos.aProducto((productos.maxOfOrNull { it.id } ?: 0) + 1)
        productos += producto
        return producto
    }

    override suspend fun actualizar(id: Long, datos: DatosProducto): Producto {
        llamadasGuardar++
        errorAlGuardar?.let { throw ErrorApiException(it) }
        val producto = datos.aProducto(id)
        val indice = productos.indexOfFirst { it.id == id }
        if (indice >= 0) productos[indice] = producto
        return producto
    }

    override suspend fun eliminar(id: Long) {
        errorAlEliminar?.let { throw ErrorApiException(it) }
        productos.removeAll { it.id == id }
    }

    override suspend fun listarCategorias(): List<Categoria> = categorias

    private fun DatosProducto.aProducto(id: Long) =
        Producto(id, nombre, precio, stock, activo, categoriaId, categorias.firstOrNull { it.id == categoriaId }?.nombre)

    companion object {
        val categorias = listOf(Categoria(1, "Analgesicos"), Categoria(2, "Antibioticos"))
        val tresProductos = listOf(
            Producto(1, "Paracetamol", 15.50, 100, categoriaId = 1),
            Producto(2, "Ibuprofeno", 18.90, 50, categoriaId = 1),
            Producto(3, "Amoxicilina", 25.00, 5, categoriaId = 2)
        )
    }
}
