package pe.edu.upeu.pharmamobil.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.dto.aDominio
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.fakes.ServidorSimulado
import pe.edu.upeu.pharmamobil.fakes.ServidorSimulado.Companion.dto
import pe.edu.upeu.pharmamobil.fakes.baseEnMemoria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Suite de la Actividad autonoma 11: base SQLDelight en memoria + servidor
 * simulado con MockEngine. Sin emulador, sin red y sin esperas.
 */
class ProductoRepositorioOfflineTest {

    private val ibuprofeno = DatosProducto("Ibuprofeno 400 mg", 4.5, 20, true, 1)

    private class Escenario(val servidor: ServidorSimulado) {
        val local = ProductoLocal(baseEnMemoria(), Dispatchers.Unconfined)
        val repo = ProductoRepositorioOffline(local, servidor.api) { 1_000L }
    }

    @Test
    fun p1SincronizacionConRedLlenaLaBaseYElFlowEmiteTodo() = runTest {
        val e = Escenario(ServidorSimulado(listOf(dto(1, "Paracetamol"), dto(2, "Amoxicilina"))))
        e.repo.sincronizar()
        assertEquals(2, e.repo.observar().first().size)
        assertEquals(1_000L, e.repo.observarUltimaSincronizacion().first())
        assertEquals(2, e.repo.observarCategorias().first().size)
    }

    @Test
    fun p2RegistroSinRedQuedaPendienteYElFlowLoEmite() = runTest {
        val e = Escenario(ServidorSimulado().apply { sinRed = true })
        val guardado = e.repo.registrar(ibuprofeno)
        val lista = e.repo.observar().first()
        assertEquals(1, lista.size)
        assertTrue(lista.single().pendiente)
        assertTrue(guardado.id < 0, "El id local debe ser negativo")
        assertNull(e.repo.observarUltimaSincronizacion().first())
    }

    @Test
    fun p3AlReconectarSeEnviaLoPendienteYDejaDeEstarMarcado() = runTest {
        val servidor = ServidorSimulado().apply { sinRed = true }
        val e = Escenario(servidor)
        e.repo.registrar(ibuprofeno)
        servidor.sinRed = false
        e.repo.sincronizar()
        val lista = e.repo.observar().first()
        assertEquals(1, servidor.productos.size)
        assertEquals(1, lista.size)
        assertFalse(lista.single().pendiente)
        assertTrue(lista.single().id > 0, "Debe tener el id real del servidor")
    }

    @Test
    fun p4LaBajadaDelServidorNoBorraLoPendiente() = runTest {
        val servidor = ServidorSimulado(listOf(dto(1, "Paracetamol")))
        val e = Escenario(servidor)
        e.local.guardarPendiente(ibuprofeno)
        // Solo la bajada (sin subir): asi se aisla la regla de borrarSincronizados.
        e.local.reemplazarSincronizados(
            servidor.api.listar().contenido.map { it.aDominio() },
            emptyList(), 2_000L
        )
        val lista = e.repo.observar().first()
        assertEquals(2, lista.size)
        assertNotNull(lista.firstOrNull { it.pendiente })
    }

    @Test
    fun p5IdLocalYDelServidorConvivenSinSobrescribirse() = runTest {
        val servidor = ServidorSimulado(listOf(dto(1, "Paracetamol")))
        val e = Escenario(servidor)
        e.repo.sincronizar()
        servidor.sinRed = true
        val primero = e.repo.registrar(ibuprofeno)
        val segundo = e.repo.registrar(ibuprofeno.copy(nombre = "Loratadina 10 mg"))
        val ids = e.repo.observar().first().map { it.id }.toSet()
        assertEquals(setOf(1L, -1L, -2L), ids)
        assertEquals(-1L, primero.id)
        assertEquals(-2L, segundo.id)
    }

    @Test
    fun p6SincronizarDosVecesNoDuplica() = runTest {
        val servidor = ServidorSimulado(listOf(dto(1, "Paracetamol")))
        val e = Escenario(servidor)
        servidor.sinRed = true
        e.repo.registrar(ibuprofeno)
        servidor.sinRed = false
        e.repo.sincronizar()
        e.repo.sincronizar()
        assertEquals(2, e.repo.observar().first().size)
        assertEquals(2, servidor.productos.size)
        assertEquals(1, servidor.peticionesCrear)
    }
}
