// Qué video sigue al terminar uno: el próximo de la misma fila, saltando las fotos (D-21). El botón
// «anterior» va al de antes, también saltando las fotos (D-23).
package pe.inkaterra.tv.pantallas.video

import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.dominio.TipoContenido

// El índice del próximo video después de actual, o null si era el último.
fun siguienteVideo(
    fila: List<Elemento>,
    actual: Int,
): Int? = (actual + 1 until fila.size).firstOrNull { fila[it].tipo == TipoContenido.VIDEO }

// El índice del video anterior a actual, o null si era el primero.
fun anteriorVideo(
    fila: List<Elemento>,
    actual: Int,
): Int? = (actual - 1 downTo 0).firstOrNull { fila[it].tipo == TipoContenido.VIDEO }
