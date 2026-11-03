package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField

/**
 * La pantalla solo dibuja el estado y avisa eventos al ViewModel: no guarda
 * estado de negocio ni valida (Sesion 5).
 */
@Composable
fun ProductoScreen(viewModel: ProductoViewModel, modifier: Modifier = Modifier) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        FormularioProductoSeccion(
            formulario = estado.formulario,
            registrando = estado.registrando,
            mensajeExito = estado.mensajeExito,
            onNombre = viewModel::onNombreChange,
            onPrecio = viewModel::onPrecioChange,
            onStock = viewModel::onStockChange,
            onRegistrar = viewModel::registrar
        )

        when (val fase = estado.fase) {
            ProductoUiState.Fase.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            ProductoUiState.Fase.SinProductos -> EstadoVacio(
                icono = Icons.Default.Inventory2,
                titulo = "Inventario vacio",
                descripcion = "Registra tu primer producto con el formulario.",
                modifier = Modifier.fillMaxWidth()
            )

            is ProductoUiState.Fase.Error -> EstadoVacio(
                icono = Icons.Default.CloudOff,
                titulo = "No se pudo cargar",
                descripcion = fase.mensaje,
                modifier = Modifier.fillMaxWidth(),
                accion = { OutlinedButton(onClick = viewModel::cargar) { Text("Reintentar") } }
            )

            is ProductoUiState.Fase.ConProductos -> ListaConPestanas(
                productos = fase.productos,
                pestana = estado.pestana,
                onPestana = viewModel::onPestana
            )
        }
    }
}

@Composable
private fun FormularioProductoSeccion(
    formulario: FormularioProducto,
    registrando: Boolean,
    mensajeExito: String?,
    onNombre: (String) -> Unit,
    onPrecio: (String) -> Unit,
    onStock: (String) -> Unit,
    onRegistrar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Registro de Producto", style = MaterialTheme.typography.titleLarge)
        ValidatedTextField(formulario.nombre, onNombre, "Nombre del producto", formulario.nombreError, habilitado = !registrando)
        ValidatedTextField(formulario.precio, onPrecio, "Precio", formulario.precioError,
            tipoTeclado = KeyboardType.Decimal, habilitado = !registrando)
        ValidatedTextField(formulario.stock, onStock, "Stock", formulario.stockError,
            tipoTeclado = KeyboardType.Number, habilitado = !registrando)
        Button(onClick = onRegistrar, enabled = !registrando, modifier = Modifier.fillMaxWidth()) {
            Text(if (registrando) "REGISTRANDO..." else "REGISTRAR")
        }
        mensajeExito?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun ListaConPestanas(productos: List<Producto>, pestana: Int, onPestana: (Int) -> Unit) {
    val visibles = filtrarPorPestana(productos, pestana)
    Column {
        PrimaryTabRow(selectedTabIndex = pestana) {
            PESTANAS.forEachIndexed { indice, titulo ->
                Tab(
                    selected = pestana == indice,
                    onClick = { onPestana(indice) },
                    text = { Text("$titulo (${filtrarPorPestana(productos, indice).size})") }
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibles, key = { it.id }) { producto -> ProductoItem(producto) }
        }
    }
}

@Composable
fun ProductoItem(producto: Producto, acciones: @Composable () -> Unit = {}) {
    Card {
        ListItem(
            headlineContent = { Text(producto.nombre) },
            supportingContent = { Text("S/ ${producto.precio} · Stock: ${producto.stock}") },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        !producto.activo -> AssistChip(onClick = {}, label = { Text("Inactivo") })
                        producto.requiereReposicion -> AssistChip(onClick = {}, label = { Text("Bajo stock") })
                    }
                    acciones()
                }
            }
        )
    }
}
