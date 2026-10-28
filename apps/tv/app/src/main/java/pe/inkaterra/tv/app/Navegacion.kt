// Las pantallas de la app y cómo se pasa de una a otra. Atrás vuelve a la anterior. Si nunca se
// eligió el idioma, se empieza por ahí.
package pe.inkaterra.tv.app

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.pantallas.abrir.AbrirPantalla
import pe.inkaterra.tv.pantallas.idioma.IdiomaPantalla
import pe.inkaterra.tv.pantallas.inicio.InicioModelo
import pe.inkaterra.tv.pantallas.inicio.InicioPantalla
import pe.inkaterra.tv.sistema.idioma.AjusteIdioma
import pe.inkaterra.tv.sistema.idioma.Idioma
import java.io.File

@Composable
fun Navegacion(
    biblioteca: Biblioteca,
    miniaturas: Miniaturas,
    idioma: AjusteIdioma,
) {
    val navegador = rememberNavController()
    val inicial = if (idioma.actual() == null) "idioma" else "inicio"
    NavHost(navegador, startDestination = inicial) {
        composable("idioma") {
            IdiomaPantalla { elegido ->
                idioma.cambiar(elegido)
                navegador.navigate("inicio") { popUpTo("idioma") { inclusive = true } }
            }
        }
        composable("inicio") {
            val modelo = viewModel { InicioModelo(biblioteca) }
            val estado by modelo.estado.collectAsState()
            // El botón ofrece el otro idioma.
            val otro = if (idioma.actual() == Idioma.INGLES) Idioma.ESPANOL else Idioma.INGLES
            InicioPantalla(
                estado = estado,
                miniaturas = miniaturas,
                otroIdioma = otro,
                alCambiarIdioma = { idioma.cambiar(otro) },
                alElegir = { navegador.navigate("abrir/${Uri.encode(it.archivo.ruta)}") },
            )
        }
        composable("abrir/{ruta}") { entrada ->
            AbrirPantalla(File(entrada.arguments?.getString("ruta").orEmpty()).nameWithoutExtension)
        }
    }
}
