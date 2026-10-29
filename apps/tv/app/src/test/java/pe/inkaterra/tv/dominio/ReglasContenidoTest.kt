// Qué archivos acepta la TV: tipo por extensión, máximo 4K, códecs y tasa (D-13, D-19).
package pe.inkaterra.tv.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReglasContenidoTest {
    private fun v(
        ancho: Int,
        alto: Int,
        mime: String? = "video/hevc",
        tasa: Int? = 20_000_000,
    ) = MetadatosVideo(ancho, alto, mime, tasa)

    @Test
    fun clasificaPorExtension() {
        assertEquals(TipoContenido.VIDEO, clasificar("Atardecer en Machu Picchu.mp4"))
        assertEquals(TipoContenido.FOTO, clasificar("Reserva Amazónica 01.heic"))
        assertEquals(TipoContenido.MUSICA, clasificar("Selva.flac"))
        assertEquals(TipoContenido.VIDEO, clasificar("Celular.webm"))
        assertNull(clasificar("documento.pdf"))
    }

    @Test
    fun extensionEnMayusculas() = assertEquals(TipoContenido.VIDEO, clasificar("VIDEO.MP4"))

    @Test
    fun sinExtensionSeIgnora() = assertNull(clasificar("LEEME"))

    @Test
    fun aceptaCuatroK() = assertNull(evaluarVideo(v(3840, 2160)))

    @Test
    fun aceptaCuatroKVertical() = assertNull(evaluarVideo(v(2160, 3840)))

    @Test
    fun rechazaOchoK() = assertEquals(MotivoRechazo.ResolucionExcedida(7680, 4320), evaluarVideo(v(7680, 4320)))

    @Test
    fun rechazaCuatroKDeCine() = assertEquals(MotivoRechazo.ResolucionExcedida(4096, 2160), evaluarVideo(v(4096, 2160)))

    @Test
    fun rechazaCodecNoSoportado() =
        assertEquals(MotivoRechazo.CodecNoSoportado("video/av01"), evaluarVideo(v(1920, 1080, "video/av01")))

    @Test
    fun aceptaH264() = assertNull(evaluarVideo(v(1920, 1080, "video/avc")))

    @Test
    fun aceptaVp9() = assertNull(evaluarVideo(v(3840, 2160, "video/x-vnd.on2.vp9")))

    @Test
    fun rechazaTasaExcedida() =
        assertEquals(
            MotivoRechazo.TasaExcedida(TASA_MAXIMA_BPS + 1),
            evaluarVideo(v(3840, 2160, tasa = TASA_MAXIMA_BPS + 1)),
        )

    // Un video exportado Â«a 60 MbpsÂ» mide un poco más (hevc-60m.mp4 da 60,6): entra con el margen.
    @Test
    fun aceptaSesentaMbpsConMargen() = assertNull(evaluarVideo(v(3840, 2160, tasa = 60_624_957)))

    @Test
    fun rechazaMasDeSesentaYDos() =
        assertEquals(MotivoRechazo.TasaExcedida(62_000_001), evaluarVideo(v(3840, 2160, tasa = 62_000_001)))

    @Test
    fun tasaDesconocidaSeAcepta() = assertNull(evaluarVideo(v(3840, 2160, tasa = null)))

    @Test
    fun rechazaSinDecodificador() =
        assertEquals(
            MotivoRechazo.SinDecodificador("video/avc"),
            evaluarVideo(MetadatosVideo(1920, 1080, "video/avc", 8_000_000, decodificable = false)),
        )

    @Test
    fun primeroLaResolucion() =
        assertEquals(
            MotivoRechazo.ResolucionExcedida(7680, 4320),
            evaluarVideo(MetadatosVideo(7680, 4320, "video/avc", null, decodificable = false)),
        )
}
