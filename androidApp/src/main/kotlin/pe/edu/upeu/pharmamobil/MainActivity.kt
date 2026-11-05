package pe.edu.upeu.pharmamobil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.koin.android.ext.android.inject
import pe.edu.upeu.pharmamobil.data.permiso.GestorPermisosAndroid
import pe.edu.upeu.pharmamobil.data.permiso.PuentePermisos

class MainActivity : ComponentActivity() {

    private val gestorPermisos: GestorPermisosAndroid by inject()
    private var puente: PuentePermisos? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // El lanzador de permisos debe registrarse en onCreate, antes de onStart.
        puente = PuentePermisos(this).also { gestorPermisos.puente = it }
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        // Al rotar se crea otra Activity: no se deja un puente que apunte a la anterior.
        if (gestorPermisos.puente === puente) gestorPermisos.puente = null
        super.onDestroy()
    }
}
