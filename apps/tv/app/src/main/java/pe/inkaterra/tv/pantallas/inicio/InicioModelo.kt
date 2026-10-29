// Carga la biblioteca una vez y la deja armada en filas para el inicio. Vive mientras el inicio
// está en la pila de navegación: al volver de un video no se vuelve a leer la carpeta. También
// recuerda qué tarjeta tenía el foco, para devolverlo a lo último que se vio (D-21).
package pe.inkaterra.tv.pantallas.inicio

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento

sealed interface EstadoInicio {
    data object Cargando : EstadoInicio

    data class Listo(
        val filas: List<Fila>,
    ) : EstadoInicio
}

data class Foco(
    val fila: Int,
    val columna: Int,
)

class InicioModelo(
    biblioteca: Biblioteca,
) : ViewModel() {
    private val _estado = MutableStateFlow<EstadoInicio>(EstadoInicio.Cargando)
    val estado: StateFlow<EstadoInicio> = _estado.asStateFlow()

    var foco by mutableStateOf(Foco(0, 0))

    init {
        viewModelScope.launch {
            val contenido = biblioteca.cargar()
            // Lo que no entra no se muestra; queda en el registro para quien cargó el contenido.
            contenido.rechazados.forEach { Log.w("Contenido", "${it.archivo.nombre}: ${it.motivo}") }
            _estado.value = EstadoInicio.Listo(filasDe(contenido))
        }
    }

    // Lleva el foco a la tarjeta de elemento en la fila tipo; se busca por la ruta.
    fun enfocar(
        tipo: TipoFila,
        elemento: Elemento,
    ) {
        val filas = (estado.value as? EstadoInicio.Listo)?.filas ?: return
        val fila = filas.indexOfFirst { it.tipo == tipo }
        val columna = filas.getOrNull(fila)?.elementos?.indexOfFirst { it.archivo.ruta == elemento.archivo.ruta } ?: -1
        if (columna >= 0) foco = Foco(fila, columna)
    }
}
