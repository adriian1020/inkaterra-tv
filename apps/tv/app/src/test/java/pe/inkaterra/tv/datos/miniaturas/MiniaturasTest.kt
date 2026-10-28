// Las miniaturas se generan una vez por archivo, se guardan en la caché y, si algo falla, la
// tarjeta se queda sin imagen pero la app sigue (D-20).
package pe.inkaterra.tv.datos.miniaturas

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.dominio.TipoContenido
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MiniaturasTest {
    @get:Rule
    val temporal = TemporaryFolder()

    private class GeneradorFalso(
        private val resultado: () -> Bitmap?,
    ) : GeneradorMiniatura {
        val llamadas = AtomicInteger()

        override fun generar(elemento: Elemento): Bitmap? {
            llamadas.incrementAndGet()
            return resultado()
        }
    }

    private fun imagen() = Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888)

    private fun elemento(
        nombre: String = "Atardecer en Machu Picchu.mp4",
        modificadoMs: Long = 0,
    ) = Elemento(archivo(nombre, modificadoMs = modificadoMs), TipoContenido.VIDEO, nombre.substringBeforeLast('.'))

    private fun miniaturas(generador: GeneradorMiniatura) = Miniaturas(generador, temporal.root, Dispatchers.Unconfined)

    @Test
    fun generaUnaVezYReutiliza() =
        runTest {
            val generador = GeneradorFalso { imagen() }
            val m = miniaturas(generador)
            val primera = m.de(elemento())
            val segunda = m.de(elemento())
            assertEquals(1, generador.llamadas.get())
            assertEquals(primera, segunda)
            assertTrue(requireNotNull(primera).length() > 0)
        }

    @Test
    fun archivoCambiadoGeneraOtra() =
        runTest {
            val generador = GeneradorFalso { imagen() }
            val m = miniaturas(generador)
            val antes = m.de(elemento(modificadoMs = 1))
            val despues = m.de(elemento(modificadoMs = 2))
            assertEquals(2, generador.llamadas.get())
            assertNotEquals(antes, despues)
        }

    @Test
    fun sinImagenDaNulo() =
        runTest {
            assertNull(miniaturas(GeneradorFalso { null }).de(elemento()))
            assertEquals(
                0,
                temporal.root
                    .listFiles()
                    .orEmpty()
                    .size,
            )
        }

    @Test
    fun errorDelGeneradorDaNulo() =
        runTest {
            assertNull(miniaturas(GeneradorFalso { error("Archivo dañado") }).de(elemento()))
            assertEquals(
                0,
                temporal.root
                    .listFiles()
                    .orEmpty()
                    .size,
            )
        }

    @Test
    fun generaComoMaximoDosALaVez() {
        val corriendo = AtomicInteger()
        val maximo = AtomicInteger()
        val soltar = CountDownLatch(1)
        val generador =
            GeneradorFalso {
                maximo.accumulateAndGet(corriendo.incrementAndGet(), ::maxOf)
                soltar.await(5, TimeUnit.SECONDS)
                corriendo.decrementAndGet()
                imagen()
            }
        val m = Miniaturas(generador, temporal.root, Dispatchers.IO)
        runBlocking {
            val pedidos = (1..5).map { async(Dispatchers.IO) { m.de(elemento("Video $it.mp4")) } }
            // Se espera a que entren dos y un poco más, por si un tercero se colara.
            repeat(50) { if (corriendo.get() < 2) Thread.sleep(20) }
            Thread.sleep(200)
            assertEquals(2, maximo.get())
            soltar.countDown()
            pedidos.awaitAll().forEach { assertNotNull(it) }
        }
        assertEquals(2, maximo.get())
        assertEquals(5, generador.llamadas.get())
    }

    // El área destacada y su tarjeta piden la misma miniatura en el mismo cuadro: se genera una
    // sola vez y las dos reciben el archivo.
    @Test
    fun mismoElementoALaVezSeGeneraUnaVez() {
        val soltar = CountDownLatch(1)
        val generador =
            GeneradorFalso {
                soltar.await(5, TimeUnit.SECONDS)
                imagen()
            }
        val m = Miniaturas(generador, temporal.root, Dispatchers.IO)
        runBlocking {
            val pedidos = (1..2).map { async(Dispatchers.IO) { m.de(elemento()) } }
            Thread.sleep(300)
            soltar.countDown()
            val archivos = pedidos.awaitAll()
            assertNotNull(archivos[0])
            assertEquals(archivos[0], archivos[1])
        }
        assertEquals(1, generador.llamadas.get())
    }
}
