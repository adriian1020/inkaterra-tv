// La memoria del huésped (D-22): se leen sus fotos y videos de cualquier carpeta, sin lo que dejan
// Mac, Windows y Android, que no son fotos del huésped.
package pe.inkaterra.tv.datos.biblioteca

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class CarpetaUsbTest {
    @get:Rule
    val temporal = TemporaryFolder()

    private fun crear(ruta: String) =
        File(temporal.root, ruta).apply {
            parentFile?.mkdirs()
            writeText("x")
        }

    @Test
    fun ignoraCarpetasDelSistemaYArchivosOcultos() {
        listOf(
            "DCIM/foto.jpg",
            "DCIM/._foto.jpg",
            ".Trashes/x.jpg",
            "Android/data/y.jpg",
            "LOST.DIR/z.jpg",
            "System Volume Information/w.jpg",
            "\$RECYCLE.BIN/v.jpg",
        ).forEach(::crear)
        val archivos = CarpetaUsb(temporal.root).listar()
        assertEquals(listOf("foto.jpg"), archivos.map { it.nombre })
        assertTrue(archivos.single().deUsb)
    }

    @Test(expected = IOException::class)
    fun sinPermisoLanza() {
        CarpetaUsb(File(temporal.root, "no-existe")).listar()
    }

    // Los nombres de los archivos del huésped van al registro solo en la versión de prueba (D-22).
    @Test
    fun nombreParaRegistroOcultaLaUsbEnLaVersionDelHotel() {
        val usb =
            ArchivoLocal(
                "/storage/AB12-34CD/DCIM/Mi viaje.mp4",
                "Mi viaje.mp4",
                1,
                0,
                esPromocion = false,
                deUsb = true,
            )
        val hotel = archivo("Río Madre de Dios.mp4")
        assertEquals("Mi viaje.mp4", usb.nombreParaRegistro(depuracion = true))
        assertTrue("Mi viaje" !in usb.nombreParaRegistro(depuracion = false))
        assertEquals("Río Madre de Dios.mp4", hotel.nombreParaRegistro(depuracion = false))
    }
}
