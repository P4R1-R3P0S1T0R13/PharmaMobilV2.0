package pe.edu.upeu.pharmamobil.data.remote.dto

import kotlinx.serialization.Serializable
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

fun ProductoDto.aDominio(): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock,
    activo = estado,
    categoriaId = categoriaId,
    categoriaNombre = categoriaNombre
)
