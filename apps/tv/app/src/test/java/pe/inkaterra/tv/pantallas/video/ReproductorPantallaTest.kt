// El reproductor ante videos que no se pueden reproducir (D-21): muestra el mensaje, sigue con el
// siguiente a los 3 s y, después del último, vuelve. En la PC las rutas de prueba no existen, así
// que ExoPlayer da error con cada una. También, que la pantalla no quede encendida en pausa.
package pe.inkaterra.tv.pantallas.video

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pe.inkaterra.tv.app.TemaInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.PosicionesVideo
import pe.inkaterra.tv.dominio.TipoContenido

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class ReproductorPantallaTest {
    @get:Rule
    val regla = createAndroidComposeRule<ComponentActivity>()

    private fun videos(cuantos: Int) =
        (1..cuantos).map { Elemento(archivo("Roto $it.mp4"), TipoContenido.VIDEO, "Roto $it") }

    private var salida: Elemento? = null
    private var vista: View? = null

    private fun abrir(lista: List<Elemento>) {
        regla.setContent {
            vista = LocalView.current
            TemaInkaterra { ReproductorPantalla(lista, 0, PosicionesVideo()) { salida = it } }
        }
        regla.waitForIdle()
    }

    private fun pantallaEncendida() = requireNotNull(vista).keepScreenOn

    // Varios videos que fallan enseguida, uno tras otro: cada uno muestra el mensaje y sigue; no
    // queda trabado en el mensaje.
    @Test
    fun variosVideosQueFallanSiguenHastaElFinal() {
        val lista = videos(3)
        abrir(lista)
        regla.waitUntil(5_000) {
            regla.onAllNodesWithText("No se puede reproducir este video").fetchSemanticsNodes().isNotEmpty()
        }
        // Los 3 s del mensaje corren con el reloj de la prueba; los errores de ExoPlayer llegan en tiempo
        // real. Se avanza de a 1 s y se le da tiempo real a cada error.
        regla.mainClock.autoAdvance = false
        repeat(15) {
            if (salida == null) {
                Thread.sleep(300)
                regla.mainClock.advanceTimeBy(1_000)
                regla.waitForIdle()
            }
        }
        assertEquals(lista[2], salida)
    }

    @Test
    fun enPausaLaPantallaPuedeApagarse() {
        abrir(videos(1))
        assertTrue(pantallaEncendida())
        regla.onRoot().performKeyInput { pressKey(Key.Enter) }
        regla.waitForIdle()
        assertFalse(pantallaEncendida())
    }
}
