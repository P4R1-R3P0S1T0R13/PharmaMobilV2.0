package pe.edu.upeu.pharmamobil.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/** Destinos tipados de la app: evita depender de cadenas repetidas. */
sealed class Screen(val titulo: String, val icono: ImageVector) {
    data object Inicio : Screen("Inicio", Icons.Default.Home)
    data object Productos : Screen("Productos", Icons.Default.Medication)
    data object Clientes : Screen("Clientes", Icons.Default.Person)
    data object Pedidos : Screen("Pedidos", Icons.Default.ShoppingCart)

    companion object {
        val todos: List<Screen> by lazy { listOf(Inicio, Productos, Clientes, Pedidos) }
    }
}
