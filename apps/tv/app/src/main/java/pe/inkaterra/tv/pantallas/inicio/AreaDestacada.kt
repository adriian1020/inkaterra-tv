// La mitad de arriba del inicio: la imagen grande, el título y el tipo de lo que tiene el foco,
// como en Netflix (D-20). La imagen se funde con el fondo hacia la izquierda, donde va el texto.
package pe.inkaterra.tv.pantallas.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.dominio.TipoContenido
import pe.inkaterra.tv.pantallas.comun.Miniatura

@Composable
fun AreaDestacada(
    elemento: Elemento,
    miniaturas: Miniaturas,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(0.6f).align(Alignment.TopEnd)) {
            Miniatura(elemento, miniaturas, Modifier.fillMaxSize())
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(ColoresInkaterra.fondo, Color.Transparent))),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.7f to Color.Transparent, 1f to ColoresInkaterra.fondo)),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(0.45f).align(Alignment.CenterStart).padding(start = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = elemento.titulo,
                style = MaterialTheme.typography.headlineLarge,
                color = ColoresInkaterra.crema,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("destacado-titulo"),
            )
            Text(
                text = stringResource(textoTipo(elemento.tipo)),
                style = MaterialTheme.typography.bodyLarge,
                color = ColoresInkaterra.crema.copy(alpha = 0.7f),
            )
        }
    }
}

private fun textoTipo(tipo: TipoContenido) =
    when (tipo) {
        TipoContenido.VIDEO -> R.string.tipo_video
        TipoContenido.FOTO -> R.string.tipo_foto
        TipoContenido.MUSICA -> R.string.tipo_musica
    }
