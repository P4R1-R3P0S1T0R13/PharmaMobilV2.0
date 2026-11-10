package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobil.data.local.AlmacenSeguroIos
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriverIos
import pe.edu.upeu.pharmamobil.data.permiso.GestorPermisosIos
import pe.edu.upeu.pharmamobil.data.permiso.NotificadorIos
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.domain.permiso.GestorPermisos
import pe.edu.upeu.pharmamobil.domain.port.Notificador
import pe.edu.upeu.pharmamobil.platform.InfoApp
import pe.edu.upeu.pharmamobil.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorIos

actual val platformModule: Module = module {
    // El simulador de iOS comparte la red de la Mac: localhost funciona directo.
    single { ConfiguracionApi(urlBase = "http://localhost:8080/") }
    single<HttpClientEngine> { Darwin.create() }
    single<Compartidor> { CompartidorIos() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single<CreadorDeDriver> { CreadorDeDriverIos() }
    single<GestorPermisos> { GestorPermisosIos() }
    single<Notificador> { NotificadorIos() }
    // Sesion 14: datos de la build. isDebugBinary es false en la configuracion Release de Xcode.
    single {
        @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
        InfoApp(
            depuracion = kotlin.native.Platform.isDebugBinary,
            version = platform.Foundation.NSBundle.mainBundle
                .objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
        )
    }
}
