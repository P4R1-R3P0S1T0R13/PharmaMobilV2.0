package pe.edu.upeu.pharmamobil.domain.usecase

import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.fakes.FakeProductoRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegistrarProductoUseCaseTest {

    private fun errores(n: String, p: String, s: String, c: Long? = 1) = RegistrarProductoUseCase.validar(n, p, s, c)

    @Test
    fun registroValidoLlamaAlRepositorio() = runTest {
        val repo = FakeProductoRepository()
        val producto = RegistrarProductoUseCase(repo)(null, "Paracetamol", "8.50", "100", true, 1).getOrThrow()
        assertEquals(1L, producto.id)
        assertEquals(1, repo.llamadasGuardar)
    }

    @Test
    fun datosInvalidosNoLlamanAlRepositorio() = runTest {
        val repo = FakeProductoRepository()
        val resultado = RegistrarProductoUseCase(repo)(null, "", "abc", "-1", true, null)
        assertTrue(resultado.exceptionOrNull() is ProductoInvalidoException)
        assertEquals(0, repo.llamadasGuardar)
    }

    @Test
    fun mensajesPorCampo() {
        assertEquals("El nombre es obligatorio.", errores(" ", "8.50", "100").nombre)
        assertEquals("El nombre debe tener al menos 3 caracteres.", errores("Ab", "8.50", "100").nombre)
        assertEquals("Ingrese un precio numérico.", errores("Ibuprofeno", "abc", "50").precio)
        assertEquals("El precio debe ser mayor que cero.", errores("Ibuprofeno", "0", "50").precio)
        assertEquals("Ingrese un stock entero.", errores("Amoxicilina", "18.50", "abc").stock)
        assertEquals("El stock no puede ser negativo.", errores("Amoxicilina", "18.50", "-5").stock)
        assertEquals("Seleccione una categoría.", errores("Amoxicilina", "18.50", "5", null).categoria)
        assertFalse(errores("Loratadina", "10", "0").hayErrores)
    }

    @Test
    fun reglaDeReposicion() {
        assertTrue(Producto(1, "Amoxicilina", 25.0, Producto.STOCK_MINIMO).requiereReposicion)
        assertFalse(Producto(2, "Loratadina", 12.5, 0, activo = false).requiereReposicion)
    }
}
