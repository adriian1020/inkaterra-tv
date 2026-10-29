// Prueba en el equipo del recorrido del sprint 3 con teclas como las del control (D-22): una canción
// suena, la tecla de música del control pasa a la siguiente y Atrás la detiene y vuelve a su
// tarjeta; la presentación de fotos suena con música de fondo y Enter la pausa. También la barra
// lateral y los botones del video (D-23). Usa la música de
// muestra de tool/copiar-contenido.sh y fotos que escribe la propia prueba. La USB del huésped no
// se puede probar así: con la depuración encendida el Xiaomi no lee la memoria (D-18).
//   tool/copiar-contenido.sh && tool/tv.sh probar pe.inkaterra.tv.FlujoSprint3Test
package pe.inkaterra.tv

import android.app.LocaleManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.os.LocaleList
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.app.MainActivity
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class FlujoSprint3Test {
    @get:Rule
    val regla = createEmptyComposeRule()

    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacion.targetContext
    private val raiz = File(requireNotNull(contexto.getExternalFilesDir(null)), "contenido-prueba")
    private val idiomas = contexto.getSystemService(LocaleManager::class.java)
    private val audio = contexto.getSystemService(AudioManager::class.java)
    private lateinit var idiomaAntes: LocaleList

    @Before
    fun preparar() {
        raiz.deleteRecursively()
        val musica = File(raiz, "musica").apply { mkdirs() }
        listOf("Selva.mp3", "Lluvia en la Amazonía.m4a").forEach {
            val muestra = File(requireNotNull(contexto.getExternalFilesDir("contenido")), "musica/$it")
            assertTrue("Falta $muestra: correr tool/copiar-contenido.sh", muestra.isFile)
            muestra.copyTo(File(musica, it))
        }
        val fotos = File(raiz, "fotos").apply { mkdirs() }
        listOf(Color.RED, Color.GREEN).forEachIndexed { i, color ->
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

    private fun esperarTitulo(titulo: String) =
        regla.waitUntilExactlyOneExists(hasTestTag("video-titulo") and hasText(titulo), 10_000)

    // El audio va por el sistema, no por Compose: se espera en tiempo real.
    private fun esperarMusica(
        sonando: Boolean,
        ms: Long,
    ) {
        val limite = System.currentTimeMillis() + ms
        while (audio.isMusicActive != sonando && System.currentTimeMillis() < limite) Thread.sleep(100)
        assertTrue("isMusicActive debía ser $sonando", audio.isMusicActive == sonando)
    }

    @Test
    fun musicaYFondo() {
        val intent =
            Intent(contexto, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_RAIZ_CONTENIDO, raiz.absolutePath)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            // 1. La primera canción de la fila suena.
            esperarFoco("tarjeta-FOTOS-0")
            tecla(KeyEvent.KEYCODE_DPAD_DOWN)
            esperarFoco("tarjeta-MUSICA-0")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            esperarTitulo("Lluvia en la Amazonía")
            esperarMusica(sonando = true, ms = 3_000)

            // 2. La tecla de música del control pasa a la siguiente.
            tecla(KeyEvent.KEYCODE_MEDIA_NEXT)
            esperarTitulo("Selva")

            // 3. Atrás detiene la música y vuelve a la tarjeta de la que sonaba.
            tecla(KeyEvent.KEYCODE_BACK)
            esperarFoco("tarjeta-MUSICA-1")
            esperarMusica(sonando = false, ms = 2_000)

            // 4. La presentación suena con música de fondo; Enter pausa las dos.
            tecla(KeyEvent.KEYCODE_DPAD_UP)
            tecla(KeyEvent.KEYCODE_DPAD_LEFT)
            esperarFoco("tarjeta-FOTOS-0")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            esperarMusica(sonando = true, ms = 3_000)
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            esperarMusica(sonando = false, ms = 2_000)
            tecla(KeyEvent.KEYCODE_BACK)
            esperarFoco("tarjeta-FOTOS-0")
        }
    }

    // Los segundos que marca la barra del video: "0:07 / 0:25" da 7.
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

    // La barra lateral y los botones del video (D-23). Usa además un video de tool/videos-prueba.sh.
    @Test
    fun barraYVideo() {
        val muestra = File(requireNotNull(contexto.getExternalFilesDir("prueba-4k")), "hevc-20m.mp4")
        assertTrue("Falta $muestra: correr tool/videos-prueba.sh", muestra.isFile)
        muestra.copyTo(File(File(raiz, "videos").apply { mkdirs() }, muestra.name))
        val intent =
            Intent(contexto, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_RAIZ_CONTENIDO, raiz.absolutePath)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            // 1. Izquierda desde la primera tarjeta abre la barra en Inicio.
            esperarFoco("tarjeta-VIDEOS-0")
            tecla(KeyEvent.KEYCODE_DPAD_LEFT)
            esperarFoco("barra-INICIO")

            // 2. «Tu USB» sin memoria pide conectarla (con la depuración encendida, el Xiaomi no la lee).
            tecla(KeyEvent.KEYCODE_DPAD_DOWN)
            esperarFoco("barra-USB")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            regla.waitUntilExactlyOneExists(hasTestTag("aviso-usb"), 10_000)

            // 3. Inicio vuelve a la tarjeta donde estaba.
            tecla(KeyEvent.KEYCODE_DPAD_UP)
            esperarFoco("barra-INICIO")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            esperarFoco("tarjeta-VIDEOS-0")

            // 4. En el video, el botón «adelantar» salta 10 s.
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            esperarFoco("video-alternar")
            val antes = segundosEnLaBarra()
            tecla(KeyEvent.KEYCODE_DPAD_RIGHT)
            esperarFoco("video-adelantar")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            // La barra lee el reproductor cada 500 ms con el reloj de la prueba: se avanza para que lea.
            Thread.sleep(500)
            regla.mainClock.advanceTimeBy(600)
            val despues = segundosEnLaBarra()
            assertTrue("De $antes s pasó a $despues s", despues >= antes + 10)

            // 5. Un solo Atrás vuelve a la tarjeta del video.
            tecla(KeyEvent.KEYCODE_BACK)
            esperarFoco("tarjeta-VIDEOS-0")
        }
    }
}
