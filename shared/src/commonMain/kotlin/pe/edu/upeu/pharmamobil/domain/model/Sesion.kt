package pe.edu.upeu.pharmamobil.domain.model

/** Permisos que PharmaSoft devuelve en el login (enum Permiso del backend). */
object Permisos {
    const val CATALOGO_LEER = "CATALOGO_LEER"
    const val CATALOGO_ESCRIBIR = "CATALOGO_ESCRIBIR"
    const val CLIENTES_LEER = "CLIENTES_LEER"
    const val CLIENTES_ESCRIBIR = "CLIENTES_ESCRIBIR"
    const val VENTAS_LEER = "VENTAS_LEER"
    const val VENTAS_REGISTRAR = "VENTAS_REGISTRAR"
    const val REPORTES_VER = "REPORTES_VER"
    const val USUARIOS_ADMINISTRAR = "USUARIOS_ADMINISTRAR"
}

/** Sesion autenticada: el token JWT y lo que el usuario puede hacer. */
data class Sesion(
    val token: String,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String>,
    val permisos: List<String>,
    /** Instante (epoch en ms) en que el token deja de ser valido. */
    val expiraEnMs: Long
) {
    fun puede(permiso: String): Boolean = permiso in permisos

    fun expirada(ahoraMs: Long): Boolean = ahoraMs >= expiraEnMs

    /** Texto de diagnostico que nunca expone el token completo. */
    override fun toString(): String =
        "Sesion(username=$username, roles=$roles, token=${token.take(10)}..., expiraEnMs=$expiraEnMs)"
}

/** Estado de la sesion que observa la app para decidir entre login y contenido. */
sealed interface EstadoSesion {
    data object Verificando : EstadoSesion
    data class Activa(val sesion: Sesion) : EstadoSesion

    /** motivo != null cuando la sesion termino sin que el usuario la cerrara (401, expiracion). */
    data class Inactiva(val motivo: String? = null) : EstadoSesion
}
