// Miniaturas del inicio guardadas en la caché de la app: se generan una vez por archivo (la clave
// cambia si el archivo cambia) y de a dos a la vez, para no agotar los decodificadores de la TV.
// Si algo falla, devuelve null y la tarjeta se queda con su recuadro verde.
package pe.inkaterra.tv.datos.miniaturas

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import pe.inkaterra.tv.datos.biblioteca.ArchivoLocal
import pe.inkaterra.tv.datos.biblioteca.Elemento
import java.io.File
import java.security.MessageDigest

class Miniaturas(
    private val generador: GeneradorMiniatura,
    private val carpeta: File,
    private val despachador: CoroutineDispatcher = Dispatchers.IO,
) {
    private val permisos = Semaphore(A_LA_VEZ)

    suspend fun de(elemento: Elemento): File? =
        withContext(despachador) {
            val destino = File(carpeta, "${clave(elemento.archivo)}.jpg")
            if (destino.exists()) return@withContext destino
            permisos.withPermit {
                // Otra llamada pudo generarla mientras esta esperaba su turno.
                if (destino.exists()) destino else generar(elemento, destino)
            }
        }

    private fun generar(
        elemento: Elemento,
        destino: File,
    ): File? {
        // Se escribe aparte y se renombra al final: una caída a mitad no deja un JPEG roto.
        val temporal = File(carpeta, "${destino.nameWithoutExtension}.tmp")
        return try {
            val imagen = generador.generar(elemento) ?: return null
            carpeta.mkdirs()
            temporal.outputStream().use { imagen.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
            check(temporal.renameTo(destino)) { "No se pudo guardar $destino" }
            destino
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Sin miniatura para ${elemento.archivo.ruta}", e)
            temporal.delete()
            null
        }
    }

    private fun clave(archivo: ArchivoLocal): String =
        MessageDigest
            .getInstance("SHA-1")
            .digest("${archivo.ruta}|${archivo.tamanoBytes}|${archivo.modificadoMs}".toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val A_LA_VEZ = 2
        const val CALIDAD = 85
        const val ETIQUETA = "Miniaturas"
    }
}
