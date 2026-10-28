// Imagen de Inkaterra en la TV (D-12): fondo oscuro con verde, Josefin Sans para el texto y Cinzel
// para los títulos, en lugar de Trajan Pro. Las dos letras van dentro de la app (licencia OFL en
// assets/licencias/).
package pe.inkaterra.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import pe.inkaterra.tv.R

object ColoresInkaterra {
    val verde = Color(0xFF5A6A40)
    val verdeSecundario = Color(0xFF626A45)
    val crema = Color(0xFFF3F2EE)
    val texto = Color(0xFF424242)
    val fondo = Color(0xFF1E2416)
}

// Las dos fuentes son variables: cada peso es la misma fuente con otro valor del eje wght.
private fun pesos(
    fuente: Int,
    vararg pesos: FontWeight,
) = FontFamily(
    pesos.map { Font(fuente, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight))) },
)

val familiaTexto = pesos(R.font.josefin_sans, FontWeight.Normal, FontWeight.SemiBold)
val familiaTitulos = pesos(R.font.cinzel, FontWeight.Normal, FontWeight.Bold)

private val colores =
    darkColorScheme(
        primary = ColoresInkaterra.verde,
        secondary = ColoresInkaterra.verdeSecundario,
        background = ColoresInkaterra.fondo,
        surface = ColoresInkaterra.fondo,
        onPrimary = Color.White,
        onBackground = ColoresInkaterra.crema,
        onSurface = ColoresInkaterra.crema,
    )

private val tipografia =
    Typography().run {
        fun titulo(estilo: TextStyle) = estilo.copy(fontFamily = familiaTitulos)

        // Las tildes de las mayúsculas de Josefin Sans («Á») suben más que la línea: sin recortar el
        // interlineado arriba y con un poco más de alto, no se cortan en los textos con «…».
        fun texto(estilo: TextStyle) =
            estilo.copy(
                fontFamily = familiaTexto,
                lineHeight = estilo.fontSize * 1.6f,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            )
        Typography(
            displayLarge = titulo(displayLarge),
            displayMedium = titulo(displayMedium),
            displaySmall = titulo(displaySmall),
            headlineLarge = titulo(headlineLarge),
            headlineMedium = titulo(headlineMedium),
            headlineSmall = titulo(headlineSmall),
            titleLarge = texto(titleLarge),
            titleMedium = texto(titleMedium),
            titleSmall = texto(titleSmall),
            bodyLarge = texto(bodyLarge),
            bodyMedium = texto(bodyMedium),
            bodySmall = texto(bodySmall),
            labelLarge = texto(labelLarge),
            labelMedium = texto(labelMedium),
            labelSmall = texto(labelSmall),
        )
    }

@Composable
fun TemaInkaterra(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, typography = tipografia, content = content)
}
