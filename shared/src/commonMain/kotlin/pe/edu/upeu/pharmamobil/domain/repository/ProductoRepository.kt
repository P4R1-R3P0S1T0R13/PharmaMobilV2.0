package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Contrato del inventario en el lenguaje del negocio. Los fallos llegan
 * como ErrorApiException con un ErrorApi tipificado.
 */
interface ProductoRepository {

    suspend fun listar(): List<Producto>

    suspend fun obtener(id: Long): Producto

    /** Registra un producto nuevo; el id lo asigna quien lo guarda. */
    suspend fun registrar(datos: DatosProducto): Producto

    suspend fun actualizar(id: Long, datos: DatosProducto): Producto

    suspend fun eliminar(id: Long)

    suspend fun listarCategorias(): List<Categoria>
}
