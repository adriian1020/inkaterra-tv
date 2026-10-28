// Arma las piezas de la app a mano, sin Hilt: la biblioteca del hotel y sus miniaturas.
package pe.inkaterra.tv.app

import android.content.Context
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.CarpetaContenido
import pe.inkaterra.tv.datos.biblioteca.LectorMetadatosAndroid
import pe.inkaterra.tv.datos.miniaturas.GeneradorMiniaturaAndroid
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import java.io.File

class Contenedor(
    context: Context,
    // Si el almacenamiento externo no está disponible, una carpeta vacía en vez de cerrar la app.
    raiz: File = context.getExternalFilesDir("contenido") ?: File(context.filesDir, "contenido"),
) {
    val biblioteca = Biblioteca(CarpetaContenido(raiz), LectorMetadatosAndroid())
    val miniaturas = Miniaturas(GeneradorMiniaturaAndroid(), File(context.cacheDir, "miniaturas"))
}
