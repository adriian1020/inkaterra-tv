// La biblioteca del hotel: lo que hay en la carpeta de contenido, separado por tipo y validado con
// las reglas de contenido. Un archivo que no se puede leer va a rechazados y no corta la carga.
package pe.inkaterra.tv.datos.biblioteca

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.inkaterra.tv.dominio.MotivoRechazo
import pe.inkaterra.tv.dominio.TipoContenido
import pe.inkaterra.tv.dominio.clasificar
import pe.inkaterra.tv.dominio.evaluarVideo
import java.text.Collator
import java.util.Locale

data class Elemento(
    val archivo: ArchivoLocal,
    val tipo: TipoContenido,
    val titulo: String,
)

data class Rechazado(
    val archivo: ArchivoLocal,
    val motivo: MotivoRechazo,
)

data class ContenidoBiblioteca(
    val destacados: List<Elemento>,
    val videos: List<Elemento>,
    val fotos: List<Elemento>,
    val musica: List<Elemento>,
    val rechazados: List<Rechazado>,
) {
    companion object {
        val VACIO = ContenidoBiblioteca(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}

class Biblioteca(
    private val fuente: FuenteArchivos,
    private val lector: LectorMetadatos,
    private val despachador: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun cargar(): ContenidoBiblioteca =
        withContext(despachador) {
            val aceptados = mutableListOf<Elemento>()
            val rechazados = mutableListOf<Rechazado>()
            for (archivo in fuente.listar()) {
                val tipo = clasificar(archivo.nombre) ?: continue
                // Las promociones son fotos y videos (CU-05): la música en promociones/ no va.
                if (archivo.esPromocion && tipo == TipoContenido.MUSICA) continue
                val motivo = if (tipo == TipoContenido.VIDEO) motivoRechazo(archivo) else null
                if (motivo != null) {
                    rechazados += Rechazado(archivo, motivo)
                } else {
                    aceptados += Elemento(archivo, tipo, archivo.nombre.substringBeforeLast('.'))
                }
            }
            val orden = Collator.getInstance(Locale.forLanguageTag("es")).apply { strength = Collator.PRIMARY }
            val ordenados = aceptados.sortedWith(compareBy(orden) { it.titulo })
            val (destacados, resto) = ordenados.partition { it.archivo.esPromocion }
            ContenidoBiblioteca(
                destacados = destacados,
                videos = resto.filter { it.tipo == TipoContenido.VIDEO },
                fotos = resto.filter { it.tipo == TipoContenido.FOTO },
                musica = resto.filter { it.tipo == TipoContenido.MUSICA },
                rechazados = rechazados,
            )
        }

    private fun motivoRechazo(archivo: ArchivoLocal): MotivoRechazo? =
        try {
            evaluarVideo(lector.video(archivo.ruta))
        } catch (e: Exception) {
            MotivoRechazo.ArchivoIlegible
        }
}
