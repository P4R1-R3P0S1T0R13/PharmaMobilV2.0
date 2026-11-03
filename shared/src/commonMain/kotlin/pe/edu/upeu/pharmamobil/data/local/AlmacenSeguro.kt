package pe.edu.upeu.pharmamobil.data.local

/**
 * Almacenamiento cifrado de valores pequenos (el token). Cada plataforma
 * usa su mecanismo nativo:
 *  - Android: clave AES-256 en Android Keystore + SharedPreferences cifradas con AES/GCM.
 *  - iOS: Keychain (kSecClassGenericPassword).
 */
interface AlmacenSeguro {
    fun guardar(clave: String, valor: String)
    fun leer(clave: String): String?
    fun borrar(clave: String)
}

/** Implementacion en memoria para pruebas en commonTest. */
class AlmacenEnMemoria : AlmacenSeguro {
    val valores = mutableMapOf<String, String>()
    override fun guardar(clave: String, valor: String) { valores[clave] = valor }
    override fun leer(clave: String): String? = valores[clave]
    override fun borrar(clave: String) { valores.remove(clave) }
}
