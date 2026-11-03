package pe.edu.upeu.pharmamobil.domain

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobil.data.InventarioSimulado
import pe.edu.upeu.pharmamobil.data.ResultadoProductos
import pe.edu.upeu.pharmamobil.domain.model.Cliente
import pe.edu.upeu.pharmamobil.domain.model.DetallePedido
import pe.edu.upeu.pharmamobil.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobil.domain.model.Pedido
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.descripcion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DominioTest {

    private val paracetamol = Producto(1, "Paracetamol", 8.50, 100)

    @Test
    fun clienteSinTelefonoMuestraNoRegistrado() {
        val cliente = Cliente(1, "Ana Quispe", "ana@correo.pe")
        assertEquals("No registrado", cliente.telefonoVisible)
    }

    @Test
    fun copyNoModificaElOriginal() {
        val actualizado = paracetamol.copy(stock = 90)
        assertEquals(100, paracetamol.stock)
        assertEquals(90, actualizado.stock)
    }

    @Test
    fun productoConStockNegativoEsRechazado() {
        assertFailsWith<IllegalArgumentException> { Producto(2, "Ibuprofeno", 12.0, -1) }
    }

    @Test
    fun consultasSobreColecciones() {
        val productos = listOf(paracetamol, Producto(2, "Ibuprofeno", 12.0, 0))
        assertEquals(1, productos.filter { it.stock > 0 }.size)
        assertEquals(listOf("Paracetamol", "Ibuprofeno"), productos.map { it.nombre })
        assertEquals(null, productos.find { it.id == 99L })
    }

    @Test
    fun totalDelPedidoSumaSubtotales() {
        val pedido = Pedido(
            id = 1,
            cliente = Cliente(1, "Ana", "ana@correo.pe"),
            detalles = listOf(DetallePedido(paracetamol, 2))
        )
        assertEquals(17.0, pedido.total)
        assertEquals("Pendiente de atencion", pedido.estado.descripcion())
        assertEquals("Rechazado: sin stock", EstadoPedido.Rechazado("sin stock").descripcion())
    }

    @Test
    fun flowEmiteCargandoYLuegoExito() = runTest {
        val estados = InventarioSimulado(latenciaMs = 10).cargarProductos().toList()
        assertIs<ResultadoProductos.Cargando>(estados[0])
        assertIs<ResultadoProductos.Exito>(estados[1])
    }

    @Test
    fun flowEmiteErrorCuandoFallaLaFuente() = runTest {
        val estados = InventarioSimulado(latenciaMs = 10, fallar = true).cargarProductos().toList()
        assertIs<ResultadoProductos.Error>(estados.last())
    }
}
