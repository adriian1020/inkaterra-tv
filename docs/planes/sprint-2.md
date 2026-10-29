# Sprint 2 — Video y fotos: plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendada) o superpowers:executing-plans para implementar este plan paso a paso. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** que al elegir un video se reproduzca en 4K con el control (pausar, adelantar, retroceder, retomar y seguir con el siguiente), y que al elegir una foto arranque una presentación con fundidos, sin que un archivo malo trabe nada.

**Arquitectura:**
- Las reglas nuevas (sin decodificador, posiciones para retomar y saltos de 10 s) son Kotlin puro en `dominio/`.
- El reproductor (`pantallas/video/`) usa Media3 con `PlayerSurface` sobre un `SurfaceView`, y una barra propia en Compose. El visor (`pantallas/fotos/`) muestra la foto completa con Coil.
- Las dos pantallas reciben la lista de la fila desde `InicioModelo`, que ahora también guarda el foco del inicio para devolverlo a la tarjeta de lo último que se vio.

**Tecnología:**
- Lo del sprint 1, más `androidx.media3:media3-exoplayer` (pasa de las pruebas a la app) y `androidx.media3:media3-ui-compose`, los dos 1.11.1.

**Spec:** [docs/plan-de-desarrollo.md](../plan-de-desarrollo.md): Fase 1, sprint 2, «Reglas de contenido», CU-02 y CU-03; [docs/decisiones.md](../decisiones.md): D-21 (reproducción y presentación), D-13, D-19 y D-20.

**Depende del sprint 1:** biblioteca, miniaturas, inicio, `Navegacion`, `InicioModelo`, `tool/copiar-contenido.sh` y los videos de `tool/videos-prueba.sh`.

## Restricciones globales

- Las mismas del sprint 1 (idioma, comentarios, commits, ramas, versiones exactas, chequeos del CI antes de cada commit, ningún texto visible en el código). Las ramas salen de `sprint-2/integracion`, que sale de `main`.
- Retomar (D-21): en memoria del proceso, nunca en disco. Se guarda la posición si es de 5 000 ms o más y faltan más de 15 000 ms para el final; si no, se olvida.
- Video (D-21): Enter, `DPAD_CENTER` o `MEDIA_PLAY_PAUSE` pausan y siguen; Izquierda o `MEDIA_REWIND` retroceden 10 000 ms; Derecha o `MEDIA_FAST_FORWARD` adelantan 10 000 ms, sin pasar de 0 ni del final. La barra se ve 5 s después de cada tecla y siempre en pausa. Al terminar, el siguiente video de la fila (saltando fotos); después del último, vuelve al inicio.
- Fotos (D-21): la presentación arranca sola en la foto elegida, 8 000 ms por foto, fundido de 1 000 ms, en bucle por las fotos de la fila. Izquierda y Derecha cambian (y dan la vuelta); Enter pausa y sigue.
- Errores (D-21): «No se puede reproducir este video» 3 000 ms y sigue; «No se puede abrir esta foto», y en la presentación pasa a los 3 000 ms.
- Al volver al inicio, el foco va a la tarjeta de lo último que se vio (el último video o la última foto).
- Superficie de video: `SURFACE_TYPE_SURFACE_VIEW`, la única que mantiene el 4K por hardware sin perder cuadros.
- Datos de prueba inventados, como en el sprint 1.

## Foco de revisión

1. **Un video que la TV no decodifica** (H.264 4:4:4, un códec raro dentro de un MP4): la biblioteca lo rechaza antes de mostrarlo; si igual llega al reproductor (por ejemplo, un archivo cortado), se ve el mensaje y sigue el siguiente, sin cerrar la app. Pruebas `rechazaSinDecodificador` (paso 2.1), `perfilCuatroCuatroCuatroNoSeDecodifica` (paso 2.1, en la TV) y la revisión en el Xiaomi del paso 2.3.
2. **Atrás en medio del video y volver a entrar:** retoma desde donde estaba; si estaba en los primeros 5 s o en los últimos 15 s, empieza de cero. Pruebas `guardaYRetoma`, `alPrincipioNoSeGuarda` y `alFinalSeOlvida` (paso 2.2) y `FlujoSprint2Test` (paso 2.6).
3. **Adelantar cerca del final o retroceder cerca del inicio:** no pasa del final ni de 0. Pruebas `adelantarNoPasaDelFinal` y `retrocederNoBajaDeCero` (paso 2.2).
4. **Una fila con una sola foto, o una foto dañada:** la presentación no falla (se queda en esa foto o muestra el mensaje y sigue). Pruebas `unaSolaFotoSeQueda` y `fotoDanadaMuestraMensajeYSigue` (paso 2.4).
5. **Volver al inicio después de varios videos seguidos o de recorrer fotos:** el foco queda en la tarjeta de lo último que se vio, y las demás filas conservan su desplazamiento. Pruebas `alVolverDeLasFotosElFocoVaALaUltima` y `alVolverLasFilasConservanSuPosicion` (paso 2.5).

