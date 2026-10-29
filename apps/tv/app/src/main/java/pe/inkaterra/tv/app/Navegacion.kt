// Las pantallas de la app y cómo se pasa de una a otra. Atrás vuelve a la anterior. Si nunca se
// eligió el idioma, se empieza por ahí. Un video abre el reproductor y una foto el visor, con los
// videos o las fotos de su fila (en Destacados, solo los de ese tipo); al volver, el foco va a la
// tarjeta de lo último que se vio (D-21). Una canción abre el reproductor de música con la fila
// Música (D-22). Si se retira la USB con una foto o un video de ella abierto, su fila desaparece y
// se vuelve al inicio.
package pe.inkaterra.tv.app

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.miniaturas.FuenteMiniaturas
import pe.inkaterra.tv.datos.usb.UsbDelHuesped
import pe.inkaterra.tv.dominio.PosicionesVideo
import pe.inkaterra.tv.dominio.TipoContenido
import pe.inkaterra.tv.pantallas.fotos.VisorFotosPantalla
import pe.inkaterra.tv.pantallas.idioma.IdiomaPantalla
import pe.inkaterra.tv.pantallas.inicio.EstadoInicio
import pe.inkaterra.tv.pantallas.inicio.InicioModelo
import pe.inkaterra.tv.pantallas.inicio.InicioPantalla
import pe.inkaterra.tv.pantallas.inicio.Seccion
import pe.inkaterra.tv.pantallas.inicio.TipoFila
import pe.inkaterra.tv.pantallas.musica.ReproductorMusicaPantalla
import pe.inkaterra.tv.pantallas.video.ReproductorPantalla
import pe.inkaterra.tv.sistema.idioma.AjusteIdioma
import pe.inkaterra.tv.sistema.idioma.Idioma

@Composable
fun Navegacion(
    biblioteca: Biblioteca,
    miniaturas: FuenteMiniaturas,
    posiciones: PosicionesVideo,
    idioma: AjusteIdioma,
    usb: UsbDelHuesped,
) {
    val navegador = rememberNavController()
    val inicial = if (idioma.actual() == null) "idioma" else "inicio"

    // El modelo del inicio es el de su entrada en la pila: el reproductor y el visor lo comparten.
    @Composable
    fun modeloDelInicio(entrada: NavBackStackEntry) = viewModel(entrada) { InicioModelo(biblioteca, usb) }

    // Sin fundido entre pantallas: durante los 700 ms del fundido, la pantalla que se va seguía
    // recibiendo Enter y Atrás del control.
    NavHost(
        navegador,
        startDestination = inicial,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable("idioma") {
            IdiomaPantalla { elegido ->
                idioma.cambiar(elegido)
                navegador.navigate("inicio") { popUpTo("idioma") { inclusive = true } }
            }
        }
        composable("inicio") { entrada ->
            val modelo = modeloDelInicio(entrada)
            val estado by modelo.estado.collectAsState()
            // La barra ofrece el otro idioma.
            val otro = if (idioma.actual() == Idioma.INGLES) Idioma.ESPANOL else Idioma.INGLES
            // El modelo sabe si el inicio está al frente: al conectar la memoria pasa solo a «Tu USB»
            // solo si el huésped no está viendo algo (D-23).
            DisposableEffect(entrada) {
                val observador =
                    LifecycleEventObserver { _, evento ->
                        when (evento) {
                            Lifecycle.Event.ON_RESUME -> modelo.inicioVisible = true
                            Lifecycle.Event.ON_PAUSE -> modelo.inicioVisible = false
                            else -> Unit
                        }
                    }
                entrada.lifecycle.addObserver(observador)
                onDispose {
                    entrada.lifecycle.removeObserver(observador)
                    modelo.inicioVisible = false
                }
            }
            val enUsb = modelo.seccion == Seccion.USB
            InicioPantalla(
                estado = estado,
                miniaturas = miniaturas,
                seccion = modelo.seccion,
                foco = if (enUsb) modelo.focoUsb else modelo.foco,
                otroIdioma = otro,
                alEnfocar = { if (enUsb) modelo.focoUsb = it else modelo.foco = it },
                alElegirSeccion = { modelo.seccion = it },
                alCambiarIdioma = { idioma.cambiar(otro) },
                // Solo si el inicio es la pantalla activa: un Enter de más no abre dos veces.
                alElegir = { fila, elemento ->
                    if (entrada.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navegador.navigate(rutaDe(estado, fila, elemento))
                    }
                },
            )
        }
        composable("video/{fila}/{indice}") { entrada ->
            DeLaFila(navegador, entrada, TipoContenido.VIDEO, ::modeloDelInicio) { videos, indice, alSalir ->
                ReproductorPantalla(videos, indice, posiciones, alSalir)
            }
        }
        composable("fotos/{fila}/{indice}") { entrada ->
            // La música de fondo son las canciones de la fila Música (D-22), también para la USB.
            val inicio = remember(entrada) { navegador.getBackStackEntry("inicio") }
            val estado by modeloDelInicio(inicio).estado.collectAsState()
            val musica = remember(estado) { deLaFila(estado, TipoFila.MUSICA, TipoContenido.MUSICA) }
            DeLaFila(navegador, entrada, TipoContenido.FOTO, ::modeloDelInicio) { fotos, indice, alSalir ->
                VisorFotosPantalla(fotos, indice, musica, alSalir)
            }
        }
        composable("musica/{fila}/{indice}") { entrada ->
            DeLaFila(navegador, entrada, TipoContenido.MUSICA, ::modeloDelInicio) { canciones, indice, alSalir ->
                ReproductorMusicaPantalla(canciones, indice, miniaturas, alSalir)
            }
        }
    }
}

