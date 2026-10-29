// Única actividad de la app: todo lo que ve el huésped se dibuja con Compose for TV.
package pe.inkaterra.tv.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import pe.inkaterra.tv.BuildConfig
import pe.inkaterra.tv.sistema.protector.mantenerPantallaEncendida
import java.io.File

// AppCompatActivity: la necesita el idioma por app de appcompat (paso 1.6).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Provisional hasta el sprint 4: la pantalla queda encendida mientras la app está al frente.
        // Desde el sprint 4 se enciende solo durante las promociones.
        mantenerPantallaEncendida(this, encendida = true)
        // Solo en depuración, las pruebas en la TV pueden leer otra carpeta de contenido.
        val raizDePrueba = intent.getStringExtra(EXTRA_RAIZ_CONTENIDO)?.takeIf { BuildConfig.DEBUG }?.let(::File)
        val contenedor = if (raizDePrueba != null) Contenedor(this, raizDePrueba) else Contenedor(this)
        setContent {
            TemaInkaterra {
                Navegacion(
                    contenedor.biblioteca,
                    contenedor.miniaturas,
                    contenedor.posiciones,
                    contenedor.idioma,
                )
            }
        }
    }

    companion object {
        const val EXTRA_RAIZ_CONTENIDO = "pe.inkaterra.tv.RAIZ_CONTENIDO"
    }
}
