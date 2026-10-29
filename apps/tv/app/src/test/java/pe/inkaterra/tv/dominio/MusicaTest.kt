// Los modos de repetir de la música (D-22): el botón pasa por los tres y vuelve al primero.
package pe.inkaterra.tv.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicaTest {
    @Test
    fun repetirRecorreLosTresModos() {
        assertEquals(Repetir.UNA, Repetir.LISTA.siguiente())
        assertEquals(Repetir.NO, Repetir.UNA.siguiente())
        assertEquals(Repetir.LISTA, Repetir.NO.siguiente())
    }
}
