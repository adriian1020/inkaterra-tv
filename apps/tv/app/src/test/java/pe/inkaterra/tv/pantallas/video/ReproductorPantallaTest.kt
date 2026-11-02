// El reproductor ante videos que no se pueden reproducir (D-21): muestra el mensaje, sigue con el
// siguiente a los 3 s y, después del último, vuelve. En la PC las rutas de prueba no existen, así
// que ExoPlayer da error con cada una. También, que la pantalla no quede encendida en pausa.
package pe.inkaterra.tv.pantallas.video

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
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

    private fun tecla(t: Key) {
        regla.onRoot().performKeyInput { pressKey(t) }
        regla.waitForIdle()
    }

    // Con la barra visible, las flechas recorren los botones en vez de saltar 10 s (D-23).
    @Test
    fun conLaBarraVisibleLasFlechasMuevenElFoco() {
        abrir(videos(3))
        regla.onNodeWithTag("video-alternar").assertIsFocused()
        tecla(Key.DirectionRight)
        regla.onNodeWithTag("video-adelantar").assertIsFocused()
    }

    @Test
    fun botonSiguientePasaAlSiguiente() {
        abrir(videos(3))
        tecla(Key.DirectionRight)
        tecla(Key.DirectionRight)
        regla.onNodeWithTag("video-siguiente").assertIsFocused()
        tecla(Key.Enter)
        regla.onNodeWithTag("video-titulo").assertTextEquals("Roto 2")
    }

    // Con el foco en los botones, un solo Atrás sale del video. En la TV, Compose usaba el primer Atrás
    // para sacar el foco de los botones a la pantalla, y recién el segundo salía (D-23).
    @Test
    fun atrasConElFocoEnLosBotonesSale() {
        abrir(videos(3))
        regla.onNodeWithTag("video-alternar").assertIsFocused()
        regla.onRoot().performKeyInput { pressKey(Key.Back) }
        regla.waitForIdle()
        assertEquals(videos(3)[0], salida)
    }

    // Un video que falla y no tiene siguiente: la pantalla queda quieta y la barra se esconde a los 5 s.
    private fun abrirConLaBarraEscondida() {
        abrir(videos(1))
        regla.mainClock.autoAdvance = false
        regla.mainClock.advanceTimeBy(5_100)
        regla.waitForIdle()
        regla.onNodeWithTag("video-alternar").assertDoesNotExist()
        // Un cuadro más para que el foco vuelva a la pantalla.
        regla.mainClock.advanceTimeBy(100)
        regla.waitForIdle()
    }

    // Mantener Derecha con la barra escondida sigue saltando 10 s (D-21): no recorre los botones.
    @Test
    fun mantenerDerechaConLaBarraEscondidaNoMueveElFoco() {
        abrirConLaBarraEscondida()
        regla.onRoot().performKeyInput { keyDown(Key.DirectionRight) }
        regla.mainClock.advanceTimeBy(100)
        regla.waitForIdle()
        // Mantenida, la tecla se repite mientras pasa el tiempo.
        regla.onRoot().performKeyInput { advanceEventTime(1_000) }
        regla.mainClock.advanceTimeBy(100)
        regla.waitForIdle()
        regla.onRoot().performKeyInput { keyUp(Key.DirectionRight) }
        regla.mainClock.advanceTimeBy(100)
        regla.waitForIdle()
        regla.onNodeWithTag("video-alternar").assertIsFocused()
    }
}
