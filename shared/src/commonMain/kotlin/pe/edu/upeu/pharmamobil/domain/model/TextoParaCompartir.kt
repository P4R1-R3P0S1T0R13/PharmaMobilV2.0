package pe.edu.upeu.pharmamobil.domain.model

/** Texto que se envia al compartir un producto (regla comun a ambas plataformas). */
fun Producto.textoParaCompartir(formatearPrecio: (Double) -> String): String = buildString {
    append("PharmaMobil · ").append(nombre).append('\n')
    categoriaNombre?.let { append("Categoria: ").append(it).append('\n') }
    append("Precio: ").append(formatearPrecio(precio)).append('\n')
    append(if (disponible) "Stock disponible: $stock unidades" else "Sin stock por el momento")
}
