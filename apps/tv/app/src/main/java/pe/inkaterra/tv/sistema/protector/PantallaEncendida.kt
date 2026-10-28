// Evita que la TV apague la pantalla o se suspenda mientras la app muestra algo que el huésped
// no controla (las promociones). Solo vale mientras la actividad está al frente: D-17.
package pe.inkaterra.tv.sistema.protector

import android.app.Activity
import android.view.WindowManager

fun mantenerPantallaEncendida(
    activity: Activity,
    encendida: Boolean,
) {
    if (encendida) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
