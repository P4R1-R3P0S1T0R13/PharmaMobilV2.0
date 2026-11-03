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

/** Escenarios de la bitacora de tokens (Sesion 10), ejecutables desde la app. */
interface DiagnosticoSesion {
    /** GET /auth/me con el token vigente. */
    suspend fun consultarMe(): String
    suspend fun peticionSinCabecera(): String
    /** Cabecera "bearer <token>" en minuscula: el servidor la ignora. */
    suspend fun peticionPrefijoMinuscula(): String
    /** Cambia un caracter del token guardado: la siguiente peticion recibe 401. */
    fun alterarToken()
}
