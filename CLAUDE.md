# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Qué es

App de Android TV / Google TV para las habitaciones de los hoteles Inkaterra (`apps/tv/`, Kotlin,
Compose for TV, Media3, Coil). La desarrolla una sola persona, Adrián, por sprints: el alcance
está en `docs/plan-de-desarrollo.md`, los pasos de cada sprint en `docs/planes/sprint-N.md` y
los informes en `docs/informes/`. Las decisiones D-01 a D-23 de `docs/decisiones.md` son
obligatorias: antes de cambiar un comportamiento, buscar la decisión que lo fija.

Todo va en español: código (salvo lo que impone Android, como `MainActivity`), comentarios,
textos, commits y documentos. Cada archivo empieza con un comentario breve de qué hace y qué
reglas sigue; los comentarios explican el porqué.

## Comandos

Desde `apps/tv/` (en Windows, en Git Bash):

```bash
./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug assembleRelease   # lo mismo que el CI
./gradlew testDebugUnitTest --tests '*NavegacionTest'                            # una clase
./gradlew testDebugUnitTest --tests '*NavegacionTest.izquierdaAbreLaBarra'       # una prueba
./gradlew ktlintFormat                                                           # arregla el formato
```

`lintDebug` tiene los avisos como errores. Correr el CI completo antes de cada commit y
encadenar el commit con `&&`, para que no se haga si algo falla.

Desde la raíz, para la TV de pruebas (el Xiaomi TV Box S, `192.168.31.242:5555`; otra TV con
`TV=ip:5555`):

```bash
tool/tv.sh conectar
tool/tv.sh instalar               # versión optimizada (release con R8): la que se usa para juzgar la fluidez
tool/tv.sh instalar depuracion    # versión de prueba: registra los nombres de los archivos de la USB
tool/tv.sh probar [pe.inkaterra.tv.Clase]
EXCLUIR=pe.inkaterra.tv.sistema.VolumenesUsbTest tool/tv.sh probar   # todas menos la de USB
tool/copiar-contenido.sh          # contenido de muestra (necesita ffmpeg)
tool/videos-prueba.sh             # videos 4K de prueba que usan FlujoSprint2Test y FlujoSprint3Test
```

- Las pruebas en la TV van con `tool/tv.sh probar`, nunca con `connectedDebugAndroidTest`: con la
  TV por red, esa tarea de AGP 9.4 falla aunque las pruebas pasen.
- `MSYS_NO_PATHCONV=1` solo para `adb` con rutas de la TV; nunca exportado al llamar a
  `tool/tv.sh` o a `gradlew`, porque rompe el classpath de Gradle.
- La versión de depuración es varias veces más lenta (arranque de 1,5 a 1,9 s contra 0,3 a 0,7 s;
  61 % de cuadros lentos contra 2 %). La fluidez se mide con la optimizada.

## Arquitectura

Capas en `pe.inkaterra.tv` (detalle en `docs/arquitectura.md`):

- `dominio/`: Kotlin puro, sin Android (reglas de contenido, reproducción, música). Se prueba
  sin Robolectric.
- `datos/`: la biblioteca del hotel (`getExternalFilesDir("contenido")`, con `promociones/` como
  Destacados), las miniaturas y la USB del huésped.
- `sistema/`: lo que toca Android TV (volúmenes USB, idioma por app, protector de pantalla),
  siempre detrás de una interfaz con un doble para las pruebas.
- `pantallas/`: Compose for TV. No toca archivos ni red.
- `app/`: `MainActivity` arma a mano un `Contenedor` (sin Hilt) y se lo pasa a `Navegacion`. Las
  miniaturas, las posiciones de los videos y la USB son una sola instancia por proceso, porque la
  actividad se recrea al cambiar el idioma.

Flujo principal:

- `InicioModelo` carga la biblioteca una vez, escucha `UsbDelHuesped.estado` y arma dos
  secciones: Inicio (filas del hotel) y Tu USB (filas de la memoria o un aviso). Guarda la
  sección y la tarjeta con el foco de cada una.
