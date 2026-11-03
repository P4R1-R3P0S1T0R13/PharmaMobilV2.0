package pe.edu.upeu.pharmamobil.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/** Campo de texto reutilizable que muestra su error debajo y se marca en rojo. */
@Composable
fun ValidatedTextField(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    error: String?,
    modifier: Modifier = Modifier,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    habilitado: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = error?.let { mensaje -> { Text(mensaje) } },
        singleLine = true,
        enabled = habilitado,
        keyboardOptions = KeyboardOptions(keyboardType = tipoTeclado),
        modifier = modifier.fillMaxWidth()
    )
}
