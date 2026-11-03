package pe.edu.upeu.pharmamobil.presentation.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pe.edu.upeu.pharmamobil.platform.infoDispositivo
import pe.edu.upeu.pharmamobil.resources.Res
import pe.edu.upeu.pharmamobil.resources.pharmamobil_logo

@Composable
fun InicioScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        // Recurso compartido de composeResources/drawable, el mismo en Android e iOS.
        Image(
            painter = painterResource(Res.drawable.pharmamobil_logo),
            contentDescription = "Logo PharmaMobil",
            modifier = Modifier.size(96.dp)
        )
        Text("PharmaMobil", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Gestion farmaceutica de productos, clientes y pedidos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val info = remember { infoDispositivo() }
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Este dispositivo", style = MaterialTheme.typography.titleMedium)
                Text("Sistema: ${info.sistema} ${info.version}")
                Text("Modelo: ${info.modelo}")
                Text("Idioma: ${info.idioma}")
            }
        }
    }
}
