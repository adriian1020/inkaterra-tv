// Las filas del inicio van en orden fijo y una fila vacía no se muestra (D-20). La de la USB va
// segunda y, si la memoria no se puede mostrar, lleva un aviso (D-22).
package pe.inkaterra.tv.pantallas.inicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.inkaterra.tv.datos.biblioteca.ContenidoBiblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.datos.usb.EstadoUsb
import pe.inkaterra.tv.datos.usb.MotivoUsb
import pe.inkaterra.tv.dominio.TipoContenido
import pe.inkaterra.tv.sistema.usb.montado
import java.io.File

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

    private val usb = montado(File("/storage/AB12-34CD"))

    @Test
    fun laUsbVaDespuesDeDestacados() {
        val contenido =
            ContenidoBiblioteca.VACIO.copy(
                destacados = listOf(e("Oferta spa.jpg", TipoContenido.FOTO)),
                videos = listOf(e("Río Madre de Dios.mp4", TipoContenido.VIDEO)),
            )
        val lista = EstadoUsb.Lista(usb, listOf(e("a.jpg", TipoContenido.FOTO), e("b.mp4", TipoContenido.VIDEO)), 0)
        assertEquals(
            listOf(TipoFila.DESTACADOS, TipoFila.USB, TipoFila.VIDEOS),
            filasDe(contenido, lista).map { it.tipo },
        )
    }

    @Test
    fun usbIlegibleMuestraAviso() {
        val contenido = ContenidoBiblioteca.VACIO.copy(videos = listOf(e("Río Madre de Dios.mp4", TipoContenido.VIDEO)))
        val fila = filasDe(contenido, EstadoUsb.NoSeLee(usb, MotivoUsb.FORMATO)).first { it.tipo == TipoFila.USB }
        assertEquals(AvisoFila.NO_SE_LEE, fila.aviso)
        assertTrue(fila.elementos.isEmpty())
    }

    @Test
    fun avisoSinOtrasFilasNoSeMuestra() =
        assertTrue(filasDe(ContenidoBiblioteca.VACIO, EstadoUsb.NoSeLee(usb, MotivoUsb.FORMATO)).isEmpty())
}
