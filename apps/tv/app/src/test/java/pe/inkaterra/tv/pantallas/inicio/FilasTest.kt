// Las filas del inicio van en orden fijo y una fila vacía no se muestra (D-20). La USB es otra
// sección de la barra lateral, con filas de videos, fotos y música, o un aviso (D-23).
package pe.inkaterra.tv.pantallas.inicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.inkaterra.tv.datos.biblioteca.ContenidoBiblioteca
import pe.inkaterra.tv.datos.biblioteca.ContenidoUsb
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
    fun usbSeparaVideosFotosYMusica() {
        val contenido =
            ContenidoUsb(
                videos = listOf(e("b.mp4", TipoContenido.VIDEO)),
                fotos = listOf(e("a.jpg", TipoContenido.FOTO)),
                musica = listOf(e("c.mp3", TipoContenido.MUSICA)),
            )
        val seccion = seccionUsb(EstadoUsb.Lista(usb, contenido, 0))
        assertEquals(
            listOf(TipoFila.USB_VIDEOS, TipoFila.USB_FOTOS, TipoFila.USB_MUSICA),
            seccion.filas.map { it.tipo },
        )
        assertEquals(null, seccion.aviso)
    }

    @Test
    fun sinMemoriaPideConectarla() = assertEquals(AvisoUsb.SIN_MEMORIA, seccionUsb(EstadoUsb.SinUsb).aviso)

    @Test
    fun memoriaIlegibleLoDice() =
        assertEquals(AvisoUsb.NO_SE_LEE, seccionUsb(EstadoUsb.NoSeLee(usb, MotivoUsb.FORMATO)).aviso)
}
