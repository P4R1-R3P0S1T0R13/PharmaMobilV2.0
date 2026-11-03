package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.pharmamobil.data.auth.GestorSesion
import pe.edu.upeu.pharmamobil.data.remote.AuthApi
import pe.edu.upeu.pharmamobil.data.remote.LoginRequestDto
import pe.edu.upeu.pharmamobil.data.remote.ReporteApi
import pe.edu.upeu.pharmamobil.domain.model.EstadoSesion
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ReporteRepository

class AuthRepositorio(
    private val api: AuthApi,
    private val gestor: GestorSesion,
    private val ahoraMs: () -> Long
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
        gestor.iniciar(sesion)
        return sesion
    }

    override fun cerrarSesion() = gestor.cerrar()
}

class ReporteRepositorio(private val api: ReporteApi) : ReporteRepository {
    override suspend fun productosMasVendidos(): Int = api.productosMasVendidos().size
}
