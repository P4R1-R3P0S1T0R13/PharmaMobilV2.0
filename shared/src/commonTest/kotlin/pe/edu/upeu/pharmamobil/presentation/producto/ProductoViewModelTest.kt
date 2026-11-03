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
import kotlin.test.assertNull

/** Transiciones de estado del ViewModel frente a exito y errores tipificados. */
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
    fun repositorioVacioProduceSinProductos() = runTest {
        assertIs<ProductoUiState.Fase.SinProductos>(viewModel(FakeProductoRepository()).uiState.value.fase)
    }

    @Test
    fun repositorioConProductosProduceConProductos() = runTest {
        val vm = viewModel(FakeProductoRepository(FakeProductoRepository.tresProductos))
        assertEquals(3, assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase).productos.size)
        assertEquals(2, vm.uiState.value.categorias.size)
    }

    @Test
    fun sinConexionProduceErrorConMensaje() = runTest {
        val vm = viewModel(FakeProductoRepository(errorAlListar = ErrorApi.SinConexion))
        assertEquals(ErrorApi.SinConexion.mensaje, assertIs<ProductoUiState.Fase.Error>(vm.uiState.value.fase).mensaje)
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
    fun registroExitosoCierraFormularioYRecarga() = runTest {
        val repo = FakeProductoRepository()
        val vm = viewModel(repo)
        vm.llenar("Paracetamol", "8.50", "100")
        vm.guardar()
        advanceUntilIdle()
        assertEquals(false, vm.uiState.value.formulario.visible)
        assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase)
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
        assertEquals(3, repo.productos.size)
        assertNull(vm.uiState.value.productoAEliminar)
    }

    @Test
    fun editarCargaElFormularioYActualiza() = runTest {
        val repo = FakeProductoRepository(FakeProductoRepository.tresProductos)
        val vm = viewModel(repo)
        vm.editar(FakeProductoRepository.tresProductos[2])
        vm.onStockChange("40")
        vm.guardar()
        advanceUntilIdle()
        assertEquals(40, repo.productos.first { it.id == 3L }.stock)
    }
}
