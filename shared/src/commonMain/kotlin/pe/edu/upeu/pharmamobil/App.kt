package pe.edu.upeu.pharmamobil

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ProductoScreen(modifier = Modifier.safeDrawingPadding())
        }
    }
}
