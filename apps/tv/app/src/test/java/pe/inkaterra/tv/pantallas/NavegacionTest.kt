// El inicio estilo Netflix con el control remoto (D-20): foco, filas, área destacada y Atrás.
package pe.inkaterra.tv.pantallas

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
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
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.datos.miniaturas.GeneradorMiniatura
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.dominio.PosicionesVideo
import pe.inkaterra.tv.sistema.idioma.Idioma
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class NavegacionTest {
    @get:Rule(order = 0)
    val temporal = TemporaryFolder()

    @get:Rule(order = 1)
    val regla = createAndroidComposeRule<ComponentActivity>()

    private fun abrir(
        archivos: List<ArchivoLocal> = contenidoDePrueba,
        miniaturas: Miniaturas = miniaturasSinImagen(temporal.root),
    ) {
        val biblioteca = bibliotecaDePrueba(archivos)
        val idioma = AjusteIdiomaFalso(Idioma.ESPANOL)
        regla.setContent { TemaInkaterra { Navegacion(biblioteca, miniaturas, PosicionesVideo(), idioma) } }
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
        regla.onNodeWithTag("video-titulo").assertTextEquals("Río Madre de Dios")
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

    // Mientras se pasa a lo abierto, el inicio ya no responde: un Enter de más no abre dos veces.
    @Test
    fun enterDosVecesAbreUnaSola() {
        abrir()
        regla.mainClock.autoAdvance = false
        regla.onRoot().performKeyInput { pressKey(Key.Enter) }
        regla.mainClock.advanceTimeBy(100)
        regla.onRoot().performKeyInput { pressKey(Key.Enter) }
        regla.mainClock.autoAdvance = true
        regla.waitForIdle()
        atras()
        regla.onNodeWithTag("foto-titulo").assertDoesNotExist()
        foco("tarjeta-DESTACADOS-0")
    }

    // Atrás enseguida de abrir cierra lo abierto, aunque se venga de otra fila.
    @Test
    fun atrasEnseguidaVuelveALaTarjeta() {
        abrir()
        tecla(Key.DirectionDown)
        regla.mainClock.autoAdvance = false
        regla.onRoot().performKeyInput { pressKey(Key.Enter) }
        regla.mainClock.advanceTimeBy(100)
        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        regla.mainClock.autoAdvance = true
        regla.waitForIdle()
        regla.onNodeWithTag("video-titulo").assertDoesNotExist()
        foco("tarjeta-VIDEOS-0")
    }

    @Test
    fun alVolverDeLasFotosElFocoVaALaUltima() {
        abrir((1..3).map { archivo("Reserva Amazónica 0$it.jpg") })
        foco("tarjeta-FOTOS-0")
        tecla(Key.Enter)
        regla.onNodeWithTag("foto-titulo").assertTextEquals("Reserva Amazónica 01")
        tecla(Key.DirectionRight)
        atras()
        foco("tarjeta-FOTOS-1")
    }

    @Test
    @Config(qualifiers = "es-w960dp-h540dp")
    fun alVolverLasFilasConservanSuPosicion() {
        abrir((1..6).map { archivo("Reserva Amazónica 0$it.jpg") } + archivo("Selva.mp3"))
        repeat(5) { tecla(Key.DirectionRight) }
        tecla(Key.DirectionDown)
        foco("tarjeta-MUSICA-0")
        tecla(Key.Enter)
        atras()
        foco("tarjeta-MUSICA-0")
        regla.onNodeWithTag("tarjeta-FOTOS-5").assertIsDisplayed()
    }

    // El área destacada no deja la imagen anterior mientras se genera la del nuevo foco.
    @Test
    fun areaDestacadaNoMuestraLaImagenAnterior() {
        val soltar = CountDownLatch(1)
        val generador =
            object : GeneradorMiniatura {
                override fun generar(elemento: Elemento): Bitmap? =
                    when (elemento.titulo) {
                        "Oferta spa" -> Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888)
                        "Reserva Amazónica 01" -> null.also { soltar.await(10, TimeUnit.SECONDS) }
                        else -> null
                    }
            }
        try {
            abrir(miniaturas = Miniaturas(generador, temporal.newFolder("miniaturas"), Dispatchers.IO))
            regla.mainClock.autoAdvance = false
            regla.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
            regla.mainClock.advanceTimeBy(600)
            regla
                .onNode(hasTestTag("miniatura-cargando") and hasAnyAncestor(hasTestTag("area-destacada")))
                .assertExists()
        } finally {
            soltar.countDown()
        }
    }
}
