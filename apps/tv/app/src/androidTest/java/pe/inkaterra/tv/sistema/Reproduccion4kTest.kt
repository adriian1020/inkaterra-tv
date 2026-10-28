// Mide qué videos 4K reproduce fluido la TV: cada video de tool/videos-prueba.sh se reproduce
// 20 s en pantalla con Media3 y se anotan el decodificador, los cuadros mostrados y los perdidos
// en el registro con la etiqueta «Prueba4k». De aquí sale la tasa máxima D-13.
//   tool/videos-prueba.sh && tool/tv.sh probar pe.inkaterra.tv.sistema.Reproduccion4kTest
package pe.inkaterra.tv.sistema

import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.app.MainActivity
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class Reproduccion4kTest {
    private data class Medicion(
        val archivo: String,
        val decodificador: String?,
        val mostrados: Int,
        val perdidos: Int,
        val error: String?,
    ) {
        val porcentajePerdidos: Double
            get() = if (mostrados + perdidos == 0) 0.0 else 100.0 * perdidos / (mostrados + perdidos)
    }

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun reproduceCadaVideo() {
        val carpeta = requireNotNull(contexto.getExternalFilesDir("prueba-4k"))
        val videos =
            carpeta
                .listFiles()
                .orEmpty()
                .filter { it.isFile }
                .sortedBy { it.name }
        assertTrue("No hay videos en $carpeta: correr tool/videos-prueba.sh", videos.isNotEmpty())

        val mediciones =
            ActivityScenario
                .launch(
                    MainActivity::class.java,
                ).use { escena -> videos.map { medir(escena, it) } }
        mediciones.forEach {
            Log.i(
                "Prueba4k",
                "${it.archivo} | ${it.decodificador ?: "-"} | mostrados=${it.mostrados} | perdidos=${it.perdidos} | " +
                    "%.2f %%".format(it.porcentajePerdidos) + (it.error?.let { e -> " | NO REPRODUCE: $e" } ?: ""),
            )
        }
        listOf("h264-20m.mp4", "hevc-20m.mp4").forEach { nombre ->
            val medicion = mediciones.firstOrNull { it.archivo == nombre }
            assertNotNull("Falta $nombre", medicion)
            assertEquals("$nombre no debe fallar", null, medicion?.error)
        }
    }

    @Test
    fun videoImposibleSeRegistraYSigue() {
        val danado = File(contexto.cacheDir, "danado.mp4").apply { writeBytes(ByteArray(4096) { it.toByte() }) }
        val medicion = ActivityScenario.launch(MainActivity::class.java).use { medir(it, danado) }
        assertNotNull("Un archivo dañado debe quedar como «no reproduce»", medicion.error)
    }

    private fun medir(
        escena: ActivityScenario<MainActivity>,
        archivo: File,
    ): Medicion {
        val fin = CountDownLatch(1)
        var reproductor: ExoPlayer? = null
        var decodificador: String? = null
        var error: String? = null

        escena.onActivity { actividad ->
            val superficie = SurfaceView(actividad)
            actividad.addContentView(superficie, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            reproductor =
                ExoPlayer.Builder(actividad).build().apply {
                    setVideoSurfaceView(superficie)
                    addAnalyticsListener(
                        object : AnalyticsListener {
                            override fun onVideoDecoderInitialized(
                                eventTime: AnalyticsListener.EventTime,
                                decoderName: String,
                                initializedTimestampMs: Long,
                                initializationDurationMs: Long,
                            ) {
                                decodificador = decoderName
                            }
                        },
                    )
                    addListener(
                        object : Player.Listener {
                            override fun onPlayerError(e: PlaybackException) {
                                error = e.errorCodeName
                                fin.countDown()
                            }
                        },
                    )
                    setMediaItem(MediaItem.fromUri(archivo.toURI().toString()))
                    prepare()
                    play()
                }
        }

        fin.await(SEGUNDOS_POR_VIDEO, TimeUnit.SECONDS)

        var medicion: Medicion? = null
        escena.onActivity {
            val contadores = reproductor?.videoDecoderCounters
            medicion =
                Medicion(
                    archivo = archivo.name,
                    decodificador = decodificador,
                    mostrados = contadores?.renderedOutputBufferCount ?: 0,
                    perdidos = contadores?.droppedBufferCount ?: 0,
                    error = error,
                )
            reproductor?.release()
        }
        return requireNotNull(medicion)
    }

    private companion object {
        const val SEGUNDOS_POR_VIDEO = 20L
    }
}
