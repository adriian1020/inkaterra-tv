// Prueba el generador de miniaturas real con los videos de tool/videos-prueba.sh, y anota en el
// registro con la etiqueta «Miniaturas» cuánto tarda cada video 4K.
//   tool/videos-prueba.sh && tool/tv.sh probar pe.inkaterra.tv.sistema.MiniaturasEquipoTest
package pe.inkaterra.tv.sistema

import android.graphics.Bitmap
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.datos.biblioteca.ArchivoLocal
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.GeneradorMiniaturaAndroid
import pe.inkaterra.tv.dominio.TipoContenido
import java.io.File
import kotlin.time.measureTimedValue

@RunWith(AndroidJUnit4::class)
class MiniaturasEquipoTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val generador = GeneradorMiniaturaAndroid()

    private fun elemento(
        archivo: File,
        tipo: TipoContenido,
    ) = Elemento(
        ArchivoLocal(archivo.absolutePath, archivo.name, archivo.length(), archivo.lastModified(), false),
        tipo,
        archivo.nameWithoutExtension,
    )

    private fun muestra(nombre: String): File {
        val archivo = File(requireNotNull(contexto.getExternalFilesDir("prueba-4k")), nombre)
        assertTrue("Falta $archivo: correr tool/videos-prueba.sh", archivo.isFile)
        return archivo
    }

    @Test
    fun cuadroDeVideoHevcYVp9() {
        listOf("hevc-20m.mp4", "vp9-20m.webm").forEach { nombre ->
            val (imagen, tiempo) =
                measureTimedValue {
                    generador.generar(
                        elemento(muestra(nombre), TipoContenido.VIDEO),
                    )
                }
            Log.i("Miniaturas", "$nombre | ${imagen?.width}x${imagen?.height} | $tiempo")
            val i = requireNotNull(imagen) { "$nombre no dio cuadro" }
            assertTrue("$nombre: ${i.width}x${i.height}", i.width == 1280 || i.height == 720)
        }
    }

    @Test
    fun fotoReducida() {
        val foto = File(contexto.cacheDir, "foto-4k.jpg")
        foto.outputStream().use {
            Bitmap.createBitmap(3840, 2160, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        val imagen = requireNotNull(generador.generar(elemento(foto, TipoContenido.FOTO)))
        assertEquals(1280 to 720, imagen.width to imagen.height)
    }

    @Test
    fun audioSinCaratulaDaNulo() =
        assertNull(generador.generar(elemento(muestra("solo-audio.m4a"), TipoContenido.MUSICA)))
}
