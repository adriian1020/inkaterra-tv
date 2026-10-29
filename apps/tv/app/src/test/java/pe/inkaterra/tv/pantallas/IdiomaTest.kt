// La primera vez la app pregunta el idioma; después va directo al inicio, desde donde se puede
// cambiar.
package pe.inkaterra.tv.pantallas

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
import pe.inkaterra.tv.app.Navegacion
import pe.inkaterra.tv.app.TemaInkaterra
import pe.inkaterra.tv.dominio.PosicionesVideo
import pe.inkaterra.tv.sistema.idioma.Idioma

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "es")
class IdiomaTest {
    @get:Rule(order = 0)
    val temporal = TemporaryFolder()

    @get:Rule(order = 1)
    val regla = createAndroidComposeRule<ComponentActivity>()

    private fun abrir(ajuste: AjusteIdiomaFalso) {
        val miniaturas = miniaturasSinImagen(temporal.root)
        regla.setContent { TemaInkaterra { Navegacion(bibliotecaDePrueba(), miniaturas, PosicionesVideo(), ajuste) } }
        regla.waitForIdle()
    }

    private fun tecla(t: Key) {
        regla.onRoot().performKeyInput { pressKey(t) }
        regla.waitForIdle()
    }

    @Test
    fun primeraVezPreguntaElIdioma() {
        abrir(AjusteIdiomaFalso(null))
        regla.onNodeWithText("Bienvenido · Welcome").assertExists()
        regla.onNodeWithText("Español").assertIsFocused()
    }

    @Test
    fun elegirInglesGuardaYPasaAlInicio() {
        val ajuste = AjusteIdiomaFalso(null)
        abrir(ajuste)
        tecla(Key.DirectionRight)
        tecla(Key.Enter)
        assertEquals(listOf(Idioma.INGLES), ajuste.cambios)
        regla.onNodeWithTag("tarjeta-DESTACADOS-0").assertExists()
    }

    @Test
    fun conIdiomaElegidoVaDirectoAlInicio() {
        abrir(AjusteIdiomaFalso(Idioma.ESPANOL))
        regla.onNodeWithText("Bienvenido · Welcome").assertDoesNotExist()
        regla.onNodeWithTag("tarjeta-DESTACADOS-0").assertIsFocused()
    }

    @Test
    fun cambiarIdiomaDesdeElInicio() {
        val ajuste = AjusteIdiomaFalso(Idioma.ESPANOL)
        abrir(ajuste)
        tecla(Key.DirectionUp)
        regla.onNodeWithTag("cambiar-idioma").assertIsFocused()
        tecla(Key.Enter)
        assertEquals(listOf(Idioma.INGLES), ajuste.cambios)
    }
}
