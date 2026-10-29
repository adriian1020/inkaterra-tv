// La barra del reproductor: título, tiempo y si está en pausa (D-21).
package pe.inkaterra.tv.pantallas.video

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pe.inkaterra.tv.app.TemaInkaterra

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class BarraVideoTest {
    @get:Rule
    val regla = createAndroidComposeRule<ComponentActivity>()

    private fun mostrar(pausado: Boolean) {
        regla.setContent { TemaInkaterra { BarraVideo("Río Madre de Dios", 65_000, 200_000, pausado) } }
        regla.waitForIdle()
    }

    @Test
    fun muestraTituloYTiempo() {
        mostrar(pausado = false)
        regla.onNodeWithTag("video-titulo").assertTextEquals("Río Madre de Dios")
        regla.onNodeWithTag("video-tiempo").assertTextEquals("1:05 / 3:20")
        regla.onNodeWithText("En pausa").assertDoesNotExist()
    }

    @Test
    fun enPausaLoDice() {
        mostrar(pausado = true)
        regla.onNodeWithText("En pausa").assertExists()
    }
}
