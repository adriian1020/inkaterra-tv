// La USB del huésped (CU-17, D-22): qué estado ve el inicio al conectar, al retirar, sin permiso,
// con un formato que la TV no lee y si se retira mientras se lee.
package pe.inkaterra.tv.datos.usb

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import pe.inkaterra.tv.datos.biblioteca.LectorFalso
import pe.inkaterra.tv.datos.biblioteca.LectorMetadatos
import pe.inkaterra.tv.dominio.MetadatosVideo
import pe.inkaterra.tv.sistema.usb.VolumenUsb
import pe.inkaterra.tv.sistema.usb.VolumenesFalsos
import pe.inkaterra.tv.sistema.usb.montado
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class UsbDelHuespedTest {
    @get:Rule
    val temporal = TemporaryFolder()

    private val alcance = CoroutineScope(Dispatchers.Unconfined)
    private val volumenes = VolumenesFalsos()
    private val retirados = mutableListOf<VolumenUsb>()

    @After
    fun cerrar() = alcance.cancel()

    private fun crear(
        nombre: String,
        modificadoMs: Long = 0,
    ) = File(temporal.root, nombre).apply {
        parentFile?.mkdirs()
        writeText("x")
        setLastModified(modificadoMs)
    }

    private fun usb(
        lector: LectorMetadatos = LectorFalso(emptyMap()),
        permiso: Boolean = true,
        despachador: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.Unconfined,
    ) = UsbDelHuesped(volumenes, lector, { permiso }, { retirados += it }, true, despachador).also {
        it.empezar(alcance)
    }

    @Test
    fun alConectarListaFotosYVideosRecientesPrimero() {
        crear("DCIM/vieja.jpg", 1_000)
        crear("DCIM/nueva.mp4", 3_000)
        crear("DCIM/media.heic", 2_000)
        crear("Música/canción.mp3", 4_000)
        val usb = usb()
        volumenes.conectar(temporal.root)
        val lista = usb.estado.value as EstadoUsb.Lista
        assertEquals(listOf("nueva", "media", "vieja"), lista.elementos.map { it.titulo })
    }

    @Test
    fun cuentaLosRechazados() {
        val video = crear("grande.mp4")
        crear("foto.jpg")
        val lector = LectorFalso(mapOf(video.absolutePath to MetadatosVideo(7680, 4320, "video/hevc", 50_000_000)))
        val usb = usb(lector)
        volumenes.conectar(temporal.root)
        val lista = usb.estado.value as EstadoUsb.Lista
        assertEquals(1, lista.rechazados)
        assertEquals(listOf("foto"), lista.elementos.map { it.titulo })
    }

    @Test
    fun alRetirarAvisaYVuelveASinUsb() {
        crear("foto.jpg")
        val usb = usb()
        volumenes.conectar(temporal.root)
        volumenes.retirar()
        assertEquals(EstadoUsb.SinUsb, usb.estado.value)
        assertEquals(listOf(montado(temporal.root)), retirados)
    }

    @Test
    fun sinPermisoNoLee() {
        val usb = usb(permiso = false)
        volumenes.conectar(temporal.root)
        assertEquals(EstadoUsb.NoSeLee(montado(temporal.root), MotivoUsb.SIN_PERMISO), usb.estado.value)
    }

    @Test
    fun formatoQueNoLeeLoDice() {
        val usb = usb()
        val ntfs = VolumenUsb("AB12-34CD", "Memoria", null, montado = false, estado = "unmountable")
        volumenes.cambiar(listOf(ntfs))
        assertEquals(EstadoUsb.NoSeLee(ntfs, MotivoUsb.FORMATO), usb.estado.value)
    }

    // Retirar la memoria mientras se leen sus videos: la lectura que termina después no vuelve a
    // poner la fila.
    @Test
    fun retirarMientrasLeeNoDejaLaFila() {
        crear("video.mp4")
        val leyendo = CountDownLatch(1)
        val soltar = CountDownLatch(1)
        val lector =
            object : LectorMetadatos {
                override fun video(ruta: String): MetadatosVideo {
                    leyendo.countDown()
                    soltar.await(5, TimeUnit.SECONDS)
                    return MetadatosVideo(1920, 1080, "video/avc", 8_000_000)
                }
            }
        val usb = usb(lector, despachador = Dispatchers.IO)
        volumenes.conectar(temporal.root)
        assertTrue(leyendo.await(5, TimeUnit.SECONDS))
        assertTrue(usb.estado.value is EstadoUsb.Leyendo)
        volumenes.retirar()
        soltar.countDown()
        Thread.sleep(500)
        assertEquals(EstadoUsb.SinUsb, usb.estado.value)
    }
}
