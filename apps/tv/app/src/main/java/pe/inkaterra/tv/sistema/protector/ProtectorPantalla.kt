// Protector de pantalla del sistema: Android TV lo muestra cuando nadie usa la TV, aunque el
// huésped esté en otra app. Por ahora muestra el nombre sobre el verde de Inkaterra; el
// sprint 4 le pone las promociones. Cómo se activa en cada TV: D-16.
package pe.inkaterra.tv.sistema.protector

import android.graphics.Color
import android.service.dreams.DreamService
import android.view.Gravity
import android.widget.TextView
import pe.inkaterra.tv.R

class ProtectorPantalla : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Sin interacción: cualquier tecla cierra el protector y devuelve al huésped.
        isInteractive = false
        isFullscreen = true
        setContentView(
            TextView(this).apply {
                text = getString(R.string.app_name)
                textSize = TAMANO_TEXTO_SP
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                setBackgroundColor(VERDE_INKATERRA)
            },
        )
    }

    private companion object {
        const val TAMANO_TEXTO_SP = 48f
        val VERDE_INKATERRA = Color.rgb(0x5A, 0x6A, 0x40)
    }
}
