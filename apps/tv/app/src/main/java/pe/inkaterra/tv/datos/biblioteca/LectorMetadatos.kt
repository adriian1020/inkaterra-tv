// Lee de un video lo que piden las reglas de contenido: tamaño, códec de la pista de video y tasa.
package pe.inkaterra.tv.datos.biblioteca

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import pe.inkaterra.tv.dominio.MetadatosVideo

interface LectorMetadatos {
    // Lanza una excepción si el archivo no se puede leer.
    fun video(ruta: String): MetadatosVideo
}

class LectorMetadatosAndroid : LectorMetadatos {
    override fun video(ruta: String): MetadatosVideo {
        val lector = MediaMetadataRetriever()
        try {
            lector.setDataSource(ruta)

            fun entero(clave: Int) = lector.extractMetadata(clave)?.toIntOrNull()
            return MetadatosVideo(
                ancho = checkNotNull(entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)) { "Sin video: $ruta" },
                alto = checkNotNull(entero(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)) { "Sin video: $ruta" },
                mime = mimeDelVideo(ruta),
                tasaBps = entero(MediaMetadataRetriever.METADATA_KEY_BITRATE),
            )
        } finally {
            lector.release()
        }
    }

    // METADATA_KEY_MIMETYPE da el del contenedor (video/mp4); el códec está en la pista de video.
    private fun mimeDelVideo(ruta: String): String? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(ruta)
            return (0 until extractor.trackCount)
                .mapNotNull { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) }
                .firstOrNull { it.startsWith("video/") }
        } finally {
            extractor.release()
        }
    }
}
