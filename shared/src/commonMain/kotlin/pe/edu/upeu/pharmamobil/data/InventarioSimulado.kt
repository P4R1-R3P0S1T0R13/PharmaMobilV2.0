package pe.edu.upeu.pharmamobil.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.pharmamobil.domain.model.Producto

/** Resultado de una carga asincrona: un estado a la vez. */
sealed class ResultadoProductos {
    data object Cargando : ResultadoProductos()
    data class Exito(val productos: List<Producto>) : ResultadoProductos()
    data class Error(val mensaje: String) : ResultadoProductos()
}

/**
 * Fuente temporal de productos (Sesion 2). Simula la latencia de red con
 * delay() hasta que exista un backend real.
 */
class InventarioSimulado(
    private val latenciaMs: Long = 1000,
    private val fallar: Boolean = false
) {
    private val productosSimulados = listOf(
        Producto(1, "Paracetamol", 8.50, 100),
        Producto(2, "Ibuprofeno", 12.00, 50),
        Producto(3, "Amoxicilina", 18.50, 20)
    )

    /** suspend: pausa la corrutina sin bloquear el hilo y devuelve un unico valor. */
    suspend fun obtenerProductos(): List<Producto> {
        delay(latenciaMs)
        if (fallar) error("Sin conexion con el inventario")
        return productosSimulados
    }

    /** Flow: emite varios valores a lo largo del tiempo. */
    fun observarProductos(): Flow<List<Producto>> = flow {
        emit(emptyList())
        delay(latenciaMs)
        emit(productosSimulados)
        delay(latenciaMs)
        // Simula una venta: copy() crea un producto nuevo con menos stock.
        emit(productosSimulados.map { if (it.id == 1L) it.copy(stock = it.stock - 10) else it })
    }

    /** Flujo integrado de estados: Cargando -> Exito | Error. */
    fun cargarProductos(): Flow<ResultadoProductos> = flow {
        emit(ResultadoProductos.Cargando)
        try {
            emit(ResultadoProductos.Exito(obtenerProductos()))
        } catch (e: IllegalStateException) {
            emit(ResultadoProductos.Error(e.message ?: "Error desconocido"))
        }
    }
}
