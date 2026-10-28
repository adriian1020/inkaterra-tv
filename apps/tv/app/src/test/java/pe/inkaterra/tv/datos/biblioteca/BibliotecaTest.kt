// La biblioteca separa la carpeta del hotel por tipo, aparta los destacados y rechaza lo que no
// cumple las reglas de contenido sin que un archivo malo corte la carga.
package pe.inkaterra.tv.datos.biblioteca

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import pe.inkaterra.tv.dominio.MetadatosVideo
import pe.inkaterra.tv.dominio.MotivoRechazo
import pe.inkaterra.tv.dominio.TipoContenido
import java.io.File

class BibliotecaTest {
    @get:Rule
    val temporal = TemporaryFolder()

    private fun biblioteca(
        archivos: List<ArchivoLocal>,
        lector: LectorMetadatos = LectorFalso(emptyMap()),
    ): ContenidoBiblioteca {
        lateinit var contenido: ContenidoBiblioteca
        runTest { contenido = Biblioteca(FuenteFalsa(archivos), lector, Dispatchers.Unconfined).cargar() }
        return contenido
    }

    @Test
    fun separaPorTipo() {
        val c =
            biblioteca(
                listOf(
                    archivo("Atardecer en Machu Picchu.mp4"),
                    archivo("Río Madre de Dios.mp4"),
                    archivo("Reserva Amazónica 01.jpg"),
                    archivo("Selva.mp3"),
                ),
            )
        assertEquals(2, c.videos.size)
        assertEquals(1, c.fotos.size)
        assertEquals(1, c.musica.size)
        assertTrue(c.destacados.isEmpty())
        assertEquals(TipoContenido.VIDEO, c.videos.first().tipo)
    }

    @Test
    fun promocionesVanADestacados() {
        val c =
            biblioteca(
                listOf(
                    archivo("Oferta spa.jpg", esPromocion = true),
                    archivo("Cena en la selva.mp4", esPromocion = true),
                    archivo("Jingle.mp3", esPromocion = true),
                ),
            )
        assertEquals(listOf("Cena en la selva", "Oferta spa"), c.destacados.map { it.titulo })
        assertEquals(listOf(TipoContenido.VIDEO, TipoContenido.FOTO), c.destacados.map { it.tipo })
        assertTrue(c.videos.isEmpty() && c.fotos.isEmpty() && c.musica.isEmpty())
        assertTrue(c.rechazados.isEmpty())
    }

    @Test
    fun rechazaVideoOchoK() {
        val ochoK = archivo("Ocho K.mp4")
        val promo = archivo("Promo ocho K.mp4", esPromocion = true)
        val enorme = MetadatosVideo(7680, 4320, "video/hevc", 50_000_000)
        val c = biblioteca(listOf(ochoK, promo), LectorFalso(mapOf(ochoK.ruta to enorme, promo.ruta to enorme)))
        assertTrue(c.videos.isEmpty() && c.destacados.isEmpty())
        assertEquals(
            listOf(Rechazado(ochoK, MotivoRechazo.ResolucionExcedida(7680, 4320))),
            c.rechazados.filter { it.archivo == ochoK },
        )
        assertEquals(2, c.rechazados.size)
    }

    @Test
    fun archivoIlegibleNoRompeLaCarga() {
        val danado = archivo("Dañado.mp4")
        val bueno = archivo("Bueno.mp4")
        val c = biblioteca(listOf(danado, bueno), LectorFalso(emptyMap(), danados = setOf(danado.ruta)))
        assertEquals(listOf("Bueno"), c.videos.map { it.titulo })
        assertEquals(listOf(Rechazado(danado, MotivoRechazo.ArchivoIlegible)), c.rechazados)
    }

    @Test
    fun ignoraLoQueNoEsContenido() {
        val c = biblioteca(listOf(archivo("notas.pdf")))
        assertEquals(ContenidoBiblioteca.VACIO, c)
    }

    @Test
    fun ordenaComoEnEspanol() {
        val c = biblioteca(listOf(archivo("Zorro.jpg"), archivo("Árbol.jpg"), archivo("Cóndor.jpg")))
        assertEquals(listOf("Árbol", "Cóndor", "Zorro"), c.fotos.map { it.titulo })
    }

    @Test
    fun carpetaInexistenteDaBibliotecaVacia() {
        val fuente = CarpetaContenido(File(temporal.root, "no-existe"))
        lateinit var c: ContenidoBiblioteca
        runTest { c = Biblioteca(fuente, LectorFalso(emptyMap()), Dispatchers.Unconfined).cargar() }
        assertEquals(ContenidoBiblioteca.VACIO, c)
    }

    @Test
    fun carpetaMarcaPromociones() {
        val oferta = File(temporal.newFolder("promociones"), "Oferta spa.jpg").apply { writeText("x") }
        val foto = File(temporal.newFolder("fotos"), "Reserva Amazónica 01.jpg").apply { writeText("x") }
        oferta.setLastModified(1_700_000_000_000)
        val archivos = CarpetaContenido(temporal.root).listar().associateBy { it.nombre }
        assertTrue(archivos.getValue("Oferta spa.jpg").esPromocion)
        assertFalse(archivos.getValue("Reserva Amazónica 01.jpg").esPromocion)
        assertEquals(1_700_000_000_000, archivos.getValue("Oferta spa.jpg").modificadoMs)
        assertEquals(foto.absolutePath, archivos.getValue("Reserva Amazónica 01.jpg").ruta)
        assertEquals(1L, archivos.getValue("Oferta spa.jpg").tamanoBytes)
    }
}
