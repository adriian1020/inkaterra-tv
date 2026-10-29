// El modelo del inicio recuerda qué tarjeta tenía el foco, para devolverlo a lo último que se vio.
package pe.inkaterra.tv.pantallas.inicio

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import pe.inkaterra.tv.pantallas.bibliotecaDePrueba

@RunWith(AndroidJUnit4::class)
class InicioModeloTest {
    @Test
    fun enfocarBuscaLaTarjetaPorSuRuta() {
        val modelo = InicioModelo(bibliotecaDePrueba())
        val filas = (modelo.estado.value as EstadoInicio.Listo).filas
        val rio = filas.first { it.tipo == TipoFila.VIDEOS }.elementos.first { it.titulo == "Río Madre de Dios" }
        modelo.enfocar(TipoFila.VIDEOS, rio)
        assertEquals(Foco(1, 1), modelo.foco)
    }
}
