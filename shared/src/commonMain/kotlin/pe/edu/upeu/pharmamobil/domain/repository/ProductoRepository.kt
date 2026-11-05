package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResumenSync

/**
 * Contrato offline-first del inventario: la pantalla se suscribe a los Flow
 * de la base local y la red solo la mantiene al dia.
 */
interface ProductoRepository {

    /** La pantalla se suscribe una vez y se actualiza sola. */
    fun observar(): Flow<List<Producto>>

    fun observarCategorias(): Flow<List<Categoria>>

    /** Instante (epoch ms) de la ultima sincronizacion correcta; null si nunca se sincronizo. */
    fun observarUltimaSincronizacion(): Flow<Long?>

    /** Filas pendientes que todavia se reintentan solas (Sesion 13). */
    fun observarPendientes(): Flow<Int>

    /** Filas pendientes que agotaron sus intentos y esperan una decision del usuario. */
    fun observarFallidas(): Flow<Int>

    /** Sube lo pendiente y despues trae la lista del servidor. En ese orden. */
    suspend fun sincronizar(): ResumenSync

    /** Devuelve las filas fallidas a la cola con la cuenta de intentos en cero. */
    suspend fun reintentarFallidas(): ResumenSync

    /** Guarda en local como pendiente e intenta enviarlo si hay conexion. */
    suspend fun registrar(datos: DatosProducto): Producto

    /** Requiere conexion, salvo que el producto aun sea un pendiente local. */
    suspend fun actualizar(id: Long, datos: DatosProducto): Producto

    /** Requiere conexion, salvo que el producto aun sea un pendiente local (descartar). */
    suspend fun eliminar(id: Long)
}
