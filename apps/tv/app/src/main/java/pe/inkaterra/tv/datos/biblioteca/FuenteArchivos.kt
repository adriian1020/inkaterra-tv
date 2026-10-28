// De dónde salen los archivos del hotel: la carpeta de contenido de la app, con sus subcarpetas
// videos/, fotos/, musica/ y promociones/. Lo que está en promociones/ se marca para la fila
// Destacados y el bucle de promociones (D-20).
package pe.inkaterra.tv.datos.biblioteca

import java.io.File

data class ArchivoLocal(
    val ruta: String,
    val nombre: String,
    val tamanoBytes: Long,
    val modificadoMs: Long,
    val esPromocion: Boolean,
)

interface FuenteArchivos {
    fun listar(): List<ArchivoLocal>
}

class CarpetaContenido(
    private val raiz: File,
) : FuenteArchivos {
    override fun listar(): List<ArchivoLocal> {
        if (!raiz.isDirectory) return emptyList()
        val promociones = File(raiz, CARPETA_PROMOCIONES)
        return raiz
            .walkTopDown()
            .filter { it.isFile }
            .map { f ->
                ArchivoLocal(
                    ruta = f.absolutePath,
                    nombre = f.name,
                    tamanoBytes = f.length(),
                    modificadoMs = f.lastModified(),
                    esPromocion = f.startsWith(promociones),
                )
            }.toList()
    }

    private companion object {
        const val CARPETA_PROMOCIONES = "promociones"
    }
}
