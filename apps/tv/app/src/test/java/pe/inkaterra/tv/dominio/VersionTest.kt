// Formato de la versión que se muestra en el inicio.
package pe.inkaterra.tv.dominio

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionTest {
    @Test
    fun formateaNombreYCodigo() {
        assertEquals("v0.1.0 (1)", textoVersion("0.1.0", 1))
    }
}
