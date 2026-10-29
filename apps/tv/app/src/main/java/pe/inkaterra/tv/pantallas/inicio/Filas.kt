// Las filas del inicio estilo Netflix (D-20), en orden fijo y sin las vacías. La memoria USB del
// huésped es otra sección de la barra lateral (D-23), con sus filas de videos, fotos y música o, si
// no hay nada que mostrar, un aviso.
package pe.inkaterra.tv.pantallas.inicio

import pe.inkaterra.tv.datos.biblioteca.ContenidoBiblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.usb.EstadoUsb

enum class TipoFila { DESTACADOS, VIDEOS, FOTOS, MUSICA, USB_VIDEOS, USB_FOTOS, USB_MUSICA }

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

enum class AvisoUsb { SIN_MEMORIA, LEYENDO, VACIA, NO_SE_LEE }

data class SeccionUsb(
    val filas: List<Fila>,
    val aviso: AvisoUsb?,
    // Archivos de la memoria que la TV no puede mostrar (CU-17 3b).
    val rechazados: Int,
)

fun seccionUsb(estado: EstadoUsb): SeccionUsb =
    when (estado) {
        EstadoUsb.SinUsb -> SeccionUsb(emptyList(), AvisoUsb.SIN_MEMORIA, 0)
        is EstadoUsb.Leyendo -> SeccionUsb(emptyList(), AvisoUsb.LEYENDO, 0)
        is EstadoUsb.NoSeLee -> SeccionUsb(emptyList(), AvisoUsb.NO_SE_LEE, 0)
        is EstadoUsb.Lista -> {
            val filas =
                listOf(
                    Fila(TipoFila.USB_VIDEOS, estado.contenido.videos),
                    Fila(TipoFila.USB_FOTOS, estado.contenido.fotos),
                    Fila(TipoFila.USB_MUSICA, estado.contenido.musica),
                ).filter { it.elementos.isNotEmpty() }
            SeccionUsb(filas, if (filas.isEmpty()) AvisoUsb.VACIA else null, estado.rechazados)
        }
    }
