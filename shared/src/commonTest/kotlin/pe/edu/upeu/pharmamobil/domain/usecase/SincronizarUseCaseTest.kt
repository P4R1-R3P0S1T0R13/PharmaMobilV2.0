package pe.edu.upeu.pharmamobil.domain.usecase

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.repository.EnvioRemotoRest
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.PoliticaReintentos
import pe.edu.upeu.pharmamobil.domain.model.PoliticaReintentos.MAX_INTENTOS
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.fakes.ServidorSimulado
import pe.edu.upeu.pharmamobil.fakes.ServidorSimulado.Companion.dto
import pe.edu.upeu.pharmamobil.fakes.baseEnMemoria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Sesion 13: sincronizacion refinada con la base SQLDelight en memoria y el
 * motor simulado de Ktor. Las esperas se registran en lugar de dormir.
 */
class SincronizarUseCaseTest {

    private val ibuprofeno = DatosProducto("Ibuprofeno 400 mg", 4.5, 20, true, 1)

    private class TelemetriaEspia : Telemetria {
        val eventos = mutableListOf<Pair<String, Map<String, String>>>()
        override fun registrarError(e: Throwable, contexto: Map<String, String>) = Unit
        override fun registrarEvento(nombre: String, datos: Map<String, String>) { eventos += nombre to datos }
    }

    private class Escenario(val servidor: ServidorSimulado = ServidorSimulado()) {
        val local = ProductoLocal(baseEnMemoria(), ahoraMs = { 1_000L }, dispatcher = Dispatchers.Unconfined)
        val esperas = mutableListOf<Long>()
        val telemetria = TelemetriaEspia()
        val caso = SincronizarUseCase(local, EnvioRemotoRest(servidor.api), telemetria) { esperas += it }
        val repo = ProductoRepositorioOffline(local, servidor.api, caso)

        fun pendiente() = local.guardarPendiente(DatosProducto("Ibuprofeno 400 mg", 4.5, 20, true, 1))
        fun fila(id: Long) = assertNotNull(local.porId(id))
    }

    @Test
    fun unFalloTransitorioSumaIntentoYNoMarcaFallida() = runTest {
        val e = Escenario().apply { servidor.estadoAlCrear = HttpStatusCode.ServiceUnavailable }
        val pendiente = e.pendiente()

        e.caso()

        val fila = e.fila(pendiente.id)
        assertEquals(1, fila.intentos)
        assertTrue(fila.pendiente)
        assertFalse(fila.fallida)
        assertNotNull(fila.ultimoError)
    }

    @Test
    fun unFalloPermanenteMarcaLaFilaYNoLaReintenta() = runTest {
        val e = Escenario().apply { servidor.estadoAlCrear = HttpStatusCode.BadRequest }
        val pendiente = e.pendiente()

        val resumen = e.caso()
        e.caso()

        assertTrue(e.fila(pendiente.id).fallida)
        assertEquals(1, resumen.fallidas)
        assertEquals(1, e.servidor.peticionesCrear, "Una fila fallida no se vuelve a enviar sola")
        assertEquals(1, e.repo.observarFallidas().first())
        assertEquals(0, e.repo.observarPendientes().first())
        assertEquals("sync_fila_fallida", e.telemetria.eventos.single().first)
    }

    @Test
    fun agotarLosIntentosDejaLaFilaFallidaYVisible() = runTest {
        val e = Escenario().apply { servidor.estadoAlCrear = HttpStatusCode.ServiceUnavailable }
        val pendiente = e.pendiente()

        repeat(MAX_INTENTOS + 2) { e.caso() }

        assertTrue(e.fila(pendiente.id).fallida)
        assertEquals(MAX_INTENTOS, e.servidor.peticionesCrear)
        assertEquals(1, e.repo.observarFallidas().first())
    }

    @Test
    fun laEsperaCreceEntreIntentosYTieneTope() = runTest {
        val e = Escenario().apply { servidor.estadoAlCrear = HttpStatusCode.ServiceUnavailable }
        e.pendiente()

        repeat(MAX_INTENTOS) { e.caso() }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L), e.esperas)
        assertEquals(16_000L, PoliticaReintentos.espera(12))
    }

    @Test
    fun sinConexionNoGastaIntentosNiBaja() = runTest {
        val e = Escenario().apply { servidor.sinRed = true }
        val pendiente = e.pendiente()

        val resumen = e.caso()

        assertTrue(resumen.sinConexion)
        assertEquals(0, e.fila(pendiente.id).intentos)
        assertEquals(1, resumen.pendientes)
        assertTrue(e.esperas.isEmpty())
    }

    @Test
    fun reenvioTrasRespuestaPerdidaNoDuplica() = runTest {
        // El primer POST llego al servidor pero la respuesta se perdio: ya existe un producto identico.
        val identico = dto(7, ibuprofeno.nombre).copy(precio = ibuprofeno.precio, stock = ibuprofeno.stock)
        val e = Escenario(ServidorSimulado(listOf(identico))).apply { servidor.estadoAlCrear = HttpStatusCode.Conflict }
        val pendiente = e.pendiente()

        val resumen = e.caso()

        assertEquals(1, resumen.enviadas)
        assertEquals(7L, resumen.confirmados[pendiente.id])
        assertEquals(1, e.servidor.productos.size, "No se duplico en el servidor")
        assertFalse(e.repo.observar().first().any { it.pendiente })
    }

    @Test
    fun conflictoConDatosDistintosEsPermanente() = runTest {
        val otro = dto(7, ibuprofeno.nombre).copy(precio = 99.0)
        val e = Escenario(ServidorSimulado(listOf(otro))).apply { servidor.estadoAlCrear = HttpStatusCode.Conflict }
        val pendiente = e.pendiente()

        e.caso()

        assertTrue(e.fila(pendiente.id).fallida)
    }

    @Test
    fun reintentarAhoraDevuelveLasFallidasALaColaYLasEnvia() = runTest {
        val e = Escenario().apply { servidor.estadoAlCrear = HttpStatusCode.BadRequest }
        e.pendiente()
        e.caso()
        e.servidor.estadoAlCrear = null

        val resumen = e.repo.reintentarFallidas()

        assertEquals(1, resumen.enviadas)
        assertEquals(0, e.repo.observarFallidas().first())
        assertEquals(1, e.servidor.productos.size)
    }

    @Test
    fun laSubidaOcurreAntesDeLaBajadaYNadaSePierde() = runTest {
        val e = Escenario(ServidorSimulado(listOf(dto(1, "Paracetamol"))))
        e.pendiente()

        val resumen = e.caso()

        assertEquals(2, resumen.descargados, "Paracetamol + el pendiente ya enviado")
        val lista = e.repo.observar().first()
        assertEquals(setOf("Paracetamol", "Ibuprofeno 400 mg"), lista.map { it.nombre }.toSet())
        assertTrue(lista.none { it.pendiente })
    }

    @Test
    fun borrarTodoDejaElDispositivoLimpio() = runTest {
        val e = Escenario(ServidorSimulado(listOf(dto(1, "Paracetamol"))))
        e.caso()
        e.pendiente()

        e.local.borrarTodo()

        assertTrue(e.repo.observar().first().isEmpty())
        assertEquals(null, e.repo.observarUltimaSincronizacion().first())
    }
}
