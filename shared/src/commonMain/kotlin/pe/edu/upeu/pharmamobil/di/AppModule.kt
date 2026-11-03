package pe.edu.upeu.pharmamobil.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.data.repository.ProductoRepositorioEnMemoria
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.presentation.producto.ProductoViewModel

/** single: todas las pantallas comparten el mismo inventario. */
val dataModule = module {
    single<ProductoRepository> { ProductoRepositorioEnMemoria() }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
}

val presentationModule = module {
    viewModel { ProductoViewModel(get(), get()) }
}

/** Dependencias que solo existen en una plataforma (Android o iOS). */
expect val platformModule: Module

fun initKoin(configuracion: KoinApplication.() -> Unit = {}) {
    startKoin {
        configuracion()
        modules(dataModule, domainModule, presentationModule, platformModule)
    }
}
