package pe.edu.upeu.pharmamobil

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pe.edu.upeu.pharmamobil.domain.model.EstadoSesion
import pe.edu.upeu.pharmamobil.domain.model.Permisos
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.domain.model.textoParaCompartir
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.navigation.Screen
import pe.edu.upeu.pharmamobil.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.formatearSoles
import pe.edu.upeu.pharmamobil.presentation.cliente.ClienteScreen
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioScreen
import pe.edu.upeu.pharmamobil.presentation.login.LoginScreen
import pe.edu.upeu.pharmamobil.presentation.pedido.PedidoScreen
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoScreen
import pe.edu.upeu.pharmamobil.presentation.sincronizacion.SincronizacionViewModel
import pe.edu.upeu.pharmamobil.theme.PharmaMobilTheme

/** Guarda el destino actual como indice para sobrevivir a la rotacion. */
private val ScreenSaver = Saver<Screen, Int>(
    save = { Screen.todos.indexOf(it) },
    restore = { Screen.todos[it] }
)

/** Acciones comunes de la estructura principal. */
private class Navegacion(
    val actual: Screen,
    val darkTheme: Boolean,
    val sesion: Sesion,
    val navegar: (Screen) -> Unit,
    val cambiarTema: () -> Unit,
    val cerrarSesion: () -> Unit,
    /** Uno por sesion: lo comparten la barra de Productos y el aviso al cerrar sesion. */
    val sincronizacion: SincronizacionViewModel
)

@Composable
fun App() = KoinContext {
    var darkTheme by rememberSaveable { mutableStateOf(false) }
    val auth = koinInject<AuthRepository>()
    val estadoSesion by auth.estado.collectAsStateWithLifecycle()

    PharmaMobilTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val estado = estadoSesion) {
                EstadoSesion.Verificando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                is EstadoSesion.Inactiva -> LoginScreen(viewModel = koinViewModel(), motivo = estado.motivo)

                is EstadoSesion.Activa -> Principal(
                    sesion = estado.sesion,
                    darkTheme = darkTheme,
                    cambiarTema = { darkTheme = !darkTheme },
                    cerrarSesion = auth::cerrarSesion
                )
            }
        }
    }
}

@Composable
private fun Principal(sesion: Sesion, darkTheme: Boolean, cambiarTema: () -> Unit, cerrarSesion: () -> Unit) {
    var pantallaActual by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Inicio) }
    val sincronizacion: SincronizacionViewModel = koinViewModel(key = "sync-${sesion.claveDeVista()}")
    val estadoSync by sincronizacion.estado.collectAsStateWithLifecycle()
    var confirmarCierre by remember { mutableStateOf(false) }
    // Sesion 13 (hueco H5): cerrar sesion borra la base local; si quedan cambios sin enviar, se avisa antes.
    val pedirCierre: () -> Unit = {
        if (estadoSync.cambiosSinEnviar > 0) {
            confirmarCierre = true
        } else {
            cerrarSesion()
        }
    }
    val nav = Navegacion(pantallaActual, darkTheme, sesion, { pantallaActual = it }, cambiarTema, pedirCierre, sincronizacion)

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("Cambios sin enviar") },
            text = {
                Text(
                    "Tienes ${estadoSync.cambiosSinEnviar} cambio(s) que todavia no llegaron a PharmaSoft. " +
                        "Si cierras sesion se borraran de este dispositivo."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmarCierre = false; cerrarSesion() }) { Text("Cerrar sesion igual") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") } }
        )
    }

    // Navegacion adaptativa segun el ancho disponible.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        when {
            maxWidth < 600.dp -> NavegacionTelefono(nav)
            maxWidth < 840.dp -> NavegacionTablet(nav)
            else -> NavegacionEscritorio(nav)
        }
    }
}

