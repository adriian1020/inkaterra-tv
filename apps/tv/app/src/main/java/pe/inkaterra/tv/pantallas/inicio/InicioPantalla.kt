// Inicio estilo Netflix (D-20): arriba el área destacada con lo que tiene el foco, abajo las filas
// de tarjetas. El foco arranca en la primera tarjeta, al regresar de lo que se abrió vuelve a la
// tarjeta de lo último que se vio (D-21), y Atrás fuera de la primera fila sube a la primera
// tarjeta; en la primera fila, Atrás no se intercepta y la app se cierra como cualquier app de
// Google TV. Arriba a la derecha, el botón para cambiar de idioma.
package pe.inkaterra.tv.pantallas.inicio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.sistema.idioma.Idioma

// El área destacada cambia cuando el foco lleva este tiempo quieto, para que no parpadee al
// recorrer una fila rápido.
private const val DEMORA_DESTACADO_MS = 500L

@Composable
fun InicioPantalla(
    estado: EstadoInicio,
    miniaturas: Miniaturas,
    foco: Foco,
    otroIdioma: Idioma,
    alEnfocar: (Foco) -> Unit,
    alCambiarIdioma: () -> Unit,
    alElegir: (TipoFila, Elemento) -> Unit,
) {
    var enBotonIdioma by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(ColoresInkaterra.fondo)) {
        if (estado !is EstadoInicio.Listo) return@Box
        if (estado.filas.isEmpty()) {
            Text(
                text = stringResource(R.string.inicio_vacio),
                style = MaterialTheme.typography.headlineMedium,
                color = ColoresInkaterra.crema,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            InicioConFilas(estado.filas, miniaturas, foco, fueraDeLasFilas = enBotonIdioma, alEnfocar, alElegir)
        }
        // Arriba a la derecha, sobre el área destacada; se llega con Arriba desde la primera fila.
        Button(
            onClick = alCambiarIdioma,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .onFocusChanged { enBotonIdioma = it.isFocused }
                    .testTag("cambiar-idioma"),
        ) {
            Text(stringResource(if (otroIdioma == Idioma.INGLES) R.string.idioma_ingles else R.string.idioma_espanol))
        }
    }
}

@Composable
private fun InicioConFilas(
    filas: List<Fila>,
    miniaturas: Miniaturas,
    foco: Foco,
    fueraDeLasFilas: Boolean,
    alEnfocar: (Foco) -> Unit,
    alElegir: (TipoFila, Elemento) -> Unit,
) {
    // La tarjeta con el foco vive en el modelo: al volver de lo que se abrió, el foco va a lo
    // último que se vio, que puede no ser la tarjeta que se eligió.
    val (fila, columna) = foco
    val enfocado = filas.getOrNull(fila)?.elementos?.getOrNull(columna) ?: filas.first().elementos.first()
    var destacado by remember { mutableStateOf(enfocado) }
    LaunchedEffect(enfocado) {
        delay(DEMORA_DESTACADO_MS)
        destacado = enfocado
    }

    val listaFilas = rememberLazyListState()
    // El desplazamiento de cada fila se guarda dentro de la fila (sobrevive a lo que se abre) y se
    // registra aquí, como los FocusRequester, para poder desplazarla al devolver el foco.
    val estadosFilas = remember { HashMap<Int, LazyListState>() }
    val solicitantes = remember { HashMap<Pair<Int, Int>, FocusRequester>() }
    val alcance = rememberCoroutineScope()

    // Hay que desplazar las listas para que la fila y la tarjeta existan antes de pedirle el foco.
    suspend fun enfocar(
        f: Int,
        c: Int,
    ) {
        listaFilas.scrollToItem(f)
        withFrameNanos { }
        estadosFilas[f]?.scrollToItem(c)
        withFrameNanos { }
        solicitantes[f to c]?.requestFocus()
    }
    LaunchedEffect(Unit) { enfocar(fila.coerceIn(filas.indices), columna) }
    BackHandler(enabled = fila > 0 || fueraDeLasFilas) { alcance.launch { enfocar(0, 0) } }

    Column(Modifier.fillMaxSize()) {
        AreaDestacada(destacado, miniaturas, Modifier.fillMaxWidth().weight(1f))
        LazyColumn(
            state = listaFilas,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            itemsIndexed(filas, key = { _, it -> it.tipo }) { f, datos ->
                val estadoFila = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                DisposableEffect(f, estadoFila) {
                    estadosFilas[f] = estadoFila
                    onDispose { estadosFilas.remove(f, estadoFila) }
                }
                Column {
                    Text(
                        text = stringResource(titulo(datos.tipo)),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColoresInkaterra.crema,
                        modifier = Modifier.padding(start = 48.dp, top = 8.dp).testTag("fila-${datos.tipo.name}"),
                    )
                    LazyRow(
                        state = estadoFila,
                        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        itemsIndexed(datos.elementos, key = { _, it -> it.archivo.ruta }) { c, elemento ->
                            val solicitante = remember { FocusRequester() }
                            DisposableEffect(f, c) {
                                solicitantes[f to c] = solicitante
                                onDispose { solicitantes.remove(f to c, solicitante) }
                            }
                            TarjetaContenido(
                                elemento = elemento,
                                miniaturas = miniaturas,
                                alElegir = { alElegir(datos.tipo, elemento) },
                                modifier =
                                    Modifier
                                        .focusRequester(solicitante)
                                        .onFocusChanged {
                                            if (it.isFocused) alEnfocar(Foco(f, c))
                                        }.testTag("tarjeta-${datos.tipo.name}-$c"),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun titulo(tipo: TipoFila) =
    when (tipo) {
        TipoFila.DESTACADOS -> R.string.fila_destacados
        TipoFila.VIDEOS -> R.string.fila_videos
        TipoFila.FOTOS -> R.string.fila_fotos
        TipoFila.MUSICA -> R.string.fila_musica
    }
