// Mide qué videos 4K reproduce fluido la TV: cada video de tool/videos-prueba.sh se reproduce
// 20 s en pantalla con Media3 y se anotan el decodificador, los cuadros mostrados y los perdidos
// en el registro con la etiqueta «Prueba4k», uno por uno y con la hora. Una medición sin cuadros
// de video o con la pantalla apagada no cuenta como fluida. De aquí sale la tasa máxima D-13.
//   tool/videos-prueba.sh && tool/tv.sh probar pe.inkaterra.tv.sistema.Reproduccion4kTest
package pe.inkaterra.tv.sistema

import android.os.PowerManager
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.app.MainActivity
import java.io.File
import java.time.LocalTime
import java.time.temporal.ChronoUnit
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
        val pantallaEncendida: Boolean,
    ) {
        val porcentajePerdidos: Double
            get() = if (mostrados + perdidos == 0) 0.0 else 100.0 * perdidos / (mostrados + perdidos)

        // Sin decodificador de video, ExoPlayer reproduce solo el audio y no da error.
        val valida: Boolean
            get() = error == null && pantallaEncendida && decodificador != null && mostrados > 0

        val estado: String
            get() =
                when {
                    error != null -> "NO REPRODUCE: $error"
                    !pantallaEncendida -> "INVÁLIDA: la TV estaba suspendida"
                    !valida -> "NO REPRODUCE: sin cuadros de video"
                    else -> "OK"
                }
    }

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val energia = contexto.getSystemService(PowerManager::class.java)

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
        listOf("h264-20m.mp4", "hevc-20m.mp4").forEach { nombre ->
            val medicion = mediciones.firstOrNull { it.archivo == nombre }
            assertNotNull("Falta $nombre", medicion)
            assertTrue("$nombre: ${medicion?.estado}", medicion?.valida == true)
        }
    }

    // Si la TV no tiene decodificador para un video, ExoPlayer reproduce solo el audio sin dar
    // error: eso no puede quedar como «fluido». solo-audio.m4a lo genera tool/videos-prueba.sh.
    @Test
    fun sinCuadrosDeVideoNoCuentaComoFluido() {
        val soloAudio = File(requireNotNull(contexto.getExternalFilesDir("prueba-4k")), "solo-audio.m4a")
        assertTrue("Falta $soloAudio: correr tool/videos-prueba.sh", soloAudio.isFile)
        val medicion = ActivityScenario.launch(MainActivity::class.java).use { medir(it, soloAudio) }
        assertFalse("Sin cuadros de video no es una medición válida", medicion.valida)
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
        var superficie: SurfaceView? = null
        var reproductor: ExoPlayer? = null
        var decodificador: String? = null
        var error: String? = null
        val encendidaAlEmpezar = energia.isInteractive
        val inicio = LocalTime.now().truncatedTo(ChronoUnit.SECONDS)

        escena.onActivity { actividad ->
            superficie = SurfaceView(actividad)
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
                    pantallaEncendida = encendidaAlEmpezar && energia.isInteractive,
                )
            reproductor?.release()
            // Cada video dibuja en su propia superficie: si se acumulan, el siguiente puede quedar tapado.
            superficie?.let { (it.parent as? ViewGroup)?.removeView(it) }
        }
        return requireNotNull(medicion).also { registrar(inicio, it) }
    }

    // Se registra cada video apenas termina: si la TV se cae a mitad, quedan los anteriores.
    private fun registrar(
        inicio: LocalTime,
        m: Medicion,
    ) {
        val cuadros = "mostrados=${m.mostrados} | perdidos=${m.perdidos}"
        val porcentaje = "%.2f %%".format(m.porcentajePerdidos)
        Log.i("Prueba4k", "$inicio | ${m.archivo} | ${m.decodificador ?: "-"} | $cuadros | $porcentaje | ${m.estado}")
    }

    private companion object {
        const val SEGUNDOS_POR_VIDEO = 20L
    }
}
