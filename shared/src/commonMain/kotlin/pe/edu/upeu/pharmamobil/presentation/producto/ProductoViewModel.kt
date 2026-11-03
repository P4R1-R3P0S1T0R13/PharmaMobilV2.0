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

    init {
        cargar()
    }

    // ---------- Lectura ----------

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }
            val fase = runCatchingSuspend { repositorio.listar() }.fold(
                onSuccess = { lista ->
                    if (lista.isEmpty()) ProductoUiState.Fase.SinProductos
                    else ProductoUiState.Fase.ConProductos(lista)
                },
                onFailure = { ProductoUiState.Fase.Error(it.comoErrorApi().mensaje) }
            )
            _uiState.update { it.copy(fase = fase) }
            if (_uiState.value.categorias.isEmpty()) cargarCategorias()
        }
    }

    private suspend fun cargarCategorias() {
        runCatchingSuspend { repositorio.listarCategorias() }
            .onSuccess { categorias -> _uiState.update { it.copy(categorias = categorias) } }
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

    // ---------- Escritura: POST / PUT ----------

    fun guardar() {
        if (_uiState.value.ocupado) return
        val f = _uiState.value.formulario
        viewModelScope.launch {
            _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Guardando) }
            registrarProducto(f.idEnEdicion, f.nombre, f.precio, f.stock, f.activo, f.categoriaId).fold(
                onSuccess = { producto ->
                    val accion = if (f.idEnEdicion == null) "registrado" else "actualizado"
                    _uiState.update {
                        it.copy(
                            operacion = ProductoUiState.Operacion.Ninguna,
                            formulario = FormularioProducto(),
                            mensaje = "Producto \"${producto.nombre}\" $accion correctamente"
                        )
                    }
                    cargar()
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

    // ---------- Escritura: DELETE ----------

    fun pedirEliminar(producto: Producto) = _uiState.update { it.copy(productoAEliminar = producto) }

    fun cancelarEliminar() = _uiState.update { it.copy(productoAEliminar = null) }

    fun confirmarEliminar() {
        val producto = _uiState.value.productoAEliminar ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(productoAEliminar = null, operacion = ProductoUiState.Operacion.Eliminando(producto.id))
            }
            runCatchingSuspend { repositorio.eliminar(producto.id) }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(operacion = ProductoUiState.Operacion.Ninguna,
                            mensaje = "Producto \"${producto.nombre}\" eliminado")
                    }
                    cargar()
                },
                onFailure = { fallo ->
                    _uiState.update {
                        it.copy(operacion = ProductoUiState.Operacion.Ninguna, mensaje = fallo.comoErrorApi().mensaje)
                    }
                }
            )
        }
    }
}
