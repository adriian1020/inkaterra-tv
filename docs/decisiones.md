# Decisiones

Las D-01 a D-12 vienen del plan de desarrollo ([plan-de-desarrollo.md](plan-de-desarrollo.md)).
Desde la D-13 se anota lo que se averigua en la TV durante el sprint 0, y desde la D-20 lo que
se decide durante los sprints siguientes.

## D-01

- **Qué:** App de TV en Kotlin con Compose for TV y Media3
- **Por qué:** Es la opción con mejor rendimiento de video y la que da acceso directo al sistema

## D-02

- **Qué:** Siempre se reproduce desde la memoria interna de la TV, también en la Fase 2
- **Por qué:** Las sedes remotas (Amazonía, Machu Picchu, Cabo Blanco) tienen internet débil; la nube solo sincroniza

## D-03

- **Qué:** Máximo 4K (3840 × 2160). Se valida al importar (Fase 1) y al subir (Fase 2)
- **Por qué:** Ningún archivo que la TV no pueda reproducir llega a la TV

## D-04

- **Qué:** Instalación por APK, sin Google Play
- **Por qué:** Son TVs del hotel; en la Fase 2 la app se actualiza desde el panel

## D-05

- **Qué:** La app no depende de la fuente de IPTV: recibe una lista de canales con su dirección
- **Por qué:** El hotel puede cambiar de proveedor o usar uno distinto por sede sin cambiar la app

## D-06

- **Qué:** Panel y API en TypeScript, desplegados en el servidor de Lima
- **Por qué:** Un solo lenguaje para una sola persona; se reutiliza la operación de asistencia

## D-07

- **Qué:** Archivos en almacenamiento de objetos (Cloudflare R2)
- **Por qué:** El disco del servidor no alcanza y R2 no cobra la descarga de cada TV

## D-08

- **Qué:** Un solo repositorio privado, `adriian1020/inkaterra-tv`, para la app, el panel y la API (`apps/tv`, `apps/panel`, `services/api`)
- **Por qué:** Una sola persona: un lugar para el contrato, la documentación y el CI

## D-09

- **Qué:** Sin horario de silencio: las promociones siguen hasta una tecla o hasta apagar la TV
- **Por qué:** Pedido del proyecto. Los anuncios dentro de los canales se definen en la Fase 3

## D-10

- **Qué:** La USB del huésped se lee sin copiar nada y sin guardar rastro
- **Por qué:** Son fotos personales; el próximo huésped no debe ver nada

## D-11

- **Qué:** El mismo contenido del hotel en todas las TVs (Fase 1)
- **Por qué:** Pedido del proyecto. La Fase 2 permite separar por hotel si hace falta

## D-12

- **Qué:** Marca tomada de la web: verde `#5A6A40`, Josefin Sans y Cinzel en lugar de Trajan Pro
- **Por qué:** Trajan Pro es de pago; Cinzel es libre y parecida

## Herramientas (sprint 0)

- **AGP 9.4.1, Gradle 9.7.1 y `compileSdk` 37.** El plan decía AGP 9.1.0 con Gradle 9.3.1 y
  `compileSdk` 36, pero Compose BOM 2026.09.00 exige compilar contra Android 37, y AGP 9.1 llega
  hasta 36. `targetSdk` también sube a 37, porque lint lo exige; las TVs con Android 11 a 14 no
  reciben esos cambios. `minSdk` sigue en 30.
- **Pruebas en la TV con `tool/tv.sh probar`.** Con la TV conectada por red (`adb connect`),
  `connectedDebugAndroidTest` de AGP 9.4 termina en error aunque todas las pruebas pasen. El
  script instala los dos APK y corre `am instrument`; falla si la salida no dice `OK (`.

## D-13. Tasa máxima de video: 60 Mbps

- **Qué:** se aceptan videos H.264 o HEVC de hasta 3840 × 2160 y **60 Mbps**. Es el límite de
  `TASA_MAXIMA_BPS` en las reglas de contenido (sprint 1).
- **Por qué:** en el Xiaomi TV Box S (paso 0.4), H.264 y HEVC a 20, 40 y 60 Mbps se
  reprodujeron 20 s sin perder ni un cuadro, igual que HEVC a 60 fps (40 Mbps) y HEVC de 10 bits
  a 120 Mbps. 60 Mbps es la tasa más alta probada en los **dos** formatos; deja margen para la
  TCL, que tiene otro chip, y un video de hotel bien exportado rara vez pasa de 40 Mbps.
- **Pendiente:** repetir `Reproduccion4kTest` en la TCL del piloto (sprint 5) y con una pantalla
  4K conectada; hoy el Xiaomi está en una pantalla 1080p, así que se midió la decodificación 4K,
  no la salida.

## D-14. Pantalla principal: Google TV no la cede a una app normal

