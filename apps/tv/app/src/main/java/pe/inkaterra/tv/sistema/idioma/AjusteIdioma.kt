// El idioma de la app, aparte del de la TV: el huésped lo elige la primera vez y Android lo
// recuerda (idioma por app, con appcompat también antes de Android 13).
package pe.inkaterra.tv.sistema.idioma

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class Idioma(
    val etiqueta: String,
) {
    ESPANOL("es"),
    INGLES("en"),
}

interface AjusteIdioma {
    // null si todavía no se eligió.
    fun actual(): Idioma?

    fun cambiar(idioma: Idioma)
}

class AjusteIdiomaAndroid : AjusteIdioma {
    override fun actual(): Idioma? {
        val idiomas = AppCompatDelegate.getApplicationLocales()
        if (idiomas.isEmpty) return null
        return Idioma.entries.firstOrNull { it.etiqueta == idiomas[0]?.language }
    }

    override fun cambiar(idioma: Idioma) =
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(idioma.etiqueta))
}