- `InicioPantalla` dibuja la barra lateral (D-23) y la sección elegida.
- Las rutas `video/{fila}/{indice}`, `fotos/{fila}/{indice}` y `musica/{fila}/{indice}` usan
  `TipoFila.name`. `DeLaFila`, en `Navegacion.kt`, lee la lista de la fila desde el modelo de la
  entrada `"inicio"`. Al salir, avisa al modelo qué fue lo último que se vio, para devolver el
  foco a esa tarjeta (D-21). Si la fila desaparece (por ejemplo, al retirar la USB), vuelve sola.
- La biblioteca valida cada video con `dominio/ReglasContenido.kt`: hasta 4K, H.264, HEVC o VP9,
  un decodificador para su perfil en la TV y 62 Mbps como máximo (D-13, D-19, D-21). Los
  rechazados quedan en el registro con la etiqueta `Contenido`.
- El video va en un `SurfaceView` (`SURFACE_TYPE_SURFACE_VIEW`), la única superficie que
  mantiene el 4K sin perder cuadros en el Xiaomi.

USB del huésped (D-10, D-18, D-22, D-23):

- Se lee en su lugar y no se escribe nada en disco: miniaturas solo en memoria
  (`MiniaturasEnMemoria`). Al retirarla se olvidan sus posiciones y se vacía la caché en memoria
  de Coil.
- Los nombres de sus archivos van al registro (etiqueta `Usb`) solo en la versión de depuración;
  para eso está `nombreParaRegistro()`.
- Los permisos de medios se dan por adb al instalar; la app nunca muestra el diálogo.
- El Xiaomi solo lee memorias con la depuración por USB **y** la inalámbrica apagadas, así que
  sin adb: la prueba es a mano, con `docs/pruebas/usb-a-mano.md`. `VolumenesUsbTest` no se puede
  correr ahí.

## Pruebas

- `src/test/` (PC): JUnit y Robolectric con SDK 34, usando las reglas
  `androidx.compose.ui.test.junit4.v2`. Los dobles están en `DoblesPantallas.kt`,
  `DoblesBiblioteca.kt` y `DoblesUsb.kt`.
- `src/androidTest/` (TV): `FlujoSprintNTest` recorre cada sprint con teclas como las del control,
  sobre una carpeta `contenido-prueba/` que se pasa con `MainActivity.EXTRA_RAIZ_CONTENIDO`.
- Datos de prueba siempre inventados («Atardecer en Machu Picchu.mp4», «Reserva Amazónica
  01.jpg»).
- **Reloj de Compose:** los `delay` de los `LaunchedEffect` corren con el reloj de la prueba.
  `Thread.sleep` y `waitUntil` no lo mueven; hace falta `mainClock.advanceTimeBy`. Los avisos de
  ExoPlayer y de Coil sí llegan en tiempo real.
- **Atrás:** en Robolectric, `performKeyInput { pressKey(Key.Back) }` solo llega a Compose. Para
  el `BackHandler` hay que llamar a `activity.onBackPressedDispatcher.onBackPressed()`.
- En la TV, Compose usa el primer Atrás para sacar el foco de un grupo cuando el contenedor puede
  tener el foco. Por eso las pantallas con raíz enfocable (el reproductor de video) atienden Atrás
  en `onPreviewKeyEvent`.
- Una captura por adb sale negra al pasar de un video H.264 a uno HEVC en el Amlogic, pero en la
  TV se ve bien: es una limitación de la captura.

## Forma de trabajo

- Cada paso del sprint va en su rama (`feat/<capa>-<tema>`, `fix/…`, `docs/…`), que sale de
  `sprint-N/integracion` y vuelve con `git merge --no-ff`. Al cerrar el sprint, PR hacia `main`.
- Commits `tipo(ámbito): resumen (paso N.M)`, en minúscula, en presente, de hasta 72 caracteres y
  **sin firma de Claude**.
- Subir a GitHub o abrir un PR solo con el visto bueno de Adrián.
- Las versiones de las dependencias son exactas en `apps/tv/gradle/libs.versions.toml`; subir o
  agregar una es un commit propio.
- Lo visible para el huésped va en «## [Sin publicar]» del `CHANGELOG.md`.
- La TV de pruebas es el Xiaomi personal de Adrián: no restablecerla de fábrica ni cambiar sus
  ajustes de sistema, y dejar como estaba lo que se toque para una prueba.
- Si se desinstala la app, la carpeta de contenido tiene que crearla la app. `tool/copiar-contenido.sh`
  la abre antes de copiar, porque una carpeta creada por adb queda ilegible para ella.
