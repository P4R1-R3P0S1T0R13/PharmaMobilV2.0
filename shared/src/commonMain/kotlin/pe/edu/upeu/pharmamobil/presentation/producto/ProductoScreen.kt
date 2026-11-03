package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField

/**
 * Formulario de registro de productos (Sesion 3). El estado vive en la
 * pantalla con remember/mutableStateOf; cada cambio provoca recomposicion.
 */
@Composable
fun ProductoScreen(modifier: Modifier = Modifier) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var precio by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var intentoRegistrar by rememberSaveable { mutableStateOf(false) }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var siguienteId by rememberSaveable { mutableStateOf(1L) }

    // Los errores solo se muestran despues del primer intento de registro.
    val errores = if (intentoRegistrar) {
        ProductoValidator.validar(nombre, precio, stock)
    } else {
        ErroresProducto()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("PHARMAMOBIL", style = MaterialTheme.typography.labelLarge)
        Text("Registro de Producto", style = MaterialTheme.typography.headlineSmall)

        ValidatedTextField(
            valor = nombre,
            onValorChange = { nombre = it },
            etiqueta = "Nombre del producto",
            error = errores.nombre
        )
        ValidatedTextField(
            valor = precio,
            onValorChange = { precio = it },
            etiqueta = "Precio",
            error = errores.precio,
            tipoTeclado = KeyboardType.Decimal
        )
        ValidatedTextField(
            valor = stock,
            onValorChange = { stock = it },
            etiqueta = "Stock",
            error = errores.stock,
            tipoTeclado = KeyboardType.Number
        )

        Button(
            onClick = {
                intentoRegistrar = true
                val resultado = ProductoValidator.validar(nombre, precio, stock)
                if (resultado.hayErrores) {
                    mensaje = listOfNotNull(resultado.nombre, resultado.precio, resultado.stock).first()
                } else {
                    val producto = Producto(
                        id = siguienteId++,
                        nombre = nombre.trim(),
                        precio = precio.trim().toDouble(),
                        stock = stock.trim().toInt()
                    )
                    mensaje = "Producto registrado correctamente: ${producto.nombre} " +
                        "(S/ ${producto.precio}, stock ${producto.stock})"
                    nombre = ""
                    precio = ""
                    stock = ""
                    intentoRegistrar = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("REGISTRAR")
        }

        if (mensaje.isNotEmpty()) {
            Text(
                text = mensaje,
                color = if (intentoRegistrar) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
        }
    }
}
