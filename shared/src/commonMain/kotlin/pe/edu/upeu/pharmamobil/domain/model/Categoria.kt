package pe.edu.upeu.pharmamobil.domain.model

data class Categoria(
    val id: Long,
    val nombre: String
)

/** Datos que el usuario ingresa para crear o actualizar un producto. */
data class DatosProducto(
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean,
    val categoriaId: Long
)