---

### Paso 2.1: videos sin decodificador y registro de rechazados

**Rama:** `feat/dominio-sin-decodificador`

**Archivos:**
- Modificar: `dominio/ReglasContenido.kt`, `datos/biblioteca/LectorMetadatos.kt`, `pantallas/inicio/InicioModelo.kt` y `tool/videos-prueba.sh`.
- Prueba: `dominio/ReglasContenidoTest.kt` y, en la TV, `androidTest/.../sistema/LectorMetadatosEquipoTest.kt`.

**Interfaces:**
- Produce:
  - `MetadatosVideo` suma `val decodificable: Boolean = true` (último parámetro, para no romper a quien ya lo construye);
  - `MotivoRechazo.SinDecodificador(val mime: String?)`;
  - `evaluarVideo` evalúa en este orden: resolución, códec, decodificable y tasa.
- `LectorMetadatosAndroid` llena `decodificable` con `MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(formatoDeLaPista) != null`, usando el `MediaFormat` de la pista de video que ya lee `MediaExtractor`.
- `InicioModelo` escribe cada rechazado en el registro: `Log.w("Contenido", "<nombre>: <motivo>")`.

- [ ] **Paso 1: escribir las pruebas que fallan**

```kotlin
@Test fun rechazaSinDecodificador() =
    assertEquals(MotivoRechazo.SinDecodificador("video/avc"), evaluarVideo(MetadatosVideo(1920, 1080, "video/avc", 8_000_000, decodificable = false)))
@Test fun primeroLaResolucion() =
    assertEquals(MotivoRechazo.ResolucionExcedida(7680, 4320), evaluarVideo(MetadatosVideo(7680, 4320, "video/avc", null, decodificable = false)))
```

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `cd apps/tv && ./gradlew testDebugUnitTest --tests '*ReglasContenidoTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar el campo, el motivo, el orden, la lectura con `MediaCodecList` y el registro en `InicioModelo`.** En `tool/videos-prueba.sh`, agregar el caso `"h264-444.mp4 libx264 8 30 yuv444p"` en 1920 × 1080 (para eso, el tamaño pasa a ser parte de cada caso; los demás siguen en 3840 × 2160).

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*ReglasContenidoTest'`
Expected: PASS, 14 pruebas.

- [ ] **Paso 5: probar el lector en el Xiaomi**

`LectorMetadatosEquipoTest`, con los videos de `getExternalFilesDir("prueba-4k")`:
- `perfilCuatroCuatroCuatroNoSeDecodifica`: `h264-444.mp4` da `decodificable == false`.
- `hevcYVp9SeDecodifican`: `hevc-20m.mp4` y `vp9-20m.webm` dan `decodificable == true`.

