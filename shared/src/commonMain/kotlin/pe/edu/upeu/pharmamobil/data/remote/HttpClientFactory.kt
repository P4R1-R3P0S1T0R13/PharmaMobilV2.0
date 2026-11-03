package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** URL del backend segun la plataforma (la provee el platformModule). */
data class ConfiguracionApi(val urlBase: String)

val JsonPharma = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

/**
 * Cliente HTTP comun. El motor (OkHttp en Android, Darwin en iOS) llega
 * desde cada plataforma; todo lo demas se configura una sola vez aqui.
 */
fun crearHttpClient(engine: HttpClientEngine, configuracion: ConfiguracionApi): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(JsonPharma) }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 15_000
        }
        defaultRequest {
            url(configuracion.urlBase)
            contentType(ContentType.Application.Json)
        }
    }
