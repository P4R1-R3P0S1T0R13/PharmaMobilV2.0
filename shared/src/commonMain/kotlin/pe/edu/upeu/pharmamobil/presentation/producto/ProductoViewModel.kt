package pe.edu.upeu.pharmamobil.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobil.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobil.domain.usecase.ProductoInvalidoException
import pe.edu.upeu.pharmamobil.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val repositorio: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }
            val fase = runCatching { repositorio.listar() }.fold(
                onSuccess = { lista ->
                    if (lista.isEmpty()) ProductoUiState.Fase.SinProductos
                    else ProductoUiState.Fase.ConProductos(lista)
                },
                onFailure = { ProductoUiState.Fase.Error(it.message ?: "No se pudo cargar el inventario") }
            )
            _uiState.update { it.copy(fase = fase) }
        }
    }

    fun onPestana(indice: Int) = _uiState.update { it.copy(pestana = indice) }

    fun onNombreChange(valor: String) = _uiState.update {
        it.copy(formulario = it.formulario.copy(nombre = valor, nombreError = null), mensajeExito = null)
    }

    fun onPrecioChange(valor: String) = _uiState.update {
        it.copy(formulario = it.formulario.copy(precio = valor, precioError = null), mensajeExito = null)
    }

    fun onStockChange(valor: String) = _uiState.update {
        it.copy(formulario = it.formulario.copy(stock = valor, stockError = null), mensajeExito = null)
    }

    fun registrar() {
        if (_uiState.value.registrando) return
        val formulario = _uiState.value.formulario
        viewModelScope.launch {
            _uiState.update { it.copy(registrando = true, mensajeExito = null) }
            registrarProducto(formulario.nombre, formulario.precio, formulario.stock).fold(
                onSuccess = { producto ->
                    _uiState.update {
                        it.copy(
                            registrando = false,
                            formulario = FormularioProducto(),
                            mensajeExito = "Producto \"${producto.nombre}\" registrado correctamente"
                        )
                    }
                    cargar()
                },
                onFailure = { fallo ->
                    _uiState.update {
                        if (fallo is ProductoInvalidoException) {
                            it.copy(
                                registrando = false,
                                formulario = it.formulario.copy(
                                    nombreError = fallo.errores.nombre,
                                    precioError = fallo.errores.precio,
                                    stockError = fallo.errores.stock
                                )
                            )
                        } else {
                            it.copy(
                                registrando = false,
                                fase = ProductoUiState.Fase.Error(fallo.message ?: "No se pudo registrar")
                            )
                        }
                    }
                }
            )
        }
    }
}
