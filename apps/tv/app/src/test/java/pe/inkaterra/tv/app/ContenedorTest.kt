// La actividad se recrea (por ejemplo, al cambiar el idioma) pero las miniaturas y las posiciones
// para retomar son unas solas por proceso: si no, cada instancia trae su propio límite de dos y se
// suman decodificaciones 4K, y al volver a la app se olvidaría dónde iba cada video.
package pe.inkaterra.tv.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContenedorTest {
    private val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun lasMiniaturasSonUnaSolaPorProceso() {
        assertSame(Contenedor(contexto).miniaturas, Contenedor(contexto).miniaturas)
    }

    @Test
    fun lasPosicionesSonUnaSolaPorProceso() {
        assertSame(Contenedor(contexto).posiciones, Contenedor(contexto).posiciones)
    }
}
