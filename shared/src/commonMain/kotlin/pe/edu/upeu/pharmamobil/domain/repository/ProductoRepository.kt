package pe.edu.upeu.pharmamobil.domain.repository

import pe.edu.upeu.pharmamobil.domain.model.Producto

/**
 * Contrato del inventario en el lenguaje del negocio. El dominio no sabe si
 * los datos viven en memoria, en un servidor REST o en una base local.
 */
interface ProductoRepository {

    /** Registra un producto nuevo; el repositorio le asigna el id. */
    suspend fun registrar(nombre: String, precio: Double, stock: Int): Producto

    suspend fun listar(): List<Producto>
}
