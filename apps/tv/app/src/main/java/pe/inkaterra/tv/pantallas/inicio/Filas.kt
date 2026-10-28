// Las filas del inicio estilo Netflix (D-20), en orden fijo y sin las vacías. En el sprint 3 se
// agrega la fila de la USB del huésped después de Destacados.
package pe.inkaterra.tv.pantallas.inicio

import pe.inkaterra.tv.datos.biblioteca.ContenidoBiblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento

enum class TipoFila { DESTACADOS, VIDEOS, FOTOS, MUSICA }

data class Fila(
    val tipo: TipoFila,
    val elementos: List<Elemento>,
)

fun filasDe(contenido: ContenidoBiblioteca): List<Fila> =
    listOf(
        Fila(TipoFila.DESTACADOS, contenido.destacados),
        Fila(TipoFila.VIDEOS, contenido.videos),
        Fila(TipoFila.FOTOS, contenido.fotos),
        Fila(TipoFila.MUSICA, contenido.musica),
    ).filter { it.elementos.isNotEmpty() }
