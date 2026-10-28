// Memorias USB conectadas a la TV: cuáles hay y aviso cuando se conecta o se retira una. Solo
// lee; nunca copia nada de la USB del huésped (D-10). Formatos y permisos: D-18.
package pe.inkaterra.tv.sistema.usb

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import java.io.File

data class VolumenUsb(
    val id: String,
    val etiqueta: String?,
    val raiz: File?,
    val montado: Boolean,
)

class VolumenesUsb(
    private val context: Context,
) {
    private val almacenamiento = context.getSystemService(StorageManager::class.java)

    fun actuales(): List<VolumenUsb> =
        almacenamiento.storageVolumes
            .filter { it.isRemovable }
            .map { it.aVolumenUsb() }

    // El aviso llega en el hilo principal; cerrar el resultado deja de escuchar.
    fun observar(alCambiar: (List<VolumenUsb>) -> Unit): AutoCloseable {
        val aviso =
            object : StorageManager.StorageVolumeCallback() {
                override fun onStateChanged(volume: StorageVolume) = alCambiar(actuales())
            }
        almacenamiento.registerStorageVolumeCallback(context.mainExecutor, aviso)
        return AutoCloseable { almacenamiento.unregisterStorageVolumeCallback(aviso) }
    }

    private fun StorageVolume.aVolumenUsb() =
        VolumenUsb(
            id = uuid ?: mediaStoreVolumeName ?: "sin-id",
            etiqueta = getDescription(context),
            raiz = directory,
            montado = state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY,
        )
}
