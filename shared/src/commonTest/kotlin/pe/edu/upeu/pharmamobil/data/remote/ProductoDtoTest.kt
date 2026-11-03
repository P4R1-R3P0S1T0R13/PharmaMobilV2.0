package pe.edu.upeu.pharmamobil.data.remote

import pe.edu.upeu.pharmamobil.data.remote.dto.PaginaDto
import pe.edu.upeu.pharmamobil.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobil.data.remote.dto.aDominio
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Verifica el contrato real de PharmaSoft sin levantar el servidor. */
class ProductoDtoTest {

    private val respuesta = """
        {
          "contenido": [
            {"id": 1, "nombre": "Paracetamol 500 mg", "precio": 15.5, "stock": 100, "estado": true,
             "categoriaId": 1, "categoriaNombre": "Analgesicos",
             "fechaCreacion": "2026-11-03T09:00:00", "fechaModificacion": null},
            {"id": 4, "nombre": "Loratadina 10 mg", "precio": 12.5, "stock": 0, "estado": false,
             "categoriaId": 3, "categoriaNombre": "Antialergicos", "campoNuevo": "se ignora"}
          ],
          "pagina": 0, "tamanio": 100, "totalElementos": 2, "totalPaginas": 1, "ultima": true
        }
    """.trimIndent()

    @Test
    fun deserializaLaPaginaYMapeaAlDominio() {
        val pagina = JsonPharma.decodeFromString<PaginaDto<ProductoDto>>(respuesta)
        assertEquals(2, pagina.contenido.size)
        val loratadina = pagina.contenido[1].aDominio()
        assertEquals("Loratadina 10 mg", loratadina.nombre)
        assertFalse(loratadina.activo)
        assertEquals(3L, loratadina.categoriaId)
    }
}
