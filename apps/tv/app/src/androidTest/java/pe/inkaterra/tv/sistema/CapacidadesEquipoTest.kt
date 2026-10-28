// Ficha de la TV: decodificadores de video, espacio y memoria, en el registro con la etiqueta
// «Ficha». Corre en la TV real:
//   tool/tv.sh probar pe.inkaterra.tv.sistema.CapacidadesEquipoTest
package pe.inkaterra.tv.sistema

import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.dominio.soporta4k
import pe.inkaterra.tv.sistema.capacidades.CapacidadesEquipo

@RunWith(AndroidJUnit4::class)
class CapacidadesEquipoTest {
    @Test
    fun registraLaFicha() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val decodificadores = CapacidadesEquipo.decodificadoresVideo()
        val gb = 1024.0 * 1024 * 1024

        Log.i("Ficha", "Equipo: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
        val memoria = CapacidadesEquipo.memoriaTotalBytes(contexto) / gb
        val externo = CapacidadesEquipo.espacioLibreBytes(requireNotNull(contexto.getExternalFilesDir(null))) / gb
        val interno = CapacidadesEquipo.espacioLibreBytes(contexto.filesDir) / gb
        Log.i("Ficha", "Memoria total: %.2f GB".format(memoria))
        Log.i("Ficha", "Libre en archivos externos: %.2f GB".format(externo))
        Log.i("Ficha", "Libre en archivos internos: %.2f GB".format(interno))
        decodificadores.forEach {
            Log.i(
                "Ficha",
                "${it.nombre} | ${it.mime} | hw=${it.porHardware} | ${it.anchoMax}x${it.altoMax} | " +
                    "${it.tasaMaxBps / 1_000_000} Mbps | 4K=${soporta4k(it)}",
            )
        }

        assertTrue("Sin H.264 4K por hardware", decodificadores.any { it.mime == "video/avc" && soporta4k(it) })
        assertTrue("Sin HEVC 4K por hardware", decodificadores.any { it.mime == "video/hevc" && soporta4k(it) })
    }
}
