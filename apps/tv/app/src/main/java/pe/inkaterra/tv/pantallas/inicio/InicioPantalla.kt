// Inicio estilo Netflix (D-20): arriba el área destacada con lo que tiene el foco, abajo las filas
// de tarjetas. El foco arranca en la primera tarjeta, vuelve a la tarjeta de origen al regresar
// de lo que se abrió, y Atrás fuera de la primera fila sube a la primera tarjeta; en la primera
// fila, Atrás no se intercepta y la app se cierra como cualquier app de Google TV. Arriba a la
// derecha, el botón para cambiar de idioma.
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
import androidx.compose.runtime.mutableIntStateOf
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
    otroIdioma: Idioma,
    alCambiarIdioma: () -> Unit,
    alElegir: (Elemento) -> Unit,
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
            InicioConFilas(estado.filas, miniaturas, fueraDeLasFilas = enBotonIdioma, alElegir)
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
    fueraDeLasFilas: Boolean,
    alElegir: (Elemento) -> Unit,
) {
    // Tarjeta con el foco: se guarda para devolverle el foco al volver de lo que se abrió.
    var fila by rememberSaveable { mutableIntStateOf(0) }
    var columna by rememberSaveable { mutableIntStateOf(0) }
    val enfocado = filas.getOrNull(fila)?.elementos?.getOrNull(columna) ?: filas.first().elementos.first()
    var destacado by remember { mutableStateOf(enfocado) }
    LaunchedEffect(enfocado) {
        delay(DEMORA_DESTACADO_MS)
        destacado = enfocado
    }

    val listaFilas = rememberLazyListState()
    val estadosFilas = remember(filas.size) { List(filas.size) { LazyListState() } }
    val solicitantes = remember { HashMap<Pair<Int, Int>, FocusRequester>() }
    val alcance = rememberCoroutineScope()

    // Hay que desplazar las listas para que la tarjeta exista antes de pedirle el foco.
    suspend fun enfocar(
        f: Int,
        c: Int,
    ) {
        listaFilas.scrollToItem(f)
        estadosFilas[f].scrollToItem(c)
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
                Column {
                    Text(
                        text = stringResource(titulo(datos.tipo)),
                        style = MaterialTheme.typography.titleMedium,
                        color = ColoresInkaterra.crema,
                        modifier = Modifier.padding(start = 48.dp, top = 8.dp).testTag("fila-${datos.tipo.name}"),
                    )
                    LazyRow(
                        state = estadosFilas[f],
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
                                alElegir = { alElegir(elemento) },
                                modifier =
                                    Modifier
                                        .focusRequester(solicitante)
                                        .onFocusChanged {
                                            if (it.isFocused) {
                                                fila = f
                                                columna = c
                                            }
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