Run: `tool/videos-prueba.sh && tool/tv.sh probar pe.inkaterra.tv.sistema.LectorMetadatosEquipoTest`
Expected: `OK (2 tests)`.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(dominio): rechazar videos sin decodificador en la tv (paso 2.1)"
```

---

### Paso 2.2: posiciones para retomar, saltos y siguiente video

**Rama:** `feat/dominio-reproduccion`

**Archivos:**
- Crear: `dominio/Reproduccion.kt` y `pantallas/video/ListaVideos.kt` (Kotlin puro: usa `Elemento`, que vive en `datos/`).
- Prueba: `dominio/ReproduccionTest.kt` y `pantallas/video/ListaVideosTest.kt`.

**Interfaces:**
- Produce, en `dominio/Reproduccion.kt`:
  - `class PosicionesVideo { fun guardar(ruta: String, posicionMs: Long, duracionMs: Long); fun posicion(ruta: String): Long }`, con `posicion` en 0 si no hay nada guardado; seguro entre hilos (`ConcurrentHashMap`);
  - `enum class AccionVideo { ALTERNAR, ADELANTAR, RETROCEDER }`;
  - `fun destinoSalto(accion: AccionVideo, posicionMs: Long, duracionMs: Long): Long?` (`null` para `ALTERNAR`);
  - `fun textoTiempo(ms: Long): String`: `m:ss`, y `h:mm:ss` desde una hora;
  - `const val SALTO_MS = 10_000L`, `INICIO_MINIMO_MS = 5_000L` y `FINAL_MINIMO_MS = 15_000L`.
- Produce, en `pantallas/video/ListaVideos.kt`: `fun siguienteVideo(fila: List<Elemento>, actual: Int): Int?`, el índice del próximo `VIDEO` después de `actual`, o `null`.

- [ ] **Paso 1: escribir las pruebas que fallan**

`ReproduccionTest`:
- `guardaYRetoma`: guardar 60 000 de 200 000 da `posicion == 60_000`.
- `alPrincipioNoSeGuarda`: guardar 4 000 de 200 000 da 0.
- `alFinalSeOlvida`: guardar 60 000 y después 190 000 de 200 000 da 0.
- `adelantarNoPasaDelFinal`: `destinoSalto(ADELANTAR, 195_000, 200_000) == 200_000`.
- `retrocederNoBajaDeCero`: `destinoSalto(RETROCEDER, 4_000, 200_000) == 0`.
- `saltosDeDiezSegundos`: `ADELANTAR` desde 50 000 da 60 000, `RETROCEDER` da 40 000 y `ALTERNAR` da `null`.
- `textoDelTiempo`: 65 000 da `"1:05"`; 3 725 000 da `"1:02:05"`; 0 da `"0:00"`.

`ListaVideosTest` (con `Elemento`s armados con `archivo()` de los dobles):
- `siguienteSaltaLasFotos`: en `[video, foto, video]`, desde 0 da 2.
- `despuesDelUltimoNoHayMas`: en `[video, video]`, desde 1 da `null`.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*ReproduccionTest' --tests '*ListaVideosTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `Reproduccion.kt` y `ListaVideos.kt`.**

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*ReproduccionTest' --tests '*ListaVideosTest'`
Expected: PASS, 9 pruebas.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(dominio): retomar, saltos de 10 s y siguiente video (paso 2.2)"
```

---

### Paso 2.3: reproductor de video

**Rama:** `feat/pantallas-video`

**Archivos:**
- Crear: `pantallas/video/ReproductorPantalla.kt` y `pantallas/video/BarraVideo.kt`.
- Modificar: `gradle/libs.versions.toml` y `app/build.gradle.kts` (`media3-exoplayer` a `implementation`, y `media3-ui-compose`), y las cadenas.
- Prueba: `test/.../pantallas/video/BarraVideoTest.kt` (Robolectric, `@Config(qualifiers = "es")`).

**Interfaces:**
- Consume: `PosicionesVideo`, `AccionVideo`, `destinoSalto` y `textoTiempo` (paso 2.2), `siguienteVideo` (paso 2.2) y `Elemento`.
- Produce:
  - `@Composable fun BarraVideo(titulo: String, posicionMs: Long, duracionMs: Long, pausado: Boolean, modifier: Modifier = Modifier)`: abajo, sobre un degradado del fondo, el título (`testTag("video-titulo")`), una barra de progreso verde `#5A6A40` y el texto `"<posición> / <duración>"` con `textoTiempo` (`testTag("video-tiempo")`). Si `pausado`, el texto `video_pausado` al lado del tiempo;
  - `@Composable fun ReproductorPantalla(videos: List<Elemento>, inicial: Int, posiciones: PosicionesVideo, alSalir: (Elemento) -> Unit)`:
    - un `ExoPlayer` por pantalla (creado con `remember`, liberado en `DisposableEffect`) que reproduce `videos[actual]` desde `posiciones.posicion(ruta)`;
    - `PlayerSurface(player, surfaceType = SURFACE_TYPE_SURFACE_VIEW)` a pantalla completa sobre fondo negro;
    - las teclas de las restricciones globales con `onPreviewKeyEvent` en un contenedor enfocable que pide el foco al abrirse. Cada tecla muestra la barra 5 s;
    - al terminar (`STATE_ENDED`), `siguienteVideo`; si es `null`, `alSalir(videos[actual])`;
    - ante `onPlayerError`, el texto `video_no_reproduce` 3 s y lo mismo que al terminar; el error queda en el registro (`Log.w("Reproductor", …)`);
    - Atrás (`BackHandler`) guarda la posición con `posiciones.guardar` y llama a `alSalir(videos[actual])`. Al pasar al siguiente, se guarda la del que termina (que por estar al final, se olvida);
    - al terminar o salir de cada video, escribe en el registro `Log.i("Reproductor", "<nombre> | mostrados=<n> | perdidos=<n>")` con `player.videoDecoderCounters`.

