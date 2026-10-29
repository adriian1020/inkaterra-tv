# Prueba a mano de la USB del huésped

El Xiaomi (y quizás la TCL) solo lee memorias USB con la depuración por USB **y** la inalámbrica
apagadas (D-18). Sin depuración no hay adb, así que la prueba se hace a mano en la TV y después se
lee lo que la app dejó en el registro de Android. Sirve igual para la TCL del piloto.

## Antes

1. Instalar la versión de prueba con `tool/tv.sh instalar depuracion`: es la que anota los nombres
   de los archivos en el registro (D-23), y además le da a la app los permisos de fotos y videos
   (D-22). Sin ellos, «Tu USB» dice «Esta memoria no se puede leer en esta TV».
2. Agrandar el registro de Android: **Ajustes → Opciones de desarrollador → «Tamaños del búfer del
   registrador» → 8 M** (el Xiaomi no deja más). Con el de 256 K que trae, el registro se pisa en
   uno o dos minutos y se pierde la prueba.
3. Preparar las memorias. Lo ideal es una en cada formato: **FAT32**, **exFAT** y, si hay,
   **NTFS**. En cada una:
   - fotos de un celular, en **JPG** y en **HEIC** (las de iPhone);
   - un video de celular, en **MP4** o **MOV**;
   - un video de más de 4K (si hay uno a mano), para ver el aviso de archivos que no se pueden ver;
   - una carpeta copiada desde una Mac (trae archivos `._foto.jpg`) o una memoria usada en
     Windows (trae `System Volume Information`).
4. Anotar qué tiene cada memoria (cuántas fotos y videos), para compararlo con lo que muestra la TV.

## En la TV

Con la depuración por USB y la inalámbrica **apagadas**:

1. Abrir la app.
2. Conectar la memoria, directo al Xiaomi (sin extensor). En unos segundos debería aparecer la fila
   **«Tu USB»**, segunda, después de Destacados. Anotar:
   - si aparece la fila, o qué aviso sale («Leyendo tu memoria…», «No hay fotos ni videos en esta
     memoria» o «Esta memoria no se puede leer en esta TV»);
   - cuánto tarda en aparecer;
   - si las fotos más nuevas salen primero;
   - si se ven las miniaturas;
   - si debajo del título dice «N archivos no se pueden ver en esta TV», y si N es correcto;
   - si aparece algún archivo basura (`._foto`, cosas de `System Volume Information`).
3. Abrir una foto JPG, una HEIC y un video. Anotar si abren y si suena la música de fondo en la
   presentación.
4. Con una foto abierta, **retirar la memoria**. La app debe volver al inicio, sin la fila, con el
   foco en la primera tarjeta.
5. Repetir con cada memoria.

## Después

1. Encender **solo la depuración inalámbrica** y conectar desde la PC: `tool/tv.sh conectar` (si
   pide autorización, aceptarla en la TV).
2. Guardar el registro **enseguida**: Android lo guarda en memoria y lo va pisando con el tiempo.

   ```bash
   adb -s 192.168.31.242:5555 logcat -d -s Usb:* Fondo:* Reproductor:* Contenido:* > docs/pruebas/usb-registro.txt
   ```

3. En el registro, la etiqueta `Usb` dice:
   - `Escuchando las memorias USB; permiso=true`: la app arrancó y tiene los permisos;
   - `Volumen <id> estado=<estado> montado=<sí o no>`: lo que Android contó de la memoria; un
     `estado=unmountable` es un formato que la TV no lee;
   - `N fotos y videos, M rechazados en X ms`: lo que encontró y cuánto tardó;
   - una línea por archivo, con su tipo o el motivo del rechazo (solo en la versión de prueba);
   - `Retirada <id>`: cuando se sacó la memoria.
4. Con lo anotado y el registro, completar D-18: qué formatos lee, si los permisos alcanzan y
   cuánto tarda en leer la memoria.
