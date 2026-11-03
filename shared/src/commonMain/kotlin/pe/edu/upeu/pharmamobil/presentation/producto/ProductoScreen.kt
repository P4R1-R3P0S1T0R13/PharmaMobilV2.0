package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField

private val productosIniciales = listOf(
    Producto(1, "Paracetamol", 15.50, 100),
    Producto(2, "Ibuprofeno", 18.90, 50),
    Producto(3, "Amoxicilina", 25.00, 5),
    Producto(4, "Loratadina", 12.50, 0, activo = false),
    Producto(5, "Diclofenaco", 20.00, 3)
)

private val PESTANAS = listOf("Activos", "Inactivos", "Bajo stock")

/** Filtro de cada pestana: 0 Activos, 1 Inactivos, 2 Bajo stock (stock <= 5). */
internal fun filtrarPorPestana(productos: List<Producto>, pestana: Int): List<Producto> =
    when (pestana) {
        0 -> productos.filter { it.activo }
        1 -> productos.filter { !it.activo }
        else -> productos.filter { it.requiereReposicion }
    }

/**
 * Sesion 4: registro + inventario clasificado en pestanas. Todo el estado
 * vive en la pantalla con remember (se reorganiza en la Sesion 5).
 */
@Composable
fun ProductoScreen(modifier: Modifier = Modifier) {
    var productos by remember { mutableStateOf(productosIniciales) }
    var tabSeleccionada by rememberSaveable { mutableStateOf(0) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var precio by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("") }
    var intentoRegistrar by rememberSaveable { mutableStateOf(false) }
    var mensaje by rememberSaveable { mutableStateOf("") }
    var esError by rememberSaveable { mutableStateOf(false) }
    var siguienteId by rememberSaveable { mutableStateOf(6L) }

    val errores = if (intentoRegistrar) ProductoValidator.validar(nombre, precio, stock) else ErroresProducto()
    val visibles = filtrarPorPestana(productos, tabSeleccionada)

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Registro de Producto", style = MaterialTheme.typography.titleLarge)
            ValidatedTextField(nombre, { nombre = it }, "Nombre del producto", errores.nombre)
            ValidatedTextField(precio, { precio = it }, "Precio", errores.precio, tipoTeclado = KeyboardType.Decimal)
            ValidatedTextField(stock, { stock = it }, "Stock", errores.stock, tipoTeclado = KeyboardType.Number)
            Button(
                onClick = {
                    intentoRegistrar = true
                    val resultado = ProductoValidator.validar(nombre, precio, stock)
                    if (resultado.hayErrores) {
                        esError = true
                        mensaje = "Revise los campos marcados."
                    } else {
                        val nuevo = Producto(siguienteId++, nombre.trim(), precio.trim().toDouble(), stock.trim().toInt())
                        productos = productos + nuevo
                        esError = false
                        mensaje = "Producto \"${nuevo.nombre}\" registrado correctamente"
                        nombre = ""
                        precio = ""
                        stock = ""
                        intentoRegistrar = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("REGISTRAR") }
            if (mensaje.isNotEmpty()) {
                Text(
                    mensaje,
                    color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }

        PrimaryTabRow(selectedTabIndex = tabSeleccionada) {
            PESTANAS.forEachIndexed { indice, titulo ->
                Tab(
                    selected = tabSeleccionada == indice,
                    onClick = { tabSeleccionada = indice },
                    text = { Text("$titulo (${filtrarPorPestana(productos, indice).size})") }
                )
            }
        }

        if (visibles.isEmpty()) {
            EstadoVacio(
                icono = Icons.Default.Inventory2,
                titulo = "Sin productos",
                descripcion = "No hay productos en la pestana ${PESTANAS[tabSeleccionada]}.",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibles, key = { it.id }) { producto -> ProductoItem(producto) }
            }
        }
    }
}

@Composable
private fun ProductoItem(producto: Producto) {
    Card {
        ListItem(
            headlineContent = { Text(producto.nombre) },
            supportingContent = { Text("S/ ${producto.precio} · Stock: ${producto.stock}") },
            trailingContent = {
                when {
                    !producto.activo -> AssistChip(onClick = {}, label = { Text("Inactivo") })
                    producto.requiereReposicion -> AssistChip(onClick = {}, label = { Text("Bajo stock") })
                }
            }
        )
    }
}
