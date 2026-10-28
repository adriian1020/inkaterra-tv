// Una tarjeta 16:9 de una fila del inicio: miniatura y título. Con el foco crece y lleva el borde
// verde de Inkaterra (D-20).
package pe.inkaterra.tv.pantallas.inicio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.pantallas.comun.Miniatura

@Composable
fun TarjetaContenido(
    elemento: Elemento,
    miniaturas: Miniaturas,
    alElegir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = alElegir,
        modifier = modifier.size(200.dp, 113.dp),
        scale = CardDefaults.scale(focusedScale = 1.1f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, ColoresInkaterra.verde))),
    ) {
        Box(Modifier.fillMaxSize()) {
            Miniatura(elemento, miniaturas, Modifier.fillMaxSize())
            // Un degradado oscuro abajo para que el título se lea sobre cualquier imagen.
            Text(
                text = elemento.titulo,
                style = MaterialTheme.typography.bodyMedium,
                color = ColoresInkaterra.crema,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, ColoresInkaterra.fondo)))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
