package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroAndroid
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverAndroid
import pe.edu.upeu.pharmamobil.data.permiso.GestorPermisosAndroid
import pe.edu.upeu.pharmamobil.data.permiso.NotificadorAndroid
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    // ConfiguracionApi (URL de PharmaSoft) la registra MainApplication desde BuildConfig (Sesion 14).
    single<HttpClientEngine> { OkHttp.create() }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
    // Sesion 13: permisos y notificaciones. MainActivity le conecta su PuentePermisos.
    single {
        GestorPermisosAndroid(
            androidContext(),
            androidContext().getSharedPreferences("pharmamobil_permisos", android.content.Context.MODE_PRIVATE)
        )
    }
    single<GestorPermisos> { get<GestorPermisosAndroid>() }
    single<Notificador> { NotificadorAndroid(androidContext()) }
}
