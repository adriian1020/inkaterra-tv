// El reproductor de música (D-22): suena toda la fila desde la canción elegida, con botones para
// anterior, reproducir o pausar, siguiente, orden aleatorio y repetir (la lista por omisión). Las
// teclas de música del control también funcionan. Atrás detiene la música y vuelve a la tarjeta de
// la que sonaba. Una canción que no se puede reproducir muestra un mensaje y sigue la siguiente; si
// fallan todas seguidas, vuelve. La MediaSession vive lo que vive la pantalla: no hay música en
// segundo plano.
package pe.inkaterra.tv.pantallas.musica

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.FuenteMiniaturas
import pe.inkaterra.tv.dominio.Repetir
import pe.inkaterra.tv.dominio.siguiente
import pe.inkaterra.tv.pantallas.comun.Miniatura
import pe.inkaterra.tv.pantallas.video.BarraVideo

private const val ETIQUETA = "Musica"
private const val ERROR_MS = 3_000L
private const val REFRESCO_MS = 500L

@Composable
fun ReproductorMusicaPantalla(
    canciones: List<Elemento>,
    inicial: Int,
    miniaturas: FuenteMiniaturas,
    alSalir: (Elemento) -> Unit,
) {
    val contexto = LocalContext.current
    val alSalirActual by rememberUpdatedState(alSalir)
    val player =
        remember {
            ExoPlayer
                .Builder(contexto)
                .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
                .build()
                .apply {
                    ponerCanciones(canciones, inicial)
                    repeatMode = Repetir.LISTA.modoMedia3()
                    prepare()
                    playWhenReady = true
                }
        }
    var actual by remember { mutableIntStateOf(inicial) }
    var pausado by remember { mutableStateOf(false) }
    var repetir by remember { mutableStateOf(Repetir.LISTA) }
    var aleatorio by remember { mutableStateOf(false) }
    // El índice de la canción que falló y cuántas fallaron seguidas: con «repetir la lista» no hay
    // última canción, así que si fallan todas se vuelve en vez de mostrar el mensaje para siempre.
    var errorEn by remember { mutableStateOf<Int?>(null) }
    var erroresSeguidos by remember { mutableIntStateOf(0) }
    var posicionMs by remember { mutableLongStateOf(0) }
    var duracionMs by remember { mutableLongStateOf(0) }
    val foco = remember { FocusRequester() }

    fun salir() = alSalirActual(canciones[actual])

    fun alternar() {
        if (player.playWhenReady) player.pause() else player.play()
        pausado = !player.playWhenReady
    }

    DisposableEffect(player) {
        val sesion = MediaSession.Builder(contexto, player).build()
        val oyente =
            object : Player.Listener {
                override fun onMediaItemTransition(
                    item: MediaItem?,
                    motivo: Int,
                ) {
                    actual = player.currentMediaItemIndex
                }

                override fun onIsPlayingChanged(sonando: Boolean) {
                    if (sonando) {
                        errorEn = null
                        erroresSeguidos = 0
                    }
                }

                // Solo con «sin repetir», al terminar la última.
                override fun onPlaybackStateChanged(estado: Int) {
                    if (estado == Player.STATE_ENDED) salir()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.w(ETIQUETA, "${canciones[actual].archivo.nombre}: ${error.errorCodeName}", error)
                    erroresSeguidos++
                    errorEn = actual
                }
            }
        player.addListener(oyente)
        onDispose {
            player.removeListener(oyente)
            sesion.release()
            player.release()
        }
    }

    LaunchedEffect(errorEn, erroresSeguidos) {
        if (errorEn == null) return@LaunchedEffect
        delay(ERROR_MS)
        if (erroresSeguidos >= canciones.size || !player.hasNextMediaItem()) {
            salir()
        } else {
            player.seekToNextMediaItem()
            player.prepare()
        }
    }

    LaunchedEffect(repetir) { player.repeatMode = repetir.modoMedia3() }
    LaunchedEffect(aleatorio) { player.shuffleModeEnabled = aleatorio }

    LaunchedEffect(Unit) {
        while (true) {
            posicionMs = player.currentPosition.coerceAtLeast(0)
            duracionMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0
            delay(REFRESCO_MS)
        }
    }

    // Si la app pasa a segundo plano (Inicio del control), la música se pausa.
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador =
            LifecycleEventObserver { _, evento ->
                if (evento == Lifecycle.Event.ON_STOP) {
                    player.pause()
                    pausado = true
                }
            }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    // Mientras suena, el protector de pantalla no tapa la carátula; en pausa sí puede aparecer.
    val vista = LocalView.current
    DisposableEffect(vista, pausado) {
        vista.keepScreenOn = !pausado
        onDispose { vista.keepScreenOn = false }
    }

    BackHandler { salir() }

    LaunchedEffect(Unit) { foco.requestFocus() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ColoresInkaterra.fondo)
                // Las flechas y Enter son de los botones; aquí solo las teclas de música del control.
                // Una tecla mantenida cuenta una vez.
                .onPreviewKeyEvent { evento ->
                    val accion: () -> Unit =
                        when (evento.key) {
                            Key.MediaPlayPause -> ::alternar
                            Key.MediaPlay -> ({ if (!player.playWhenReady) alternar() })
                            Key.MediaPause -> ({ if (player.playWhenReady) alternar() })
                            Key.MediaNext -> player::seekToNext
                            Key.MediaPrevious -> player::seekToPrevious
                            else -> return@onPreviewKeyEvent false
                        }
                    if (evento.type == KeyEventType.KeyDown && evento.nativeKeyEvent.repeatCount == 0) accion()
                    true
                },
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Miniatura(canciones[actual], miniaturas, Modifier.size(448.dp, 252.dp))
                if (errorEn == actual) {
                    Text(
                        text = stringResource(R.string.musica_no_reproduce),
                        style = MaterialTheme.typography.titleLarge,
                        color = ColoresInkaterra.crema,
                        modifier = Modifier.background(ColoresInkaterra.fondo).padding(16.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Boton(R.drawable.ic_anterior, stringResource(R.string.musica_anterior), "musica-anterior") {
                    player.seekToPrevious()
                }
                Boton(
                    if (pausado) R.drawable.ic_reproducir else R.drawable.ic_pausa,
                    stringResource(if (pausado) R.string.musica_reproducir else R.string.musica_pausar),
                    "musica-alternar",
                    Modifier.focusRequester(foco),
                ) { alternar() }
                Boton(R.drawable.ic_siguiente, stringResource(R.string.musica_siguiente), "musica-siguiente") {
                    player.seekToNext()
                }
                Boton(
                    R.drawable.ic_aleatorio,
                    stringResource(if (aleatorio) R.string.musica_aleatorio_si else R.string.musica_aleatorio_no),
                    "musica-aleatorio",
                    activo = aleatorio,
                ) { aleatorio = !aleatorio }
                Boton(
                    if (repetir == Repetir.UNA) R.drawable.ic_repetir_una else R.drawable.ic_repetir,
                    stringResource(
                        when (repetir) {
                            Repetir.LISTA -> R.string.musica_repetir_lista
                            Repetir.UNA -> R.string.musica_repetir_una
                            Repetir.NO -> R.string.musica_sin_repetir
                        },
                    ),
                    "musica-repetir",
                    activo = repetir != Repetir.NO,
                ) { repetir = repetir.siguiente() }
            }
        }
        BarraVideo(
            titulo = canciones[actual].titulo,
            posicionMs = posicionMs,
            duracionMs = duracionMs,
            pausado = pausado,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// Un botón con ícono; apagado (aleatorio o sin repetir) se ve tenue. El color lo pone el botón, que cambia con el foco. La descripción es lo que
// dice el lector de pantalla y lo que leen las pruebas.
@Composable
private fun Boton(
    @DrawableRes icono: Int,
    descripcion: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    activo: Boolean = true,
    alElegir: () -> Unit,
) {
    IconButton(
        onClick = alElegir,
        modifier = modifier.semantics { contentDescription = descripcion }.testTag(etiqueta),
    ) {
        Icon(
            painter = painterResource(icono),
            contentDescription = null,
            modifier = Modifier.alpha(if (activo) 1f else 0.4f),
        )
    }
}
