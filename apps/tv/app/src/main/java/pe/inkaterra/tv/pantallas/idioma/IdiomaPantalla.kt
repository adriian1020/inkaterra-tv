// La primera vez, la app pregunta el idioma. Cada opción va escrita en su propio idioma.
package pe.inkaterra.tv.pantallas.idioma

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.sistema.idioma.Idioma

@Composable
fun IdiomaPantalla(alElegir: (Idioma) -> Unit) {
    val espanol = remember { FocusRequester() }
    LaunchedEffect(Unit) { espanol.requestFocus() }
    Column(
        modifier = Modifier.fillMaxSize().background(ColoresInkaterra.fondo),
        verticalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.idioma_bienvenida),
            style = MaterialTheme.typography.displaySmall,
            color = ColoresInkaterra.crema,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Button(onClick = { alElegir(Idioma.ESPANOL) }, modifier = Modifier.focusRequester(espanol)) {
                Text(stringResource(R.string.idioma_espanol))
            }
            Button(onClick = { alElegir(Idioma.INGLES) }) {
                Text(stringResource(R.string.idioma_ingles))
            }
        }
    }
}
