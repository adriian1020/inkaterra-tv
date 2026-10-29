// Reglas del reproductor (D-21): dónde retomar, saltos de 10 s y el texto del tiempo.
package pe.inkaterra.tv.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReproduccionTest {
    private val ruta = "/contenido/Atardecer en Machu Picchu.mp4"

    @Test
    fun guardaYRetoma() {
        val posiciones = PosicionesVideo()
        posiciones.guardar(ruta, 60_000, 200_000)
        assertEquals(60_000, posiciones.posicion(ruta))
    }

    @Test
    fun alPrincipioNoSeGuarda() {
        val posiciones = PosicionesVideo()
        posiciones.guardar(ruta, 4_000, 200_000)
        assertEquals(0, posiciones.posicion(ruta))
    }

    @Test
    fun alFinalSeOlvida() {
        val posiciones = PosicionesVideo()
        posiciones.guardar(ruta, 60_000, 200_000)
        posiciones.guardar(ruta, 190_000, 200_000)
        assertEquals(0, posiciones.posicion(ruta))
    }

    @Test
    fun adelantarNoPasaDelFinal() = assertEquals(200_000L, destinoSalto(AccionVideo.ADELANTAR, 195_000, 200_000))

    @Test
    fun retrocederNoBajaDeCero() = assertEquals(0L, destinoSalto(AccionVideo.RETROCEDER, 4_000, 200_000))

    @Test
    fun saltosDeDiezSegundos() {
        assertEquals(60_000L, destinoSalto(AccionVideo.ADELANTAR, 50_000, 200_000))
        assertEquals(40_000L, destinoSalto(AccionVideo.RETROCEDER, 50_000, 200_000))
        assertNull(destinoSalto(AccionVideo.ALTERNAR, 50_000, 200_000))
    }

    @Test
    fun textoDelTiempo() {
        assertEquals("1:05", textoTiempo(65_000))
        assertEquals("1:02:05", textoTiempo(3_725_000))
        assertEquals("0:00", textoTiempo(0))
    }
}
