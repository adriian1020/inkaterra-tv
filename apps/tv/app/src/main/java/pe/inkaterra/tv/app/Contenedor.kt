// Arma las piezas de la app a mano, sin Hilt: la biblioteca del hotel, sus miniaturas, dónde
// retomar cada video y el idioma.
package pe.inkaterra.tv.app

import android.content.Context
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.CarpetaContenido
import pe.inkaterra.tv.datos.biblioteca.LectorMetadatosAndroid
import pe.inkaterra.tv.datos.miniaturas.GeneradorMiniaturaAndroid
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.dominio.PosicionesVideo
import pe.inkaterra.tv.sistema.idioma.AjusteIdioma
import pe.inkaterra.tv.sistema.idioma.AjusteIdiomaAndroid
import java.io.File

class Contenedor(
    context: Context,
    // Si el almacenamiento externo no está disponible, una carpeta vacía en vez de cerrar la app.
    raiz: File = context.getExternalFilesDir("contenido") ?: File(context.filesDir, "contenido"),
) {
    val biblioteca = Biblioteca(CarpetaContenido(raiz), LectorMetadatosAndroid())
    val miniaturas = miniaturasDelProceso(context)
    val posiciones: PosicionesVideo = posicionesDelProceso
    val idioma: AjusteIdioma = AjusteIdiomaAndroid()

    private companion object {
        // Las posiciones viven mientras viva el proceso (D-21), aunque la actividad se recree.
        val posicionesDelProceso = PosicionesVideo()

        // Una sola por proceso: la actividad se recrea (al cambiar el idioma) y una instancia nueva
        // traería su propio límite de dos, sumando decodificaciones 4K a las que siguen en curso.
        @Volatile private var miniaturas: Miniaturas? = null

        fun miniaturasDelProceso(context: Context): Miniaturas =
            miniaturas ?: synchronized(this) {
                miniaturas ?: Miniaturas(GeneradorMiniaturaAndroid(), File(context.cacheDir, "miniaturas"))
                    .also { miniaturas = it }
            }
    }
}
