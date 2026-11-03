package pe.edu.upeu.pharmamobil.presentation.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import pe.edu.upeu.pharmamobil.resources.Res
import pe.edu.upeu.pharmamobil.resources.pharmamobil_logo

/** motivo: por que termino la sesion anterior (token expirado, 401...). */
@Composable
fun LoginScreen(viewModel: LoginViewModel, motivo: String?) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Image(painterResource(Res.drawable.pharmamobil_logo), contentDescription = "Logo", modifier = Modifier.size(80.dp))
        Text("PharmaMobil", style = MaterialTheme.typography.headlineSmall)
        Text("Inicia sesion con tu usuario de PharmaSoft", color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (motivo != null && estado.error == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(motivo, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }

        Column(modifier = Modifier.widthIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = estado.usuario,
                onValueChange = viewModel::onUsuario,
                label = { Text("Usuario") },
                singleLine = true,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = estado.clave,
                onValueChange = viewModel::onClave,
                label = { Text("Contrasena") },
                singleLine = true,
                enabled = !estado.cargando,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = estado.error != null,
                supportingText = estado.error?.let { mensaje -> { Text(mensaje) } },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = viewModel::ingresar, enabled = !estado.cargando, modifier = Modifier.fillMaxWidth()) {
                if (estado.cargando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("INGRESAR")
            }
        }
    }
}
