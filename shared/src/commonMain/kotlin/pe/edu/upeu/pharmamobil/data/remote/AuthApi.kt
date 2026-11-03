package pe.edu.upeu.pharmamobil.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(val username: String, val password: String)

/** Respuesta de POST /api/v1/auth/login (LoginResponseDTO). */
@Serializable
data class LoginResponseDto(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long,
    val username: String,
    val nombreCompleto: String = "",
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList()
)

class AuthApi(private val client: HttpClient) {

    suspend fun login(request: LoginRequestDto): LoginResponseDto = llamadaApi {
        client.post("api/v1/auth/login") { setBody(request) }.body()
    }
}

/** Reportes: solo ADMIN (REPORTES_VER). Sirve para comprobar el 403. */
class ReporteApi(private val client: HttpClient) {

    suspend fun productosMasVendidos(): List<Map<String, kotlinx.serialization.json.JsonElement>> = llamadaApi {
        client.get("api/v1/reportes/productos-mas-vendidos").body()
    }
}
