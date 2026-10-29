// Saca la imagen de una tarjeta del inicio (D-20): un cuadro del video, la foto reducida o la
// carátula de la canción, para cubrir 1280 × 720 (o el tamaño pedido) sin deformar.
package pe.inkaterra.tv.datos.miniaturas

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.dominio.TipoContenido
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.roundToInt

interface GeneradorMiniatura {
    // null si el archivo no trae imagen (una canción sin carátula); lanza si está dañado.
    fun generar(elemento: Elemento): Bitmap?
}

const val ANCHO_MINIATURA = 1280
const val ALTO_MINIATURA = 720

// La USB usa un tamaño más chico, porque sus miniaturas viven en memoria (D-22).
class GeneradorMiniaturaAndroid(
    private val ancho: Int = ANCHO_MINIATURA,
    private val alto: Int = ALTO_MINIATURA,
) : GeneradorMiniatura {
    override fun generar(elemento: Elemento): Bitmap? {
        val ruta = elemento.archivo.ruta
        return when (elemento.tipo) {
            TipoContenido.VIDEO -> conLector(ruta, ::cuadroDeVideo)
            TipoContenido.FOTO -> decodificar(ImageDecoder.createSource(File(ruta)))
            TipoContenido.MUSICA ->
                conLector(ruta) { lector ->
                    lector.embeddedPicture?.let { decodificar(ImageDecoder.createSource(ByteBuffer.wrap(it))) }
                }
        }
    }

    // Al 10 % de la duración, para no tomar el negro del primer cuadro.
    private fun cuadroDeVideo(lector: MediaMetadataRetriever): Bitmap? {
        val duracionMs = lector.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
        return lector.getScaledFrameAtTime(
            duracionMs * 1000 / 10,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            ancho,
            alto,
        )
    }

    private fun <T> conLector(
        ruta: String,
        leer: (MediaMetadataRetriever) -> T,
    ): T {
        val lector = MediaMetadataRetriever()
        try {
            lector.setDataSource(ruta)
            return leer(lector)
        } finally {
            lector.release()
        }
    }

    // Se reduce lo justo para cubrir el tamaño pedido (nunca se agranda); el recorte lo hace la tarjeta.
    // En memoria normal y no de hardware, para poder guardarla en JPEG.
    private fun decodificar(fuente: ImageDecoder.Source): Bitmap =
        ImageDecoder.decodeBitmap(fuente) { decodificador, info, _ ->
            val escala =
                max(
                    ancho / info.size.width.toFloat(),
                    alto / info.size.height.toFloat(),
                ).coerceAtMost(1f)
            decodificador.setTargetSize(
                (info.size.width * escala).roundToInt(),
                (info.size.height * escala).roundToInt(),
            )
            decodificador.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
}
