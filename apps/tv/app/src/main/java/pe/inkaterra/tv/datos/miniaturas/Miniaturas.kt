// Miniaturas del inicio guardadas en la caché de la app: se generan una vez por archivo (la clave
// cambia si el archivo cambia) y de a dos a la vez, para no agotar los decodificadores de la TV.
// Si algo falla, devuelve null y la tarjeta se queda con su recuadro verde.
package pe.inkaterra.tv.datos.miniaturas

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import pe.inkaterra.tv.datos.biblioteca.ArchivoLocal
import pe.inkaterra.tv.datos.biblioteca.Elemento
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class Miniaturas(
    private val generador: GeneradorMiniatura,
    private val carpeta: File,
    private val despachador: CoroutineDispatcher = Dispatchers.IO,
) {
    private val permisos = Semaphore(A_LA_VEZ)

    // Un candado por archivo: el área destacada y su tarjeta piden la misma miniatura a la vez, y
    // la segunda espera a la primera en vez de decodificar el mismo video otra vez.
    private val candados = ConcurrentHashMap<String, Mutex>()

    suspend fun de(elemento: Elemento): File? =
        withContext(despachador) {
            val clave = clave(elemento.archivo)
            val destino = File(carpeta, "$clave.jpg")
            if (destino.exists()) return@withContext destino
            candados.getOrPut(clave) { Mutex() }.withLock {
                // Otra llamada pudo generarla mientras esta esperaba.
                if (destino.exists()) return@withLock destino
                permisos.withPermit { generar(elemento, destino) }
            }
        }

    private fun generar(
        elemento: Elemento,
        destino: File,
    ): File? {
        // Se escribe aparte y se renombra al final: una caída a mitad no deja un JPEG roto.
        var temporal: File? = null
        return try {
            val imagen = generador.generar(elemento) ?: return null
            carpeta.mkdirs()
            val archivo = File.createTempFile(destino.nameWithoutExtension, ".tmp", carpeta).also { temporal = it }
            archivo.outputStream().use { imagen.compress(Bitmap.CompressFormat.JPEG, CALIDAD, it) }
            check(archivo.renameTo(destino)) { "No se pudo guardar $destino" }
            destino
        } catch (e: Exception) {
            Log.w(ETIQUETA, "Sin miniatura para ${elemento.archivo.ruta}", e)
            temporal?.delete()
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
