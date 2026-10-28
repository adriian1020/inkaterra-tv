// Memoria USB conectada a la TV: que se detecte, que se lea sin copiar nada y que las fotos
// HEIC de celular se puedan abrir. Necesita una USB conectada; el resultado queda en el
// registro con la etiqueta «Usb».
//   tool/tv.sh probar pe.inkaterra.tv.sistema.VolumenesUsbTest
package pe.inkaterra.tv.sistema

import android.graphics.ImageDecoder
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.sistema.usb.VolumenesUsb

@RunWith(AndroidJUnit4::class)
class VolumenesUsbTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun listaLaUsbConectada() {
        val volumenes = VolumenesUsb(contexto).actuales()
        volumenes.forEach { Log.i("Usb", "Volumen ${it.id} «${it.etiqueta}» montado=${it.montado} raiz=${it.raiz}") }
        val usb = volumenes.firstOrNull { it.montado && it.raiz != null }
        assertTrue("No hay una memoria USB montada", usb != null)

        val raiz = requireNotNull(usb?.raiz)
        Log.i("Usb", "Lectura directa de $raiz: canRead=${raiz.canRead()}")
        val archivos = raiz.walkTopDown().filter { it.isFile }.toList()
        archivos.forEach { Log.i("Usb", "${it.relativeTo(raiz)} | ${it.length()} bytes | canRead=${it.canRead()}") }
        assertTrue("No se pudo leer ningún archivo de la USB", archivos.any { it.canRead() })

        archivos.filter { it.extension.equals("heic", ignoreCase = true) }.forEach { heic ->
            val resultado =
                runCatching { ImageDecoder.decodeBitmap(ImageDecoder.createSource(heic)) }
                    .fold({ "abre ${it.width}x${it.height}" }, { "NO ABRE: ${it.message}" })
            Log.i("Usb", "HEIC ${heic.name}: $resultado")
        }
    }
}
