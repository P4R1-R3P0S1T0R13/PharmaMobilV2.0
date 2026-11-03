package pe.edu.upeu.pharmamobil.presentation.producto

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.fakes.FakeProductoRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Pruebas de flujo: una por cada estado de la pantalla de Productos. */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    @BeforeTest
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeProductoRepository) =
        ProductoViewModel(RegistrarProductoUseCase(repo), repo)

    @Test
    fun repositorioVacioProduceSinProductos() = runTest {
        val vm = viewModel(FakeProductoRepository())
        advanceUntilIdle()
        assertIs<ProductoUiState.Fase.SinProductos>(vm.uiState.value.fase)
    }

    @Test
    fun repositorioConProductosProduceConProductos() = runTest {
        val vm = viewModel(FakeProductoRepository(FakeProductoRepository.tresProductos))
        advanceUntilIdle()
        val fase = assertIs<ProductoUiState.Fase.ConProductos>(vm.uiState.value.fase)
        assertEquals(3, fase.productos.size)
    }

    @Test
    fun repositorioQueFallaProduceError() = runTest {
        val vm = viewModel(FakeProductoRepository(fallo = RuntimeException("Sin conexion")))
        advanceUntilIdle()
        val fase = assertIs<ProductoUiState.Fase.Error>(vm.uiState.value.fase)
        assertEquals("Sin conexion", fase.mensaje)
    }

    @Test
    fun precioCeroDejaErrorEnFormularioSinLlamarAlRepositorio() = runTest {
        val repo = FakeProductoRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onNombreChange("Paracetamol")
        vm.onPrecioChange("0")
        vm.onStockChange("10")
        vm.registrar()
        advanceUntilIdle()
        assertEquals("El precio debe ser mayor que cero.", vm.uiState.value.formulario.precioError)
        assertEquals(0, repo.llamadasRegistrar)
    }
}
