// Miniaturas de la USB del huésped (D-10, D-22): se generan como las del hotel, pero quedan solo en
// memoria, hasta 48 MB, y se borran al retirar la memoria. Si algo falla, devuelve null y la
// tarjeta se queda con su recuadro verde.
package pe.inkaterra.tv.datos.miniaturas

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import pe.inkaterra.tv.datos.biblioteca.Elemento

class MiniaturasEnMemoria(
    private val generador: GeneradorMiniatura,
    private val despachador: CoroutineDispatcher = Dispatchers.IO,
) : FuenteMiniaturas {
    // De a dos a la vez, como las del hotel, para no agotar los decodificadores de la TV.
    private val permisos = Semaphore(A_LA_VEZ)

    private val cache =
        object : LruCache<String, Bitmap>(BYTES_MAXIMOS) {
            override fun sizeOf(
                clave: String,
                imagen: Bitmap,
            ) = imagen.byteCount
        }

    override suspend fun de(elemento: Elemento): Bitmap? =
        withContext(despachador) {
            val a = elemento.archivo
            val clave = "${a.ruta}|${a.tamanoBytes}|${a.modificadoMs}"
            cache.get(clave) ?: permisos.withPermit {
                // Otra llamada pudo generarla mientras esta esperaba.
                cache.get(clave) ?: generar(elemento)?.also { cache.put(clave, it) }
            }
        }

    fun limpiar() = cache.evictAll()

    private fun generar(elemento: Elemento): Bitmap? =
        try {
            generador.generar(elemento)
        } catch (e: Exception) {
            // Solo la clase del error: el mensaje trae la ruta del archivo del huésped (D-22).
            Log.w(ETIQUETA, "Sin miniatura en memoria: ${e.javaClass.simpleName}")
            null
        }

    private companion object {
        const val A_LA_VEZ = 2
        const val BYTES_MAXIMOS = 48 * 1024 * 1024
        const val ETIQUETA = "Miniaturas"
    }
}
