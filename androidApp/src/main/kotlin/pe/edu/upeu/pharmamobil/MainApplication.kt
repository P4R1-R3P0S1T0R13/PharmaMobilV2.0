package pe.edu.upeu.pharmamobil

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.di.initKoin
import pe.edu.upeu.pharmamobil.platform.InfoApp

/** Arranca Koin una sola vez, antes de cualquier Activity. */
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@MainApplication)
            // Datos que solo conoce el modulo de aplicacion (BuildConfig).
            modules(
                module {
                    single { InfoApp(depuracion = BuildConfig.DEBUG, version = BuildConfig.VERSION_NAME) }
                }
            )
        }
    }
}
