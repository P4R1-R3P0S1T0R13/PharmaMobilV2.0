package pe.edu.upeu.pharmamobil.domain.repository

import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.pharmamobil.domain.model.EstadoSesion
import pe.edu.upeu.pharmamobil.domain.model.Sesion

interface AuthRepository {

    val estado: StateFlow<EstadoSesion>

    /** Lanza ErrorApiException(NoAutorizado) si las credenciales son incorrectas. */
    suspend fun iniciarSesion(usuario: String, clave: String): Sesion

    /** Borra el token del almacenamiento seguro y de memoria. */
    fun cerrarSesion()
}

interface ReporteRepository {
    /** Requiere REPORTES_VER (solo ADMIN). Devuelve cuantos productos trae el reporte. */
    suspend fun productosMasVendidos(): Int
}
