// El tema de la app usa los colores y las letras de Inkaterra (Imagen de marca, D-12).
package pe.inkaterra.tv.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemaTest {
    @get:Rule
    val regla = createComposeRule()

    @Test
    fun usaLosColoresYLetrasDeInkaterra() {
        lateinit var esquema: ColorScheme
        lateinit var tipos: Typography
        regla.setContent {
            TemaInkaterra {
                esquema = MaterialTheme.colorScheme
                tipos = MaterialTheme.typography
            }
        }
        assertEquals(Color(0xFF5A6A40), esquema.primary)
        assertEquals(Color(0xFF1E2416), esquema.background)
        assertEquals(familiaTitulos, tipos.headlineLarge.fontFamily)
        assertEquals(familiaTexto, tipos.bodyLarge.fontFamily)
    }
}