Textos:

| Clave | Español | Inglés |
| --- | --- | --- |
| `video_pausado` | En pausa | Paused |
| `video_no_reproduce` | No se puede reproducir este video | This video can't be played |

- [ ] **Paso 1: escribir las pruebas que fallan**

`BarraVideoTest`:
- `muestraTituloYTiempo`: `BarraVideo("Río Madre de Dios", 65_000, 200_000, pausado = false)` muestra «Río Madre de Dios» y `video-tiempo` dice `"1:05 / 3:20"`; no aparece «En pausa».
- `enPausaLoDice`: con `pausado = true` aparece «En pausa».

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*BarraVideoTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `BarraVideo` y `ReproductorPantalla`, y agregar las dependencias.** La posición que muestra la barra se lee del reproductor cada 500 ms mientras la barra está visible.

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*BarraVideoTest'`
Expected: PASS, 2 pruebas.

- [ ] **Paso 5: commit y unión** (la pantalla se conecta a la navegación en el paso 2.5).

```bash
git commit -m "feat(pantallas): reproductor de video con el control (paso 2.3)"
```

---

### Paso 2.4: visor de fotos con presentación

**Rama:** `feat/pantallas-fotos`

**Archivos:**
- Crear: `pantallas/fotos/VisorFotosPantalla.kt`.
- Modificar: las cadenas.
- Prueba: `test/.../pantallas/fotos/VisorFotosTest.kt` (Robolectric, `@Config(qualifiers = "es")`).

**Interfaces:**
- Consume: `Elemento`.
- Produce: `@Composable fun VisorFotosPantalla(fotos: List<Elemento>, inicial: Int, alSalir: (Elemento) -> Unit)`:
  - la foto con `coil3.compose.AsyncImage` y `ContentScale.Fit` sobre fondo negro; Coil la reduce al tamaño de la pantalla (fotos de más de 4K);
  - `Crossfade` de 1 000 ms entre fotos;
  - el título abajo (`testTag("foto-titulo")`), y el texto `presentacion_pausada` mientras está en pausa;
  - presentación: `LaunchedEffect(actual, pausada) { delay(8_000); siguiente }`, en bucle; con una sola foto no cambia;
  - Izquierda y Derecha cambian a mano (y reinician los 8 s); Enter o `DPAD_CENTER` pausa y sigue;
  - si Coil da error, el texto `foto_no_abre` (`testTag("foto-error")`) y, en presentación, la siguiente a los 3 000 ms;
  - Atrás llama a `alSalir(fotos[actual])`.

Textos:

| Clave | Español | Inglés |
| --- | --- | --- |
| `presentacion_pausada` | En pausa | Paused |
| `foto_no_abre` | No se puede abrir esta foto | This photo can't be opened |

- [ ] **Paso 1: escribir las pruebas que fallan**

