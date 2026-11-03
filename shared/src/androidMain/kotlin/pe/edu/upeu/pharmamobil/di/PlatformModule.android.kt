package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    // 10.0.2.2 es la IP con la que el emulador de Android ve al localhost de la PC.
    single { ConfiguracionApi(urlBase = "http://10.0.2.2:8080/") }
    single<HttpClientEngine> { OkHttp.create() }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
}
