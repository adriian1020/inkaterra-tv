// Las filas del inicio estilo Netflix (D-20), en orden fijo y sin las vacías. La fila de la USB del
// huésped va segunda, después de Destacados (D-22); si la memoria se está leyendo, está vacía o no
// se puede leer, la fila lleva un aviso en vez de tarjetas, y solo se muestra si hay otras filas.
package pe.inkaterra.tv.pantallas.inicio

import pe.inkaterra.tv.datos.biblioteca.ContenidoBiblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.usb.EstadoUsb

enum class TipoFila { DESTACADOS, USB, VIDEOS, FOTOS, MUSICA }

enum class AvisoFila { LEYENDO, VACIA, NO_SE_LEE }

data class Fila(
    val tipo: TipoFila,
    val elementos: List<Elemento>,
    val aviso: AvisoFila? = null,
    // Archivos de la USB que la TV no puede mostrar (CU-17 3b).
    val rechazados: Int = 0,
)

fun filasDe(
    contenido: ContenidoBiblioteca,
    usb: EstadoUsb = EstadoUsb.SinUsb,
): List<Fila> {
    val filas =
        listOfNotNull(
            Fila(TipoFila.DESTACADOS, contenido.destacados),
            filaUsb(usb),
            Fila(TipoFila.VIDEOS, contenido.videos),
            Fila(TipoFila.FOTOS, contenido.fotos),
            Fila(TipoFila.MUSICA, contenido.musica),
        )
    val conTarjetas = filas.filter { it.elementos.isNotEmpty() }
    return if (conTarjetas.isEmpty()) emptyList() else filas.filter { it.elementos.isNotEmpty() || it.aviso != null }
}

private fun filaUsb(usb: EstadoUsb): Fila? =
    when (usb) {
        EstadoUsb.SinUsb -> null
        is EstadoUsb.Leyendo -> Fila(TipoFila.USB, emptyList(), AvisoFila.LEYENDO)
        is EstadoUsb.NoSeLee -> Fila(TipoFila.USB, emptyList(), AvisoFila.NO_SE_LEE)
        is EstadoUsb.Lista ->
            Fila(
                TipoFila.USB,
                usb.elementos,
                if (usb.elementos.isEmpty()) AvisoFila.VACIA else null,
                usb.rechazados,
            )
    }
