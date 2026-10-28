// Las pantallas de la app y cómo se pasa de una a otra. Atrás vuelve a la anterior.
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
import pe.inkaterra.tv.pantallas.inicio.InicioModelo
import pe.inkaterra.tv.pantallas.inicio.InicioPantalla
import java.io.File

@Composable
fun Navegacion(
    biblioteca: Biblioteca,
    miniaturas: Miniaturas,
) {
    val navegador = rememberNavController()
    NavHost(navegador, startDestination = "inicio") {
        composable("inicio") {
            val modelo = viewModel { InicioModelo(biblioteca) }
            val estado by modelo.estado.collectAsState()
            InicioPantalla(estado, miniaturas) { navegador.navigate("abrir/${Uri.encode(it.archivo.ruta)}") }
        }
        composable("abrir/{ruta}") { entrada ->
            AbrirPantalla(File(entrada.arguments?.getString("ruta").orEmpty()).nameWithoutExtension)
        }
    }
}
