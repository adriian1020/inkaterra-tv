// Reglas del reproductor de video (D-21): dónde retomar cada video, los saltos de 10 s y el texto
// del tiempo. Las posiciones viven en la memoria del proceso: al cerrar la app se olvidan, y el
// próximo huésped empieza de cero.
package pe.inkaterra.tv.dominio

import java.util.concurrent.ConcurrentHashMap

const val SALTO_MS = 10_000L
const val INICIO_MINIMO_MS = 5_000L
const val FINAL_MINIMO_MS = 15_000L

class PosicionesVideo {
    private val posiciones = ConcurrentHashMap<String, Long>()

    // En los primeros 5 s no vale la pena retomar, y en los últimos 15 s el video ya se vio. Si la
    // duración no se conoce (0 o negativa), solo cuenta el principio.
    fun guardar(
        ruta: String,
        posicionMs: Long,
        duracionMs: Long,
    ) {
        val quedaPorVer = duracionMs <= 0 || duracionMs - posicionMs > FINAL_MINIMO_MS
        if (posicionMs >= INICIO_MINIMO_MS && quedaPorVer) {
            posiciones[ruta] = posicionMs
        } else {
            posiciones.remove(ruta)
        }
    }

    fun posicion(ruta: String): Long = posiciones[ruta] ?: 0
}

enum class AccionVideo { ALTERNAR, ADELANTAR, RETROCEDER }

// Adónde lleva un salto, sin pasar de 0 ni del final; ALTERNAR no salta.
fun destinoSalto(
    accion: AccionVideo,
    posicionMs: Long,
    duracionMs: Long,
): Long? =
    when (accion) {
        AccionVideo.ALTERNAR -> null
        AccionVideo.ADELANTAR -> (posicionMs + SALTO_MS).let { if (duracionMs > 0) minOf(it, duracionMs) else it }
        AccionVideo.RETROCEDER -> maxOf(posicionMs - SALTO_MS, 0)
    }

// m:ss, y h:mm:ss desde una hora.
fun textoTiempo(ms: Long): String {
    val segundos = maxOf(ms, 0) / 1_000
    val horas = segundos / 3_600
    val minutos = segundos % 3_600 / 60
    val resto = segundos % 60
    return if (horas > 0) {
        "%d:%02d:%02d".format(horas, minutos, resto)
    } else {
        "%d:%02d".format(minutos, resto)
    }
}
