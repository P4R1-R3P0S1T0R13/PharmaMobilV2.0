package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.mensajeDeRed
import pe.edu.upeu.pharmamobil.data.remote.dto.aDominio
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/** Inventario leido desde PharmaSoft. Reemplaza al repositorio en memoria. */
class ProductoRepositorioRemoto(
    private val api: ProductoApi
) : ProductoRepository {

    override suspend fun listar(): List<Producto> = try {
        api.listar().contenido.map { it.aDominio() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw IllegalStateException(mensajeDeRed(e), e)
    }

    override suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto =
        throw UnsupportedOperationException("El registro remoto (POST) se implementa en la Sesion 8")
}
