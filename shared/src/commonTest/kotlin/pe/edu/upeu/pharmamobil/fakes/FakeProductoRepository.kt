package pe.edu.upeu.pharmamobil.fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Implementacion falsa del contrato offline-first para probar el ViewModel:
 * "local" es un StateFlow y "servidor" la lista que baja al sincronizar.
 */
class FakeProductoRepository(
    private val delServidor: List<Producto> = emptyList(),
    var errorAlSincronizar: ErrorApi? = null,
    var errorAlGuardar: ErrorApi? = null,
    var errorAlEliminar: ErrorApi? = null
) : ProductoRepository {

    val local = MutableStateFlow<List<Producto>>(emptyList())
    private val ultima = MutableStateFlow<Long?>(null)
    private val categorias = MutableStateFlow<List<Categoria>>(emptyList())
    var llamadasGuardar = 0
        private set

    override fun observar(): Flow<List<Producto>> = local
    override fun observarCategorias(): Flow<List<Categoria>> = categorias
    override fun observarUltimaSincronizacion(): Flow<Long?> = ultima

    override suspend fun sincronizar() {
        errorAlSincronizar?.let { throw ErrorApiException(it) }
        local.value = local.value.filter { it.pendiente } + delServidor
        categorias.value = CATEGORIAS
        ultima.value = 1_000L
    }

    override suspend fun registrar(datos: DatosProducto): Producto {
        llamadasGuardar++
        errorAlGuardar?.let { throw ErrorApiException(it) }
        val sinRed = errorAlSincronizar == ErrorApi.SinConexion
        val id = if (sinRed) -(local.value.size + 1L) else (local.value.maxOfOrNull { it.id } ?: 0) + 1
        val producto = datos.aProducto(id).copy(pendiente = sinRed)
        local.value = local.value + producto
        return producto
    }

    override suspend fun actualizar(id: Long, datos: DatosProducto): Producto {
        llamadasGuardar++
        errorAlGuardar?.let { throw ErrorApiException(it) }
        val producto = datos.aProducto(id)
        local.value = local.value.map { if (it.id == id) producto else it }
        return producto
    }

    override suspend fun eliminar(id: Long) {
        errorAlEliminar?.let { throw ErrorApiException(it) }
        local.value = local.value.filterNot { it.id == id }
    }

    private fun DatosProducto.aProducto(id: Long) =
        Producto(id, nombre, precio, stock, activo, categoriaId, CATEGORIAS.firstOrNull { it.id == categoriaId }?.nombre)

    companion object {
        val CATEGORIAS = listOf(Categoria(1, "Analgesicos"), Categoria(2, "Antibioticos"))
        val tresProductos = listOf(
            Producto(1, "Paracetamol", 15.50, 100, categoriaId = 1),
            Producto(2, "Ibuprofeno", 18.90, 50, categoriaId = 1),
            Producto(3, "Amoxicilina", 25.00, 5, categoriaId = 2)
        )
    }
}
