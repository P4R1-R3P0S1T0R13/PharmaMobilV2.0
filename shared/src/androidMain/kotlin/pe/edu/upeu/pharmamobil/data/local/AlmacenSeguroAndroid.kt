package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra cada valor con AES-256/GCM. La clave vive en Android Keystore (no se
 * puede extraer del dispositivo) y en SharedPreferences solo queda
 * "iv:textoCifrado" en Base64: el token no es legible aunque se copie el XML.
 */
class AlmacenSeguroAndroid(context: Context) : AlmacenSeguro {

    private val preferencias = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    override fun guardar(clave: String, valor: String) {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, claveSecreta())
        val cifrado = cifrador.doFinal(valor.encodeToByteArray())
        val guardado = base64(cifrador.iv) + SEPARADOR + base64(cifrado)
        preferencias.edit().putString(clave, guardado).apply()
    }

    override fun leer(clave: String): String? {
        val guardado = preferencias.getString(clave, null) ?: return null
        return try {
            val (iv, cifrado) = guardado.split(SEPARADOR).let { it[0] to it[1] }
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(Cipher.DECRYPT_MODE, claveSecreta(), GCMParameterSpec(128, desdeBase64(iv)))
            cifrador.doFinal(desdeBase64(cifrado)).decodeToString()
        } catch (e: Exception) {
            // Valor alterado o clave invalidada: se descarta en lugar de fallar.
            borrar(clave)
            null
        }
    }

    override fun borrar(clave: String) {
        preferencias.edit().remove(clave).apply()
    }

    private fun claveSecreta(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
        generador.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    private fun base64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun desdeBase64(texto: String) = Base64.decode(texto, Base64.NO_WRAP)

    private companion object {
        const val ARCHIVO = "pharmamobil_seguro"
        const val PROVEEDOR = "AndroidKeyStore"
        const val ALIAS = "pharmamobil_clave_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val SEPARADOR = ":"
    }
}
