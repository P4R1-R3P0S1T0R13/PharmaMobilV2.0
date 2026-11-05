package pe.edu.upeu.pharmamobil.presentation.permiso

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobil.domain.permiso.EstadoPermiso
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.permiso.Permiso
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El gestor es una interfaz: en las pruebas se usa uno falso, sin dispositivo. */
@OptIn(ExperimentalCoroutinesApi::class)
class AvisosViewModelTest {

    private class GestorFalso(
        var actual: EstadoPermiso,
        val respuestaAlPedir: EstadoPermiso
    ) : GestorPermisos {
        var solicitudes = 0
        var aperturasDeAjustes = 0
        override suspend fun estado(permiso: Permiso) = actual
        override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
            solicitudes++
            actual = respuestaAlPedir
            return actual
        }
        override fun abrirAjustes() { aperturasDeAjustes++ }
    }

    @BeforeTest
    fun antes() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun despues() = Dispatchers.resetMain()

    @Test
    fun seExplicaAntesDePedirYSeConcede() = runTest {
        val gestor = GestorFalso(EstadoPermiso.Denegado, EstadoPermiso.Concedido)
        val vm = AvisosViewModel(gestor)
        vm.refrescar()
        advanceUntilIdle()

        vm.accionPrincipal()
        assertTrue(vm.uiState.value.mostrarExplicacion)
        assertEquals(0, gestor.solicitudes, "No se pide sin explicar primero")

        vm.aceptarExplicacion()
        advanceUntilIdle()
        assertEquals(EstadoPermiso.Concedido, vm.uiState.value.estado)
        assertEquals(1, gestor.solicitudes)
    }

    @Test
    fun denegadoParaSiempreOfreceAjustesEnLugarDePedir() = runTest {
        val gestor = GestorFalso(EstadoPermiso.Denegado, EstadoPermiso.DenegadoParaSiempre)
        val vm = AvisosViewModel(gestor)
        vm.accionPrincipal()
        vm.aceptarExplicacion()
        advanceUntilIdle()
        assertEquals(EstadoPermiso.DenegadoParaSiempre, vm.uiState.value.estado)

        vm.accionPrincipal()
        assertEquals(1, gestor.aperturasDeAjustes)
        assertFalse(vm.uiState.value.mostrarExplicacion)
        assertEquals("Abrir Ajustes", textosAvisos(vm.uiState.value.estado).second)
    }

    @Test
    fun concedidoNoOfreceAccion() = runTest {
        val vm = AvisosViewModel(GestorFalso(EstadoPermiso.Concedido, EstadoPermiso.Concedido))
        vm.refrescar()
        advanceUntilIdle()
        vm.accionPrincipal()
        assertFalse(vm.uiState.value.mostrarExplicacion)
        assertNull(textosAvisos(vm.uiState.value.estado).second)
    }
}
