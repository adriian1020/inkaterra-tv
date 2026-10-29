// Dobles de las memorias USB para las pruebas en la PC: volúmenes que se conectan y se retiran a
// mano. También los usan las pruebas de pantallas.
package pe.inkaterra.tv.sistema.usb

import java.io.File

class VolumenesFalsos(
    private var volumenes: List<VolumenUsb> = emptyList(),
) : Volumenes {
    private val oyentes = mutableListOf<(List<VolumenUsb>) -> Unit>()

    override fun actuales() = volumenes

    override fun observar(alCambiar: (List<VolumenUsb>) -> Unit): AutoCloseable {
        oyentes += alCambiar
        return AutoCloseable { oyentes -= alCambiar }
    }

    fun cambiar(nuevos: List<VolumenUsb>) {
        volumenes = nuevos
        oyentes.toList().forEach { it(nuevos) }
    }

    fun conectar(raiz: File) = cambiar(listOf(montado(raiz)))

    fun retirar() = cambiar(emptyList())
}

fun montado(raiz: File) = VolumenUsb("AB12-34CD", "Memoria", raiz, montado = true, estado = "mounted")
