package pe.edu.upeu.pharmamobil.data.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.AlmacenEnMemoria
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.EstadoSesion
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Escenarios de token de la Sesion 10, automatizados con reloj y servidor simulados. */
class SesionJwtTest {

    private var ahora = 1_000_000L
    private val almacen = AlmacenEnMemoria()
    private fun gestor() = GestorSesion(almacen) { ahora }

    private val sesion = Sesion(
        token = "eyJhbGciOiJIUzUxMiJ9.prueba.firma",
        username = "admin",
        nombreCompleto = "Carlos Ramirez Torres",
        roles = listOf("ADMIN"),
        permisos = listOf("CATALOGO_LEER", "CATALOGO_ESCRIBIR"),
        expiraEnMs = 1_000_000L + 60_000
    )

    @Test
    fun sinTokenGuardadoLaSesionEstaInactiva() {
        assertEquals(EstadoSesion.Inactiva(), gestor().estado.value)
    }

    @Test
    fun tokenVigenteSeRecuperaAlReiniciarLaApp() {
        gestor().iniciar(sesion)
        val reiniciado = gestor()
        assertEquals("admin", assertIs<EstadoSesion.Activa>(reiniciado.estado.value).sesion.username)
    }

    @Test
    fun tokenExpiradoNoCierraLaSesionHastaQueElServidorLoRechace() {
        // Sesion 11: sin red, el usuario sigue viendo sus datos locales.
        gestor().iniciar(sesion)
        ahora += 61_000
        val reiniciado = gestor()
        assertIs<EstadoSesion.Activa>(reiniciado.estado.value)
        assertTrue(reiniciado.tokenVencido())
        assertEquals(sesion.token, reiniciado.tokenActual())
    }

    @Test
    fun valorAlteradoEnElAlmacenSeDescarta() {
        almacen.guardar(GestorSesion.CLAVE, "{esto no es json")
        assertEquals(EstadoSesion.Inactiva(), gestor().estado.value)
        assertNull(almacen.leer(GestorSesion.CLAVE))
    }

    @Test
    fun cerrarSesionEliminaElTokenPorCompleto() {
        val g = gestor()
        g.iniciar(sesion)
        g.cerrar()
        assertNull(almacen.leer(GestorSesion.CLAVE))
        assertNull(g.tokenActual())
        assertEquals(EstadoSesion.Inactiva(null), g.estado.value)
    }

    @Test
    fun elPluginAdjuntaElTokenYUn401TerminaLaSesion() = runTest {
        val g = gestor()
        g.iniciar(sesion)
        var cabecera: String? = null
        val engine = MockEngine { request ->
            cabecera = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"status":401,"message":"El token ha expirado. Inicie sesión nuevamente"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val api = ProductoApi(crearHttpClient(engine, ConfiguracionApi("http://localhost:8080/"), g))

        assertFailsWith<ErrorApiException> { api.listar() }

        assertEquals("Bearer ${sesion.token}", cabecera)
        assertEquals(EstadoSesion.Inactiva(GestorSesion.MENSAJE_RECHAZADA), g.estado.value)
        assertNull(almacen.leer(GestorSesion.CLAVE))
    }
}
