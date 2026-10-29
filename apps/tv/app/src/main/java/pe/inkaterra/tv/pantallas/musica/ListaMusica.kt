// Lo que conecta las canciones de una fila con Media3: la lista entera, empezando por la elegida,
// y los modos de repetir de la app traducidos a los del reproductor.
package pe.inkaterra.tv.pantallas.musica

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.dominio.Repetir
import java.io.File

fun Repetir.modoMedia3(): Int =
    when (this) {
        Repetir.LISTA -> Player.REPEAT_MODE_ALL
        Repetir.UNA -> Player.REPEAT_MODE_ONE
        Repetir.NO -> Player.REPEAT_MODE_OFF
    }

// La ruta queda como identificador de cada canción, para saber cuál suena.
fun Player.ponerCanciones(
    canciones: List<Elemento>,
    inicial: Int,
) {
    val items =
        canciones.map {
            MediaItem
                .Builder()
                .setUri(Uri.fromFile(File(it.archivo.ruta)))
                .setMediaId(it.archivo.ruta)
                .build()
        }
    setMediaItems(items, inicial, 0)
}
