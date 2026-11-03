package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals

class PestanasTest {

    private val productos = listOf(
        Producto(1, "Paracetamol", 15.50, 100),
        Producto(3, "Amoxicilina", 25.00, 5),
        Producto(4, "Loratadina", 12.50, 0, activo = false),
        Producto(5, "Diclofenaco", 20.00, 3)
    )

    @Test
    fun activosIncluyeLosDeBajoStock() {
        assertEquals(listOf(1L, 3L, 5L), filtrarPorPestana(productos, 0).map { it.id })
    }

    @Test
    fun inactivos() {
        assertEquals(listOf(4L), filtrarPorPestana(productos, 1).map { it.id })
    }

    @Test
    fun bajoStockEsMenorOIgualACincoYExcluyeInactivos() {
        // Loratadina tiene stock 0 pero esta inactiva: no se repone.
        assertEquals(listOf(3L, 5L), filtrarPorPestana(productos, 2).map { it.id })
    }
}
