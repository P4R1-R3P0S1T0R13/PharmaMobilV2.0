package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import pe.edu.upeu.pharmamobil.data.remote.dto.CategoriaDto
import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoRequestDto

/** Endpoints de /api/v1/productos y /api/v1/categorias. Solo HTTP y DTO. */
class ProductoApi(private val client: HttpClient) {

    suspend fun listar(pagina: Int = 0, tamanio: Int = 100): PaginaDto<ProductoDto> = llamadaApi {
        client.get(RUTA) {
            parameter("pagina", pagina)
            parameter("tamanio", tamanio)
            parameter("ordenarPor", "id")
            parameter("direccion", "asc")
        }.body()
    }

    suspend fun obtener(id: Long): ProductoDto = llamadaApi {
        client.get("$RUTA/$id").body()
    }

    /** 201 Created con el producto y su id asignado por el servidor. */
    suspend fun crear(request: ProductoRequestDto): ProductoDto = llamadaApi {
        client.post(RUTA) { setBody(request) }.body()
    }

    suspend fun actualizar(id: Long, request: ProductoRequestDto): ProductoDto = llamadaApi {
        client.put("$RUTA/$id") { setBody(request) }.body()
    }

    /** 204 No Content. */
    suspend fun eliminar(id: Long) {
        llamadaApi { client.delete("$RUTA/$id") }
    }

    suspend fun listarCategorias(): List<CategoriaDto> = llamadaApi {
        client.get(RUTA_CATEGORIAS).body()
    }

    companion object {
        const val RUTA = "api/v1/productos"
        const val RUTA_CATEGORIAS = "api/v1/categorias"
    }
}
