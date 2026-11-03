package pe.edu.upeu.pharmamobil

/** Informacion minima de la plataforma en la que corre el codigo comun. */
interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
