package pe.edu.upeu.pharmamobil.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.remote.JsonPharma
import pe.edu.upeu.pharmamobil.domain.model.EstadoSesion
import pe.edu.upeu.pharmamobil.domain.model.Sesion

@Serializable
private data class SesionGuardada(
    val token: String,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String>,
    val permisos: List<String>,
    val expiraEnMs: Long
)

/**
 * Fuente unica de la sesion: la conserva cifrada en AlmacenSeguro, entrega
 * el token al cliente HTTP y la termina ante un 401 o al vencer.
 */
class GestorSesion(
    private val almacen: AlmacenSeguro,
    private val ahoraMs: () -> Long
) {
    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Verificando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    init {
        val guardada = leerGuardada()
        _estado.value = when {
            guardada == null -> EstadoSesion.Inactiva()
            guardada.expirada(ahoraMs()) -> {
                almacen.borrar(CLAVE)
                EstadoSesion.Inactiva(MENSAJE_EXPIRADA)
            }
            else -> EstadoSesion.Activa(guardada)
        }
    }

    fun iniciar(sesion: Sesion) {
        almacen.guardar(
            CLAVE,
            JsonPharma.encodeToString(
                SesionGuardada.serializer(),
                SesionGuardada(sesion.token, sesion.username, sesion.nombreCompleto,
                    sesion.roles, sesion.permisos, sesion.expiraEnMs)
            )
        )
        _estado.value = EstadoSesion.Activa(sesion)
    }

    /** Token vigente o null. Si ya vencio localmente, termina la sesion. */
    fun tokenVigente(): String? {
        val sesion = (estado.value as? EstadoSesion.Activa)?.sesion ?: return null
        if (sesion.expirada(ahoraMs())) {
            terminar(MENSAJE_EXPIRADA)
            return null
        }
        return sesion.token
    }

    /** Cierre por el usuario: sin motivo que mostrar. */
    fun cerrar() = terminar(null)

    /** El servidor rechazo el token (401): vencido, alterado o usuario deshabilitado. */
    fun rechazadoPorServidor(mensaje: String?) =
        terminar(mensaje ?: MENSAJE_EXPIRADA)

    private fun terminar(motivo: String?) {
        almacen.borrar(CLAVE)
        _estado.value = EstadoSesion.Inactiva(motivo)
    }

    private fun leerGuardada(): Sesion? {
        val json = almacen.leer(CLAVE) ?: return null
        return runCatching {
            val g = JsonPharma.decodeFromString(SesionGuardada.serializer(), json)
            Sesion(g.token, g.username, g.nombreCompleto, g.roles, g.permisos, g.expiraEnMs)
        }.getOrElse {
            // Valor corrupto o manipulado: se descarta.
            almacen.borrar(CLAVE)
            null
        }
    }

    companion object {
        const val CLAVE = "sesion_jwt"
        const val MENSAJE_EXPIRADA = "Tu sesion expiro. Inicia sesion nuevamente."
    }
}
