package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.platform.ahoraMs
import pe.edu.upeu.pharmamobil.presentation.components.EstadoVacio
import pe.edu.upeu.pharmamobil.presentation.components.ValidatedTextField
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.SincronizacionUiState
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.SincronizacionViewModel
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.textoEstadoSync
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.textoFallidas

/**
 * CRUD de productos. La pantalla solo dibuja el estado y avisa eventos al
 * ViewModel; no guarda estado de negocio ni valida.
 */
@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    sincronizacion: SincronizacionViewModel,
    modifier: Modifier = Modifier,
    formatearPrecio: (Double) -> String = { "S/ $it" },
    /** false para roles sin CATALOGO_ESCRIBIR (VENDEDOR): oculta crear, editar y eliminar. */
    puedeEscribir: Boolean = true,
    accionesExtra: @Composable (Producto) -> Unit = {}
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val sync by sincronizacion.estado.collectAsStateWithLifecycle()
    // remember aqui es estado de UI (el host del snackbar), no de negocio.
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (puedeEscribir && !estado.formulario.visible) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::nuevo,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nuevo") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            BarraSincronizacion(
                estado = sync,
                onSincronizar = sincronizacion::sincronizar,
                onReintentarFallidas = sincronizacion::reintentarFallidas,
                onRevisar = { viewModel.onPestana(PESTANA_POR_ENVIAR) }
            )
            if (estado.formulario.visible) {
                FormularioProductoSeccion(
                    formulario = estado.formulario,
                    categorias = estado.categorias,
                    guardando = estado.operacion == ProductoUiState.Operacion.Guardando,
                    viewModel = viewModel
                )
            }
            when (val fase = estado.fase) {
                ProductoUiState.Fase.Cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                ProductoUiState.Fase.SinProductos -> EstadoVacio(
                    icono = if (sync.sinConexion) Icons.Default.CloudOff else Icons.Default.Inventory2,
                    titulo = if (sync.sinConexion && sync.ultimaSync == null)
                        "Aun no hay datos guardados" else "Inventario vacio",
                    descripcion = if (sync.sinConexion && sync.ultimaSync == null)
                        "Conectate a internet una vez para descargar el inventario. Luego podras usarlo sin conexion."
                    else "Registra tu primer producto con el boton Nuevo.",
                    modifier = Modifier.fillMaxWidth()
                )

                is ProductoUiState.Fase.Error -> EstadoVacio(
                    icono = Icons.Default.CloudOff,
                    titulo = "No se pudo cargar",
                    descripcion = fase.mensaje,
                    modifier = Modifier.fillMaxWidth(),
                    accion = { OutlinedButton(onClick = sincronizacion::sincronizar) { Text("Reintentar") } }
                )

                is ProductoUiState.Fase.ConProductos -> ListaConPestanas(
                    productos = fase.productos,
                    estado = estado,
                    formatearPrecio = formatearPrecio,
                    puedeEscribir = puedeEscribir,
                    onPestana = viewModel::onPestana,
                    onEditar = viewModel::editar,
                    onEliminar = viewModel::pedirEliminar,
                    accionesExtra = accionesExtra
                )
            }
        }
    }

    estado.productoAEliminar?.let { producto ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            title = { Text("Eliminar producto") },
            text = { Text("¿Eliminar \"${producto.nombre}\"? Esta accion no se puede deshacer.") },
            confirmButton = { TextButton(onClick = viewModel::confirmarEliminar) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarEliminar) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FormularioProductoSeccion(
    formulario: FormularioProducto,
    categorias: List<Categoria>,
    guardando: Boolean,
    viewModel: ProductoViewModel
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            if (formulario.idEnEdicion == null) "Registro de Producto" else "Editar Producto",
            style = MaterialTheme.typography.titleLarge
        )
        ValidatedTextField(formulario.nombre, viewModel::onNombreChange, "Nombre del producto",
            formulario.nombreError, habilitado = !guardando)
        ValidatedTextField(formulario.precio, viewModel::onPrecioChange, "Precio", formulario.precioError,
            tipoTeclado = KeyboardType.Decimal, habilitado = !guardando)
        ValidatedTextField(formulario.stock, viewModel::onStockChange, "Stock", formulario.stockError,
            tipoTeclado = KeyboardType.Number, habilitado = !guardando)
        SelectorCategoria(categorias, formulario.categoriaId, formulario.categoriaError, viewModel::onCategoriaChange)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activo", modifier = Modifier.weight(1f))
            Switch(checked = formulario.activo, onCheckedChange = viewModel::onActivoChange, enabled = !guardando)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::cancelarFormulario, enabled = !guardando, modifier = Modifier.weight(1f)) {
                Text("CANCELAR")
            }
            Button(onClick = viewModel::guardar, enabled = !guardando, modifier = Modifier.weight(1f)) {
                if (guardando) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(if (formulario.idEnEdicion == null) "REGISTRAR" else "ACTUALIZAR")
            }
        }
    }
}

