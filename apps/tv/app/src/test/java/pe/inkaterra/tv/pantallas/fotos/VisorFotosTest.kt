// El visor de fotos (D-21): la presentación pasa sola cada 8 s y da la vuelta, Enter la pausa y una
// foto que no abre muestra su mensaje y sigue a los 3 s. En la PC las rutas de prueba no existen,
// así que esas fotos dan error; donde importa que una abra, se escribe un PNG real.
package pe.inkaterra.tv.pantallas.fotos

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pe.inkaterra.tv.app.TemaInkaterra
import pe.inkaterra.tv.datos.biblioteca.ArchivoLocal
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.TipoContenido
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisorFotosTest {
    @get:Rule(order = 0)
    val temporal = TemporaryFolder()

    @get:Rule(order = 1)
    val regla = createAndroidComposeRule<ComponentActivity>()

    private fun foto(archivo: ArchivoLocal) =
        Elemento(archivo, TipoContenido.FOTO, archivo.nombre.substringBeforeLast('.'))

    private fun fotos(cuantas: Int) = (1..cuantas).map { foto(archivo("Reserva Amazónica 0$it.jpg")) }

    // Una foto que sí abre: un PNG real en una carpeta temporal.
    private fun fotoReal(nombre: String): Elemento {
        val archivo = File(temporal.root, nombre)
        archivo.outputStream().use {
            Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return foto(ArchivoLocal(archivo.absolutePath, archivo.name, archivo.length(), archivo.lastModified(), false))
    }

    private var salida: Elemento? = null

    private fun abrir(
        lista: List<Elemento>,
        inicial: Int = 0,
    ) {
        regla.setContent { TemaInkaterra { VisorFotosPantalla(lista, inicial) { salida = it } } }
        regla.waitForIdle()
    }

    private fun tecla(t: Key) {
        regla.onRoot().performKeyInput { pressKey(t) }
        regla.waitForIdle()
    }

    private fun titulo(texto: String) = regla.onNodeWithTag("foto-titulo").assertTextEquals(texto)

    @Test
    fun abreEnLaFotoElegida() {
        abrir(fotos(3), inicial = 1)
        titulo("Reserva Amazónica 02")
    }

    @Test
    fun derechaPasaALaSiguienteYDaLaVuelta() {
        abrir(fotos(3), inicial = 2)
        tecla(Key.DirectionRight)
        titulo("Reserva Amazónica 01")
    }

    @Test
    fun pasaSolaALos8Segundos() {
        regla.mainClock.autoAdvance = false
        abrir(listOf(fotoReal("Lodge.png"), fotoReal("Canopy.png")))
        regla.mainClock.advanceTimeBy(7_900)
        titulo("Lodge")
        regla.mainClock.advanceTimeBy(200)
        titulo("Canopy")
    }

    @Test
    fun enterPausaLaPresentacion() {
        abrir(listOf(fotoReal("Lodge.png"), fotoReal("Canopy.png")))
        tecla(Key.Enter)
        regla.onNodeWithText("En pausa").assertExists()
        regla.mainClock.autoAdvance = false
        regla.mainClock.advanceTimeBy(9_000)
        titulo("Lodge")
    }

    @Test
    fun unaSolaFotoSeQueda() {
        abrir(listOf(fotoReal("Lodge.png")))
        regla.mainClock.autoAdvance = false
        regla.mainClock.advanceTimeBy(9_000)
        titulo("Lodge")
    }

    @Test
    fun fotoDanadaMuestraMensajeYSigue() {
        abrir(listOf(foto(archivo("Dañada.jpg")), fotoReal("Canopy.png")))
        regla.waitUntil(5_000) { regla.onAllNodesWithTag("foto-error").fetchSemanticsNodes().isNotEmpty() }
        regla.onNodeWithText("No se puede abrir esta foto").assertExists()
        regla.mainClock.autoAdvance = false
        regla.mainClock.advanceTimeBy(3_100)
        titulo("Canopy")
    }

    @Test
    fun atrasDevuelveLaFotoActual() {
        val lista = fotos(3)
        abrir(lista)
        tecla(Key.DirectionRight)
        regla.runOnUiThread { regla.activity.onBackPressedDispatcher.onBackPressed() }
        regla.waitForIdle()
        assertEquals(lista[1], salida)
    }
}
