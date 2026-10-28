// Inicio provisional del sprint 0: solo el nombre de la app, para comprobar que se instala y
// abre en la TV. El sprint 1 lo reemplaza por el inicio con secciones.
package pe.inkaterra.tv.pantallas.inicio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import pe.inkaterra.tv.BuildConfig
import pe.inkaterra.tv.R
import pe.inkaterra.tv.dominio.textoVersion

// Fondo oscuro de la TV: en una habitación con poca luz, una pantalla clara encandila.
private val fondo = Color(0xFF1E2416)

@Composable
fun InicioPantalla() {
    Column(
        modifier = Modifier.fillMaxSize().background(fondo),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = Color.White,
            style = MaterialTheme.typography.displayMedium,
        )
        Text(
            text = textoVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 16.sp,
        )
    }
}
