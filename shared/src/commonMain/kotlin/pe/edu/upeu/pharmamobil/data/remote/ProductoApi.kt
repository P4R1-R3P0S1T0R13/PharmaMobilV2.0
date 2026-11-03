package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto

/** Endpoints de /api/v1/productos. Solo conoce HTTP y DTO, no el dominio. */
class ProductoApi(private val client: HttpClient) {

    suspend fun listar(pagina: Int = 0, tamanio: Int = 100): PaginaDto<ProductoDto> =
        client.get(RUTA) {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
            parameter("ordenarPor", "id")
            parameter("direccion", "asc")
        }.body()

    companion object {
        const val RUTA = "api/v1/productos"
    }
}
