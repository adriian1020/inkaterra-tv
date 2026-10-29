// La barra del reproductor: abajo, sobre un degradado, el título, el avance en verde y el tiempo
// (D-21). Se ve unos segundos después de cada tecla y siempre en pausa.
package pe.inkaterra.tv.pantallas.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.dominio.textoTiempo

@Composable
fun BarraVideo(
    titulo: String,
    posicionMs: Long,
    duracionMs: Long,
    pausado: Boolean,
    modifier: Modifier = Modifier,
) {
    val avance = if (duracionMs > 0) (posicionMs.toFloat() / duracionMs).coerceIn(0f, 1f) else 0f
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, ColoresInkaterra.fondo)))
                .padding(start = 48.dp, end = 48.dp, top = 64.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            color = ColoresInkaterra.crema,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("video-titulo"),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(ColoresInkaterra.crema.copy(alpha = 0.3f)),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(avance).background(ColoresInkaterra.verde))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(
                text = "${textoTiempo(posicionMs)} / ${textoTiempo(duracionMs)}",
                style = MaterialTheme.typography.titleMedium,
                color = ColoresInkaterra.crema,
                modifier = Modifier.testTag("video-tiempo"),
            )
            if (pausado) {
                Text(
                    text = stringResource(R.string.video_pausado),
                    style = MaterialTheme.typography.titleMedium,
                    color = ColoresInkaterra.crema,
                )
            }
        }
    }
}
