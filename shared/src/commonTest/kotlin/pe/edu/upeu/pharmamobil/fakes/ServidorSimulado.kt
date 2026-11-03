package pe.edu.upeu.pharmamobil.fakes

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.builtins.ListSerializer
import pe.edu.upeu.pharmamobil.data.remote.ConfiguracionApi
import pe.edu.upeu.pharmamobil.data.remote.JsonPharma
import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.crearHttpClient
import pe.edu.upeu.pharmamobil.data.remote.dto.CategoriaDto
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto

/**
 * PharmaSoft en memoria sobre MockEngine. Con sinRed = true toda peticion
 * falla como en modo avion.
 */
class ServidorSimulado(productosIniciales: List<ProductoDto> = emptyList()) {

    var sinRed = false
    val productos = productosIniciales.toMutableList()
    var peticionesCrear = 0
        private set
    private var siguienteId = (productos.maxOfOrNull { it.id } ?: 0) + 1

    private val categorias = listOf(CategoriaDto(1, "Analgesicos"), CategoriaDto(2, "Antibioticos"))
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    val api: ProductoApi by lazy {
        val engine = MockEngine { request ->
            if (sinRed) throw IllegalStateException("Modo avion: sin red")
            val ruta = request.url.encodedPath
            when {
                request.method == HttpMethod.Get && ruta.endsWith("api/v1/productos") -> respond(
                    JsonPharma.encodeToString(
                        PaginaDto.serializer(ProductoDto.serializer()),
                        PaginaDto(productos.toList(), 0, 100, productos.size.toLong(), 1, true)
                    ), HttpStatusCode.OK, json
                )

                request.method == HttpMethod.Get && ruta.endsWith("api/v1/categorias") -> respond(
                    JsonPharma.encodeToString(ListSerializer(CategoriaDto.serializer()), categorias),
                    HttpStatusCode.OK, json
                )

                request.method == HttpMethod.Post && ruta.endsWith("api/v1/productos") -> {
                    peticionesCrear++
                    val cuerpo = JsonPharma.decodeFromString(
                        ProductoRequestDto.serializer(), request.body.toByteArray().decodeToString()
                    )
                    val creado = ProductoDto(
                        siguienteId++, cuerpo.nombre, cuerpo.precio, cuerpo.stock, cuerpo.estado,
                        cuerpo.categoriaId, categorias.firstOrNull { it.id == cuerpo.categoriaId }?.nombre
                    )
                    productos += creado
                    respond(JsonPharma.encodeToString(ProductoDto.serializer(), creado), HttpStatusCode.Created, json)
                }

                else -> respond("""{"status":404,"message":"Ruta no simulada"}""", HttpStatusCode.NotFound, json)
            }
        }
        ProductoApi(crearHttpClient(engine, ConfiguracionApi("http://localhost:8080/")))
    }

    companion object {
        fun dto(id: Long, nombre: String, stock: Int = 10) =
            ProductoDto(id, nombre, 10.0, stock, true, 1, "Analgesicos")
    }
}
