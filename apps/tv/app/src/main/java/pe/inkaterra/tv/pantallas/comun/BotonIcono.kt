// Un botón redondo con ícono, para los controles de la música y del video. Apagado (aleatorio o sin
// repetir) se ve tenue. El color lo pone el botón, que cambia con el foco. La descripción es lo
// que dice el lector de pantalla y lo que leen las pruebas.
package pe.inkaterra.tv.pantallas.comun

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton

@Composable
fun BotonIcono(
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
