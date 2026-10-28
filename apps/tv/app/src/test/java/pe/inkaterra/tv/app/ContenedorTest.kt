// La actividad se recrea (por ejemplo, al cambiar el idioma) pero las miniaturas son una sola por
// proceso: si no, cada instancia trae su propio límite de dos y se suman decodificaciones 4K.
package pe.inkaterra.tv.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContenedorTest {
    @Test
    fun lasMiniaturasSonUnaSolaPorProceso() {
        val contexto = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertSame(Contenedor(contexto).miniaturas, Contenedor(contexto).miniaturas)
    }
}
