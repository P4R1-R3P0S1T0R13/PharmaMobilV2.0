package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.dto.aDominio
import pe.edu.upeu.pharmamobil.data.remote.dto.aRequest
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository

/**
 * Offline-first: la base local es la fuente de verdad. La red sube lo
 * pendiente y luego baja el inventario del servidor.
 */
class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoApi,
    private val ahoraMs: () -> Long
) : ProductoRepository {

    override fun observar(): Flow<List<Producto>> = local.observar()

    override fun observarCategorias(): Flow<List<Categoria>> = local.observarCategorias()

    override fun observarUltimaSincronizacion(): Flow<Long?> = local.observarUltimaSincronizacion()

    override suspend fun sincronizar() {
        // 1. Primero subir: si se bajara antes, lo creado sin red se perderia.
        subirPendientes()
        // 2. Despues bajar y reemplazar solo lo que vino del servidor.
        bajar()
    }

    override suspend fun registrar(datos: DatosProducto): Producto {
        val guardado = local.guardarPendiente(datos)
        var enviados = emptyMap<Long, Producto>()
        intentar {
            enviados = subirPendientes()
            bajar()
        }
        // Con red se devuelve tal como quedo en PharmaSoft (id real, ya no pendiente).
        return enviados[guardado.id] ?: guardado
    }

    override suspend fun actualizar(id: Long, datos: DatosProducto): Producto {
        if (id < 0) {
            // Aun no existe en el servidor: se edita la fila pendiente.
            val pendiente = local.pendientes().first { it.id == id }
            val editado = pendiente.copy(
                nombre = datos.nombre, precio = datos.precio, stock = datos.stock,
                activo = datos.activo, categoriaId = datos.categoriaId
            )
            local.guardar(editado)
            return editado
        }
        val actualizado = remoto.actualizar(id, datos.aRequest()).aDominio()
        local.guardar(actualizado)
        return actualizado
    }

    override suspend fun eliminar(id: Long) {
        if (id >= 0) remoto.eliminar(id)
        local.eliminar(id)
    }

    /** Devuelve lo que el servidor confirmo, indexado por el id local negativo. */
    private suspend fun subirPendientes(): Map<Long, Producto> {
        val enviados = mutableMapOf<Long, Producto>()
        local.pendientes().forEach { pendiente ->
            val creado = remoto.crear(
                DatosProducto(
                    pendiente.nombre, pendiente.precio, pendiente.stock,
                    pendiente.activo, pendiente.categoriaId
                ).aRequest()
            )
            // Ya existe en PharmaSoft: la fila local con id negativo se retira y
            // vuelve en la bajada con su id real.
            local.eliminar(pendiente.id)
            enviados[pendiente.id] = creado.aDominio()
        }
        return enviados
    }

    private suspend fun bajar() {
        val productos = remoto.listar().contenido.map { it.aDominio() }
        val categorias = remoto.listarCategorias().filter { it.estado }.map { it.aDominio() }
        local.reemplazarSincronizados(productos, categorias, ahoraMs())
    }

    /** Un fallo de red no debe tumbar la operacion local: queda pendiente. */
    private suspend fun intentar(bloque: suspend () -> Unit) {
        try {
            bloque()
        } catch (e: ErrorApiException) {
            if (e.error != ErrorApi.SinConexion && e.error != ErrorApi.TiempoAgotado) throw e
        }
    }
}
