// Las filas del inicio van en orden fijo y una fila vacía no se muestra (D-20).
package pe.inkaterra.tv.pantallas.inicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.inkaterra.tv.datos.biblioteca.ContenidoBiblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.TipoContenido

class FilasTest {
    private fun e(
        nombre: String,
        tipo: TipoContenido,
    ) = Elemento(archivo(nombre), tipo, nombre.substringBeforeLast('.'))

    @Test
    fun filasEnOrdenSinLasVacias() {
        val contenido =
            ContenidoBiblioteca.VACIO.copy(
                videos =
                    listOf(
                        e("Atardecer en Machu Picchu.mp4", TipoContenido.VIDEO),
                        e("Río Madre de Dios.mp4", TipoContenido.VIDEO),
                    ),
                musica = listOf(e("Selva.mp3", TipoContenido.MUSICA)),
            )
        assertEquals(listOf(TipoFila.VIDEOS, TipoFila.MUSICA), filasDe(contenido).map { it.tipo })
        assertEquals(2, filasDe(contenido).first().elementos.size)
    }

    @Test
    fun todoVacioDaSinFilas() = assertTrue(filasDe(ContenidoBiblioteca.VACIO).isEmpty())
}
