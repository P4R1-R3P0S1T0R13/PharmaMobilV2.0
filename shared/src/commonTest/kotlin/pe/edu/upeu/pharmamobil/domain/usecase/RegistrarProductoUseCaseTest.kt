package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.fakes.FakeProductoRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Casos de validacion de la Sesion 3, ahora sobre el caso de uso. */
class RegistrarProductoUseCaseTest {

    private fun errores(n: String, p: String, s: String) = RegistrarProductoUseCase.validar(n, p, s)

    @Test
    fun registroValidoAsignaIdEnElRepositorio() = runTest {
        val repo = FakeProductoRepository()
        val producto = RegistrarProductoUseCase(repo)("Paracetamol", "8.50", "100").getOrThrow()
        assertEquals(1L, producto.id)
        assertEquals(1, repo.llamadasRegistrar)
    }

    @Test
    fun datosInvalidosNoLlamanAlRepositorio() = runTest {
        val repo = FakeProductoRepository()
        val resultado = RegistrarProductoUseCase(repo)("", "abc", "-1")
        assertTrue(resultado.exceptionOrNull() is ProductoInvalidoException)
        assertEquals(0, repo.llamadasRegistrar)
    }

    @Test
    fun mensajesPorCampo() {
        assertEquals("El nombre es obligatorio.", errores(" ", "8.50", "100").nombre)
        assertEquals("Ingrese un precio numérico.", errores("Ibuprofeno", "abc", "50").precio)
        assertEquals("El precio debe ser mayor que cero.", errores("Ibuprofeno", "0", "50").precio)
        assertEquals("Ingrese un stock entero.", errores("Amoxicilina", "18.50", "abc").stock)
        assertEquals("El stock no puede ser negativo.", errores("Amoxicilina", "18.50", "-5").stock)
        assertFalse(errores("Loratadina", "10", "0").hayErrores)
    }

    @Test
    fun reglaDeReposicion() {
        assertTrue(Producto(1, "Amoxicilina", 25.0, Producto.STOCK_MINIMO).requiereReposicion)
        assertFalse(Producto(2, "Loratadina", 12.5, 0, activo = false).requiereReposicion)
    }
}
