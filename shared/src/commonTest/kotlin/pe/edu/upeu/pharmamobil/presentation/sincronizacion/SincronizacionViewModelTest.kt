package pe.edu.upeu.pharmamobil.presentation.sincronizacion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.data.firebase.sinDatosSensibles
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.domain.port.TelemetriaNula
import pe.edu.upeu.pharmamobil.fakes.FakeProductoRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SincronizacionViewModelTest {

    private class NotificadorEspia : Notificador {
        val titulos = mutableListOf<String>()
        override fun notificar(titulo: String, mensaje: String) { titulos += titulo }
    }

    @BeforeTest
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repo: FakeProductoRepository, notificador: Notificador = NotificadorEspia()) =
        SincronizacionViewModel(repo, TelemetriaNula, notificador).also {
            it.estado.launchIn(backgroundScope) // WhileSubscribed necesita un suscriptor
            advanceUntilIdle()
        }

    private val pendiente = Producto(-1, "Ibuprofeno", 4.5, 20, pendiente = true)

    @Test
    fun sinConexionMuestraCuantosCambiosFaltanPorEnviar() = runTest {
        val repo = FakeProductoRepository(errorAlSincronizar = ErrorApi.SinConexion)
        repo.local.value = listOf(pendiente)
        val vm = viewModel(repo)

        val estado = vm.estado.value
        assertTrue(estado.sinConexion)
        assertEquals(1, estado.pendientes)
        assertNull(estado.ultimaSync)
        assertEquals("Sin conexion · 1 cambio por enviar", textoEstadoSync(estado))
    }

    @Test
    fun lasFallidasSeCuentanAparteYOfrecenAccion() = runTest {
        val repo = FakeProductoRepository(errorAlSincronizar = ErrorApi.SinConexion)
        repo.local.value = listOf(pendiente, pendiente.copy(id = -2, intentos = 5, ultimoError = "Conflicto"))
        val vm = viewModel(repo)

        assertEquals(1, vm.estado.value.pendientes)
        assertEquals(1, vm.estado.value.fallidas)
        assertEquals("1 cambio no se pudo enviar", textoFallidas(vm.estado.value.fallidas))
    }

    @Test
    fun alEnviarLoPendienteSeNotificaYQuedaTodoEnviado() = runTest {
        val notificador = NotificadorEspia()
        val repo = FakeProductoRepository()
        repo.local.value = listOf(pendiente)
        val vm = viewModel(repo, notificador)

        assertEquals("Todo enviado", textoEstadoSync(vm.estado.value))
        assertEquals(1_000L, vm.estado.value.ultimaSync)
        assertTrue("Cambios enviados" in notificador.titulos)
    }

    @Test
    fun bajoStockSeAvisaSoloCuandoAumenta() = runTest {
        val notificador = NotificadorEspia()
        val repo = FakeProductoRepository(listOf(Producto(3, "Amoxicilina", 25.0, 5)))
        val vm = viewModel(repo, notificador)
        vm.sincronizar()
        advanceUntilIdle()

        assertEquals(1, notificador.titulos.count { it == "Bajo stock" })
    }

    @Test
    fun elDetalleIndicaHoraYFilasDescargadas() {
        val estado = SincronizacionUiState(ultimaSync = 0L, descargados = 5)
        assertEquals(
            "Actualizado hace 2 minutos (14:32) · 5 productos descargados",
            textoDetalleSync(estado, ahoraMs = 120_000L) { "14:32" }
        )
        assertEquals("Aun no se sincronizo con el servidor", textoDetalleSync(SincronizacionUiState(), 0L) { "" })
    }

    @Test
    fun laTelemetriaNuncaRecibeElTokenNiDatosPersonales() {
        val limpio = mapOf(
            "pantalla" to "Productos",
            "token" to "eyJhbGciOi...",
            "email" to "ana@correo.pe",
            "detalle" to "eyJhbGciOiJIUzUxMiJ9.x.y"
        ).sinDatosSensibles()
        assertEquals(mapOf("pantalla" to "Productos", "detalle" to "***"), limpio)
    }
}
