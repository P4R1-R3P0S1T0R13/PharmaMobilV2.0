package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Todo lo que la pantalla de Productos necesita dibujar, en un solo objeto
 * inmutable. La fase del inventario es excluyente: solo una a la vez.
 */
data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val pestana: Int = 0,
    val registrando: Boolean = false,
    val mensajeExito: String? = null
) {
    sealed interface Fase {
        data object Cargando : Fase
        data object SinProductos : Fase
        data class ConProductos(val productos: List<Producto>) : Fase
        data class Error(val mensaje: String) : Fase
    }
}

data class FormularioProducto(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null
)

val PESTANAS = listOf("Activos", "Inactivos", "Bajo stock")

/** Filtro de cada pestana: 0 Activos, 1 Inactivos, 2 Bajo stock (stock <= 5). */
fun filtrarPorPestana(productos: List<Producto>, pestana: Int): List<Producto> =
    when (pestana) {
        0 -> productos.filter { it.activo }
        1 -> productos.filter { !it.activo }
        else -> productos.filter { it.requiereReposicion }
    }
