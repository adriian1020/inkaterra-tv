// La imagen de una tarjeta o del área destacada. Mientras se genera, o si no se pudo, queda un
// recuadro verde; una canción sin carátula muestra la imagen de la marca.
package pe.inkaterra.tv.pantallas.comun

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.dominio.TipoContenido
import java.io.File

private sealed interface EstadoMiniatura {
    data object Cargando : EstadoMiniatura

    data class Lista(
        val archivo: File?,
    ) : EstadoMiniatura
}

@Composable
fun Miniatura(
    elemento: Elemento,
    miniaturas: Miniaturas,
    modifier: Modifier = Modifier,
) {
    // Al cambiar de elemento vuelve a Cargando: si no, el área destacada seguía mostrando la imagen
    // anterior mientras se generaba la nueva.
    val estado by produceState<EstadoMiniatura>(EstadoMiniatura.Cargando, elemento) {
        value = EstadoMiniatura.Cargando
        value = EstadoMiniatura.Lista(miniaturas.de(elemento))
    }
    val cargando = estado !is EstadoMiniatura.Lista
    Box(
        modifier
            .background(
                ColoresInkaterra.verdeSecundario,
            ).then(if (cargando) Modifier.testTag("miniatura-cargando") else Modifier),
    ) {
        val lista = estado as? EstadoMiniatura.Lista ?: return@Box
        when {
            lista.archivo != null ->
                AsyncImage(
                    model = lista.archivo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            elemento.tipo == TipoContenido.MUSICA ->
                Image(
                    painter = painterResource(R.drawable.sin_caratula),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}
