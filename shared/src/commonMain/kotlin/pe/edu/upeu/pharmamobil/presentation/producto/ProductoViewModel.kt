package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.error.ErrorApi
import pe.edu.upeu.pharmamobil.domain.error.comoErrorApi
import pe.edu.upeu.pharmamobil.domain.model.Producto
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobil.domain.usecase.runCatchingSuspend

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val repositorio: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    /** Ultima lista emitida por la base local. */
    private var productos: List<Producto>? = null

    init {
        // Offline-first: la pantalla se suscribe una vez a la base local y se
        // actualiza sola. Nunca se vuelve a pedir la lista a mano.
        viewModelScope.launch {
            repositorio.observar().collect { lista ->
                productos = lista
                _uiState.update { it.copy(fase = faseDe(lista, it)) }
            }
        }
        viewModelScope.launch {
            repositorio.observarCategorias().collect { categorias ->
                _uiState.update { it.copy(categorias = categorias) }
            }
        }
        viewModelScope.launch {
            repositorio.observarUltimaSincronizacion().collect { ultima ->
                _uiState.update { it.copy(ultimaSincronizacion = ultima) }
            }
        }
        sincronizar()
    }

    private fun faseDe(lista: List<Producto>?, estado: ProductoUiState): ProductoUiState.Fase = when {
        lista == null -> ProductoUiState.Fase.Cargando
        lista.isNotEmpty() -> ProductoUiState.Fase.ConProductos(lista)
        // Primera apertura: aun no hay datos locales y se esta consultando al servidor.
        estado.sincronizando && estado.ultimaSincronizacion == null -> ProductoUiState.Fase.Cargando
        else -> ProductoUiState.Fase.SinProductos
    }

    // ---------- Sincronizacion ----------

    fun sincronizar() {
        if (_uiState.value.sincronizando) return
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true).let { e -> e.copy(fase = faseDe(productos, e)) } }
            val resultado = runCatchingSuspend { repositorio.sincronizar() }
            _uiState.update { estado ->
                val error = resultado.exceptionOrNull()?.comoErrorApi()
                    ?: ErrorApi.SinConexion.takeIf { resultado.getOrNull()?.sinConexion == true }
                val aviso = when (error) {
                    null -> null
                    ErrorApi.SinConexion, ErrorApi.TiempoAgotado -> "Sin conexion: mostrando los datos guardados en el dispositivo."
                    else -> error.mensaje
                }
                estado.copy(sincronizando = false, avisoRed = aviso).let { e -> e.copy(fase = faseDe(productos, e)) }
            }
        }
    }

    fun onPestana(indice: Int) = _uiState.update { it.copy(pestana = indice) }

    fun mensajeMostrado() = _uiState.update { it.copy(mensaje = null) }

    // ---------- Formulario ----------

    fun nuevo() = _uiState.update { it.copy(formulario = FormularioProducto(visible = true)) }

    fun editar(producto: Producto) = _uiState.update {
        it.copy(
            formulario = FormularioProducto(
                visible = true,
                idEnEdicion = producto.id,
                nombre = producto.nombre,
                precio = producto.precio.toString(),
                stock = producto.stock.toString(),
                activo = producto.activo,
                categoriaId = producto.categoriaId.takeIf { id -> id > 0 }
            )
        )
    }

    fun cancelarFormulario() = _uiState.update { it.copy(formulario = FormularioProducto()) }

    fun onNombreChange(valor: String) = actualizarFormulario { it.copy(nombre = valor, nombreError = null) }
    fun onPrecioChange(valor: String) = actualizarFormulario { it.copy(precio = valor, precioError = null) }
    fun onStockChange(valor: String) = actualizarFormulario { it.copy(stock = valor, stockError = null) }
    fun onActivoChange(valor: Boolean) = actualizarFormulario { it.copy(activo = valor) }
    fun onCategoriaChange(id: Long) = actualizarFormulario { it.copy(categoriaId = id, categoriaError = null) }

    private fun actualizarFormulario(cambio: (FormularioProducto) -> FormularioProducto) =
        _uiState.update { it.copy(formulario = cambio(it.formulario)) }

    // ---------- Escritura ----------

    fun guardar() {
        if (_uiState.value.ocupado) return
        val f = _uiState.value.formulario
        viewModelScope.launch {
            _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Guardando) }
            registrarProducto(f.idEnEdicion, f.nombre, f.precio, f.stock, f.activo, f.categoriaId).fold(
                onSuccess = { producto ->
                    val accion = when {
                        producto.pendiente -> "guardado sin conexion; se enviara al sincronizar"
                        f.idEnEdicion == null -> "registrado correctamente"
                        else -> "actualizado correctamente"
                    }
                    // No se recarga nada: la base local emite y la lista se actualiza sola.
                    _uiState.update {
                        it.copy(
                            operacion = ProductoUiState.Operacion.Ninguna,
                            formulario = FormularioProducto(),
                            mensaje = "Producto \"${producto.nombre}\" $accion"
                        )
                    }
                },
                onFailure = { fallo -> mostrarErrorDeGuardado(fallo) }
            )
        }
    }

    private fun mostrarErrorDeGuardado(fallo: Throwable) {
        if (fallo is ProductoInvalidoException) {
            val e = fallo.errores
            _uiState.update {
                it.copy(
                    operacion = ProductoUiState.Operacion.Ninguna,
                    formulario = it.formulario.copy(
                        nombreError = e.nombre, precioError = e.precio,
                        stockError = e.stock, categoriaError = e.categoria
                    )
                )
            }
            return
        }
        val error = fallo.comoErrorApi()
        _uiState.update {
            val formulario = if (error is ErrorApi.Validacion) {
                // Errores 400 del servidor: debajo del campo correspondiente.
                it.formulario.copy(
                    nombreError = error.campos["nombre"] ?: it.formulario.nombreError,
                    precioError = error.campos["precio"] ?: it.formulario.precioError,
                    stockError = error.campos["stock"] ?: it.formulario.stockError,
                    categoriaError = error.campos["categoriaId"] ?: it.formulario.categoriaError
                )
            } else {
                it.formulario
            }
            it.copy(operacion = ProductoUiState.Operacion.Ninguna, formulario = formulario, mensaje = error.mensaje)
        }
    }

    fun pedirEliminar(producto: Producto) = _uiState.update { it.copy(productoAEliminar = producto) }

    fun cancelarEliminar() = _uiState.update { it.copy(productoAEliminar = null) }

    fun confirmarEliminar() {
        val producto = _uiState.value.productoAEliminar ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(productoAEliminar = null, operacion = ProductoUiState.Operacion.Eliminando(producto.id))
            }
            val resultado = runCatchingSuspend { repositorio.eliminar(producto.id) }
            _uiState.update {
                it.copy(
                    operacion = ProductoUiState.Operacion.Ninguna,
                    mensaje = resultado.fold(
                        onSuccess = { "Producto \"${producto.nombre}\" eliminado" },
                        onFailure = { fallo -> fallo.comoErrorApi().mensaje }
                    )
                )
            }
        }
    }
}
