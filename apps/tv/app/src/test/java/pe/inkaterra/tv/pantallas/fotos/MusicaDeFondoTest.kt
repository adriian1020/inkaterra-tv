// La música de fondo de la presentación (D-22): las canciones de la fila, mezcladas y en bucle.
package pe.inkaterra.tv.pantallas.fotos

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.TipoContenido

@RunWith(AndroidJUnit4::class)
class MusicaDeFondoTest {
    @Test
    fun prepararFondoMezclaYRepite() {
        val canciones = listOf("Selva.mp3", "Lluvia.m4a").map { Elemento(archivo(it), TipoContenido.MUSICA, it) }
        val player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
        try {
            player.prepararFondo(canciones, 1)
            assertTrue(player.shuffleModeEnabled)
            assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
            assertEquals(2, player.mediaItemCount)
            assertEquals(1, player.currentMediaItemIndex)
        } finally {
            player.release()
        }
    }
}
