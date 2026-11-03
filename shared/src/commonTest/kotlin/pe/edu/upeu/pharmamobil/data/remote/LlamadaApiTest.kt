package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** Traduccion de respuestas HTTP a ErrorApi con un servidor simulado (MockEngine). */
class LlamadaApiTest {

    private fun api(status: HttpStatusCode, cuerpo: String): ProductoApi {
        val engine = MockEngine {
            respond(
                content = ByteReadChannel(cuerpo),
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        return ProductoApi(crearHttpClient(engine, ConfiguracionApi("http://localhost:8080/")))
    }

    private suspend fun errorDe(status: HttpStatusCode, cuerpo: String = "{}"): ErrorApi =
        assertFailsWith<ErrorApiException> { api(status, cuerpo).obtener(1) }.error

    @Test
    fun error400TraeLosCamposDelServidor() = runTest {
        val error = errorDe(
            HttpStatusCode.BadRequest,
            """{"status":400,"message":"Existen errores de validación",
               "validationErrors":{"precio":"El precio debe ser mayor que cero"}}"""
        )
        val validacion = assertIs<ErrorApi.Validacion>(error)
        assertEquals("El precio debe ser mayor que cero", validacion.campos["precio"])
    }

    @Test
    fun codigosSeTraducenAlTipoCorrecto() = runTest {
        assertIs<ErrorApi.NoAutorizado>(errorDe(HttpStatusCode.Unauthorized))
        assertIs<ErrorApi.Prohibido>(errorDe(HttpStatusCode.Forbidden))
        assertIs<ErrorApi.NoEncontrado>(errorDe(HttpStatusCode.NotFound))
        assertIs<ErrorApi.Conflicto>(errorDe(HttpStatusCode.Conflict))
        assertEquals(ErrorApi.Servidor(500), errorDe(HttpStatusCode.InternalServerError))
    }

    @Test
    fun cuerpoMalFormadoEsRespuestaInvalida() = runTest {
        assertEquals(ErrorApi.RespuestaInvalida, errorDe(HttpStatusCode.OK, """{"id": "no-es-numero"}"""))
    }
}