Todas con `createAndroidComposeRule<ComponentActivity>()`, `TemaInkaterra` y fotos de `archivo("Reserva Amazónica 0N.jpg")` (rutas que no existen: Coil da error, así que en la PC todas las fotos «fallan»; lo que se prueba es la navegación, el tiempo y el mensaje). Teclas con `performKeyInput`; Atrás con el despachador de la actividad, como en `NavegacionTest`.
- `abreEnLaFotoElegida`: con `inicial = 1`, `foto-titulo` dice «Reserva Amazónica 02».
- `derechaPasaALaSiguienteYDaLaVuelta`: con 3 fotos desde la 3, Derecha muestra la 1.
- `pasaSolaALos8Segundos`: con `mainClock.autoAdvance = false` y una foto que no da error (un `Elemento` cuya ruta es un PNG real escrito en un `TemporaryFolder`), a los 7 900 ms sigue la misma y a los 8 100 ms la siguiente.
- `enterPausaLaPresentacion`: Enter muestra «En pausa» y a los 9 000 ms sigue la misma foto.
- `unaSolaFotoSeQueda`: con una foto, a los 9 000 ms no cambia y no hay error de índice.
- `fotoDanadaMuestraMensajeYSigue`: con la foto 1 inexistente, se ve «No se puede abrir esta foto» y a los 3 100 ms está la 2.
- `atrasDevuelveLaFotoActual`: Derecha y Atrás llaman a `alSalir` con la segunda foto.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*VisorFotosTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `VisorFotosPantalla`.**

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*VisorFotosTest'`
Expected: PASS, 7 pruebas.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(pantallas): visor de fotos con presentación (paso 2.4)"
```

---

### Paso 2.5: navegación, foco al volver y arreglos del inicio

**Rama:** `feat/pantallas-navegacion-medios`

**Archivos:**
- Modificar: `app/Navegacion.kt`, `app/Contenedor.kt`, `pantallas/inicio/InicioModelo.kt`, `pantallas/inicio/InicioPantalla.kt` y `pantallas/comun/Miniatura.kt`.
- Prueba: `test/.../pantallas/NavegacionTest.kt` y `test/.../pantallas/inicio/InicioModeloTest.kt` (Robolectric).

**Interfaces:**
- Consume: `ReproductorPantalla` (paso 2.3), `VisorFotosPantalla` (paso 2.4) y `PosicionesVideo` (paso 2.2).
- Produce:
  - `data class Foco(val fila: Int, val columna: Int)`; `InicioModelo` suma `var foco: Foco` (estado de Compose, empieza en `Foco(0, 0)`) y `fun enfocar(tipo: TipoFila, elemento: Elemento)`, que busca la columna de `elemento` en la fila `tipo` por su ruta. `InicioPantalla` deja de usar `rememberSaveable` para la tarjeta enfocada y lee y escribe `modelo.foco` (recibe `foco: Foco` y `alEnfocar: (Foco) -> Unit`);
  - `Contenedor` suma `val posiciones: PosicionesVideo`, una sola por proceso, como las miniaturas. `Navegacion` la recibe como parámetro nuevo (después de `miniaturas`);
  - rutas: `"video/{fila}/{indice}"` y `"fotos/{fila}/{indice}"` (`fila` es el `name` de un `TipoFila`). Los videos y las fotos de Destacados abren las mismas pantallas, con la fila Destacados filtrada por tipo; la música sigue en `"abrir/{ruta}"` hasta el sprint 3. Las pantallas de video y fotos leen las filas del `InicioModelo` de la entrada `"inicio"` (`navegador.getBackStackEntry("inicio")`); su `alSalir` llama a `modelo.enfocar(tipo, elemento)` y a `popBackStack()`;
  - **arreglo 1 (revisión del sprint 1):** cada `LazyRow` guarda su estado con `rememberSaveable(saver = LazyListState.Saver)` dentro de su ítem y lo registra en un mapa por fila, como los `FocusRequester`, para que `enfocar` lo desplace;
  - **arreglo 2 (revisión del sprint 1):** `Miniatura` vuelve a `Cargando` cuando cambia el elemento (`produceState` asigna `Cargando` antes de pedir la miniatura) y, mientras carga, lleva `testTag("miniatura-cargando")`. `AreaDestacada` lleva `testTag("area-destacada")`.

- [ ] **Paso 1: escribir las pruebas que fallan**

`InicioModeloTest`:
- `enfocarBuscaLaTarjetaPorSuRuta`: con la biblioteca de prueba, `enfocar(VIDEOS, «Río Madre de Dios»)` deja `foco == Foco(1, 1)`.

`NavegacionTest` (con `Navegacion(biblioteca, miniaturas, PosicionesVideo(), idioma)`):
- `alVolverDeLasFotosElFocoVaALaUltima`: con 3 fotos en la fila Fotos, Enter en `tarjeta-FOTOS-0`, Derecha y Atrás dejan el foco en `tarjeta-FOTOS-1`.
- `alVolverLasFilasConservanSuPosicion` (`@Config(qualifiers = "es-w960dp-h540dp")`, con 6 fotos y 1 canción): Derecha 4 veces, Abajo (Música), Enter y Atrás dejan el foco en `tarjeta-MUSICA-0` y `tarjeta-FOTOS-4` sigue a la vista (`assertIsDisplayed`).
- `areaDestacadaNoMuestraLaImagenAnterior`: con un generador que da imagen al primer destacado y se bloquea con el segundo, Derecha y 600 ms dejan un nodo `miniatura-cargando` con un ancestro `area-destacada` (`hasTestTag("miniatura-cargando") and hasAnyAncestor(hasTestTag("area-destacada"))`).

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*InicioModeloTest' --tests '*NavegacionTest'`
Expected: FAIL en las 4 nuevas.

