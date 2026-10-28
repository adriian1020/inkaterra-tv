// Prueba en el equipo: la app abre en la pantalla de idioma (la primera vez) o en el inicio. Corre
// en la TV real, no en la PC:
//   tool/tv.sh probar pe.inkaterra.tv.AppArrancaTest
package pe.inkaterra.tv

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.app.MainActivity

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppArrancaTest {
    @get:Rule val regla = createAndroidComposeRule<MainActivity>()

    @Test
    fun abreElIdiomaOElInicio() {
        regla.waitUntilExactlyOneExists(hasText("Bienvenido · Welcome") or hasTestTag("cambiar-idioma"), 10_000)
    }
}