- **Qué se probó (paso 0.5, Xiaomi con Google TV 14):** la app con el filtro `HOME` y
  `adb shell cmd package set-home-activity pe.inkaterra.tv/.app.MainActivity`. El comando
  responde «Success», pero la tecla Inicio sigue abriendo el inicio de Google TV
  (`com.google.android.apps.tv.launcherx`).
- **Qué queda para decidir antes del sprint 5:**
  1. **App normal** (lo de hoy): el huésped la abre desde la fila de apps; las promociones
     salen con el protector de pantalla (D-16).
  2. **Equipo administrado** (`device owner`): la app manda en la TV (pantalla principal,
     arranque, actualizaciones sin preguntar). Exige restablecer de fábrica cada TCL y
     configurarla sin cuenta de Google antes de `adb shell dpm set-device-owner`. No se probó:
     no se restablece el equipo de Adrián.
  3. **Desactivar el inicio de Google TV** con `adb shell pm disable-user --user 0
     com.google.android.apps.tv.launcherx`, para que Android use la app como inicio. Es
     reversible (`pm enable`), pero deja la TV sin el inicio de Google y sin las
     recomendaciones de YouTube o Netflix. No se probó en el equipo de Adrián.

## D-15. Arranque al encender: bloqueado en Google TV 14

- **Qué se probó:** un receptor de `BOOT_COMPLETED` que abre la app. Android lo despierta, pero
  bloquea la pantalla («Background activity launch blocked», `BAL_BLOCK`).
- **Con el permiso de mostrarse sobre otras apps** (`appops set … SYSTEM_ALERT_WINDOW allow`):
  Google TV lo devuelve a `default` al reiniciar y el arranque sigue bloqueado.
- **Decisión:** no se agrega el receptor. Abrir la app al encender solo es posible con la
  opción 2 o 3 de D-14, y esa decisión llega antes del sprint 5.

## D-16. Protector de pantalla: sí, configurado por adb en cada TV

- **Qué se probó (paso 0.6, Xiaomi con Google TV 14):** `ProtectorPantalla` (un `DreamService`)
  activado con `adb shell settings put secure screensaver_components
  pe.inkaterra.tv/.sistema.protector.ProtectorPantalla`. A 1 minuto sin uso en el inicio de
  Google TV salió «Inkaterra TV» sobre el verde (`mWakefulness=Dreaming`).
- **Decisión:** las promociones fuera de la app (CU-05, variante 1a) se muestran con este
  protector. Google TV no deja elegirlo desde sus ajustes, así que al instalar cada TV se
  corre:

  ```bash
  adb shell settings put secure screensaver_components pe.inkaterra.tv/.sistema.protector.ProtectorPantalla
  adb shell settings put secure screensaver_enabled 1
  adb shell settings put system screen_off_timeout 300000   # 5 minutos sin uso
  ```

- **Pendiente:** comprobarlo en la TCL del piloto (sprint 5); en algunas versiones de Google TV
  el fabricante bloquea protectores de terceros.

## D-17. Suspensión: la app la evita; fuera de la app hay que desactivarla

- **Dentro de la app:** con `mantenerPantallaEncendida(activity, true)`
  (`FLAG_KEEP_SCREEN_ON`), la TV siguió encendida 3 minutos con la suspensión a 2.
- **Con el protector en pantalla:** la suspensión manda. Con `sleep_timeout` a 2 minutos la
  TV se apagó a los 2 minutos aunque el protector estaba activo, y al apagarse corta la red
  (adb queda desconectado). Con `sleep_timeout` en `-1` («nunca») el protector siguió 4
  minutos sin apagarse.
- **Decisión:** para que las promociones sigan sin fin (D-09), al instalar cada TV también se
  corre `adb shell settings put secure sleep_timeout -1`. Lo que apague la TV de noche lo hace
  el huésped con el control.
- **Apagado por inactividad (`attentive_timeout`):** Android puede apagar la TV tras horas sin
  teclas aunque la app pida la pantalla encendida. En el Xiaomi está desactivado (`-1`); en
  la TCL hay que leerlo (`adb shell settings get secure attentive_timeout`) y, si tiene valor,
  ponerlo en `-1` al instalarla. Pendiente del piloto.
- **Alcance de la medición:** la app al frente se midió 3 minutos (el plan pedía 5); las
  promociones de horas se comprueban en el piloto del sprint 5.

## D-18. Memorias USB: código listo, prueba en la TV pendiente

- **Qué hay:** `VolumenesUsb` lista las memorias montadas (`StorageManager`, solo las
  extraíbles) y avisa cuando se conectan o se retiran. Lee la carpeta de la memoria directo,
  sin copiar nada (D-10). `VolumenesUsbTest` comprueba en la TV que se lean los archivos y que
  las fotos HEIC se abran.
- **Qué pasó en el Xiaomi (28 de octubre):** la memoria de prueba (Dahua U156 de 8 GB, FAT32,
  que la PC lee bien) no apareció ni como dispositivo en el puerto USB de la caja, conectada
  directo; Android solo veía el adaptador wifi interno. No es un problema de formato: con un
  formato que no lee, Android igual mostraría el disco.
