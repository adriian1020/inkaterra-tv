// Prueba en el equipo del recorrido del sprint 2 con teclas como las del control (D-21): ver un
// video 4K, pausarlo, salir y retomarlo, adelantar hasta que pase solo al siguiente, y abrir la
// presentación de fotos; cada vez, Atrás vuelve a la tarjeta de lo último que se vio. Usa los
// videos de tool/videos-prueba.sh (25 s cada uno) y fotos que escribe la propia prueba.
//   tool/videos-prueba.sh && tool/tv.sh probar pe.inkaterra.tv.FlujoSprint2Test
package pe.inkaterra.tv

import android.app.LocaleManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.LocaleList
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.app.MainActivity
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class FlujoSprint2Test {
    @get:Rule
    val regla = createEmptyComposeRule()

    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacion.targetContext
    private val raiz = File(requireNotNull(contexto.getExternalFilesDir(null)), "contenido-prueba")
    private val idiomas = contexto.getSystemService(LocaleManager::class.java)
    private lateinit var idiomaAntes: LocaleList

    @Before
    fun preparar() {
        raiz.deleteRecursively()
        val videos = File(raiz, "videos").apply { mkdirs() }
        listOf("hevc-20m.mp4", "vp9-20m.webm").forEach {
            val muestra = File(requireNotNull(contexto.getExternalFilesDir("prueba-4k")), it)
            assertTrue("Falta $muestra: correr tool/videos-prueba.sh", muestra.isFile)
            muestra.copyTo(File(videos, it))
        }
        val fotos = File(raiz, "fotos").apply { mkdirs() }
        listOf(Color.RED, Color.GREEN, Color.BLUE).forEachIndexed { i, color ->
            val imagen = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
            File(fotos, "Foto 0${i + 1}.jpg").outputStream().use { imagen.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }
        // En español desde el principio, sin pasar por la pantalla de idioma; se restaura al final.
        idiomaAntes = idiomas.applicationLocales
        idiomas.applicationLocales = LocaleList.forLanguageTags("es")
    }

    @After
    fun limpiar() {
        idiomas.applicationLocales = idiomaAntes
        raiz.deleteRecursively()
    }

    private fun tecla(codigo: Int) {
        instrumentacion.sendKeyDownUpSync(codigo)
        regla.waitForIdle()
    }

    private fun esperarFoco(tag: String) = regla.waitUntilExactlyOneExists(hasTestTag(tag) and isFocused(), 10_000)

    // Los segundos que marca la barra: "0:07 / 0:25" da 7.
    private fun segundosEnLaBarra(): Int {
        regla.waitUntilExactlyOneExists(hasTestTag("video-tiempo"), 10_000)
        val texto =
            regla
                .onNodeWithTag("video-tiempo")
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Text)
                ?.joinToString()
                .orEmpty()
        val (minutos, segundos) = texto.substringBefore(" /").split(":").map { it.trim().toInt() }
        return minutos * 60 + segundos
    }

    @Test
    fun videoYFotos() {
        val intent =
            Intent(contexto, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_RAIZ_CONTENIDO, raiz.absolutePath)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            // 1. El primer video; a los 7 s se pausa (desde los 5 s se guarda para retomar).
            esperarFoco("tarjeta-VIDEOS-0")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            regla.waitUntilExactlyOneExists(hasTestTag("video-titulo") and hasText("hevc-20m"), 10_000)
            Thread.sleep(7_000)
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            val pausado = segundosEnLaBarra()
            assertTrue("Pausado en $pausado s", pausado >= 5)
            // La barra lee el reproductor cada 500 ms con el reloj de la prueba, que Thread.sleep no
            // mueve: se esperan 2 s reales y se avanza el reloj para que vuelva a leer.
            Thread.sleep(2_000)
            regla.mainClock.advanceTimeBy(600)
            assertEquals(pausado, segundosEnLaBarra())

            // 2. Atrás vuelve a su tarjeta; al entrar otra vez, retoma.
            tecla(KeyEvent.KEYCODE_BACK)
            esperarFoco("tarjeta-VIDEOS-0")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            assertTrue(segundosEnLaBarra() >= pausado)

            // 3. Dos saltos de 10 s llegan al final (dura 25 s) y pasa solo al siguiente.
            tecla(KeyEvent.KEYCODE_DPAD_RIGHT)
            tecla(KeyEvent.KEYCODE_DPAD_RIGHT)
            regla.waitUntilExactlyOneExists(hasTestTag("video-titulo") and hasText("vp9-20m"), 15_000)

            // 4. Atrás vuelve a la tarjeta del último video que se vio.
            tecla(KeyEvent.KEYCODE_BACK)
            esperarFoco("tarjeta-VIDEOS-1")

            // 5. La presentación pasa sola a la segunda foto, y Atrás vuelve a esa tarjeta.
            tecla(KeyEvent.KEYCODE_DPAD_LEFT)
            tecla(KeyEvent.KEYCODE_DPAD_DOWN)
            esperarFoco("tarjeta-FOTOS-0")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            regla.onNodeWithTag("foto-titulo").assertTextEquals("Foto 01")
            regla.mainClock.advanceTimeBy(9_000)
            regla.onNodeWithTag("foto-titulo").assertTextEquals("Foto 02")
            tecla(KeyEvent.KEYCODE_BACK)
            esperarFoco("tarjeta-FOTOS-1")
        }
    }
}
