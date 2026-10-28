// Texto de la versión de la app, para el inicio y los reportes de errores.
package pe.inkaterra.tv.dominio

fun textoVersion(
    nombre: String,
    codigo: Int,
): String = "v$nombre ($codigo)"
