package pe.edu.upeu.pharmamobil.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import pe.edu.upeu.pharmamobil.platform.CompartidorAndroid

actual val platformModule: Module = module {
    // androidContext() dentro de un lazy: el Context se resuelve al
    // compartir, no al montar el grafo (el grafo se ensambla en tests JVM
    // donde no hay Context de Android registrado).
    single<Compartidor> { CompartidorAndroid(lazy { androidContext() }) }
}
