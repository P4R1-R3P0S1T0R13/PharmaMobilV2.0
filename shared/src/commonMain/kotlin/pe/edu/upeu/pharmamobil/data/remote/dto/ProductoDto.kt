package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable
import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Producto

/** Respuesta de GET /api/v1/productos/{id} (ProductoResponseDTO de PharmaSoft). */
@Serializable
data class ProductoDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean,
    val categoriaId: Long,
    val categoriaNombre: String? = null,
    val fechaCreacion: String? = null,
    val fechaModificacion: String? = null
)

/** Cuerpo de POST y PUT /api/v1/productos (ProductoRequestDTO). */
@Serializable
data class ProductoRequestDto(
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean,
    val categoriaId: Long
)

/** Envoltorio de paginacion (PaginaResponseDTO<T> de PharmaSoft). */
@Serializable
data class PaginaDto<T>(
    val contenido: List<T>,
    val pagina: Int,
    val tamanio: Int,
    val totalElementos: Long,
    val totalPaginas: Int,
    val ultima: Boolean
)

/** Respuesta de GET /api/v1/categorias. */
@Serializable
data class CategoriaDto(
    val id: Long,
    val nombre: String,
    val descripcion: String? = null,
    val estado: Boolean = true
)

/** Cuerpo de error comun de PharmaSoft (ErrorResponseDTO). */
@Serializable
data class ErrorResponseDto(
    val timestamp: String? = null,
    val status: Int = 0,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val validationErrors: Map<String, String>? = null
)

fun ProductoDto.aDominio(): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock,
    activo = estado,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre
)

fun DatosProducto.aRequest(): ProductoRequestDto = ProductoRequestDto(
    nombre = nombre,
    precio = precio,
    stock = stock,
    estado = activo,
    categoriaId = categoriaId
)

fun CategoriaDto.aDominio(): Categoria = Categoria(id = id, nombre = nombre)
