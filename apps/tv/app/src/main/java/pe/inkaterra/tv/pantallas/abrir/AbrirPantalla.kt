// Provisional hasta el sprint 2: muestra el título de lo que se eligió en el inicio. El sprint 2
// la reemplaza por el reproductor de video y el visor de fotos.
package pe.inkaterra.tv.pantallas.abrir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pe.inkaterra.tv.app.ColoresInkaterra

@Composable
fun AbrirPantalla(titulo: String) {
    Box(Modifier.fillMaxSize().background(ColoresInkaterra.fondo), contentAlignment = Alignment.Center) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.displaySmall,
            color = ColoresInkaterra.crema,
            modifier = Modifier.testTag("abrir-titulo"),
        )
    }
}
