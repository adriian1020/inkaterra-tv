# Changelog

Formato [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y versiones
[SemVer](https://semver.org/lang/es/).

## [Sin publicar]

### Agregado

- App mínima para Android TV / Google TV: aparece en la lista de apps con su banner y abre una
  pantalla con el nombre y la versión de la app.
- Protector de pantalla de Inkaterra para Google TV, que se activa por adb en cada TV.
- La TV no se suspende mientras la app está en pantalla.
- Inicio estilo Netflix: un área destacada arriba con lo que tiene el foco y filas Destacados,
  Videos, Fotos y Música con la miniatura de cada archivo de la carpeta del hotel.
- Las promociones de la carpeta `promociones/` salen en la fila Destacados.
- Solo aparecen los archivos que la TV puede reproducir: hasta 4K, H.264, HEVC o VP9 y hasta
  60 Mbps.
- La primera vez la app pregunta el idioma (español o inglés) y lo recuerda; se cambia desde el
  inicio.
- Colores y letras de Inkaterra: verde oscuro, Josefin Sans y Cinzel.
- Los videos se ven a pantalla completa en 4K: Enter pausa, Izquierda y Derecha saltan 10 s, y
  una barra muestra el título y el tiempo.
- Un video retoma donde quedó mientras la app esté abierta, y al terminar sigue el próximo
  video de la fila.
- Las fotos se ven en una presentación que pasa sola cada 8 s con un fundido; Enter la pausa.
- Al volver al inicio, el foco queda en lo último que se vio.

### Cambiado

- Ya no aparecen los videos que la TV no puede decodificar, como H.264 4:4:4.

### Corregido

- Las filas del inicio conservan su posición al volver de un video o una foto.
- El área destacada ya no muestra un instante la imagen anterior.
- Un video o una foto que no abre muestra un mensaje y sigue con el siguiente.
