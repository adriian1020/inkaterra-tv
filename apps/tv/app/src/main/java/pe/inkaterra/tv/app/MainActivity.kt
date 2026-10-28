// Única actividad de la app: todo lo que ve el huésped se dibuja con Compose for TV.
package pe.inkaterra.tv.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import pe.inkaterra.tv.sistema.protector.mantenerPantallaEncendida

// AppCompatActivity: la necesita el idioma por app de appcompat (paso 1.6).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Provisional hasta el sprint 4: la pantalla queda encendida mientras la app está al frente.
        // Desde el sprint 4 se enciende solo durante las promociones.
        mantenerPantallaEncendida(this, encendida = true)
        val contenedor = Contenedor(this)
        setContent { TemaInkaterra { Navegacion(contenedor.biblioteca, contenedor.miniaturas, contenedor.idioma) } }
    }
}
