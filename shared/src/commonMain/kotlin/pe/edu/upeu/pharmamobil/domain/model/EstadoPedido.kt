package pe.edu.upeu.pharmamobil.domain.model

/**
 * Conjunto cerrado de estados de un pedido. Al ser sealed, un when sobre
 * EstadoPedido es exhaustivo y el compilador avisa si falta un caso.
 */
sealed class EstadoPedido {
    data object Pendiente : EstadoPedido()
    data object Procesando : EstadoPedido()
    data object Entregado : EstadoPedido()
    data class Rechazado(val motivo: String) : EstadoPedido()
}

fun EstadoPedido.descripcion(): String = when (this) {
    EstadoPedido.Pendiente -> "Pendiente de atencion"
    EstadoPedido.Procesando -> "En preparacion"
    EstadoPedido.Entregado -> "Entregado al cliente"
    is EstadoPedido.Rechazado -> "Rechazado: $motivo"
}
