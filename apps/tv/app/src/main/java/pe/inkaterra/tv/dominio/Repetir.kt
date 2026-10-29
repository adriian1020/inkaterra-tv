// Los modos de repetir de la música (D-22): la lista (por omisión), una canción o sin repetir. El
// botón del reproductor pasa de uno al siguiente y vuelve al primero.
package pe.inkaterra.tv.dominio

enum class Repetir { LISTA, UNA, NO }

fun Repetir.siguiente(): Repetir =
    when (this) {
        Repetir.LISTA -> Repetir.UNA
        Repetir.UNA -> Repetir.NO
        Repetir.NO -> Repetir.LISTA
    }
