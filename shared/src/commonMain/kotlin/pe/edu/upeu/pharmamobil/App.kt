package pe.edu.upeu.pharmamobil

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.pedido.PedidoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

/** Guarda el destino actual como indice para sobrevivir a la rotacion. */
private val ScreenSaver = Saver<Screen, Int>(
    save = { Screen.todos.indexOf(it) },
    restore = { Screen.todos[it] }
)

@Composable
fun App() = KoinContext {
    var pantallaActual by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Inicio) }
    var darkTheme by rememberSaveable { mutableStateOf(false) }

    PharmaMobilTheme(darkTheme = darkTheme) {
        // Navegacion adaptativa segun el ancho disponible.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val navegar: (Screen) -> Unit = { pantallaActual = it }
            val cambiarTema = { darkTheme = !darkTheme }
            when {
                maxWidth < 600.dp -> NavegacionTelefono(pantallaActual, darkTheme, navegar, cambiarTema)
                maxWidth < 840.dp -> NavegacionTablet(pantallaActual, darkTheme, navegar, cambiarTema)
                else -> NavegacionEscritorio(pantallaActual, darkTheme, navegar, cambiarTema)
            }
        }
    }
}

/** Telefono: drawer modal que se abre desde el icono de menu. */
@Composable
private fun NavegacionTelefono(
    actual: Screen, darkTheme: Boolean, navegar: (Screen) -> Unit, cambiarTema: () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ContenidoDrawer(actual) {
                    navegar(it)
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Estructura(actual, darkTheme, cambiarTema, onMenu = { scope.launch { drawerState.open() } })
    }
}

/** Tablet: NavigationRail fijo a la izquierda. */
@Composable
private fun NavegacionTablet(
    actual: Screen, darkTheme: Boolean, navegar: (Screen) -> Unit, cambiarTema: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            Spacer(Modifier.height(12.dp))
            Screen.todos.forEach { destino ->
                NavigationRailItem(
                    selected = actual == destino,
                    onClick = { navegar(destino) },
                    icon = { Icon(destino.icono, contentDescription = null) },
                    label = { Text(destino.titulo) }
                )
            }
        }
        Estructura(actual, darkTheme, cambiarTema, onMenu = null)
    }
}

/** Escritorio o plegable abierto: drawer permanente. */
@Composable
private fun NavegacionEscritorio(
    actual: Screen, darkTheme: Boolean, navegar: (Screen) -> Unit, cambiarTema: () -> Unit
) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(modifier = Modifier.width(260.dp)) { ContenidoDrawer(actual, navegar) }
        }
    ) {
        Estructura(actual, darkTheme, cambiarTema, onMenu = null)
    }
}

@Composable
private fun ContenidoDrawer(actual: Screen, onSeleccion: (Screen) -> Unit) {
    Column {
        Text(
            "PharmaMobil",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(24.dp)
        )
        Screen.todos.forEach { destino ->
            NavigationDrawerItem(
                label = { Text(destino.titulo) },
                selected = actual == destino,
                onClick = { onSeleccion(destino) },
                icon = { Icon(destino.icono, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Estructura(
    actual: Screen,
    darkTheme: Boolean,
    cambiarTema: () -> Unit,
    onMenu: (() -> Unit)?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(actual.titulo) },
                navigationIcon = {
                    if (onMenu != null) {
                        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu") }
                    }
                },
                actions = {
                    IconButton(onClick = cambiarTema) {
                        Icon(
                            if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        val contenido = Modifier.padding(padding)
        when (actual) {
            Screen.Inicio -> InicioScreen(contenido)
            Screen.Productos -> ProductoScreen(viewModel = koinViewModel(), modifier = contenido)
            Screen.Clientes -> ClienteScreen(contenido)
            Screen.Pedidos -> PedidoScreen(contenido)
        }
    }
}
