package pe.edu.upeu.pharmamobil.data.auth

import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import pe.edu.upeu.pharmamobil.data.remote.OmitirAutenticacion

class ConfiguracionAutenticacion {
    var gestor: GestorSesion? = null
}

/**
 * Adjunta "Authorization: Bearer <token>" a cada peticion protegida y, si el
 * servidor responde 401 a una peticion que llevaba token, termina la sesion.
 */
val AutenticacionJwt = createClientPlugin("AutenticacionJwt", ::ConfiguracionAutenticacion) {
    val gestor = requireNotNull(pluginConfig.gestor) { "Falta el GestorSesion" }

    onRequest { request, _ ->
        val esLogin = request.url.encodedPath.endsWith("auth/login")
        val omitir = request.attributes.getOrNull(OmitirAutenticacion) == true
        if (!esLogin && !omitir && !request.headers.contains(HttpHeaders.Authorization)) {
            gestor.tokenActual()?.let { request.headers.append(HttpHeaders.Authorization, "Bearer $it") }
        }
    }

    on(Send) { request ->
        val llevabaToken = request.headers.contains(HttpHeaders.Authorization) &&
            request.attributes.getOrNull(OmitirAutenticacion) != true
        fun revisar(call: HttpClientCall) {
            if (llevabaToken && call.response.status == HttpStatusCode.Unauthorized) {
                gestor.rechazadoPorServidor()
            }
        }
        try {
            proceed(request).also { revisar(it) }
        } catch (e: ClientRequestException) {
            revisar(e.response.call)
            throw e
        }
    }
}
