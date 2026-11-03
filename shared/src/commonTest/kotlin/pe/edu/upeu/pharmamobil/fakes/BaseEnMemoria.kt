package pe.edu.upeu.pharmamobil.fakes

import app.cash.sqldelight.db.SqlDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

/** Driver en memoria de cada plataforma de prueba (JDBC en la JVM, nativo en iOS). */
expect fun crearDriverEnMemoria(): SqlDriver

/** Una base nueva por prueba: compartirla hace que las pruebas dependan del orden. */
fun baseEnMemoria(): PharmaDb = PharmaDb(crearDriverEnMemoria())
