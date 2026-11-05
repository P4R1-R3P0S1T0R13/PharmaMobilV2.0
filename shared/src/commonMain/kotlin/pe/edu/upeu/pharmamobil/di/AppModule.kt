package pe.edu.upeu.pharmamobil.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.auth.GestorSesion
import pe.edu.upeu.pharmamobil.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobil.data.local.ProductoLocal
import pe.edu.upeu.pharmamobil.data.remote.AuthApi
import pe.edu.upeu.pharmamobil.data.remote.DiagnosticoApi
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.ReporteApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.repository.AuthRepositorio
import pe.edu.upeu.pharmamobil.data.repository.EnvioRemotoRest
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioOffline
import pe.edu.upeu.pharmamobil.data.repository.ReporteRepositorio
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.domain.port.Telemetria
import pe.edu.upeu.pharmamobil.domain.port.TelemetriaNula
import pe.edu.upeu.pharmamobil.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobil.domain.repository.ColaSincronizacion
import pe.edu.upeu.pharmamobil.domain.repository.DatosLocales
import pe.edu.upeu.pharmamobil.domain.repository.EnvioRemoto
import pe.edu.upeu.pharmamobil.domain.repository.DiagnosticoSesion
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.repository.ReporteRepository
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.SincronizarUseCase
import pe.edu.upeu.pharmamobil.platform.ahoraMs
import pe.edu.upeu.pharmamobil.presentation.inicio.InicioViewModel
import pe.edu.upeu.pharmamobil.presentation.login.LoginViewModel
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel

/** single: una sola sesion, un solo cliente HTTP y un solo inventario para toda la app. */
val dataModule = module {
    single { GestorSesion(almacen = get(), ahoraMs = ::ahoraMs) }
    single { crearHttpClient(engine = get(), configuracion = get(), gestorSesion = get()) }
    single { ProductoApi(get()) }
    single { AuthApi(get()) }
    single { ReporteApi(get()) }
    // Sesion 11: base local como fuente de verdad.
    single { PharmaDb(get<CreadorDeDriver>().crear()) }
    single { ProductoLocal(get(), ::ahoraMs) }
    // Sesion 13: la cola local y el envio remoto se exponen por sus interfaces de dominio.
    single<ColaSincronizacion> { get<ProductoLocal>() }
    single<DatosLocales> { get<ProductoLocal>() }
    single<EnvioRemoto> { EnvioRemotoRest(get()) }
    single<Telemetria> { TelemetriaNula }
    factory { SincronizarUseCase(get(), get(), get()) }
    single<ProductoRepository> { ProductoRepositorioOffline(get(), get(), get()) }
    single<AuthRepository> { AuthRepositorio(get(), get(), ::ahoraMs) }
    single<ReporteRepository> { ReporteRepositorio(get()) }
    single<DiagnosticoSesion> { DiagnosticoApi(get(), get()) }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
}

val presentationModule = module {
    viewModel { ProductoViewModel(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { InicioViewModel(get(), get()) }
}

/** Dependencias que solo existen en una plataforma (Android o iOS). */
expect val platformModule: Module

fun initKoin(configuracion: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracion()
        modules(dataModule, domainModule, presentationModule, platformModule)
    }
}
