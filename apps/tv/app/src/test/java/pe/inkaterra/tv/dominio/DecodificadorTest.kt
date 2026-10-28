// Cuándo un decodificador de la TV sirve para video 4K.
package pe.inkaterra.tv.dominio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodificadorTest {
    private fun d(
        ancho: Int,
        alto: Int,
        hw: Boolean = true,
    ) = Decodificador("c2.prueba.hevc.decoder", "video/hevc", hw, ancho, alto, 100_000_000)

    @Test
    fun cuatroKPorHardware() = assertTrue(soporta4k(d(3840, 2160)))

    @Test
    fun soloFullHdNoAlcanza() = assertFalse(soporta4k(d(1920, 1080)))

    @Test
    fun porSoftwareNoCuenta() = assertFalse(soporta4k(d(3840, 2160, hw = false)))

    // Un decodificador que declara 2160 × 3840 también sirve para video horizontal.
    @Test
    fun verticalTambienCuenta() = assertTrue(soporta4k(d(2160, 3840)))
}
