package pe.edu.upeu.pharmamobil.data.repository

import pe.edu.upeu.pharmamobil.data.remote.ProductoApi
import pe.edu.upeu.pharmamobil.data.remote.dto.aDominio
import pe.edu.upeu.pharmamobil.data.remote.dto.aRequest
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobil.domain.model.DatosProducto
import pe.edu.upeu.pharmamobil.domain.model.Inventario
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.model.ResultadoEnvio
import pe.edu.upeu.pharmamobil.domain.repository.EnvioRemoto

/**
 * Envio de filas pendientes a PharmaSoft. Aqui, en la capa de datos, vive la
 * traduccion de ErrorApi (Sesion 8) a las tres ramas de ResultadoEnvio.
 */
class EnvioRemotoRest(private val api: ProductoApi) : EnvioRemoto {

    override suspend fun enviar(producto: Producto): ResultadoEnvio {
        val datos = DatosProducto(producto.nombre, producto.precio, producto.stock, producto.activo, producto.categoriaId)
        return try {
            ResultadoEnvio.Exito(api.crear(datos.aRequest()).id)
        } catch (e: ErrorApiException) {
            when (val error = e.error) {
                // Idempotencia: el nombre es unico en PharmaSoft. Si el 409 es porque YA existe
                // un producto identico, es nuestro envio anterior cuya respuesta se perdio.
                is ErrorApi.Conflicto -> mismoEnvioAnterior(datos)?.let { ResultadoEnvio.Exito(it) }
                    ?: ResultadoEnvio.Permanente(error.mensaje)

                is ErrorApi.NoAutorizado -> throw e // problema de la sesion, no del dato
                else -> error.aResultado()
            }
        }
    }

    override suspend fun descargar(): Inventario = Inventario(
        productos = api.listar().contenido.map { it.aDominio() },
        categorias = api.listarCategorias().filter { it.estado }.map { it.aDominio() }
    )

    private suspend fun mismoEnvioAnterior(datos: DatosProducto): Long? = try {
        api.listar().contenido.firstOrNull {
            it.nombre.equals(datos.nombre, ignoreCase = true) && it.precio == datos.precio &&
                it.stock == datos.stock && it.categoriaId == datos.categoriaId
        }?.id
    } catch (e: ErrorApiException) {
        null
    }
}

/** Transitorio: puede funcionar mas tarde. Permanente: reintentar no cambia nada. */
internal fun ErrorApi.aResultado(): ResultadoEnvio = when (this) {
    ErrorApi.SinConexion -> ResultadoEnvio.Transitorio(mensaje, sinConexion = true)
    ErrorApi.TiempoAgotado, is ErrorApi.Servidor, ErrorApi.RespuestaInvalida ->
        ResultadoEnvio.Transitorio(mensaje)

    is ErrorApi.Validacion -> ResultadoEnvio.Permanente(
        if (campos.isEmpty()) mensaje else campos.values.joinToString("; ")
    )

    is ErrorApi.NoEncontrado, is ErrorApi.Conflicto, is ErrorApi.Prohibido ->
        ResultadoEnvio.Permanente(mensaje)

    is ErrorApi.NoAutorizado -> ResultadoEnvio.Permanente("Sesion vencida")
}