@Composable
private fun SelectorCategoria(
    categorias: List<Categoria>,
    seleccionada: Long?,
    error: String?,
    onSeleccion: (Long) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val nombre = categorias.firstOrNull { it.id == seleccionada }?.nombre ?: "Seleccione una categoria"
    Box {
        OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth()) {
            Text(nombre, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            categorias.forEach { categoria ->
                DropdownMenuItem(
                    text = { Text(categoria.nombre) },
                    onClick = {
                        onSeleccion(categoria.id)
                        abierto = false
                    }
                )
            }
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ListaConPestanas(
    productos: List<Producto>,
    estado: ProductoUiState,
    formatearPrecio: (Double) -> String,
    puedeEscribir: Boolean,
    onPestana: (Int) -> Unit,
    onEditar: (Producto) -> Unit,
    onEliminar: (Producto) -> Unit,
    accionesExtra: @Composable (Producto) -> Unit
) {
    val visibles = filtrarPorPestana(productos, estado.pestana)
    val listaEstado = rememberLazyListState()
    // Lo creado sin red va primero (ORDER BY pendiente DESC): se sube la vista para que se vea.
    val pendientes = productos.count { it.pendiente }
    LaunchedEffect(pendientes) {
        if (pendientes > 0) listaEstado.animateScrollToItem(0)
    }
    Column {
        PrimaryScrollableTabRow(selectedTabIndex = estado.pestana, edgePadding = 8.dp) {
            PESTANAS.forEachIndexed { indice, titulo ->
                Tab(
                    selected = estado.pestana == indice,
                    onClick = { onPestana(indice) },
                    text = { Text("$titulo (${filtrarPorPestana(productos, indice).size})") }
                )
            }
        }
        LazyColumn(
            state = listaEstado,
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visibles, key = { it.id }) { producto ->
                val eliminando = estado.operacion == ProductoUiState.Operacion.Eliminando(producto.id)
                ProductoItem(producto, formatearPrecio) {
                    Row {
                        accionesExtra(producto)
                        if (puedeEscribir) {
                            IconButton(onClick = { onEditar(producto) }, enabled = !estado.ocupado) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar")
                            }
                            if (eliminando) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(2.dp))
                            } else {
                                IconButton(onClick = { onEliminar(producto) }, enabled = !estado.ocupado) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoItem(
    producto: Producto,
    formatearPrecio: (Double) -> String,
    acciones: @Composable () -> Unit
) {
    Card {
        ListItem(
            headlineContent = { Text(producto.nombre) },
            overlineContent = producto.categoriaNombre?.let { { Text(it) } },
            supportingContent = {
                Column {
                    Text("${formatearPrecio(producto.precio)} · Stock: ${producto.stock}")
                    when {
                        // Sesion 13: la fila que agoto sus intentos queda visible con su motivo.
                        producto.fallida -> Column {
                            AssistChip(
                                onClick = {},
                                label = { Text("No se pudo enviar") },
                                leadingIcon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            producto.ultimoError?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        producto.pendiente -> AssistChip(
                            onClick = {},
                            label = { Text("Pendiente de envio") },
                            leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        !producto.activo -> AssistChip(onClick = {}, label = { Text("Inactivo") })
                        producto.requiereReposicion -> AssistChip(onClick = {}, label = { Text("Bajo stock") })
                    }
                }
            },
            trailingContent = acciones
        )
    }
}

/**
 * Estado de la sincronizacion (Sesiones 11 y 13): conexion, cambios por
 * enviar, filas fallidas y antiguedad de los datos. El reloj se refresca cada
 * minuto para que "hace X minutos" no se quede congelado.
 */
@Composable
private fun BarraSincronizacion(
    estado: SincronizacionUiState,
    onSincronizar: () -> Unit,
    onReintentarFallidas: () -> Unit,
    onRevisar: () -> Unit
) {
    var ahora by remember { mutableLongStateOf(ahoraMs()) }
    LaunchedEffect(estado.ultimaSync) {
        while (true) {
            ahora = ahoraMs()
            delay(60_000)
        }
    }
    val alerta = estado.sinConexion || estado.fallidas > 0 || estado.error != null
    Surface(
        color = if (alerta) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(textoEstadoSync(estado), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        textoAntiguedad(estado.ultimaSync, ahora),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    estado.error?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                IconButton(onClick = onSincronizar, enabled = !estado.enCurso) {
                    Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
                }
            }
            textoFallidas(estado.fallidas)?.let { texto ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        texto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRevisar) { Text("Revisar") }
                    TextButton(onClick = onReintentarFallidas, enabled = !estado.enCurso) { Text("Reintentar ahora") }
                }
            }
            if (estado.enCurso) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
