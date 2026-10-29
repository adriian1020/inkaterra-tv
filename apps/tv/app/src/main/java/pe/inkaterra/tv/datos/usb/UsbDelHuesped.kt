// La memoria USB del huésped (CU-17, D-22): escucha cuándo se conecta y se retira, la lee con las
// mismas reglas que el contenido del hotel y deja el resultado en `estado`, que el inicio convierte
// en la fila «Tu USB». Al retirarla avisa con `alRetirar`, para que se olvide todo lo de la
// memoria (D-10). Lo que pasa queda en el registro con la etiqueta «Usb», para la prueba a mano
// con la depuración apagada (D-18); los nombres de los archivos, solo si `registrarArchivos`.
package pe.inkaterra.tv.datos.usb

import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.inkaterra.tv.BuildConfig
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.CarpetaUsb
import pe.inkaterra.tv.datos.biblioteca.ContenidoBiblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.LectorMetadatos
import pe.inkaterra.tv.datos.biblioteca.paraLaUsb
import pe.inkaterra.tv.sistema.usb.VolumenUsb
import pe.inkaterra.tv.sistema.usb.Volumenes
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

enum class MotivoUsb { FORMATO, SIN_PERMISO, ERROR }

sealed interface EstadoUsb {
    data object SinUsb : EstadoUsb

    data class Leyendo(
        val volumen: VolumenUsb,
    ) : EstadoUsb

    data class Lista(
        val volumen: VolumenUsb,
        val elementos: List<Elemento>,
        val rechazados: Int,
    ) : EstadoUsb

    data class NoSeLee(
        val volumen: VolumenUsb,
        val motivo: MotivoUsb,
    ) : EstadoUsb
}

private val EstadoUsb.volumen: VolumenUsb?
    get() =
        when (this) {
            EstadoUsb.SinUsb -> null
            is EstadoUsb.Leyendo -> volumen
            is EstadoUsb.Lista -> volumen
            is EstadoUsb.NoSeLee -> volumen
        }

private const val ETIQUETA = "Usb"

// Estados de StorageVolume en los que la memoria ya no está (o se está yendo).
private val AUSENTE = setOf("removed", "bad_removal", "ejecting", "unmounted")

// Estados en los que la memoria está, pero Android no entiende su formato (por ejemplo, NTFS).
private val FORMATO_DESCONOCIDO = setOf("unmountable", "nofs")

class UsbDelHuesped(
    private val volumenes: Volumenes,
    private val lector: LectorMetadatos,
    private val permiso: () -> Boolean,
    private val alRetirar: (VolumenUsb) -> Unit,
    private val registrarArchivos: Boolean = BuildConfig.DEBUG,
    private val despachador: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _estado = MutableStateFlow<EstadoUsb>(EstadoUsb.SinUsb)
    val estado: StateFlow<EstadoUsb> = _estado.asStateFlow()

    private var lectura: Job? = null

    // Revisa lo que ya está conectado y escucha los cambios hasta que se cierre el resultado.
    fun empezar(alcance: CoroutineScope): AutoCloseable {
        Log.i(ETIQUETA, "Escuchando las memorias USB; permiso=${permiso()}")
        revisar(alcance, volumenes.actuales())
        val escucha = volumenes.observar { revisar(alcance, it) }
        return AutoCloseable {
            escucha.close()
            lectura?.cancel()
        }
    }

    private fun revisar(
        alcance: CoroutineScope,
        lista: List<VolumenUsb>,
    ) {
        lista.forEach { Log.i(ETIQUETA, "Volumen ${it.id} estado=${it.estado} montado=${it.montado} raiz=${it.raiz}") }
        val volumen = lista.firstOrNull { it.estado !in AUSENTE }
        val anterior = _estado.value.volumen
        if (anterior != null && anterior.id != volumen?.id) retirar(anterior)
        when {
            volumen == null -> Unit
            volumen.montado && volumen.raiz != null -> {
                // Android avisa varias veces por la misma memoria: no se vuelve a leer. Pero una lectura que
                // se cortó (se cerró la app mientras leía) se repite, para no quedar en «Leyendo».
                val actual = _estado.value
                val leyendo = actual is EstadoUsb.Leyendo && lectura?.isActive == true
                if ((leyendo || actual is EstadoUsb.Lista) && anterior?.id == volumen.id) return
                if (permiso()) {
                    leer(alcance, volumen, volumen.raiz)
                } else {
                    noSeLee(volumen, MotivoUsb.SIN_PERMISO)
                }
            }
            volumen.estado in FORMATO_DESCONOCIDO -> noSeLee(volumen, MotivoUsb.FORMATO)
        }
    }

    private fun retirar(volumen: VolumenUsb) {
        lectura?.cancel()
        lectura = null
        _estado.value = EstadoUsb.SinUsb
        Log.i(ETIQUETA, "Retirada ${volumen.id}")
        alRetirar(volumen)
    }

    private fun noSeLee(
        volumen: VolumenUsb,
        motivo: MotivoUsb,
    ) {
        Log.w(ETIQUETA, "No se lee ${volumen.id}: $motivo")
        _estado.value = EstadoUsb.NoSeLee(volumen, motivo)
    }

    private fun leer(
        alcance: CoroutineScope,
        volumen: VolumenUsb,
        raiz: File,
    ) {
        lectura?.cancel()
        _estado.value = EstadoUsb.Leyendo(volumen)
        lectura =
            alcance.launch {
                val inicio = System.currentTimeMillis()
                val contenido =
                    try {
                        Biblioteca(CarpetaUsb(raiz), lector, despachador).cargar()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(ETIQUETA, "Error al leer ${volumen.id}", e)
                        _estado.value = EstadoUsb.NoSeLee(volumen, MotivoUsb.ERROR)
                        return@launch
                    }
                val elementos = contenido.paraLaUsb()
                registrar(contenido, elementos, System.currentTimeMillis() - inicio)
                _estado.value = EstadoUsb.Lista(volumen, elementos, contenido.rechazados.size)
            }
    }

    private fun registrar(
        contenido: ContenidoBiblioteca,
        elementos: List<Elemento>,
        ms: Long,
    ) {
        Log.i(ETIQUETA, "${elementos.size} fotos y videos, ${contenido.rechazados.size} rechazados en $ms ms")
        if (!registrarArchivos) return
        elementos.forEach { Log.i(ETIQUETA, "${it.archivo.ruta} | ${it.tipo}") }
        contenido.rechazados.forEach { Log.i(ETIQUETA, "${it.archivo.ruta} | rechazado: ${it.motivo}") }
    }
}
