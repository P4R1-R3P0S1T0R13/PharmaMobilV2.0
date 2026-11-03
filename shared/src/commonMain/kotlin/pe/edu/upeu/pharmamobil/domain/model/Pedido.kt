package pe.edu.upeu.pharmamobil.domain.model

data class DetallePedido(
    val producto: Producto,
    val cantidad: Int
) {
    init {
        require(cantidad > 0) { "La cantidad debe ser mayor que cero" }
    }

    val subtotal: Double
        get() = producto.precio * cantidad
}

data class Pedido(
    val id: Long,
    val cliente: Cliente,
    val detalles: List<DetallePedido>,
    val estado: EstadoPedido = EstadoPedido.Pendiente
) {
    val total: Double
        get() = detalles.sumOf { it.subtotal }
}
