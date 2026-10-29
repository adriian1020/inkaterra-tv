// Los botones del reproductor de video (D-23): anterior, retroceder 10 s, pausar o seguir, adelantar
// 10 s y siguiente. Van con la barra del video, se recorren con las flechas y, al aparecer, el foco
// va a pausar.
package pe.inkaterra.tv.pantallas.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pe.inkaterra.tv.R
import pe.inkaterra.tv.pantallas.comun.BotonIcono

@Composable
fun BotonesVideo(
    pausado: Boolean,
    focoInicial: FocusRequester,
    alAnterior: () -> Unit,
    alRetroceder: () -> Unit,
    alAlternar: () -> Unit,
    alAdelantar: () -> Unit,
    alSiguiente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Al aparecer, el foco va a pausar.
    LaunchedEffect(Unit) { focoInicial.requestFocus() }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        BotonIcono(
            R.drawable.ic_anterior,
            stringResource(R.string.video_anterior),
            "video-anterior",
            alElegir = alAnterior,
        )
        BotonIcono(
            R.drawable.ic_retroceder,
            stringResource(R.string.video_retroceder),
            "video-retroceder",
            alElegir = alRetroceder,
        )
        BotonIcono(
            if (pausado) R.drawable.ic_reproducir else R.drawable.ic_pausa,
            stringResource(if (pausado) R.string.musica_reproducir else R.string.musica_pausar),
            "video-alternar",
            Modifier.focusRequester(focoInicial),
            alElegir = alAlternar,
        )
        BotonIcono(
            R.drawable.ic_adelantar,
            stringResource(R.string.video_adelantar),
            "video-adelantar",
            alElegir = alAdelantar,
        )
        BotonIcono(
            R.drawable.ic_siguiente,
            stringResource(R.string.video_siguiente),
            "video-siguiente",
            alElegir = alSiguiente,
        )
    }
}
