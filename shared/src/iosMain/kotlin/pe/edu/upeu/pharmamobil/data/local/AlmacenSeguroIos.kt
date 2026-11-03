package pe.edu.upeu.pharmamobil.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Keychain de iOS: el sistema cifra el valor y lo liga a este dispositivo
 * (AfterFirstUnlockThisDeviceOnly). Codigo escrito sin compilar en macOS.
 */
@OptIn(ExperimentalForeignApi::class)
class AlmacenSeguroIos : AlmacenSeguro {

    override fun guardar(clave: String, valor: String) {
        borrar(clave)
        @Suppress("CAST_NEVER_SUCCEEDS")
        val datos = (valor as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val consulta = consultaBase(clave)
        CFDictionaryAddValue(consulta, kSecValueData, CFBridgingRetain(datos))
        CFDictionaryAddValue(consulta, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        SecItemAdd(consulta, null)
    }

    override fun leer(clave: String): String? = memScoped {
        val consulta = consultaBase(clave)
        CFDictionaryAddValue(consulta, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(consulta, kSecMatchLimit, kSecMatchLimitOne)
        val resultado = alloc<CFTypeRefVar>()
        if (SecItemCopyMatching(consulta, resultado.ptr) != errSecSuccess) return@memScoped null
        val datos = CFBridgingRelease(resultado.value) as? NSData ?: return@memScoped null
        NSString.create(data = datos, encoding = NSUTF8StringEncoding)?.toString()
    }

    override fun borrar(clave: String) {
        SecItemDelete(consultaBase(clave))
    }

    private fun consultaBase(clave: String): CFMutableDictionaryRef? {
        val consulta = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionaryAddValue(consulta, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(consulta, kSecAttrService, CFBridgingRetain(SERVICIO))
        CFDictionaryAddValue(consulta, kSecAttrAccount, CFBridgingRetain(clave))
        return consulta
    }

    private companion object {
        const val SERVICIO = "pe.edu.upeu.pharmamobil"
    }
}
