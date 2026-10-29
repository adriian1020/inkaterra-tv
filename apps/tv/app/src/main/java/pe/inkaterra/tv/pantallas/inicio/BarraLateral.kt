// La barra lateral del inicio, como la de Netflix (D-23): una columna angosta de íconos (Inicio, Tu
// USB e Idioma) que, al recibir el foco, se abre encima del contenido y muestra los nombres. La
// opción de la sección actual va en verde y «Tu USB» lleva un punto si hay una memoria conectada.
// Arriba y Abajo recorren las opciones; Derecha vuelve a la tarjeta donde estaba el foco.
package pe.inkaterra.tv.pantallas.inicio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.sistema.idioma.Idioma

private val ANCHO_CERRADA = 72.dp
private val ANCHO_ABIERTA = 260.dp

private enum class OpcionBarra { INICIO, USB, IDIOMA }

@Composable
fun BarraLateral(
    seccion: Seccion,
    hayMemoria: Boolean,
    otroIdioma: Idioma,
    // La opción de la sección actual, para llevarle el foco desde el contenido.
    focoSeccion: FocusRequester,
    alCambiarFoco: (Boolean) -> Unit,
    alElegirSeccion: (Seccion) -> Unit,
    alCambiarIdioma: () -> Unit,
    alVolverAlContenido: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var abierta by remember { mutableStateOf(false) }
    val focos = remember { OpcionBarra.entries.associateWith { FocusRequester() } }
    val fondo =
        if (abierta) {
            Brush.horizontalGradient(listOf(ColoresInkaterra.fondo, ColoresInkaterra.fondo.copy(alpha = 0.92f)))
        } else {
            Brush.horizontalGradient(listOf(ColoresInkaterra.fondo, ColoresInkaterra.fondo))
        }
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .width(if (abierta) ANCHO_ABIERTA else ANCHO_CERRADA)
                .background(fondo)
                .onFocusChanged {
                    abierta = it.hasFocus
                    alCambiarFoco(it.hasFocus)
                }.onPreviewKeyEvent { evento ->
                    if (evento.key != Key.DirectionRight) return@onPreviewKeyEvent false
                    if (evento.type == KeyEventType.KeyDown) alVolverAlContenido()
                    true
                }.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        OpcionBarra.entries.forEachIndexed { i, opcion ->
            val anterior = OpcionBarra.entries.getOrNull(i - 1)?.let { focos.getValue(it) }
            val siguiente = OpcionBarra.entries.getOrNull(i + 1)?.let { focos.getValue(it) }
            val actual =
                (opcion == OpcionBarra.INICIO && seccion == Seccion.INICIO) ||
                    (opcion == OpcionBarra.USB && seccion == Seccion.USB)
            Opcion(
                icono =
                    when (opcion) {
                        OpcionBarra.INICIO -> R.drawable.ic_inicio
                        OpcionBarra.USB -> R.drawable.ic_usb
                        OpcionBarra.IDIOMA -> R.drawable.ic_idioma
                    },
                nombre =
                    stringResource(
                        when (opcion) {
                            OpcionBarra.INICIO -> R.string.barra_inicio
                            OpcionBarra.USB -> R.string.fila_usb
                            OpcionBarra.IDIOMA ->
                                if (otroIdioma == Idioma.INGLES) R.string.idioma_ingles else R.string.idioma_espanol
                        },
                    ),
                abierta = abierta,
                actual = actual,
                conPunto = opcion == OpcionBarra.USB && hayMemoria,
                alElegir = {
                    when (opcion) {
                        OpcionBarra.INICIO -> alElegirSeccion(Seccion.INICIO)
                        OpcionBarra.USB -> alElegirSeccion(Seccion.USB)
                        OpcionBarra.IDIOMA -> alCambiarIdioma()
                    }
                },
                modifier =
                    Modifier
                        .focusRequester(focos.getValue(opcion))
                        .then(if (actual) Modifier.focusRequester(focoSeccion) else Modifier)
                        // Arriba y Abajo no se escapan de la barra hacia las tarjetas.
                        .focusProperties {
                            up = anterior ?: FocusRequester.Cancel
                            down = siguiente ?: FocusRequester.Cancel
                            left = FocusRequester.Cancel
                        }.testTag("barra-${opcion.name}"),
            )
        }
    }
}

@Composable
private fun Opcion(
    @DrawableRes icono: Int,
    nombre: String,
    abierta: Boolean,
    actual: Boolean,
    conPunto: Boolean,
    alElegir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = alElegir,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = nombre },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(24.dp)),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = if (actual) ColoresInkaterra.verde else Color.Transparent,
                contentColor = ColoresInkaterra.crema,
                focusedContainerColor = ColoresInkaterra.crema,
                focusedContentColor = ColoresInkaterra.fondo,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(24.dp))
                if (conPunto) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .background(ColoresInkaterra.verde, CircleShape),
                    )
                }
            }
            if (abierta) {
                Spacer(Modifier.width(16.dp))
                Text(
                    nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
