package pe.edu.upeu.pharmamobil.presentation.producto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Los 7 casos de prueba de la guia autonoma de la Sesion 3. */
class ProductoValidatorTest {

    @Test
    fun caso1RegistroValido() {
        assertFalse(ProductoValidator.validar("Paracetamol", "8.50", "100").hayErrores)
    }

    @Test
    fun caso2NombreVacio() {
        assertEquals("El nombre es obligatorio.", ProductoValidator.validar("  ", "8.50", "100").nombre)
    }

    @Test
    fun caso3PrecioNoNumerico() {
        assertEquals("Ingrese un precio numérico.", ProductoValidator.validar("Ibuprofeno", "abc", "50").precio)
    }

    @Test
    fun caso4PrecioCero() {
        assertEquals("El precio debe ser mayor que cero.", ProductoValidator.validar("Ibuprofeno", "0", "50").precio)
    }

    @Test
    fun caso5StockNoEntero() {
        assertEquals("Ingrese un stock entero.", ProductoValidator.validar("Amoxicilina", "18.50", "abc").stock)
    }

    @Test
    fun caso6StockNegativo() {
        assertEquals("El stock no puede ser negativo.", ProductoValidator.validar("Amoxicilina", "18.50", "-5").stock)
    }

    @Test
    fun caso7StockCeroEsValido() {
        assertFalse(ProductoValidator.validar("Loratadina", "10", "0").hayErrores)
    }
}
