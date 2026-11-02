// De dónde saca su imagen cada tarjeta: lo del hotel, de la caché en disco; lo de la USB del
// huésped, de la memoria, porque de su memoria no se guarda nada (D-10).
package pe.inkaterra.tv.datos.miniaturas

import pe.inkaterra.tv.datos.biblioteca.Elemento

interface FuenteMiniaturas {
    // Lo que Coil sabe mostrar: un File o un Bitmap; null si no hay imagen.
    suspend fun de(elemento: Elemento): Any?
}

class MiniaturasApp(
    private val hotel: Miniaturas,
    private val usb: MiniaturasEnMemoria,
) : FuenteMiniaturas {
    override suspend fun de(elemento: Elemento): Any? =
        if (elemento.archivo.deUsb) usb.de(elemento) else hotel.de(elemento)
}
