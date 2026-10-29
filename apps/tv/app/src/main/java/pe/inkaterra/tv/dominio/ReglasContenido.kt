// Reglas de contenido: qué archivos acepta la TV. El tipo sale de la extensión; un video entra
// si no pasa de 4K (en cualquier orientación), usa un códec que la TV decodifica, la TV tiene un
// decodificador para su perfil (D-21) y no supera la tasa medida en el Xiaomi (D-13). Kotlin puro: lo usan la carpeta del hotel, la USB y, en la
// Fase 2, el panel al subir.
package pe.inkaterra.tv.dominio

enum class TipoContenido { VIDEO, FOTO, MUSICA }

const val ANCHO_MAXIMO = 3840
const val ALTO_MAXIMO = 2160
const val TASA_MAXIMA_BPS = 60_000_000

private val extensiones =
    mapOf(
        TipoContenido.VIDEO to setOf("mp4", "mkv", "mov", "webm"),
        TipoContenido.FOTO to setOf("jpg", "jpeg", "png", "webp", "heic"),
        TipoContenido.MUSICA to setOf("mp3", "m4a", "aac", "flac"),
    )

private val codecsVideo = setOf("video/avc", "video/hevc", "video/x-vnd.on2.vp9")

fun clasificar(nombreArchivo: String): TipoContenido? {
    val punto = nombreArchivo.lastIndexOf('.')
    if (punto < 0) return null
    val extension = nombreArchivo.substring(punto + 1).lowercase()
    return extensiones.entries.firstOrNull { extension in it.value }?.key
}

data class MetadatosVideo(
    val ancho: Int,
    val alto: Int,
    val mime: String?,
    val tasaBps: Int?,
    // Si la TV tiene un decodificador para el perfil exacto: H.264 4:4:4 es AVC y no lo tiene.
    val decodificable: Boolean = true,
)

sealed interface MotivoRechazo {
    data class ResolucionExcedida(
        val ancho: Int,
        val alto: Int,
    ) : MotivoRechazo

    data class CodecNoSoportado(
        val mime: String?,
    ) : MotivoRechazo

    data class SinDecodificador(
        val mime: String?,
    ) : MotivoRechazo

    data class TasaExcedida(
        val tasaBps: Int,
    ) : MotivoRechazo

    data object ArchivoIlegible : MotivoRechazo
}

// Se informa el primer motivo: resolución, códec, decodificador y tasa. Una tasa desconocida se
// acepta porque algunos MKV no la declaran.
fun evaluarVideo(m: MetadatosVideo): MotivoRechazo? {
    val mayor = maxOf(m.ancho, m.alto)
    val menor = minOf(m.ancho, m.alto)
    return when {
        mayor > ANCHO_MAXIMO || menor > ALTO_MAXIMO -> MotivoRechazo.ResolucionExcedida(m.ancho, m.alto)
        m.mime !in codecsVideo -> MotivoRechazo.CodecNoSoportado(m.mime)
        !m.decodificable -> MotivoRechazo.SinDecodificador(m.mime)
        m.tasaBps != null && m.tasaBps > TASA_MAXIMA_BPS -> MotivoRechazo.TasaExcedida(m.tasaBps)
        else -> null
    }
}
