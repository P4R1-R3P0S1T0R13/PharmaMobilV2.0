package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.pharmamobil.data.auth.GestorSesion
import pe.edu.upeu.pharmamobil.data.remote.AuthApi
import pe.edu.upeu.pharmamobil.data.remote.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.ReporteApi
import pe.edu.upeu.pharmamobil.domain.model.EstadoSesion
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.DatosLocales
import pe.edu.upeu.pharmamobil.domain.repository.ReporteRepository

class AuthRepositorio(
    private val api: AuthApi,
    private val gestor: GestorSesion,
    private val ahoraMs: () -> Long,
    private val datosLocales: DatosLocales
) : AuthRepository {

    override val estado: StateFlow<EstadoSesion> = gestor.estado

    override suspend fun iniciarSesion(usuario: String, clave: String): Sesion {
        val respuesta = api.login(LoginRequestDto(usuario.trim(), clave))
        val sesion = Sesion(
            token = respuesta.token,
            username = respuesta.username,
            nombreCompleto = respuesta.nombreCompleto,
            roles = respuesta.roles,
            permisos = respuesta.permisos,
            expiraEnMs = ahoraMs() + respuesta.expiraEnSegundos * 1000
        )
        // Sesion 13 (H5): si entra otro usuario, no debe ver ni enviar los datos del anterior.
        val anterior = gestor.ultimoUsuario()
        if (anterior != null && !anterior.equals(sesion.username, ignoreCase = true)) datosLocales.borrarTodo()
        gestor.iniciar(sesion)
        return sesion
    }

    /** Cierre explicito: se borra el token y tambien la base local del dispositivo. */
    override fun cerrarSesion() {
        datosLocales.borrarTodo()
        gestor.cerrar()
    }
}

class ReporteRepositorio(private val api: ReporteApi) : ReporteRepository {
    override suspend fun productosMasVendidos(): Int = api.productosMasVendidos().size
}
