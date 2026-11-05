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
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase

/**
 * Offline-first: la base local es la fuente de verdad. La sincronizacion
 * (subir lo pendiente y luego bajar) la resuelve SincronizarUseCase.
 */
class ProductoRepositorioOffline(
    private val local: ProductoLocal,
    private val remoto: ProductoApi,
    private val sincronizarUseCase: SincronizarUseCase
) : ProductoRepository {

    override fun observar(): Flow<List<Producto>> = local.observar()

    override fun observarCategorias(): Flow<List<Categoria>> = local.observarCategorias()

    override fun observarUltimaSincronizacion(): Flow<Long?> = local.observarUltimaSincronizacion()

    override fun observarPendientes(): Flow<Int> = local.observarPendientes()

    override fun observarFallidas(): Flow<Int> = local.observarFallidas()

    override suspend fun sincronizar(): ResumenSync = sincronizarUseCase()

    override suspend fun reintentarFallidas(): ResumenSync {
        local.reiniciarIntentos()
        return sincronizarUseCase()
    }

    override suspend fun registrar(datos: DatosProducto): Producto {
        val guardado = local.guardarPendiente(datos)
        val resumen = intentar { sincronizarUseCase() }
        // Con red se devuelve tal como quedo en PharmaSoft (id real, ya no pendiente).
        val idServidor = resumen?.confirmados?.get(guardado.id)
        return idServidor?.let { local.porId(it) } ?: local.porId(guardado.id) ?: guardado
    }

    override suspend fun actualizar(id: Long, datos: DatosProducto): Producto {
        if (id < 0) {
            // Aun no existe en el servidor: se edita la fila pendiente (y vuelve a la cola).
            val pendiente = local.pendientes().first { it.id == id }
            val editado = pendiente.copy(
                nombre = datos.nombre, precio = datos.precio, stock = datos.stock,
                activo = datos.activo, categoriaId = datos.categoriaId,
                intentos = 0, ultimoError = null
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

    /** Un fallo transitorio de red no debe tumbar la operacion local: queda pendiente. */
    private suspend fun <T> intentar(bloque: suspend () -> T): T? = try {
        bloque()
    } catch (e: ErrorApiException) {
        val transitorio = e.error == ErrorApi.SinConexion || e.error == ErrorApi.TiempoAgotado ||
            e.error is ErrorApi.Servidor
        if (!transitorio) throw e
        null
    }
}
