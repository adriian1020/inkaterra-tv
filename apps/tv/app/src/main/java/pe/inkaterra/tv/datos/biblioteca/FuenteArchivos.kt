// De dónde salen los archivos del hotel: la carpeta de contenido de la app, con sus subcarpetas
// videos/, fotos/, musica/ y promociones/. Lo que está en promociones/ se marca para la fila
// Destacados y el bucle de promociones (D-20). La memoria USB del huésped es otra fuente, con sus
// fotos y videos de cualquier carpeta (D-22).
package pe.inkaterra.tv.datos.biblioteca

import java.io.File
import java.io.IOException

data class ArchivoLocal(
    val ruta: String,
    val nombre: String,
    val tamanoBytes: Long,
    val modificadoMs: Long,
    val esPromocion: Boolean,
    // De la memoria del huésped: sus miniaturas no se guardan en disco (D-10).
    val deUsb: Boolean = false,
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

// La memoria del huésped, leída en su lugar y sin copiar nada (D-10). Se salta lo que dejan Mac,
// Windows y Android y que no son fotos del huésped: lo que empieza con punto (como «._foto.jpg»)
// y sus carpetas de sistema.
class CarpetaUsb(
    private val raiz: File,
) : FuenteArchivos {
    override fun listar(): List<ArchivoLocal> {
        // Sin permiso de fotos y videos, Android no deja listar la memoria.
        raiz.listFiles() ?: throw IOException("No se puede listar $raiz")
        return raiz
            .walkTopDown()
            .onEnter { it == raiz || !ignorar(it) }
            .filter { it.isFile && !ignorar(it) }
            .map { f ->
                ArchivoLocal(f.absolutePath, f.name, f.length(), f.lastModified(), esPromocion = false, deUsb = true)
            }.toList()
    }

    private fun ignorar(f: File) = f.name.startsWith(".") || f.name in CARPETAS_DE_SISTEMA

    private companion object {
        val CARPETAS_DE_SISTEMA = setOf("Android", "LOST.DIR", "System Volume Information", "\$RECYCLE.BIN")
    }
}
