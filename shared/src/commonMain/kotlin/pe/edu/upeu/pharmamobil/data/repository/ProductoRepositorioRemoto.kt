package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.dto.aDominio
import pe.edu.upeu.pharmamobil.data.remote.dto.aRequest
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Inventario en PharmaSoft. Los errores ya llegan tipificados desde ProductoApi. */
class ProductoRepositorioRemoto(
    private val api: ProductoApi
) : ProductoRepository {

    override suspend fun listar(): List<Producto> = api.listar().contenido.map { it.aDominio() }

    override suspend fun obtener(id: Long): Producto = api.obtener(id).aDominio()

    override suspend fun registrar(datos: DatosProducto): Producto = api.crear(datos.aRequest()).aDominio()

    override suspend fun actualizar(id: Long, datos: DatosProducto): Producto =
        api.actualizar(id, datos.aRequest()).aDominio()

    override suspend fun eliminar(id: Long) = api.eliminar(id)

    override suspend fun listarCategorias(): List<Categoria> =
        api.listarCategorias().filter { it.estado }.map { it.aDominio() }
}
