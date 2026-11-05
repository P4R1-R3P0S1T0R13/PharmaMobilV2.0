package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.fakes.FakeProductoRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Transiciones de estado del ViewModel suscrito al Flow de la base local. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repo: FakeProductoRepository): ProductoViewModel {
        val vm = ProductoViewModel(RegistrarProductoUseCase(repo), repo)
        advanceUntilIdle()
        return vm
    }

    private fun ProductoViewModel.llenar(nombre: String, precio: String, stock: String, categoria: Long? = 1) {
        nuevo()
        onNombreChange(nombre)
        onPrecioChange(precio)
        onStockChange(stock)
        categoria?.let { onCategoriaChange(it) }
    }

    @Test
    fun servidorVacioProduceSinProductos() = runTest {
        assertIs<ProductoUiState.Fase.SinProductos>(viewModel(FakeProductoRepository()).uiState.value.fase)
    }

    @Test
    fun alSincronizarElFlowLlevaLosProductosALaPantallaSinRecargar() = runTest {
        val repo = FakeProductoRepository(FakeProductoRepository.tresProductos)
        val vm = viewModel(repo)
        // La sincronizacion la hace otro ViewModel: este solo observa la base local.
        repo.sincronizar()
        advanceUntilIdle()
        assertEquals(3, assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase).productos.size)
        assertEquals(2, vm.uiState.value.categorias.size)
    }

    @Test
    fun precioCeroDejaErrorEnFormularioSinLlamarAlRepositorio() = runTest {
        val repo = FakeProductoRepository()
        val vm = viewModel(repo)
        vm.llenar("Paracetamol", "0", "10")
        vm.guardar()
        advanceUntilIdle()
        assertEquals("El precio debe ser mayor que cero.", vm.uiState.value.formulario.precioError)
        assertEquals(0, repo.llamadasGuardar)
    }

    @Test
    fun registroSinRedApareceEnLaListaComoPendienteSinRecargar() = runTest {
        val repo = FakeProductoRepository(errorAlSincronizar = ErrorApi.SinConexion)
        val vm = viewModel(repo)
        vm.llenar("Ibuprofeno", "4.50", "20")
        vm.guardar()
        advanceUntilIdle()
        val productos = assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase).productos
        assertTrue(productos.single().pendiente)
        assertTrue(vm.uiState.value.mensaje!!.contains("sin conexion"))
    }

    @Test
    fun error400DelServidorSeMuestraDebajoDelCampo() = runTest {
        val repo = FakeProductoRepository(
            errorAlGuardar = ErrorApi.Validacion("Existen errores de validación", mapOf("nombre" to "Nombre ya usado"))
        )
        val vm = viewModel(repo)
        vm.llenar("Paracetamol", "8.50", "100")
        vm.guardar()
        advanceUntilIdle()
        assertEquals("Nombre ya usado", vm.uiState.value.formulario.nombreError)
        assertEquals(ProductoUiState.Operacion.Ninguna, vm.uiState.value.operacion)
    }

    @Test
    fun conflictoAlEliminarMuestraMensajeYConservaElProducto() = runTest {
        val repo = FakeProductoRepository(
            FakeProductoRepository.tresProductos,
            errorAlEliminar = ErrorApi.Conflicto("El producto tiene ventas registradas")
        )
        val vm = viewModel(repo)
        vm.pedirEliminar(FakeProductoRepository.tresProductos.first())
        vm.confirmarEliminar()
        advanceUntilIdle()
        assertEquals("El producto tiene ventas registradas", vm.uiState.value.mensaje)
        assertEquals(3, repo.local.value.size)
    }

    @Test
    fun textoDeAntiguedadEnLenguajeNatural() {
        val hora = 3_600_000L
        assertEquals("Aun no se sincronizo con el servidor", textoAntiguedad(null, hora))
        assertEquals("Actualizado hace un momento", textoAntiguedad(hora, hora + 30_000))
        assertEquals("Actualizado hace 5 minutos", textoAntiguedad(hora, hora + 5 * 60_000))
        assertEquals("Actualizado hace 2 horas", textoAntiguedad(hora, hora + 2 * hora))
        assertEquals("Actualizado hace 1 dia", textoAntiguedad(hora, hora + 25 * hora))
    }
}
