package pe.edu.upeu.pharmamobil.domain.model

/**
 * Cliente de la botica. El id y los nombres son obligatorios (no anulables);
 * el telefono es opcional, por eso se declara String?.
 */
data class Cliente(
    val id: Long,
    val nombres: String,
    val correo: String,
    val telefono: String? = null
) {
    init {
        require(nombres.isNotBlank()) { "El nombre del cliente es obligatorio" }
        require(correo.contains('@')) { "El correo no es valido" }
    }

    /** Operador Elvis: valor por defecto seguro cuando no hay telefono. */
    val telefonoVisible: String
        get() = telefono ?: "No registrado"
}