- **Pendiente para el sprint 3 (USB del huésped):** repetir con otra memoria, con el Xiaomi en
  su propio cargador o con un hub USB con alimentación, y luego en la TCL. Hasta entonces no
  se sabe qué formatos lee (FAT32, exFAT, NTFS) ni si hace falta pedir permisos de fotos y
  videos.
- **También pendiente:** si la lectura directa de la carpeta falla (Android 11 en adelante
  limita el acceso), leer por `MediaStore` y anotar el permiso que hace falta
  (`READ_MEDIA_IMAGES` y `READ_MEDIA_VIDEO`); y retirar la memoria mientras la app la lee, para
  confirmar que no se cierra.

## D-19. Se acepta WEBM con VP9

- **Qué:** además de MP4, MKV y MOV con H.264 o HEVC, se aceptan videos **WEBM con VP9**
  (`video/x-vnd.on2.vp9`), con los mismos límites: hasta 3840 × 2160 y 60 Mbps (D-13). Vale
  para el contenido del hotel y para la USB del huésped.
- **Por qué:** pedido de Adrián. Los celulares Android y muchas descargas de internet graban o
  entregan WEBM con VP9 (la memoria de prueba traía uno). En el Xiaomi, VP9 4K a 20, 40 y
  60 Mbps se reprodujo sin perder cuadros con `c2.amlogic.vp9.decoder`, igual que H.264 y HEVC.
- **No se acepta AV1:** el Xiaomi lo decodifica, pero solo hasta 40 Mbps declarados, y la TCL
  podría no tenerlo. Se revisa si algún video real lo necesita.
- **Pendiente:** repetir la prueba de VP9 en la TCL del piloto (sprint 5).

## D-20. Inicio estilo Netflix: área destacada y filas con miniaturas

- **Qué:** el inicio deja de ser tres tarjetas de texto (Videos, Fotos y Música) con una lista
  de títulos en cada sección, y pasa a ser como el de Netflix en la TV:
  - **Área destacada** arriba: imagen grande, título y tipo de lo que tiene el foco. Cambia
    cuando el foco se queda medio segundo en una tarjeta, para que no parpadee al recorrer
    rápido. Al abrir la app, el foco está en el primer destacado.
  - **Filas horizontales** de tarjetas 16:9 con miniatura y título: **Destacados**, **Videos**,
    **Fotos** y **Música**. Las flechas recorren la fila a los lados y cambian de fila arriba y
    abajo. La tarjeta con el foco crece un poco y lleva borde verde `#5A6A40`. Una fila sin
    contenido no se muestra; si no hay nada, el área destacada dice «Todavía no hay contenido».
  - **Destacados** sale de la carpeta `promociones/`: marketing carga una sola vez lo que se
    ve en la portada y en el bucle de los 5 minutos (CU-05).
  - **Sin pantallas de sección:** al elegir una tarjeta se abre directo (el reproductor y el
    visor llegan en el sprint 2). Atrás vuelve a la misma tarjeta. En el inicio, Atrás con el
    foco en otra fila sube a la primera; en la primera fila sale de la app, como cualquier app
    de Google TV.
  - **Miniaturas:** video, un cuadro del propio video; foto, la misma foto reducida; música,
    la carátula que traiga el archivo o, si no trae, una imagen de la marca. Una sola miniatura
    de 1280 × 720 en JPEG por archivo, que sirve para la tarjeta y para el área destacada. Se
    generan en segundo plano la primera vez y se guardan en la caché de la app, con la ruta, el
    tamaño y la fecha del archivo como clave; mientras tanto se ve un recuadro verde.
  - **En el sprint 3** se suma la fila «Tu USB», segunda después de Destacados, que aparece al
    conectar la memoria y se va al retirarla. Sus miniaturas no se guardan en disco (D-10).
- **Por qué:** pedido a Adrián de que la app sea «estilo Netflix», sin más detalle. Se tomó lo
  que Netflix hace en la TV y encaja en una habitación de hotel.
- **Qué no se toma de Netflix, a propósito:**
  - «Seguir viendo»: el siguiente huésped vería lo que miró el anterior.
  - Perfiles: cada habitación es una TV con huéspedes que cambian.
  - Ficha con descripción antes de reproducir: en la Fase 1 solo existe el nombre del archivo.
    Las descripciones pueden llegar con el panel de la Fase 2.
  - Avance de video que se reproduce solo en el área destacada: competiría con el 4K por el
    decodificador. Se puede probar en el Xiaomi más adelante.
- **Fase 2:** el panel genera las miniaturas al subir (sprint 7) y la TV usa esas en vez de
  generarlas.
- **Pendiente:** mostrar una maqueta a quien pidió el estilo Netflix y confirmar que es lo que
  esperaba.
