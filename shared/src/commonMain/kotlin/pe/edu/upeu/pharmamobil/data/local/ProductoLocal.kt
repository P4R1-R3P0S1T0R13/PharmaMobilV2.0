package pe.edu.upeu.pharmamobil.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Inventario
import pe.edu.upeu.pharmamobil.domain.model.PoliticaReintentos.MAX_INTENTOS
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ColaSincronizacion
import pe.edu.upeu.pharmamobil.domain.repository.DatosLocales
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
    private val ahoraMs: () -> Long = { 0L },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ColaSincronizacion, DatosLocales {

    private val productos = base.productoQueries
    private val categorias = base.categoriaQueries
    private val sincronizacion = base.sincronizacionQueries

    // ---------- Lectura observable ----------

    fun observar(): Flow<List<Producto>> =
        productos.listarTodos().asFlow().mapToList(dispatcher).map { filas -> filas.map { it.aDominio() } }

    fun observarCategorias(): Flow<List<Categoria>> =
        categorias.listarCategorias().asFlow().mapToList(dispatcher)
            .map { filas -> filas.map { Categoria(it.id, it.nombre) } }

    fun observarUltimaSincronizacion(): Flow<Long?> =
        sincronizacion.ultima().asFlow().mapToOneOrNull(dispatcher)

    fun observarPendientes(): Flow<Int> =
        productos.contarPendientes(MAX_INTENTOS.toLong()).asFlow().mapToOne(dispatcher).map { it.toInt() }

    fun observarFallidas(): Flow<Int> =
        productos.contarFallidas(MAX_INTENTOS.toLong()).asFlow().mapToOne(dispatcher).map { it.toInt() }

    // ---------- Lectura puntual ----------

    fun pendientes(): List<Producto> = productos.listarPendientes().executeAsList().map { it.aDominio() }

    fun porId(id: Long): Producto? = productos.porId(id).executeAsOneOrNull()?.aDominio()

    fun cantidad(): Long = productos.contar().executeAsOne()

    // ---------- Escritura ----------

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

    /** Accion del usuario sobre las filas fallidas: vuelven a la cola con la cuenta en cero. */
    fun reiniciarIntentos() = productos.reiniciarIntentos()

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

    // ---------- ColaSincronizacion (Sesion 13) ----------

    override fun pendientesPorEnviar(maxIntentos: Int): List<Producto> =
        productos.pendientesPorEnviar(maxIntentos.toLong()).executeAsList().map { it.aDominio() }

    override fun confirmar(idLocal: Long) {
        productos.eliminarPorId(idLocal)
    }

    override fun sumarIntento(idLocal: Long, motivo: String) {
        productos.sumarIntento(motivo = motivo, id = idLocal)
    }

    override fun marcarFallida(idLocal: Long, motivo: String, maxIntentos: Int) {
        productos.marcarFallida(maxIntentos = maxIntentos.toLong(), motivo = motivo, id = idLocal)
    }

    override fun conciliar(inventario: Inventario) =
        reemplazarSincronizados(inventario.productos, inventario.categorias, ahoraMs())

    override fun contarPendientes(maxIntentos: Int): Int =
        productos.contarPendientes(maxIntentos.toLong()).executeAsOne().toInt()

    // ---------- DatosLocales ----------

    /** Cierre de sesion o cambio de usuario: el dispositivo queda limpio. */
    override fun borrarTodo() {
        productos.transaction {
            productos.borrarTodo()
            categorias.borrarCategorias()
            sincronizacion.borrarSincronizacion()
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
        pendiente = pendiente == 1L,
        intentos = intentos.toInt(),
        ultimoError = ultimo_error
    )
}
