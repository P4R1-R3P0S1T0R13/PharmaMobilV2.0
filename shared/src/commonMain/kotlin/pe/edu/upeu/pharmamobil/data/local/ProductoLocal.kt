package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.db.Producto as ProductoFila

/** Cada plataforma abre el archivo de base de datos con su propio driver. */
interface CreadorDeDriver {
    fun crear(): SqlDriver
}

/**
 * Capa local: concentra las consultas de SQLDelight y traduce entre filas y
 * dominio. No conoce Ktor ni los DTO del backend.
 */
class ProductoLocal(
    base: PharmaDb,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val productos = base.productoQueries
    private val categorias = base.categoriaQueries
    private val sincronizacion = base.sincronizacionQueries

    fun observar(): Flow<List<Producto>> =
        productos.listarTodos().asFlow().mapToList(dispatcher).map { filas -> filas.map { it.aDominio() } }

    fun observarCategorias(): Flow<List<Categoria>> =
        categorias.listarCategorias().asFlow().mapToList(dispatcher)
            .map { filas -> filas.map { Categoria(it.id, it.nombre) } }

    fun observarUltimaSincronizacion(): Flow<Long?> =
        sincronizacion.ultima().asFlow().mapToOneOrNull(dispatcher)

    fun pendientes(): List<Producto> = productos.listarPendientes().executeAsList().map { it.aDominio() }

    fun cantidad(): Long = productos.contar().executeAsOne()

    /** Guarda lo creado sin conexion con un id local negativo, que el servidor nunca usa. */
    fun guardarPendiente(datos: DatosProducto): Producto {
        val menor = productos.siguienteIdLocal().executeAsOne()
        val nombreCategoria = categorias.listarCategorias().executeAsList()
            .firstOrNull { it.id == datos.categoriaId }?.nombre
        val local = Producto(
            id = minOf(menor, 0L) - 1L,
            nombre = datos.nombre,
            precio = datos.precio,
            stock = datos.stock,
            activo = datos.activo,
            categoriaId = datos.categoriaId,
            categoriaNombre = nombreCategoria,
            pendiente = true
        )
        insertar(local)
        return local
    }

    fun guardar(producto: Producto) = insertar(producto)

    fun eliminar(id: Long) = productos.eliminarPorId(id)

    /**
     * Reemplaza lo que vino del servidor sin tocar lo pendiente. Va en una
     * transaccion para que el Flow emita una sola vez y la lista no parpadee.
     */
    fun reemplazarSincronizados(delServidor: List<Producto>, categoriasDelServidor: List<Categoria>, ahoraMs: Long) {
        productos.transaction {
            productos.borrarSincronizados()
            delServidor.forEach { insertar(it.copy(pendiente = false)) }
            categorias.borrarCategorias()
            categoriasDelServidor.forEach { categorias.insertarCategoria(it.id, it.nombre) }
            sincronizacion.guardarUltima(ahoraMs)
        }
    }

    private fun insertar(p: Producto) = productos.insertar(
        id = p.id,
        nombre = p.nombre,
        precio = p.precio,
        stock = p.stock.toLong(),
        activo = if (p.activo) 1L else 0L,
        categoria_id = p.categoriaId,
        categoria_nombre = p.categoriaNombre,
        pendiente = if (p.pendiente) 1L else 0L
    )

    private fun ProductoFila.aDominio() = Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock.toInt(),
        activo = activo == 1L,
        categoriaId = categoria_id,
        categoriaNombre = categoria_nombre,
        pendiente = pendiente == 1L
    )
}
