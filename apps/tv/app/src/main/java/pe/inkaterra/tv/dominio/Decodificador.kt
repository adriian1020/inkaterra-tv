// Decodificador de video de la TV y la regla de cuándo sirve para 4K. Kotlin puro: la lectura
// real de los decodificadores está en sistema/capacidades.
package pe.inkaterra.tv.dominio

const val LADO_MAYOR_4K = 3840
const val LADO_MENOR_4K = 2160

data class Decodificador(
    val nombre: String,
    val mime: String,
    val porHardware: Boolean,
    val anchoMax: Int,
    val altoMax: Int,
    val tasaMaxBps: Int,
)

// Solo cuenta el decodificador por hardware: por software, 4K se traba en una TV box.
// Se comparan los lados mayor y menor porque algunos declaran el límite en vertical.
fun soporta4k(d: Decodificador): Boolean =
    d.porHardware &&
        maxOf(d.anchoMax, d.altoMax) >= LADO_MAYOR_4K &&
        minOf(d.anchoMax, d.altoMax) >= LADO_MENOR_4K
