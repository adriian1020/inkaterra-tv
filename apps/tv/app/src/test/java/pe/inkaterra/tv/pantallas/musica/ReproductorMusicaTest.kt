// El reproductor de música (D-22): canciones que no se pueden reproducir, el botón de repetir y
// dónde empieza el foco. En la PC las rutas de prueba no existen, así que ExoPlayer da error con
// cada canción.
package pe.inkaterra.tv.pantallas.musica

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pe.inkaterra.tv.app.TemaInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.TipoContenido
import pe.inkaterra.tv.pantallas.miniaturasSinImagen

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class ReproductorMusicaTest {
    @get:Rule(order = 0)
    val temporal = TemporaryFolder()

    @get:Rule(order = 1)
    val regla = createAndroidComposeRule<ComponentActivity>()

    private val canciones = (1..3).map { Elemento(archivo("Rota $it.mp3"), TipoContenido.MUSICA, "Rota $it") }
    private var salida: Elemento? = null

    private fun abrir() {
        val miniaturas = miniaturasSinImagen(temporal.root)
        regla.setContent { TemaInkaterra { ReproductorMusicaPantalla(canciones, 0, miniaturas) { salida = it } } }
        regla.waitForIdle()
    }

    private fun tecla(t: Key) {
        regla.onRoot().performKeyInput { pressKey(t) }
        regla.waitForIdle()
    }

    // Con «repetir la lista» no hay última canción: si fallan todas seguidas, vuelve en vez de
    // mostrar el mensaje para siempre.
    @Test
    fun cancionesQueFallanVuelvenAlInicio() {
        abrir()
        regla.waitUntil(5_000) {
            regla.onAllNodesWithText("No se puede reproducir esta canción").fetchSemanticsNodes().isNotEmpty()
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
        assertTrue(salida in canciones)
    }

    @Test
    fun botonRepetirRecorreLosModos() {
        abrir()
        val repetir = regla.onNodeWithTag("musica-repetir")
        repetir.assertContentDescriptionEquals("Repetir la lista")
        repeat(3) { tecla(Key.DirectionRight) }
        repetir.assertIsFocused()
        tecla(Key.Enter)
        repetir.assertContentDescriptionEquals("Repetir esta canción")
        tecla(Key.Enter)
        repetir.assertContentDescriptionEquals("Sin repetir")
        tecla(Key.Enter)
        repetir.assertContentDescriptionEquals("Repetir la lista")
    }

    @Test
    fun elFocoEmpiezaEnReproducir() {
        abrir()
        regla.onNodeWithTag("musica-alternar").assertIsFocused()
    }

    // Con Inicio del control la app pasa a segundo plano: la pantalla de música se cierra, así la tecla
    // Play del control no vuelve a hacer sonar la música fuera de la app (D-22).
    @Test
    fun alPasarASegundoPlanoSale() {
        abrir()
        regla.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        regla.waitForIdle()
        assertTrue(salida in canciones)
    }
}
