// Única actividad de la app: todo lo que ve el huésped se dibuja con Compose for TV.
package pe.inkaterra.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pe.inkaterra.tv.pantallas.inicio.InicioPantalla
import pe.inkaterra.tv.sistema.protector.mantenerPantallaEncendida

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Provisional hasta el sprint 4: la pantalla queda encendida mientras la app está al frente.
        // Desde el sprint 4 se enciende solo durante las promociones.
        mantenerPantallaEncendida(this, encendida = true)
        setContent { InicioPantalla() }
    }
}
