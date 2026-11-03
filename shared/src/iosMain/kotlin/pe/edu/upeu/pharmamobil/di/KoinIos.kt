package pe.edu.upeu.pharmamobil.di

/**
 * Arranque de Koin en iOS, invocado desde iOSApp.swift. Kotlin/Native
 * antepone "do" a las funciones que empiezan con "init": en Swift se llama
 * KoinIosKt.doInitKoinIos().
 */
fun initKoinIos() {
    initKoin()
}
