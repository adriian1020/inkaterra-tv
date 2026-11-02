// Inicio estilo Netflix (D-20) con barra lateral (D-23): a la izquierda la barra (Inicio, Tu USB e
// Idioma); al lado, la sección elegida, con el área destacada arriba y las filas de tarjetas abajo,
// o un aviso si la memoria no tiene nada que mostrar. El foco arranca en la primera tarjeta y, al
// regresar de lo que se abrió, vuelve a la tarjeta de lo último que se vio (D-21). Izquierda desde
// la primera tarjeta de una fila abre la barra y Derecha vuelve. Atrás fuera de la primera fila sube
// a la primera tarjeta; en la primera fila va a la barra; en la barra no se intercepta y la app se
// cierra como cualquier app de Google TV.
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.inkaterra.tv.R
import pe.inkaterra.tv.app.ColoresInkaterra
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.FuenteMiniaturas
import pe.inkaterra.tv.sistema.idioma.Idioma

// El área destacada cambia cuando el foco lleva este tiempo quieto, para que no parpadee al
// recorrer una fila rápido.
private const val DEMORA_DESTACADO_MS = 500L

@Composable
fun InicioPantalla(
    estado: EstadoInicio,
    miniaturas: FuenteMiniaturas,
    seccion: Seccion,
    // La tarjeta con el foco en la sección actual.
    foco: Foco,
    otroIdioma: Idioma,
    alEnfocar: (Foco) -> Unit,
    alElegirSeccion: (Seccion) -> Unit,
    alCambiarIdioma: () -> Unit,
    alElegir: (TipoFila, Elemento) -> Unit,
) {
    var enBarra by remember { mutableStateOf(false) }
    // Cada aumento vuelve a llevar el foco a la tarjeta guardada: al salir de la barra o al elegir
    // una sección.
    var pedidoFoco by remember { mutableIntStateOf(0) }
    val focoBarra = remember { FocusRequester() }

    Box(Modifier.fillMaxSize().background(ColoresInkaterra.fondo)) {
        if (estado !is EstadoInicio.Listo) return@Box
        val filas = if (seccion == Seccion.USB) estado.usb.filas else estado.filas
        // En la primera fila (o sin tarjetas), Atrás va a la barra; en la barra se deja pasar.
        BackHandler(enabled = !enBarra) { focoBarra.requestFocus() }
        Box(Modifier.fillMaxSize().padding(start = ANCHO_BARRA)) {
            if (filas.isEmpty()) {
                // A la izquierda deja lugar para la barra abierta, que se dibuja encima.
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(start = 200.dp, end = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            stringResource(
                                if (seccion == Seccion.USB) textoAviso(estado.usb.aviso) else R.string.inicio_vacio,
                            ),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ColoresInkaterra.crema,
                        textAlign = TextAlign.Center,
                        modifier = if (seccion == Seccion.USB) Modifier.testTag("aviso-usb") else Modifier,
                    )
                    // Si todo lo de la memoria se rechazó, el huésped sabe por qué no ve sus videos (CU-17 3b).
                    val rechazados = estado.usb.rechazados
                    if (seccion == Seccion.USB && rechazados > 0) {
                        Text(
                            text = pluralStringResource(R.plurals.usb_rechazados, rechazados, rechazados),
                            style = MaterialTheme.typography.titleMedium,
                            color = ColoresInkaterra.crema.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
                // Sin tarjetas, el foco queda en la barra.
                LaunchedEffect(seccion) { focoBarra.requestFocus() }
            } else {
                // Cada sección con su propio estado: sus filas y su desplazamiento.
                key(seccion) {
                    InicioConFilas(
                        filas = filas,
                        miniaturas = miniaturas,
                        foco = foco,
                        nota =
                            estado.usb.rechazados
                                .takeIf { seccion == Seccion.USB && it > 0 }
                                ?.let { pluralStringResource(R.plurals.usb_rechazados, it, it) },
                        enBarra = enBarra,
                        pedidoFoco = pedidoFoco,
                        alEnfocar = alEnfocar,
                        alIrALaBarra = { focoBarra.requestFocus() },
                        alElegir = alElegir,
                    )
                }
            }
        }
        BarraLateral(
            seccion = seccion,
            hayMemoria = estado.usb.aviso != AvisoUsb.SIN_MEMORIA,
            otroIdioma = otroIdioma,
            focoSeccion = focoBarra,
            alCambiarFoco = { enBarra = it },
            alElegirSeccion = {
                alElegirSeccion(it)
                pedidoFoco++
            },
            alCambiarIdioma = alCambiarIdioma,
            alVolverAlContenido = { pedidoFoco++ },
        )
    }
}

private val ANCHO_BARRA = 72.dp

@Composable
private fun InicioConFilas(
    filas: List<Fila>,
    miniaturas: FuenteMiniaturas,
    foco: Foco,
    // Una línea sobre las filas: los archivos de la USB que no se pueden ver.
    nota: String?,
    enBarra: Boolean,
    pedidoFoco: Int,
    alEnfocar: (Foco) -> Unit,
    alIrALaBarra: () -> Unit,
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
    LaunchedEffect(pedidoFoco) {
        val f = fila.coerceIn(filas.indices)
        enfocar(f, columna.coerceIn(filas[f].elementos.indices))
    }
    BackHandler(enabled = fila > 0 && !enBarra) { alcance.launch { enfocar(0, 0) } }

    Column(Modifier.fillMaxSize()) {
        AreaDestacada(destacado, miniaturas, Modifier.fillMaxWidth().weight(1f))
        if (nota != null) {
            Text(
                text = nota,
                style = MaterialTheme.typography.bodyMedium,
                color = ColoresInkaterra.crema.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 48.dp, top = 4.dp),
            )
        }
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
                                        }
                                        // Arriba desde la primera fila no busca la barra (un OK ahí
                                        // cambiaba el idioma sin querer).
                                        .focusProperties { if (f == 0) up = FocusRequester.Cancel }
                                        // Izquierda desde la primera tarjeta abre la barra, en la opción
                                        // de la sección actual.
                                        .onPreviewKeyEvent { evento ->
                                            if (c != 0 ||
                                                evento.key != Key.DirectionLeft
                                            ) {
                                                return@onPreviewKeyEvent false
                                            }
                                            if (evento.type == KeyEventType.KeyDown) alIrALaBarra()
                                            true
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
        TipoFila.VIDEOS, TipoFila.USB_VIDEOS -> R.string.fila_videos
        TipoFila.FOTOS, TipoFila.USB_FOTOS -> R.string.fila_fotos
        TipoFila.MUSICA, TipoFila.USB_MUSICA -> R.string.fila_musica
    }

private fun textoAviso(aviso: AvisoUsb?) =
    when (aviso) {
        AvisoUsb.LEYENDO -> R.string.usb_leyendo
        AvisoUsb.VACIA -> R.string.usb_vacia
        AvisoUsb.NO_SE_LEE -> R.string.usb_no_se_lee
        AvisoUsb.SIN_MEMORIA, null -> R.string.usb_conecta
    }
