package pe.edu.upeu.pharmamobil.presentation.producto

import pe.edu.upeu.pharmamobil.domain.model.Categoria
import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Todo lo que la pantalla de Productos necesita dibujar, en un solo objeto
 * inmutable. La fase del inventario es excluyente: solo una a la vez.
 */
data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val categorias: List<Categoria> = emptyList(),
    val pestana: Int = 0,
    val operacion: Operacion = Operacion.Ninguna,
    val productoAEliminar: Producto? = null,
    val mensaje: String? = null
) {
    sealed interface Fase {
        data object Cargando : Fase
        data object SinProductos : Fase
        data class ConProductos(val productos: List<Producto>) : Fase
        data class Error(val mensaje: String) : Fase
    }

    /** Operacion de escritura en curso: deshabilita botones y muestra progreso. */
    sealed interface Operacion {
        data object Ninguna : Operacion
        data object Guardando : Operacion
        data class Eliminando(val id: Long) : Operacion
    }

    val ocupado: Boolean
        get() = operacion != Operacion.Ninguna
}

data class FormularioProducto(
    val visible: Boolean = false,
    /** null = producto nuevo (POST); con valor = edicion (PUT). */
    val idEnEdicion: Long? = null,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val activo: Boolean = true,
    val categoriaId: Long? = null,
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null,
    val categoriaError: String? = null
)

val PESTANAS = listOf("Activos", "Inactivos", "Bajo stock")

/** Filtro de cada pestana: 0 Activos, 1 Inactivos, 2 Bajo stock (stock <= 5). */
fun filtrarPorPestana(productos: List<Producto>, pestana: Int): List<Producto> =
    when (pestana) {
        0 -> productos.filter { it.activo }
        1 -> productos.filter { !it.activo }
        else -> productos.filter { it.requiereReposicion }
    }
