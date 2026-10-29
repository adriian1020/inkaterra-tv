// El reproductor de video a pantalla completa con el control (D-21): Enter pausa, Izquierda y
// Derecha saltan 10 s, Atrás vuelve al inicio y retoma la próxima vez. Al terminar sigue el
// próximo video de la fila; un video que no se puede reproducir muestra un mensaje y también sigue.
// La imagen va en un SurfaceView: es la única superficie que mantiene el 4K por hardware sin perder
// cuadros en el Xiaomi.
@file:OptIn(UnstableApi::class)

package pe.inkaterra.tv.pantallas.video

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderCounters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.dominio.AccionVideo
import pe.inkaterra.tv.dominio.PosicionesVideo
import pe.inkaterra.tv.dominio.destinoSalto
import java.io.File

private const val ETIQUETA = "Reproductor"
private const val BARRA_MS = 5_000L
private const val ERROR_MS = 3_000L
private const val REFRESCO_MS = 500L

private val teclas =
    mapOf(
        Key.Enter to AccionVideo.ALTERNAR,
        Key.DirectionCenter to AccionVideo.ALTERNAR,
        Key.MediaPlayPause to AccionVideo.ALTERNAR,
        Key.DirectionLeft to AccionVideo.RETROCEDER,
        Key.MediaRewind to AccionVideo.RETROCEDER,
        Key.DirectionRight to AccionVideo.ADELANTAR,
        Key.MediaFastForward to AccionVideo.ADELANTAR,
    )

private class Cuadros(
    val contadores: DecoderCounters? = null,
    val mostrados: Int = 0,
    val perdidos: Int = 0,
)

@Composable
fun ReproductorPantalla(
    videos: List<Elemento>,
    inicial: Int,
    posiciones: PosicionesVideo,
    alSalir: (Elemento) -> Unit,
) {
    val contexto = LocalContext.current
    val alSalirActual by rememberUpdatedState(alSalir)
    val player =
        remember {
            ExoPlayer
                .Builder(contexto)
                .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
                .build()
        }
    var actual by remember { mutableIntStateOf(inicial) }
    var pausado by remember { mutableStateOf(false) }
    var conError by remember { mutableStateOf(false) }
    // Cada tecla suma uno y vuelve a mostrar la barra por 5 s.
    var toques by remember { mutableIntStateOf(0) }
    var barraVisible by remember { mutableStateOf(true) }
    var posicionMs by remember { mutableLongStateOf(0) }
    var duracionMs by remember { mutableLongStateOf(0) }
    // Los contadores del decodificador suman desde que se activa; se resta lo que había al empezar
    // cada video para que el registro sea de ese video.
    var alEmpezar by remember { mutableStateOf(Cuadros()) }
    val foco = remember { FocusRequester() }

    fun cuadros(): Cuadros {
        val contadores = player.videoDecoderCounters ?: return Cuadros()
        contadores.ensureUpdated()
        return Cuadros(contadores, contadores.renderedOutputBufferCount, contadores.droppedBufferCount)
    }

    fun registrarCuadros() {
        val ahora = cuadros()
        val base = if (ahora.contadores === alEmpezar.contadores) alEmpezar else Cuadros()
        Log.i(
            ETIQUETA,
            "${videos[actual].archivo.nombre} | mostrados=${ahora.mostrados - base.mostrados} | " +
                "perdidos=${ahora.perdidos - base.perdidos}",
        )
    }

    fun guardarPosicion() = posiciones.guardar(videos[actual].archivo.ruta, player.currentPosition, player.duration)

    // Al terminar o fallar: sigue el próximo video de la fila o, después del último, vuelve.
    fun pasarAlSiguiente() {
        registrarCuadros()
        guardarPosicion()
        val siguiente = siguienteVideo(videos, actual)
        if (siguiente == null) alSalirActual(videos[actual]) else actual = siguiente
    }

    fun refrescarTiempo() {
        posicionMs = player.currentPosition.coerceAtLeast(0)
        duracionMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0
    }

    LaunchedEffect(actual) {
        val ruta = videos[actual].archivo.ruta
        conError = false
        pausado = false
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(ruta))), posiciones.posicion(ruta))
        player.prepare()
        player.playWhenReady = true
        alEmpezar = cuadros()
        toques++
    }

    DisposableEffect(player) {
        val oyente =
            object : Player.Listener {
                override fun onPlaybackStateChanged(estado: Int) {
                    if (estado == Player.STATE_ENDED) pasarAlSiguiente()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.w(ETIQUETA, "${videos[actual].archivo.nombre}: ${error.errorCodeName}", error)
                    conError = true
                }
            }
        player.addListener(oyente)
        onDispose {
            player.removeListener(oyente)
            player.release()
        }
    }

    LaunchedEffect(conError) {
        if (conError) {
            delay(ERROR_MS)
            pasarAlSiguiente()
        }
    }

    LaunchedEffect(toques, pausado) {
        barraVisible = true
        if (!pausado) {
            delay(BARRA_MS)
            barraVisible = false
        }
    }

    LaunchedEffect(barraVisible) {
        while (barraVisible) {
            refrescarTiempo()
            delay(REFRESCO_MS)
        }
    }

    // Si la app pasa a segundo plano (Inicio del control), el video se pausa y guarda dónde iba.
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador =
            LifecycleEventObserver { _, evento ->
                if (evento == Lifecycle.Event.ON_STOP) {
                    player.pause()
                    pausado = true
                    guardarPosicion()
                }
            }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    // Sin esto, el protector de pantalla de Google TV aparece en medio del video.
    val vista = LocalView.current
    DisposableEffect(vista) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }

    BackHandler {
        registrarCuadros()
        guardarPosicion()
        alSalirActual(videos[actual])
    }

    LaunchedEffect(Unit) { foco.requestFocus() }

    val presentacion = rememberPresentationState(player)
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(foco)
                .onPreviewKeyEvent { evento ->
                    val accion = teclas[evento.key] ?: return@onPreviewKeyEvent false
                    if (evento.type == KeyEventType.KeyDown) {
                        when (accion) {
                            AccionVideo.ALTERNAR -> {
                                if (player.playWhenReady) player.pause() else player.play()
                                pausado = !player.playWhenReady
                            }
                            else -> destinoSalto(accion, player.currentPosition, player.duration)?.let(player::seekTo)
                        }
                        refrescarTiempo()
                        toques++
                    }
                    true
                }.focusable(),
        contentAlignment = Alignment.Center,
    ) {
        PlayerSurface(
            player = player,
            modifier = Modifier.resizeWithContentScale(ContentScale.Fit, presentacion.videoSizeDp),
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        )
        if (conError) {
            Text(
                text = stringResource(R.string.video_no_reproduce),
                style = MaterialTheme.typography.titleLarge,
                color = ColoresInkaterra.crema,
            )
        }
        if (barraVisible && !conError) {
            BarraVideo(
                titulo = videos[actual].titulo,
                posicionMs = posicionMs,
                duracionMs = duracionMs,
                pausado = pausado,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
