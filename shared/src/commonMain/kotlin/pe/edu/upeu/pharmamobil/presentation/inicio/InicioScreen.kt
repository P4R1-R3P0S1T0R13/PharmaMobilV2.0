package pe.edu.upeu.pharmamobil.presentation.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import pe.edu.upeu.pharmamobil.domain.model.Sesion
import pe.edu.upeu.pharmamobil.platform.infoDispositivo
import pe.edu.upeu.pharmamobil.presentation.permiso.AvisosViewModel
import pe.edu.upeu.pharmamobil.presentation.permiso.textosAvisos
import pe.edu.upeu.pharmamobil.resources.Res
import pe.edu.upeu.pharmamobil.resources.pharmamobil_logo

@Composable
fun InicioScreen(
    sesion: Sesion,
    viewModel: InicioViewModel,
    avisos: AvisosViewModel,
    modifier: Modifier = Modifier
) {
    val resultadoReportes by viewModel.resultado.collectAsStateWithLifecycle()
    val resultadoDiagnostico by viewModel.resultadoDiagnostico.collectAsStateWithLifecycle()
    val info = remember { infoDispositivo() }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Recurso compartido de composeResources/drawable, el mismo en Android e iOS.
        Image(
            painter = painterResource(Res.drawable.pharmamobil_logo),
            contentDescription = "Logo PharmaMobil",
            modifier = Modifier.size(96.dp)
        )
        Text("Hola, ${sesion.nombreCompleto.ifBlank { sesion.username }}", style = MaterialTheme.typography.headlineSmall)

        Tarjeta("Tu sesion") {
            Text("Usuario: ${sesion.username}")
            Text("Rol: ${sesion.roles.joinToString()}")
            Text("Permisos: ${sesion.permisos.joinToString()}", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = viewModel::probarAccesoReportes) { Text("Probar acceso a reportes") }
            resultadoReportes?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        // Escenarios de la bitacora de tokens (Actividad autonoma 10).
        Tarjeta("Pruebas de sesion") {
            OutlinedButton(onClick = viewModel::consultarMe) { Text("1. Consultar /auth/me (token valido)") }
            OutlinedButton(onClick = viewModel::alterarToken) { Text("3. Alterar token guardado") }
            OutlinedButton(onClick = viewModel::sinCabecera) { Text("4. Peticion sin cabecera") }
            OutlinedButton(onClick = viewModel::prefijoMinuscula) { Text("5. Prefijo 'bearer' en minuscula") }
            resultadoDiagnostico?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }

        TarjetaAvisos(avisos)

        Tarjeta("Este dispositivo") {
            Text("Sistema: ${info.sistema} ${info.version}")
            Text("Modelo: ${info.modelo}")
            Text("Idioma: ${info.idioma}")
        }
    }
}

@Composable
private fun Tarjeta(titulo: String, contenido: @Composable () -> Unit) {
    Card(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

/**
 * Permiso de notificaciones (Sesion 13): se explica antes de pedir y, si el
 * usuario lo bloqueo, se ofrece ir a Ajustes. Al volver de Ajustes se refresca.
 */
@Composable
private fun TarjetaAvisos(viewModel: AvisosViewModel) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refrescar()
        onPauseOrDispose { }
    }
    val (texto, accion) = textosAvisos(estado.estado)
    Tarjeta("Avisos de PharmaMobil") {
        Text(texto)
        accion?.let { Button(onClick = viewModel::accionPrincipal) { Text(it) } }
    }
    if (estado.mostrarExplicacion) {
        AlertDialog(
            onDismissRequest = viewModel::cancelarExplicacion,
            title = { Text("¿Activar avisos?") },
            text = {
                Text(
                    "PharmaMobil te enviara una notificacion cuando tus cambios guardados sin conexion " +
                        "lleguen a PharmaSoft, cuando alguno no se pueda enviar y cuando haya productos con bajo stock. " +
                        "No se usan para publicidad."
                )
            },
            confirmButton = { TextButton(onClick = viewModel::aceptarExplicacion) { Text("Continuar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarExplicacion) { Text("Ahora no") } }
        )
    }
}
