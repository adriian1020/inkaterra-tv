// Prueba en el equipo del recorrido del sprint 1 con teclas como las del control: elegir el
// idioma, recorrer el inicio, abrir una tarjeta y volver con Atrás a la misma. Usa su propia
// carpeta de contenido con archivos vacíos (sus miniaturas fallan y quedan los recuadros verdes).
//   tool/tv.sh probar pe.inkaterra.tv.FlujoSprint1Test
package pe.inkaterra.tv

import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.app.MainActivity
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class FlujoSprint1Test {
    @get:Rule
    val regla = createEmptyComposeRule()

    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacion.targetContext
    private val raiz = File(requireNotNull(contexto.getExternalFilesDir(null)), "contenido-prueba")

    @Before
    fun preparar() {
        raiz.deleteRecursively()
        File(raiz, "fotos").mkdirs()
        File(raiz, "fotos/Reserva Amazónica 01.jpg").createNewFile()
        File(raiz, "musica").mkdirs()
        File(raiz, "musica/Selva.mp3").createNewFile()
        // Como la primera vez: sin idioma elegido. Desde Android 13 lo guarda el sistema, y appcompat
        // sin una actividad abierta no llega a borrarlo.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            contexto.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.getEmptyLocaleList()
        } else {
            instrumentacion.runOnMainSync {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.getEmptyLocaleList(),
                )
            }
        }
    }

    @After
    fun limpiar() {
        raiz.deleteRecursively()
    }

    private fun tecla(codigo: Int) {
        instrumentacion.sendKeyDownUpSync(codigo)
        regla.waitForIdle()
    }

    // Esperar a que el foco llegue: elegir el idioma recrea la actividad.
    private fun esperarFoco(tag: String) = regla.waitUntilExactlyOneExists(hasTestTag(tag) and isFocused(), 10_000)

    @Test
    fun recorreIdiomaInicioYAbre() {
        val intent =
            Intent(contexto, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_RAIZ_CONTENIDO, raiz.absolutePath)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            regla.waitUntilExactlyOneExists(hasText("Bienvenido · Welcome"), 10_000)
            regla.onNodeWithText("Español").assertIsFocused()
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)

            // Sin destacados ni videos, la primera fila es Fotos.
            esperarFoco("tarjeta-FOTOS-0")
            tecla(KeyEvent.KEYCODE_DPAD_DOWN)
            esperarFoco("tarjeta-MUSICA-0")
            tecla(KeyEvent.KEYCODE_DPAD_CENTER)
            regla.onNodeWithTag("abrir-titulo").assertTextEquals("Selva")
            tecla(KeyEvent.KEYCODE_BACK)
            esperarFoco("tarjeta-MUSICA-0")
        }
    }
}
