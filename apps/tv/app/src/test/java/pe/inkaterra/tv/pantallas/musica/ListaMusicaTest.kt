// La lista de canciones que recibe el reproductor: todas las de la fila, empezando por la elegida.
package pe.inkaterra.tv.pantallas.musica

import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.TipoContenido

@RunWith(AndroidJUnit4::class)
class ListaMusicaTest {
    @Test
    fun ponerCancionesEmpiezaEnLaElegida() {
        val canciones =
            listOf("Selva.mp3", "Lluvia.m4a", "Río.flac").map { Elemento(archivo(it), TipoContenido.MUSICA, it) }
        val player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
        try {
            player.ponerCanciones(canciones, 1)
            assertEquals(3, player.mediaItemCount)
            assertEquals(1, player.currentMediaItemIndex)
            assertEquals(canciones[2].archivo.ruta, player.getMediaItemAt(2).mediaId)
        } finally {
            player.release()
        }
    }
}
