// Única actividad de la app: todo lo que ve el huésped se dibuja con Compose for TV.
package pe.inkaterra.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pe.inkaterra.tv.pantallas.inicio.InicioPantalla

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InicioPantalla() }
    }
}
