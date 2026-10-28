// Carga la biblioteca una vez y la deja armada en filas para el inicio. Vive mientras el inicio
// está en la pila de navegación: al volver de un video no se vuelve a leer la carpeta.
package pe.inkaterra.tv.pantallas.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.inkaterra.tv.datos.biblioteca.Biblioteca

sealed interface EstadoInicio {
    data object Cargando : EstadoInicio

    data class Listo(
        val filas: List<Fila>,
    ) : EstadoInicio
}

class InicioModelo(
    biblioteca: Biblioteca,
) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoInicio>(EstadoInicio.Cargando)
    val estado: StateFlow<EstadoInicio> = _estado.asStateFlow()

    init {
        viewModelScope.launch { _estado.value = EstadoInicio.Listo(filasDe(biblioteca.cargar())) }
    }
}
