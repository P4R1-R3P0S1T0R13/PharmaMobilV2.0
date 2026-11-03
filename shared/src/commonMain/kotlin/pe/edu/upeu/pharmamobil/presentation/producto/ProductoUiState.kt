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
    val mensaje: String? = null,
    /** Sesion 11: sincronizacion en segundo plano con PharmaSoft. */
    val sincronizando: Boolean = false,
    /** Aviso persistente cuando se muestran datos locales sin conexion. */
    val avisoRed: String? = null,
    /** Instante de la ultima sincronizacion correcta; null si nunca se sincronizo. */
    val ultimaSincronizacion: Long? = null
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

/** Antiguedad de los datos en lenguaje natural: "Actualizado hace 2 horas". */
fun textoAntiguedad(ultimaMs: Long?, ahoraMs: Long): String {
    if (ultimaMs == null) return "Aun no se sincronizo con el servidor"
    val minutos = ((ahoraMs - ultimaMs).coerceAtLeast(0)) / 60_000
    val horas = minutos / 60
    val dias = horas / 24
    return when {
        minutos < 1 -> "Actualizado hace un momento"
        minutos < 60 -> "Actualizado hace $minutos ${if (minutos == 1L) "minuto" else "minutos"}"
        horas < 24 -> "Actualizado hace $horas ${if (horas == 1L) "hora" else "horas"}"
        else -> "Actualizado hace $dias ${if (dias == 1L) "dia" else "dias"}"
    }
}
