# Prueba a mano de la USB del huésped

El Xiaomi (y quizás la TCL) solo lee memorias USB con la depuración por USB **y** la inalámbrica
apagadas (D-18). Sin depuración no hay adb, así que la prueba se hace a mano en la TV y después se
lee lo que la app dejó en el registro de Android. Sirve igual para la TCL del piloto.

## Antes

1. Instalar la versión de prueba con `tool/tv.sh instalar depuracion`: es la que anota los nombres
   de los archivos en el registro (D-23), y además le da a la app los permisos de fotos, videos y
   música (D-22, D-23). Sin ellos, «Tu USB» dice «Esta memoria no se puede leer en esta TV».
2. Agrandar el registro de Android: **Ajustes → Opciones de desarrollador → «Tamaños del búfer del
   registrador» → 8 M** (el Xiaomi no deja más). Con el de 256 K que trae, el registro se pisa en
   uno o dos minutos y se pierde la prueba.
3. Preparar las memorias. Lo ideal es una en cada formato: **FAT32**, **exFAT** y, si hay,
   **NTFS**. En cada una:
   - fotos de un celular, en **JPG** y en **HEIC** (las de iPhone);
   - un video de celular, en **MP4** o **MOV**;
   - un par de canciones, en **MP3** o **M4A**;
   - un video de más de 4K (si hay uno a mano), para ver el aviso de archivos que no se pueden ver;
   - una carpeta copiada desde una Mac (trae archivos `._foto.jpg`) o una memoria usada en
     Windows (trae `System Volume Information`).
4. Anotar qué tiene cada memoria (cuántas fotos, videos y canciones), para compararlo con lo que muestra la TV.

## En la TV

Con la depuración por USB y la inalámbrica **apagadas**:

1. Abrir la app.
2. Antes de conectar, entrar a **«Tu USB»** desde la barra lateral (Izquierda): debe decir «Conecta
   tu memoria USB a la TV para ver tus fotos, videos y música». Volver a **Inicio**.
3. Con el inicio a la vista, conectar la memoria, directo al Xiaomi (sin extensor). La app debe pasar
   sola a la sección **«Tu USB»** (D-23), con las filas **Videos**, **Fotos** y **Música** de la
   memoria. Anotar:
   - qué aparece: las filas o un aviso («Leyendo tu memoria…», «No hay fotos, videos ni música en
     esta memoria» o «Esta memoria no se puede leer en esta TV»);
   - cuánto tarda en aparecer;
   - si en cada fila lo más nuevo sale primero;
   - si se ven las miniaturas;
   - si arriba de las filas (o debajo del aviso) dice «N archivos no se pueden ver en esta TV», y si
     N es correcto;
   - si aparece algún archivo basura (`._foto`, cosas de `System Volume Information`).
4. Abrir una foto JPG, una HEIC, un video y una canción de la memoria. Anotar si abren, si la
   presentación de fotos suena con la música del hotel de fondo y si la canción de la USB suena.
5. **Retirar la memoria** con una foto de ella abierta: la app debe volver a la sección **Inicio**,
   con el foco en una tarjeta. Conectarla otra vez y retirarla con la sección «Tu USB» a la vista:
   lo mismo.
6. Con un video del hotel en pantalla, conectar la memoria: el video debe seguir y, al volver con
   Atrás, la app debe quedar en Inicio, con el punto en «Tu USB» de la barra.
7. Repetir con cada memoria.

## Después

1. Encender **solo la depuración inalámbrica** y conectar desde la PC: `tool/tv.sh conectar` (si
   pide autorización, aceptarla en la TV).
2. Guardar el registro **enseguida**: Android lo guarda en memoria y lo va pisando con el tiempo.

   ```bash
   adb -s 192.168.31.242:5555 logcat -d -s Usb:* Fondo:* Reproductor:* Musica:* Contenido:* > docs/pruebas/usb-registro.txt
   ```

3. En el registro, la etiqueta `Usb` dice:
   - `Escuchando las memorias USB; permiso=true`: la app arrancó y tiene los permisos;
   - `Volumen <id> estado=<estado> montado=<sí o no>`: lo que Android contó de la memoria; un
     `estado=unmountable` es un formato que la TV no lee;
   - `N videos, M fotos, K canciones, R rechazados en X ms`: lo que encontró y cuánto tardó;
   - una línea por archivo, con su tipo o el motivo del rechazo (solo en la versión de prueba);
   - `Retirada <id>`: cuando se sacó la memoria.
4. Con lo anotado y el registro, completar D-18: qué formatos lee, si los permisos alcanzan y
   cuánto tarda en leer la memoria.
