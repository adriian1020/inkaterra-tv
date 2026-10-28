// Inicio provisional del sprint 0: solo el nombre de la app, para comprobar que se instala y
// abre en la TV. El sprint 1 lo reemplaza por el inicio con secciones.
package pe.inkaterra.tv.pantallas.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import pe.inkaterra.tv.R

// Fondo oscuro de la TV: en una habitación con poca luz, una pantalla clara encandila.
private val fondo = Color(0xFF1E2416)

@Composable
fun InicioPantalla() {
    Box(
        modifier = Modifier.fillMaxSize().background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(R.string.app_name), color = Color.White, fontSize = 48.sp)
    }
}
