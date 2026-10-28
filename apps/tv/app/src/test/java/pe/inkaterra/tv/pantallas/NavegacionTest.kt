// El inicio estilo Netflix con el control remoto (D-20): foco, filas, área destacada y Atrás.
package pe.inkaterra.tv.pantallas

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import pe.inkaterra.tv.app.Navegacion
import pe.inkaterra.tv.app.TemaInkaterra
import pe.inkaterra.tv.datos.biblioteca.ArchivoLocal
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.FuenteFalsa
import pe.inkaterra.tv.datos.biblioteca.LectorFalso
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.datos.miniaturas.GeneradorMiniatura
import pe.inkaterra.tv.datos.miniaturas.Miniaturas

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class NavegacionTest {
    @get:Rule(order = 0)
    val temporal = TemporaryFolder()

    @get:Rule(order = 1)
    val regla = createAndroidComposeRule<ComponentActivity>()

    private val completa =
        listOf(
            archivo("Oferta spa.jpg", esPromocion = true),
            archivo("Reserva Amazónica 01.jpg", esPromocion = true),
            archivo("Atardecer en Machu Picchu.mp4"),
            archivo("Río Madre de Dios.mp4"),
            archivo("Selva.mp3"),
        )

    private fun abrir(archivos: List<ArchivoLocal> = completa) {
        val biblioteca = Biblioteca(FuenteFalsa(archivos), LectorFalso(emptyMap()), Dispatchers.Unconfined)
        val sinImagen =
            object : GeneradorMiniatura {
                override fun generar(elemento: Elemento): Bitmap? = null
            }
        val miniaturas = Miniaturas(sinImagen, temporal.root, Dispatchers.Unconfined)
        regla.setContent { TemaInkaterra { Navegacion(biblioteca, miniaturas) } }
        regla.waitForIdle()
    }

    private fun tecla(t: Key) {
        regla.onRoot().performKeyInput { pressKey(t) }
        regla.waitForIdle()
    }

    // Atrás pasa por el despachador de la actividad, como la tecla del control; performKeyInput
    // solo llega a Compose.
    private fun atras() {
        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        regla.waitForIdle()
    }

    private fun foco(tag: String) = regla.onNodeWithTag(tag).assertIsFocused()

    @Test
    fun alAbrirElFocoEstaEnElPrimerDestacado() {
        abrir()
        foco("tarjeta-DESTACADOS-0")
        regla.onNodeWithTag("destacado-titulo").assertTextEquals("Oferta spa")
    }

    @Test
    fun laFlechaDerechaRecorreLaFila() {
        abrir()
        tecla(Key.DirectionRight)
        foco("tarjeta-DESTACADOS-1")
    }

    @Test
    fun laFlechaAbajoBajaDeFila() {
        abrir()
        tecla(Key.DirectionDown)
        foco("tarjeta-VIDEOS-0")
    }

    @Test
    fun areaDestacadaSigueAlFocoConDemora() {
        abrir()
        regla.mainClock.autoAdvance = false
        regla.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        regla.mainClock.advanceTimeBy(100)
        regla.onNodeWithTag("destacado-titulo").assertTextEquals("Oferta spa")
        regla.mainClock.advanceTimeBy(500)
        regla.onNodeWithTag("destacado-titulo").assertTextEquals("Atardecer en Machu Picchu")
    }

    @Test
    fun sinContenidoMuestraMensaje() {
        abrir(emptyList())
        regla.onNodeWithText("Todavía no hay contenido").assertExists()
        regla.onNodeWithTag("fila-DESTACADOS").assertDoesNotExist()
        regla.onNodeWithTag("fila-VIDEOS").assertDoesNotExist()
    }

    @Test
    fun atrasDevuelveElFocoALaTarjeta() {
        abrir()
        tecla(Key.DirectionDown)
        tecla(Key.DirectionRight)
        tecla(Key.Enter)
        regla.onNodeWithTag("abrir-titulo").assertTextEquals("Río Madre de Dios")
        atras()
        foco("tarjeta-VIDEOS-1")
    }

    @Test
    fun atrasFueraDeLaPrimeraFilaSubeAlInicio() {
        abrir()
        tecla(Key.DirectionDown)
        tecla(Key.DirectionDown)
        foco("tarjeta-MUSICA-0")
        atras()
        foco("tarjeta-DESTACADOS-0")
    }

    @Test
    fun atrasEnLaPrimeraFilaSaleDeLaApp() {
        abrir()
        atras()
        assertTrue(regla.activity.isFinishing)
    }
}
