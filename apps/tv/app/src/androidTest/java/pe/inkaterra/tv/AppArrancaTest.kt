// Prueba en el equipo: la app abre y muestra su nombre. Corre en la TV real, no en la PC:
//   tool/tv.sh probar pe.inkaterra.tv.AppArrancaTest
package pe.inkaterra.tv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.app.MainActivity

@RunWith(AndroidJUnit4::class)
class AppArrancaTest {
    @get:Rule val regla = createAndroidComposeRule<MainActivity>()

    @Test
    fun abreElInicioConElNombreDeLaApp() {
        regla.onNodeWithText("Inkaterra TV").assertIsDisplayed()
    }
}
