// Dobles de la biblioteca para las pruebas en la PC: una carpeta y un lector de metadatos falsos.
// También los usan las pruebas de pantallas.
package pe.inkaterra.tv.datos.biblioteca

import pe.inkaterra.tv.dominio.MetadatosVideo

class FuenteFalsa(
    private val archivos: List<ArchivoLocal>,
) : FuenteArchivos {
    override fun listar(): List<ArchivoLocal> = archivos
}

class LectorFalso(
    private val metadatos: Map<String, MetadatosVideo>,
    private val danados: Set<String> = emptySet(),
) : LectorMetadatos {
    override fun video(ruta: String): MetadatosVideo {
        check(ruta !in danados) { "Archivo dañado: $ruta" }
        return metadatos[ruta] ?: MetadatosVideo(1920, 1080, "video/avc", 8_000_000)
    }
}

fun archivo(
    nombre: String,
    esPromocion: Boolean = false,
    modificadoMs: Long = 0,
): ArchivoLocal {
    val carpeta = if (esPromocion) "promociones" else "contenido"
    return ArchivoLocal("/$carpeta/$nombre", nombre, 1_000, modificadoMs, esPromocion)
}
