package pe.edu.upeu.pharmamobil.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi

actual val platformModule: Module = module {
    // El simulador de iOS comparte la red de la Mac: localhost funciona directo.
    single { ConfiguracionApi(urlBase = "http://localhost:8080/") }
    single<HttpClientEngine> { Darwin.create() }
}