- [ ] **Paso 3: implementar el foco en el modelo, las rutas, `posiciones` y los dos arreglos.** Ajustar `IdiomaTest` y las pruebas existentes a la nueva firma de `Navegacion`.

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest`
Expected: PASS en todas.

- [ ] **Paso 5: probar en el Xiaomi**

Run: `tool/copiar-contenido.sh && tool/tv.sh instalar`
Expected:
- Un video 4K se ve; Enter pausa (barra con «En pausa»), Izquierda y Derecha saltan 10 s, Atrás vuelve a su tarjeta y al entrar de nuevo retoma.
- Al terminar «Atardecer en Machu Picchu» sigue «Río Madre de Dios»; al terminar ese, vuelve al inicio con el foco en «Río Madre de Dios».
- Una foto abre la presentación, que pasa sola cada 8 s con fundido; Enter pausa; Atrás vuelve a la última foto vista.
- Las filas no saltan al volver y el área destacada no muestra imágenes viejas.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(pantallas): abrir videos y fotos y volver a su tarjeta (paso 2.5)"
```

---

### Paso 2.6: prueba integral en el Xiaomi, informe y PR

**Rama:** `test/flujo-sprint2`

**Archivos:**
- Prueba en el equipo: `androidTest/.../FlujoSprint2Test.kt`.
- Crear: `docs/informes/sprint-2.md`.
- Modificar: `CHANGELOG.md`, `docs/arquitectura.md` (reproductor y visor) y `docs/plan-de-desarrollo.md` (sprint 2 en los próximos pasos).

**Interfaces:**
- Consume: todo lo anterior y, como `FlujoSprint1Test`, una carpeta propia `contenido-prueba/` pasada con `MainActivity.EXTRA_RAIZ_CONTENIDO`. El idioma se fija en español con el `LocaleManager` antes de abrir, para no pasar por la pantalla de idioma.

- [ ] **Paso 1: escribir la prueba en el equipo**

`FlujoSprint2Test.videoYFotos`, con `hevc-20m.mp4` y `vp9-20m.webm` copiados de `prueba-4k/` a `contenido-prueba/videos/`, y 3 JPEG de 1920 × 1080 escritos por la prueba en `contenido-prueba/fotos/`:
1. Enter en `tarjeta-VIDEOS-0`; a los 4 s, Enter (pausa): `video-tiempo` marca un tiempo `t1` de 2 s o más, y 2 s después sigue igual.
2. Atrás: foco en `tarjeta-VIDEOS-0`. Enter otra vez: el tiempo arranca en `t1` o después.
3. Derecha dos veces (hasta el final del primero, que dura 25 s) y esperar: `video-titulo` dice «vp9-20m».
4. Atrás: foco en `tarjeta-VIDEOS-1`.
5. Abajo y Enter en `tarjeta-FOTOS-0`; a los 9 s `foto-titulo` muestra la segunda foto. Atrás: foco en `tarjeta-FOTOS-1`.

Teclas con `sendKeyDownUpSync`, esperas con `waitUntil` y tiempos leídos del texto de `video-tiempo`.

- [ ] **Paso 2: ver un 4K completo y medir**

Run: `tool/copiar-contenido.sh` (con `hevc-60m.mp4` agregado a `muestras/contenido/videos/`), reproducirlo entero en la app y leer `adb logcat -d -s Reproductor:I`.
Expected: `perdidos=0` en la línea de `hevc-60m`.

- [ ] **Paso 3: correr todo lo del CI y las pruebas en el Xiaomi**

Run: `(cd apps/tv && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug) && EXCLUIR=pe.inkaterra.tv.sistema.VolumenesUsbTest tool/tv.sh probar`
Expected: BUILD SUCCESSFUL y todas en verde.

- [ ] **Paso 4: escribir el informe y el CHANGELOG, hacer el commit, unir y abrir el PR** de `sprint-2/integracion` hacia `main` (con el visto bueno de Adrián).

```bash
git commit -m "test(app): flujo del sprint 2 en el Xiaomi e informe (paso 2.6)"
```

**Criterio de aceptación del sprint:** en el Xiaomi se ve un video 4K completo sin perder cuadros, con pausa, saltos, retomar y paso al siguiente; las fotos pasan solas con fundido; un archivo malo muestra su mensaje y no traba nada; y Atrás siempre vuelve a la tarjeta de lo último que se vio.
