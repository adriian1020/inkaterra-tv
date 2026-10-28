// Lee lo que la TV puede hacer: decodificadores de video, espacio libre y memoria. Sirve para
// la ficha del equipo y, más adelante, para rechazar un video que la TV no podría reproducir.
package pe.inkaterra.tv.sistema.capacidades

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.StatFs
import pe.inkaterra.tv.dominio.Decodificador
import java.io.File

object CapacidadesEquipo {
    fun decodificadoresVideo(): List<Decodificador> =
        MediaCodecList(MediaCodecList.REGULAR_CODECS)
            .codecInfos
            .filter { !it.isEncoder }
            .flatMap { info ->
                info.supportedTypes
                    .filter { it.startsWith("video/") }
                    .mapNotNull { mime ->
                        // Un tipo video/ sin capacidades de video es un caso raro del fabricante: se omite.
                        val video = info.getCapabilitiesForType(mime).videoCapabilities ?: return@mapNotNull null
                        Decodificador(
                            nombre = info.name,
                            mime = mime,
                            porHardware = info.isHardwareAccelerated,
                            anchoMax = video.supportedWidths.upper,
                            altoMax = video.supportedHeights.upper,
                            tasaMaxBps = video.bitrateRange.upper,
                        )
                    }
            }

    fun espacioLibreBytes(dir: File): Long = StatFs(dir.path).availableBytes

    fun memoriaTotalBytes(context: Context): Long {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        return info.totalMem
    }
}
