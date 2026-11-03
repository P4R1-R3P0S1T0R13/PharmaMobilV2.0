package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Contrato offline-first del inventario (Sesion 11): la pantalla se suscribe
 * a observar() y la red solo mantiene la base local al dia.
 */
interface ProductoRepository {

    /** La pantalla se suscribe una vez y se actualiza sola. */
    fun observar(): Flow<List<Producto>>

    fun observarCategorias(): Flow<List<Categoria>>

    /** Instante (epoch ms) de la ultima sincronizacion correcta; null si nunca se sincronizo. */
    fun observarUltimaSincronizacion(): Flow<Long?>

    /** Sube lo pendiente y despues trae la lista del servidor. En ese orden. */
    suspend fun sincronizar()

    /** Guarda en local como pendiente e intenta enviarlo si hay conexion. */
    suspend fun registrar(datos: DatosProducto): Producto

    /** Requiere conexion, salvo que el producto aun sea un pendiente local. */
    suspend fun actualizar(id: Long, datos: DatosProducto): Producto

    /** Requiere conexion, salvo que el producto aun sea un pendiente local. */
    suspend fun eliminar(id: Long)
}
