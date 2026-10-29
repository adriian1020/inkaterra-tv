// Lee de un video lo que piden las reglas de contenido: tamaño, códec de la pista de video, si la
// TV tiene un decodificador para ella y tasa.
package pe.inkaterra.tv.datos.biblioteca

import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import pe.inkaterra.tv.dominio.MetadatosVideo

interface LectorMetadatos {
    // Lanza una excepción si el archivo no se puede leer.
    fun video(ruta: String): MetadatosVideo
}

class LectorMetadatosAndroid : LectorMetadatos {
    private val decodificadores by lazy { MediaCodecList(MediaCodecList.REGULAR_CODECS) }

    override fun video(ruta: String): MetadatosVideo {
        val lector = MediaMetadataRetriever()
        try {
            lector.setDataSource(ruta)

            fun entero(clave: Int) = lector.extractMetadata(clave)?.toIntOrNull()
            val pista = pistaDeVideo(ruta)
            return MetadatosVideo(
                ancho = checkNotNull(entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)) { "Sin video: $ruta" },
                alto = checkNotNull(entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)) { "Sin video: $ruta" },
                mime = pista?.getString(MediaFormat.KEY_MIME),
                tasaBps = entero(MediaMetadataRetriever.METADATA_KEY_BITRATE),
                decodificable = pista != null && decodificadores.findDecoderForFormat(pista) != null,
            )
        } finally {
            lector.release()
        }
    }

    // METADATA_KEY_MIMETYPE da el del contenedor (video/mp4); el códec y el perfil están en la
    // pista de video.
    private fun pistaDeVideo(ruta: String): MediaFormat? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(ruta)
            return (0 until extractor.trackCount)
                .map { extractor.getTrackFormat(it) }
                .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true }
        } finally {
            extractor.release()
        }
    }
}
