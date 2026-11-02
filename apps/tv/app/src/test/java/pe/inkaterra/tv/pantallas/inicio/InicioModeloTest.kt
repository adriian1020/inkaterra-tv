// El modelo del inicio recuerda qué tarjeta tenía el foco, para devolverlo a lo último que se vio,
// y pasa a la sección «Tu USB» al conectar la memoria si el inicio está a la vista (D-23).
package pe.inkaterra.tv.pantallas.inicio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun alConectarConElInicioVisiblePasaAUsb() {
        modelo.inicioVisible = true
        conectar()
        assertEquals(Seccion.USB, modelo.seccion)
    }

    // Si el huésped está viendo algo, no se lo interrumpe: la sección no cambia y la barra marca la memoria.
    @Test
    fun alConectarViendoAlgoSoloMarca() {
        modelo.inicioVisible = false
        conectar()
        assertEquals(Seccion.INICIO, modelo.seccion)
        assertTrue(modelo.hayMemoria())
    }

    @Test
    fun alRetirarVuelveAlInicio() {
        modelo.inicioVisible = true
        conectar()
        assertEquals(Seccion.USB, modelo.seccion)
        volumenes.retirar()
        assertEquals(Seccion.INICIO, modelo.seccion)
    }
}
