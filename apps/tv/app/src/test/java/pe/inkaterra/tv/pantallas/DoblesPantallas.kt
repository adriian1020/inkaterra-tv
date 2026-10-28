// Dobles para las pruebas de pantallas: una biblioteca con contenido inventado, miniaturas que no
// generan imagen (las tarjetas quedan con su recuadro verde) y un ajuste de idioma en memoria.
package pe.inkaterra.tv.pantallas

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import pe.inkaterra.tv.datos.biblioteca.ArchivoLocal
import pe.inkaterra.tv.datos.biblioteca.Biblioteca
import pe.inkaterra.tv.datos.biblioteca.Elemento
import pe.inkaterra.tv.datos.biblioteca.FuenteFalsa
import pe.inkaterra.tv.datos.biblioteca.LectorFalso
import pe.inkaterra.tv.datos.biblioteca.archivo
import pe.inkaterra.tv.datos.miniaturas.GeneradorMiniatura
import pe.inkaterra.tv.datos.miniaturas.Miniaturas
import pe.inkaterra.tv.sistema.idioma.AjusteIdioma
import pe.inkaterra.tv.sistema.idioma.Idioma
import java.io.File

val contenidoDePrueba =
    listOf(
        archivo("Oferta spa.jpg", esPromocion = true),
        archivo("Reserva Amazónica 01.jpg", esPromocion = true),
        archivo("Atardecer en Machu Picchu.mp4"),
        archivo("Río Madre de Dios.mp4"),
        archivo("Selva.mp3"),
    )

fun bibliotecaDePrueba(archivos: List<ArchivoLocal> = contenidoDePrueba) =
    Biblioteca(FuenteFalsa(archivos), LectorFalso(emptyMap()), Dispatchers.Unconfined)

fun miniaturasSinImagen(carpeta: File) =
    Miniaturas(
        object : GeneradorMiniatura {
            override fun generar(elemento: Elemento): Bitmap? = null
        },
        carpeta,
        Dispatchers.Unconfined,
    )

class AjusteIdiomaFalso(
    private var idioma: Idioma?,
) : AjusteIdioma {
    val cambios = mutableListOf<Idioma>()

    override fun actual() = idioma

    override fun cambiar(idioma: Idioma) {
        cambios += idioma
        this.idioma = idioma
    }
}
