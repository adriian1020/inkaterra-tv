// La música de fondo de la presentación de fotos (D-22): las canciones de la fila Música en orden
// aleatorio y en bucle, desde una al azar. Suena mientras la presentación avanza y la app está al
// frente. Una canción que falla se salta sin mensaje; si fallan todas seguidas, se calla. Sin
// canciones, la presentación pasa en silencio.
package pe.inkaterra.tv.pantallas.fotos

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.dominio.Repetir
import pe.inkaterra.tv.pantallas.musica.modoMedia3
import pe.inkaterra.tv.pantallas.musica.ponerCanciones
import kotlin.random.Random

private const val ETIQUETA = "Fondo"

fun Player.prepararFondo(
    canciones: List<Elemento>,
    inicial: Int,
) {
    ponerCanciones(canciones, inicial)
    shuffleModeEnabled = true
    repeatMode = Repetir.LISTA.modoMedia3()
    prepare()
}

@Composable
fun MusicaDeFondo(
    canciones: List<Elemento>,
    sonando: Boolean,
) {
    if (canciones.isEmpty()) return
    val contexto = LocalContext.current
    val player =
        remember {
            ExoPlayer
                .Builder(contexto)
                .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                .build()
                .apply { prepararFondo(canciones, Random.nextInt(canciones.size)) }
        }
    var alFrente by remember { mutableStateOf(true) }
    var erroresSeguidos by remember { mutableIntStateOf(0) }

    DisposableEffect(player) {
        val oyente =
            object : Player.Listener {
                override fun onIsPlayingChanged(sonandoAhora: Boolean) {
                    if (sonandoAhora) erroresSeguidos = 0
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.w(ETIQUETA, "${player.currentMediaItem?.mediaId}: ${error.errorCodeName}", error)
                    erroresSeguidos++
                    if (erroresSeguidos < canciones.size) {
                        player.seekToNextMediaItem()
                        player.prepare()
                    }
                }
            }
        player.addListener(oyente)
        onDispose {
            player.removeListener(oyente)
            player.release()
        }
    }

    // Si la app pasa a segundo plano (Inicio del control), la música se pausa y sigue al volver.
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador =
            LifecycleEventObserver { _, evento ->
                when (evento) {
                    Lifecycle.Event.ON_STOP -> alFrente = false
                    Lifecycle.Event.ON_START -> alFrente = true
                    else -> Unit
                }
            }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    LaunchedEffect(sonando, alFrente) { player.playWhenReady = sonando && alFrente }
}
