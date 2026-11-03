package pe.edu.upeu.pharmamobil.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** Archivo data/data/pe.edu.upeu.pharmamobil/databases/pharma.db */
class CreadorDeDriverAndroid(private val contexto: Context) : CreadorDeDriver {
    override fun crear(): SqlDriver = AndroidSqliteDriver(
        schema = PharmaDb.Schema,
        context = contexto,
        name = "pharma.db"
    )
}
