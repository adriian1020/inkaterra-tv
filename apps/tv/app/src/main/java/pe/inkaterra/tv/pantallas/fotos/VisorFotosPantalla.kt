// El visor de fotos (D-21): arranca una presentación en la foto elegida que pasa sola cada 8 s con
// un fundido de 1 s y da la vuelta por las fotos de la fila. Izquierda y Derecha cambian a mano,
// Enter pausa y Atrás vuelve al inicio. Una foto que no abre muestra su mensaje y, en la
// presentación, sigue a los 3 s. Coil reduce cada foto al tamaño de la pantalla.
package pe.inkaterra.tv.pantallas.fotos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import java.io.File

private const val FOTO_MS = 8_000L
private const val ERROR_MS = 3_000L
private const val FUNDIDO_MS = 1_000

@Composable
fun VisorFotosPantalla(
    fotos: List<Elemento>,
    inicial: Int,
    alSalir: (Elemento) -> Unit,
) {
    val contexto = LocalContext.current
    var actual by remember { mutableIntStateOf(inicial) }
    var pausada by remember { mutableStateOf(false) }
    // Qué fotos no abrieron; una foto que abre después (por ejemplo, al dar la vuelta) se borra.
    val conError = remember { mutableStateMapOf<Int, Boolean>() }
    val foco = remember { FocusRequester() }

    fun mover(paso: Int) {
        actual = (actual + paso).mod(fotos.size)
    }

    // Cambiar de foto (sola o a mano) vuelve a contar desde cero.
    val error = conError[actual] == true
    LaunchedEffect(actual, pausada, error) {
        if (pausada || fotos.size < 2) return@LaunchedEffect
        delay(if (error) ERROR_MS else FOTO_MS)
        mover(1)
    }

    // Sin esto, el protector de pantalla de Google TV tapa la presentación.
    val vista = LocalView.current
    DisposableEffect(vista) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }

    BackHandler { alSalir(fotos[actual]) }

    LaunchedEffect(Unit) { foco.requestFocus() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(foco)
                .onPreviewKeyEvent { evento ->
                    val accion: (() -> Unit) =
                        when (evento.key) {
                            Key.DirectionLeft -> ({ mover(-1) })
                            Key.DirectionRight -> ({ mover(1) })
                            Key.Enter, Key.DirectionCenter -> ({ pausada = !pausada })
                            else -> return@onPreviewKeyEvent false
                        }
                    if (evento.type == KeyEventType.KeyDown) accion()
                    true
                }.focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = actual, animationSpec = tween(FUNDIDO_MS), label = "foto") { indice ->
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(contexto)
                        .data(File(fotos[indice].archivo.ruta))
                        .build(),
                contentDescription = fotos[indice].titulo,
                contentScale = ContentScale.Fit,
                onSuccess = { conError[indice] = false },
                onError = { conError[indice] = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (error) {
            Text(
                text = stringResource(R.string.foto_no_abre),
                style = MaterialTheme.typography.titleLarge,
                color = ColoresInkaterra.crema,
                modifier = Modifier.testTag("foto-error"),
            )
        }
        Row(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, ColoresInkaterra.fondo)))
                    .padding(start = 48.dp, end = 48.dp, top = 48.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = fotos[actual].titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = ColoresInkaterra.crema,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).testTag("foto-titulo"),
            )
            if (pausada) {
                Text(
                    text = stringResource(R.string.presentacion_pausada),
                    style = MaterialTheme.typography.titleMedium,
                    color = ColoresInkaterra.crema,
                )
            }
        }
    }
}
