// El modelo del inicio recuerda qué tarjeta tenía el foco, para devolverlo a lo último que se vio,
// y lo mantiene en su tarjeta cuando la fila de la USB aparece o se va (D-22).
package pe.inkaterra.tv.pantallas.inicio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.pantallas.bibliotecaDePrueba
import pe.inkaterra.tv.pantallas.usbDePrueba
import pe.inkaterra.tv.sistema.usb.VolumenesFalsos
import java.io.File

@RunWith(AndroidJUnit4::class)
class InicioModeloTest {
    @get:Rule
    val temporal = TemporaryFolder()

    private val volumenes = VolumenesFalsos()
    private val modelo by lazy { InicioModelo(bibliotecaDePrueba(), usbDePrueba(volumenes)) }

    private fun filas() = (modelo.estado.value as EstadoInicio.Listo).filas

    private fun elemento(
        tipo: TipoFila,
        titulo: String,
    ): Elemento = filas().first { it.tipo == tipo }.elementos.first { it.titulo == titulo }

    private fun conectar() {
        File(temporal.root, "DCIM/foto.jpg").apply { parentFile?.mkdirs() }.writeText("x")
        volumenes.conectar(temporal.root)
    }

    @Test
    fun enfocarBuscaLaTarjetaPorSuRuta() {
        modelo.enfocar(TipoFila.VIDEOS, elemento(TipoFila.VIDEOS, "Río Madre de Dios"))
        assertEquals(Foco(1, 1), modelo.foco)
    }

    @Test
    fun alConectarLaUsbElFocoSigueEnSuTarjeta() {
        modelo.enfocar(TipoFila.VIDEOS, elemento(TipoFila.VIDEOS, "Río Madre de Dios"))
        conectar()
        assertEquals(Foco(2, 1), modelo.foco)
    }

    @Test
    fun alRetirarLaUsbElFocoVaALaPrimera() {
        conectar()
        modelo.enfocar(TipoFila.USB, elemento(TipoFila.USB, "foto"))
        assertEquals(Foco(1, 0), modelo.foco)
        volumenes.retirar()
        assertEquals(Foco(0, 0), modelo.foco)
    }
}
