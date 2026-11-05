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
        // Sesion 11 (offline-first): un token vencido NO cierra la sesion al abrir
        // la app. Sin red el usuario sigue viendo sus datos locales; al
        // reconectar, el servidor responde 401 y recien ahi se pide el login.
        _estado.value = if (guardada == null) EstadoSesion.Inactiva() else EstadoSesion.Activa(guardada)
    }

    /** Ultimo usuario que inicio sesion en este dispositivo (para limpiar datos al cambiar de usuario). */
    fun ultimoUsuario(): String? = almacen.leer(CLAVE_ULTIMO_USUARIO)

    fun iniciar(sesion: Sesion) {
        almacen.guardar(CLAVE_ULTIMO_USUARIO, sesion.username)
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

    /**
     * Token de la sesion activa, aunque haya vencido: quien decide si sigue
     * siendo valido es el servidor (401). Asi, sin red, la app no expulsa al
     * usuario de datos que ya tiene en el dispositivo.
     */
    fun tokenActual(): String? = (estado.value as? EstadoSesion.Activa)?.sesion?.token

    /** true si el token ya paso su hora de expiracion (solo informativo). */
    fun tokenVencido(): Boolean =
        (estado.value as? EstadoSesion.Activa)?.sesion?.expirada(ahoraMs()) == true

    /** Escenario 3 de la bitacora: altera un caracter de la firma del token guardado. */
    fun alterarTokenParaPrueba() {
        val sesion = (estado.value as? EstadoSesion.Activa)?.sesion ?: return
        // Se cambia un caracter interior de la firma: el ultimo puede llevar solo bits de relleno.
        val posicion = (sesion.token.length - 10).coerceAtLeast(0)
        val original = sesion.token[posicion]
        val alterado = sesion.token.substring(0, posicion) + (if (original == 'A') 'B' else 'A') +
            sesion.token.substring(posicion + 1)
        iniciar(sesion.copy(token = alterado))
    }

    /** Cierre por el usuario: sin motivo que mostrar. */
    fun cerrar() = terminar(null)

    /** El servidor rechazo el token (401): vencido, alterado o usuario deshabilitado. */
    fun rechazadoPorServidor() = terminar(MENSAJE_RECHAZADA)

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
        const val CLAVE_ULTIMO_USUARIO = "ultimo_usuario"
        const val MENSAJE_EXPIRADA = "Tu sesion expiro. Inicia sesion nuevamente."
        const val MENSAJE_RECHAZADA =
            "El servidor ya no acepta tu sesion (401: token vencido, alterado o usuario deshabilitado). Inicia sesion nuevamente."
    }
}
