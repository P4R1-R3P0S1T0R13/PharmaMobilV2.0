package pe.edu.upeu.pharmamobil.presentation.pedido

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio

/** Modulo de pedidos (demostrativo). */
@Composable
fun PedidoScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EstadoVacio(
            icono = Icons.Default.ShoppingCart,
            titulo = "Gestion de Pedidos",
            descripcion = "El control de ordenes y despachos se implementa en una proxima sesion."
        )
    }
}
