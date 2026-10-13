package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.domain.platform.Compartidor

class FakeCompartidor : Compartidor {
    var ultimoTextoCompartido: String? = null

    override fun compartir(texto: String) {
        ultimoTextoCompartido = texto
    }
}
