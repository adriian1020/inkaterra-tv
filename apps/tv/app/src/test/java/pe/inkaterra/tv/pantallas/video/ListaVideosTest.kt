// Al terminar un video sigue el próximo de la fila, saltando las fotos (D-21).
package pe.inkaterra.tv.pantallas.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.TipoContenido

class ListaVideosTest {
    private fun elemento(
        nombre: String,
        tipo: TipoContenido,
    ) = Elemento(archivo(nombre), tipo, nombre.substringBeforeLast('.'))

    private val video1 = elemento("Atardecer en Machu Picchu.mp4", TipoContenido.VIDEO)
    private val video2 = elemento("Río Madre de Dios.mp4", TipoContenido.VIDEO)
    private val foto = elemento("Reserva Amazónica.jpg", TipoContenido.FOTO)

    @Test
    fun siguienteSaltaLasFotos() = assertEquals(2, siguienteVideo(listOf(video1, foto, video2), 0))

    @Test
    fun despuesDelUltimoNoHayMas() = assertNull(siguienteVideo(listOf(video1, video2), 1))
}
