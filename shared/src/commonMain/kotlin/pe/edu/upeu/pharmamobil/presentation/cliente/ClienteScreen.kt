package pe.edu.upeu.pharmamobil.presentation.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.domain.model.Cliente

private val clientesDemo = listOf(
    Cliente(1, "Ana Quispe Mamani", "ana.quispe@correo.pe", "951000111"),
    Cliente(2, "Luis Condori Apaza", "luis.condori@correo.pe")
)

/** Modulo de clientes (demostrativo; el CRUD completo llega en sesiones posteriores). */
@Composable
fun ClienteScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(clientesDemo, key = { it.id }) { cliente ->
            Card {
                ListItem(
                    headlineContent = { Text(cliente.nombres) },
                    supportingContent = { Text("${cliente.correo} · Tel: ${cliente.telefonoVisible}") },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null) }
                )
            }
        }
    }
}
