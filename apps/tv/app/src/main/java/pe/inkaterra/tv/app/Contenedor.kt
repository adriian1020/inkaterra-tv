// Arma las piezas de la app a mano, sin Hilt: la biblioteca del hotel, sus miniaturas, dónde
// retomar cada video, el idioma y la USB del huésped. Al retirar la USB se olvida todo lo de la
// memoria: dónde iban sus videos, sus miniaturas y sus fotos en la memoria de Coil (D-10).
package pe.inkaterra.tv.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import coil3.SingletonImageLoader
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.CarpetaContenido
import pe.inkaterra.tv.datos.biblioteca.LectorMetadatosAndroid
import pe.inkaterra.tv.datos.miniaturas.FuenteMiniaturas
import pe.inkaterra.tv.datos.miniaturas.GeneradorMiniaturaAndroid
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.datos.miniaturas.MiniaturasApp
import pe.inkaterra.tv.datos.miniaturas.MiniaturasEnMemoria
import pe.inkaterra.tv.datos.usb.UsbDelHuesped
import pe.inkaterra.tv.dominio.PosicionesVideo
import pe.inkaterra.tv.sistema.idioma.AjusteIdioma
import pe.inkaterra.tv.sistema.idioma.AjusteIdiomaAndroid
import pe.inkaterra.tv.sistema.usb.VolumenesUsb
import java.io.File

class Contenedor(
    context: Context,
    // Si el almacenamiento externo no está disponible, una carpeta vacía en vez de cerrar la app.
    raiz: File = context.getExternalFilesDir("contenido") ?: File(context.filesDir, "contenido"),
) {
    val biblioteca = Biblioteca(CarpetaContenido(raiz), LectorMetadatosAndroid())
    val miniaturas: FuenteMiniaturas = miniaturasDelProceso(context)
    val posiciones: PosicionesVideo = posicionesDelProceso
    val idioma: AjusteIdioma = AjusteIdiomaAndroid()
    val usb: UsbDelHuesped = usbDelProceso(context.applicationContext)

    private companion object {
        // Las posiciones viven mientras viva el proceso (D-21), aunque la actividad se recree.
        val posicionesDelProceso = PosicionesVideo()

        // Las miniaturas de la USB, en memoria y más chicas que las del hotel (D-22).
        val miniaturasUsb = MiniaturasEnMemoria(GeneradorMiniaturaAndroid(ANCHO_USB, ALTO_USB))

        const val ANCHO_USB = 640
        const val ALTO_USB = 360

        // Una sola por proceso: la actividad se recrea (al cambiar el idioma) y una instancia nueva
        // traería su propio límite de dos, sumando decodificaciones 4K a las que siguen en curso.
        @Volatile private var miniaturas: FuenteMiniaturas? = null

        // Una sola por proceso, como las miniaturas: escucha a Android mientras el inicio existe.
        @Volatile private var usb: UsbDelHuesped? = null

        fun miniaturasDelProceso(context: Context): FuenteMiniaturas =
            miniaturas ?: synchronized(this) {
                val hotel = Miniaturas(GeneradorMiniaturaAndroid(), File(context.cacheDir, "miniaturas"))
                miniaturas ?: MiniaturasApp(hotel, miniaturasUsb).also { miniaturas = it }
            }

        fun usbDelProceso(context: Context): UsbDelHuesped =
            usb ?: synchronized(this) {
                usb ?: UsbDelHuesped(
                    VolumenesUsb(context),
                    LectorMetadatosAndroid(),
                    permiso = { tienePermiso(context) },
                    alRetirar = { volumen ->
                        volumen.raiz?.let { posicionesDelProceso.olvidarDentroDe(it.path) }
                        miniaturasUsb.limpiar()
                        SingletonImageLoader.get(context).memoryCache?.clear()
                    },
                ).also { usb = it }
            }

        // Se dan al instalar por adb (D-22). Antes de Android 13 era un solo permiso.
        fun tienePermiso(context: Context): Boolean {
            val permisos =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            return permisos.all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
}