// Los elementos de la fila que abre una pantalla: los de su tipo, en el orden de la fila.
private fun deLaFila(
    estado: EstadoInicio,
    fila: TipoFila,
    tipo: TipoContenido,
): List<Elemento> =
    (estado as? EstadoInicio.Listo)
        ?.let { it.filas + it.usb.filas }
        ?.firstOrNull { it.tipo == fila }
        ?.elementos
        ?.filter { it.tipo == tipo }
        .orEmpty()

private fun rutaDe(
    estado: EstadoInicio,
    fila: TipoFila,
    elemento: Elemento,
): String {
    val indice = deLaFila(estado, fila, elemento.tipo).indexOf(elemento)
    return when (elemento.tipo) {
        TipoContenido.VIDEO -> "video/${fila.name}/$indice"
        TipoContenido.FOTO -> "fotos/${fila.name}/$indice"
        TipoContenido.MUSICA -> "musica/${fila.name}/$indice"
    }
}

// Arma la pantalla de un video o una foto con los de su fila, que lee del modelo del inicio. Al
// salir deja el foco del inicio en lo último que se vio y vuelve. Si la fila ya no está (la app se
// recreó sin la biblioteca cargada), vuelve directamente.
@Composable
private fun DeLaFila(
    navegador: NavHostController,
    entrada: NavBackStackEntry,
    tipo: TipoContenido,
    modeloDelInicio: @Composable (NavBackStackEntry) -> InicioModelo,
    pantalla: @Composable (List<Elemento>, Int, (Elemento) -> Unit) -> Unit,
) {
    val inicio = remember(entrada) { navegador.getBackStackEntry("inicio") }
    val modelo = modeloDelInicio(inicio)
    val estado by modelo.estado.collectAsState()
    val fila = TipoFila.valueOf(entrada.arguments?.getString("fila").orEmpty())
    val indice = entrada.arguments?.getString("indice")?.toIntOrNull() ?: 0
    val elementos = remember(estado) { deLaFila(estado, fila, tipo) }
    if (indice !in elementos.indices) {
        LaunchedEffect(Unit) { navegador.popBackStack() }
        return
    }
    // Solo una vez: el video puede terminar justo cuando se aprieta Atrás. No se exige que la pantalla
    // esté al frente: la música sale también al pasar a segundo plano (D-22).
    var salio by remember(entrada) { mutableStateOf(false) }
    pantalla(elementos, indice) { elemento ->
        if (!salio && entrada.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            salio = true
            modelo.enfocar(fila, elemento)
            navegador.popBackStack()
        }
    }
}
