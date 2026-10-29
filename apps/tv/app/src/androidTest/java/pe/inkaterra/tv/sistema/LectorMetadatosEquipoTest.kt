// Prueba el lector de metadatos real con los videos de tool/videos-prueba.sh: la TV dice si tiene
// un decodificador para el perfil exacto de cada video (D-21).
//   tool/videos-prueba.sh && tool/tv.sh probar pe.inkaterra.tv.sistema.LectorMetadatosEquipoTest
package pe.inkaterra.tv.sistema

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.datos.biblioteca.LectorMetadatosAndroid
import java.io.File

@RunWith(AndroidJUnit4::class)
class LectorMetadatosEquipoTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val lector = LectorMetadatosAndroid()

    private fun muestra(nombre: String): String {
        val archivo = File(requireNotNull(contexto.getExternalFilesDir("prueba-4k")), nombre)
        assertTrue("Falta $archivo: correr tool/videos-prueba.sh", archivo.isFile)
        return archivo.absolutePath
    }

    @Test
    fun perfilCuatroCuatroCuatroNoSeDecodifica() {
        val metadatos = lector.video(muestra("h264-444.mp4"))
        assertEquals("video/avc", metadatos.mime)
        assertFalse(metadatos.decodificable)
    }

    @Test
    fun hevcYVp9SeDecodifican() {
        listOf("hevc-20m.mp4", "vp9-20m.webm").forEach {
            assertTrue("$it debería decodificarse", lector.video(muestra(it)).decodificable)
        }
    }
}