/** Telefono: drawer modal que se abre desde el icono de menu. */
@Composable
private fun NavegacionTelefono(nav: Navegacion) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ContenidoDrawer(nav) {
                    nav.navegar(it)
                    scope.launch { drawerState.close() }
                }
            }
        }
    ) {
        Estructura(nav, onMenu = { scope.launch { drawerState.open() } })
    }
}

/** Tablet: NavigationRail fijo a la izquierda. */
@Composable
private fun NavegacionTablet(nav: Navegacion) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            Spacer(Modifier.height(12.dp))
            Screen.todos.forEach { destino ->
                NavigationRailItem(
                    selected = nav.actual == destino,
                    onClick = { nav.navegar(destino) },
                    icon = { Icon(destino.icono, contentDescription = null) },
                    label = { Text(destino.titulo) }
                )
            }
        }
        Estructura(nav, onMenu = null)
    }
}

/** Escritorio o plegable abierto: drawer permanente. */
@Composable
private fun NavegacionEscritorio(nav: Navegacion) {
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(modifier = Modifier.width(280.dp)) { ContenidoDrawer(nav, nav.navegar) }
        }
    ) {
        Estructura(nav, onMenu = null)
    }
}

@Composable
private fun ContenidoDrawer(nav: Navegacion, onSeleccion: (Screen) -> Unit) {
    Column {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("PharmaMobil", style = MaterialTheme.typography.titleLarge)
            Text(
                "${nav.sesion.nombreCompleto.ifBlank { nav.sesion.username }} · ${nav.sesion.roles.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Screen.todos.forEach { destino ->
            NavigationDrawerItem(
                label = { Text(destino.titulo) },
                selected = nav.actual == destino,
                onClick = { onSeleccion(destino) },
                icon = { Icon(destino.icono, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            label = { Text("Cerrar sesion") },
            selected = false,
            onClick = nav.cerrarSesion,
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Estructura(nav: Navegacion, onMenu: (() -> Unit)?) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(nav.actual.titulo) },
                navigationIcon = {
                    if (onMenu != null) {
                        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Abrir menu") }
                    }
                },
                actions = {
                    IconButton(onClick = nav.cambiarTema) {
                        Icon(
                            if (nav.darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Cambiar tema"
                        )
                    }
                    IconButton(onClick = nav.cerrarSesion) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesion")
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
        val clave = nav.sesion.claveDeVista()
        when (nav.actual) {
            Screen.Inicio -> InicioScreen(nav.sesion, koinViewModel(key = "inicio-$clave"), contenido)
            Screen.Productos -> ProductosConCapacidadesNativas(nav.sesion, nav.sincronizacion, contenido)
            Screen.Clientes -> ClienteScreen(contenido)
            Screen.Pedidos -> PedidoScreen(contenido)
        }
    }
}

/**
 * Clave de los ViewModel: cambia en cada inicio de sesion (expiraEnMs es distinto),
 * asi que ni otro usuario ni el mismo al volver a entrar ven datos de la sesion anterior.
 */
private fun Sesion.claveDeVista(): String = "$username-$expiraEnMs"

/** Productos con precio en soles (expect/actual), compartir nativo y permisos por rol. */
@Composable
private fun ProductosConCapacidadesNativas(
    sesion: Sesion,
    sincronizacion: SincronizacionViewModel,
    modifier: Modifier
) {
    val compartidor = koinInject<Compartidor>()
    ProductoScreen(
        viewModel = koinViewModel(key = "productos-${sesion.claveDeVista()}"),
        sincronizacion = sincronizacion,
        modifier = modifier,
        formatearPrecio = ::formatearSoles,
        puedeEscribir = sesion.puede(Permisos.CATALOGO_ESCRIBIR),
        accionesExtra = { producto ->
            IconButton(onClick = { compartidor.compartir(producto.textoParaCompartir(::formatearSoles)) }) {
                Icon(Icons.Default.Share, contentDescription = "Compartir")
            }
        }
    )
}
