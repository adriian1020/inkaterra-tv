# Sprint 3 — Música y USB del huésped: plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendada) o superpowers:executing-plans para implementar este plan paso a paso. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** que la música suene con el control (lista, repetir y orden aleatorio), que la presentación de fotos tenga música de fondo, y que la memoria USB del huésped aparezca como una fila del inicio al conectarla y no deje nada al retirarla.

**Arquitectura:**
- El reproductor de música (`pantallas/musica/`) usa un `ExoPlayer` con toda la lista y una `MediaSession` de Media3 mientras la pantalla está abierta, sin servicio en segundo plano: Atrás detiene la música (D-22).
- La música de fondo es otro `ExoPlayer` dentro del visor de fotos, con las canciones de la fila Música en orden aleatorio.
- La USB se lee con la misma `Biblioteca` y las mismas reglas que la carpeta del hotel, sobre una fuente nueva (`CarpetaUsb`). `UsbDelHuesped` escucha los avisos de `VolumenesUsb` y expone un `StateFlow<EstadoUsb>` que `InicioModelo` convierte en la fila «Tu USB». Las miniaturas de la USB viven solo en memoria.

**Tecnología:**
- Lo del sprint 2, más `androidx.media3:media3-session` 1.11.1.

**Spec:** [docs/plan-de-desarrollo.md](../plan-de-desarrollo.md): Fase 1, sprint 3, «Reglas de contenido», CU-03, CU-04 y CU-17; [docs/decisiones.md](../decisiones.md): D-22 (música y USB, se escribe en el paso 3.0), D-10, D-18, D-20 y D-21.

**Depende del sprint 2:** biblioteca, miniaturas, inicio, `Navegacion`, `InicioModelo`, `ReproductorPantalla`, `BarraVideo`, `VisorFotosPantalla`, `PosicionesVideo`, `VolumenesUsb` (sprint 0) y la música de muestra de `tool/copiar-contenido.sh` (`Selva.mp3` con carátula y `Lluvia en la Amazonía.m4a`).

## Restricciones globales

- Las mismas de los sprints 1 y 2 (idioma, comentarios, commits sin firma, ramas por paso, versiones exactas, chequeos del CI antes de cada commit con `&&`, ningún texto visible en el código, datos de prueba inventados). Las ramas salen de `sprint-3/integracion`, que sale de `main`.
- Música (D-22): al abrir una canción suena toda la fila Música desde ella. Repetir empieza en «la lista» y el orden aleatorio apagado, cada vez que se abre. Atrás detiene la música y vuelve a la tarjeta de la canción que sonaba. Con «sin repetir», al terminar la última vuelve al inicio.
- Teclas de música: Izquierda y Derecha mueven el foco entre los botones (anterior, reproducir o pausar, siguiente, aleatorio, repetir) y Enter los activa. `MEDIA_PLAY_PAUSE`, `MEDIA_PLAY` y `MEDIA_PAUSE` pausan y siguen; `MEDIA_NEXT` y `MEDIA_PREVIOUS` cambian de canción. Una tecla mantenida cuenta una vez.
- Errores de música: «No se puede reproducir esta canción» 3 000 ms y sigue la siguiente; si fallan seguidas tantas como tiene la lista, vuelve al inicio.
- Música de fondo (D-22): las canciones de la fila Música, en orden aleatorio y en bucle, en toda presentación (también las fotos de la USB). Enter pausa la presentación y la música. Sin canciones, la presentación pasa en silencio. Una canción que falla se salta sin mensaje.
- USB (D-22): una sola fila «Tu USB», segunda después de Destacados, con todas las fotos y videos de la memoria (de cualquier carpeta), del más nuevo al más viejo. La música de la USB no se muestra. Se saltan los archivos y carpetas que empiezan con punto y las carpetas `Android`, `LOST.DIR`, `System Volume Information` y `$RECYCLE.BIN`.
- Sin rastro (D-10): nada de la USB se escribe en disco. Al retirarla se olvidan sus posiciones de video, sus miniaturas y las fotos que Coil tenga en memoria. Los nombres de los archivos de la USB van al registro solo en las versiones de depuración.
- Registro para la prueba a mano (D-18): etiqueta `Usb` para lo que pasa con la memoria y `Fondo` para la música de fondo.

## Foco de revisión

1. **Retirar la USB mientras se mira una foto o un video de ella:** la app vuelve al inicio con el foco en la primera tarjeta, y no quedan posiciones ni miniaturas de la memoria. Pruebas `alRetirarLaUsbElVisorVuelveAlInicio` y `olvidarDentroDeBorraSoloEsaCarpeta` (paso 3.5) y `retirarMientrasLeeNoDejaLaFila` (paso 3.4).
2. **Una memoria usada en Mac o Windows** (`._foto.jpg`, `.Trashes`, `System Volume Information`, `Android/`): esos archivos no aparecen como fotos rotas. Prueba `ignoraCarpetasDelSistemaYArchivosOcultos` (paso 3.4).
3. **Todas las canciones fallan con «repetir la lista»:** no queda en un bucle de mensajes; vuelve al inicio. Prueba `cancionesQueFallanVuelvenAlInicio` (paso 3.2).
4. **Conectar o retirar la USB con el foco en otra fila:** el foco se queda en su tarjeta; si estaba en la fila de la USB, va a la primera tarjeta. Pruebas `alConectarLaUsbElFocoSigueEnSuTarjeta` y `alRetirarLaUsbElFocoVaALaPrimera` (paso 3.5).
5. **Una memoria que no se puede leer y nada del hotel cargado:** la app no se cierra; el inicio dice «Todavía no hay contenido». Prueba `avisoSinOtrasFilasNoSeMuestra` (paso 3.5).

---

### Paso 3.0: decisión D-22 y plan

**Rama:** `docs/plan-sprint-3`

- [ ] **Paso 1: escribir D-22 en `docs/decisiones.md`** con lo elegido por Adrián (la música se detiene al salir; música de fondo de la fila Música en orden aleatorio, también en la USB; la USB en una sola fila, recientes primero; repetir la lista por omisión) y lo decidido al planificar: los permisos de fotos y videos se dan al instalar por adb (no se le muestra un diálogo al huésped); las miniaturas de la USB solo en memoria; un aviso en la fila si la memoria está vacía, no se lee o trae archivos que la TV no puede ver (CU-17 1a y 3b).
- [ ] **Paso 2: en `docs/plan-de-desarrollo.md`**, en el sprint 3, cambiar «recorrer sus carpetas» por «una fila con todas sus fotos y videos, recientes primero (D-22)».
- [ ] **Paso 3: commit y unión**

```bash
git commit -m "docs(planes): plan del sprint 3 y decisión d-22"
```

---

### Paso 3.1: lista de canciones y modos de repetir

**Rama:** `feat/dominio-lista-musica`

**Archivos:**
- Crear: `dominio/Musica.kt` y `pantallas/musica/ListaMusica.kt`.
- Prueba: `test/.../dominio/MusicaTest.kt` y `test/.../pantallas/musica/ListaMusicaTest.kt` (Robolectric).

**Interfaces:**
- Produce:
  - `enum class Repetir { LISTA, UNA, NO }` y `fun Repetir.siguiente(): Repetir` (LISTA → UNA → NO → LISTA), en `dominio/`;
  - `fun Repetir.modoMedia3(): Int` (`Player.REPEAT_MODE_ALL`, `REPEAT_MODE_ONE`, `REPEAT_MODE_OFF`);
  - `fun Player.ponerCanciones(canciones: List<Elemento>, inicial: Int)`: un `MediaItem` por canción (`Uri.fromFile`, `mediaId` = ruta), con `setMediaItems(items, inicial, 0)`.

- [ ] **Paso 1: escribir las pruebas que fallan**

```kotlin
@Test fun repetirRecorreLosTresModos() {
    assertEquals(Repetir.UNA, Repetir.LISTA.siguiente())
    assertEquals(Repetir.NO, Repetir.UNA.siguiente())
    assertEquals(Repetir.LISTA, Repetir.NO.siguiente())
}
```

`ListaMusicaTest.ponerCancionesEmpiezaEnLaElegida`: con un `ExoPlayer` y tres canciones de `archivo(...)`, `ponerCanciones(c, 1)` deja `mediaItemCount == 3`, `currentMediaItemIndex == 1` y `getMediaItemAt(2).mediaId == c[2].archivo.ruta`.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `cd apps/tv && ./gradlew testDebugUnitTest --tests '*MusicaTest' --tests '*ListaMusicaTest'`
Expected: FAIL (no compila).

- [ ] **Paso 3: implementar `Repetir`, `siguiente`, `modoMedia3` y `ponerCanciones`.**

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*MusicaTest' --tests '*ListaMusicaTest'`
Expected: PASS, 2 pruebas.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(dominio): lista de canciones y modos de repetir (paso 3.1)"
```

---

### Paso 3.2: reproductor de música

**Rama:** `feat/pantallas-musica`

**Archivos:**
- Modificar: `gradle/libs.versions.toml` y `app/build.gradle.kts` (`media3-session`, en su propio commit), `app/Navegacion.kt`, `res/values/strings.xml` y `res/values-en/strings.xml`.
- Crear: `pantallas/musica/ReproductorMusicaPantalla.kt` y los íconos `res/drawable/ic_anterior.xml`, `ic_reproducir.xml`, `ic_pausa.xml`, `ic_siguiente.xml`, `ic_aleatorio.xml`, `ic_repetir.xml` y `ic_repetir_una.xml` (vectores de Material Symbols, licencia Apache 2.0).
- Borrar: `pantallas/abrir/AbrirPantalla.kt` y la ruta `"abrir/{ruta}"`.
- Prueba: `test/.../pantallas/musica/ReproductorMusicaTest.kt` y `test/.../pantallas/NavegacionTest.kt` (Robolectric).

**Interfaces:**
- Consume: `Repetir`, `siguiente`, `modoMedia3` y `ponerCanciones` (paso 3.1); `BarraVideo(titulo, posicionMs, duracionMs, pausado)` del sprint 2, que se usa tal cual para el título, el tiempo y «En pausa»; `Miniatura(elemento, miniaturas)` para la carátula.
- Produce:
  - `@Composable fun ReproductorMusicaPantalla(canciones: List<Elemento>, inicial: Int, miniaturas: Miniaturas, alSalir: (Elemento) -> Unit)`;
  - un `ExoPlayer` por pantalla con `AUDIO_CONTENT_TYPE_MUSIC` y el foco de audio manejado, y una `MediaSession` sobre él; los dos se liberan al salir;
  - botones con `testTag` `musica-anterior`, `musica-alternar`, `musica-siguiente`, `musica-aleatorio` y `musica-repetir`, y con su texto para la accesibilidad como `contentDescription` (el de `musica-repetir` dice el modo actual). El foco empieza en `musica-alternar`;
  - la ruta `"musica/{fila}/{indice}"` con `DeLaFila(..., TipoContenido.MUSICA, ...)`, como video y fotos;
  - textos (es / en): `musica_no_reproduce` «No se puede reproducir esta canción» / «This song can’t be played»; `musica_anterior` «Anterior» / «Previous»; `musica_reproducir` «Reproducir» / «Play»; `musica_pausar` «Pausar» / «Pause»; `musica_siguiente` «Siguiente» / «Next»; `musica_aleatorio_si` «Orden aleatorio activado» / «Shuffle on»; `musica_aleatorio_no` «Orden aleatorio desactivado» / «Shuffle off»; `musica_repetir_lista` «Repetir la lista» / «Repeat all»; `musica_repetir_una` «Repetir esta canción» / «Repeat this song»; `musica_sin_repetir` «Sin repetir» / «Repeat off».
- Comportamiento que no fijan las pruebas: «Anterior» usa `seekToPrevious()` (vuelve al principio si pasaron más de 3 s); la pantalla queda encendida solo mientras suena; `ON_STOP` pausa; la carátula es la miniatura de la canción a 640 × 360 dp.

- [ ] **Paso 1: escribir las pruebas que fallan**

`ReproductorMusicaTest` (en la PC las rutas no existen, así que cada canción falla, como en `ReproductorPantallaTest`):
- `cancionesQueFallanVuelvenAlInicio`: con 3 canciones rotas y repetir en «la lista», el mensaje «No se puede reproducir esta canción» aparece y `alSalir` llega con una canción de la lista antes de 15 s (avanzando el reloj de a 1 s con espera real, como en el sprint 2).
- `botonRepetirRecorreLosModos`: Enter en `musica-repetir` tres veces cambia su `contentDescription` de «Repetir la lista» a «Repetir esta canción», «Sin repetir» y otra vez «Repetir la lista».
- `elFocoEmpiezaEnReproducir`: `musica-alternar` tiene el foco al abrir.

`NavegacionTest`:
- `alElegirUnaCancionSeAbreElReproductor` (reemplaza a la prueba que buscaba `abrir-titulo`): Enter en `tarjeta-MUSICA-0` muestra `video-titulo` con «Selva».
- `atrasEnLaMusicaVuelveASuTarjeta`: Enter en `tarjeta-MUSICA-0` y Atrás dejan el foco en `tarjeta-MUSICA-0`.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*ReproductorMusicaTest' --tests '*NavegacionTest'`
Expected: FAIL en las 5.

- [ ] **Paso 3: agregar `media3-session` 1.11.1 en su propio commit**

```bash
git commit -m "build(tv): agregar media3-session 1.11.1 (paso 3.2)"
```

- [ ] **Paso 4: implementar la pantalla, los íconos, los textos y la ruta; borrar `AbrirPantalla`.** El manejo de errores sigue el patrón de `ReproductorPantalla` (índice de la canción que falló como clave del efecto) y suma un contador de errores seguidos que vuelve a 0 cuando una canción empieza a sonar.

- [ ] **Paso 5: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest`
Expected: PASS en todas.

- [ ] **Paso 6: probar en el Xiaomi**

Run: `tool/copiar-contenido.sh && tool/tv.sh instalar`
Expected:
- «Selva» suena con su carátula; Enter en reproducir o pausar pausa (se ve «En pausa») y sigue.
- «Siguiente» pasa a «Lluvia en la Amazonía»; al terminar, vuelve a «Selva» (repetir la lista).
- Con «Sin repetir», al terminar la última vuelve al inicio en su tarjeta. Atrás detiene la música.
- Con el orden aleatorio activado, «Siguiente» sigue funcionando.

- [ ] **Paso 7: commit y unión**

```bash
git commit -m "feat(pantallas): reproductor de música con repetir y orden aleatorio (paso 3.2)"
```

---

### Paso 3.3: música de fondo en la presentación

**Rama:** `feat/pantallas-musica-fondo`

**Archivos:**
- Crear: `pantallas/fotos/MusicaDeFondo.kt`.
- Modificar: `pantallas/fotos/VisorFotosPantalla.kt` y `app/Navegacion.kt`.
- Prueba: `test/.../pantallas/fotos/MusicaDeFondoTest.kt` (Robolectric) y `VisorFotosTest` (ajustar a la firma nueva).

**Interfaces:**
- Consume: `ponerCanciones` (paso 3.1).
- Produce:
  - `fun Player.prepararFondo(canciones: List<Elemento>, inicial: Int)`: `ponerCanciones`, orden aleatorio activado, `REPEAT_MODE_ALL` y `prepare()`;
  - `@Composable fun MusicaDeFondo(canciones: List<Elemento>, sonando: Boolean)`: sin canciones no crea nada; si no, un `ExoPlayer` con `AUDIO_CONTENT_TYPE_MUSIC` y foco de audio, que empieza en una canción al azar y suena mientras `sonando` y la pantalla no esté en `ON_STOP`. Una canción que falla se salta (`Log.w("Fondo", ...)`); si fallan todas seguidas, se detiene;
  - `VisorFotosPantalla(fotos: List<Elemento>, inicial: Int, musica: List<Elemento>, alSalir: (Elemento) -> Unit)` llama a `MusicaDeFondo(musica, sonando = !pausada)`;
  - en `Navegacion`, la ruta de fotos pasa las canciones de la fila Música (vacía si no hay).

- [ ] **Paso 1: escribir las pruebas que fallan**

`MusicaDeFondoTest.prepararFondoMezclaYRepite`: con un `ExoPlayer` y dos canciones, `prepararFondo(c, 1)` deja `shuffleModeEnabled == true`, `repeatMode == Player.REPEAT_MODE_ALL`, `mediaItemCount == 2` y `currentMediaItemIndex == 1`.

- [ ] **Paso 2: correrla y ver que falla**

Run: `./gradlew testDebugUnitTest --tests '*MusicaDeFondoTest'`
Expected: FAIL (no compila).

- [ ] **Paso 3: implementar `prepararFondo`, `MusicaDeFondo`, el parámetro del visor y la ruta.** Las pruebas de `VisorFotosTest` pasan `musica = emptyList()`.

- [ ] **Paso 4: correr todas**

Run: `./gradlew testDebugUnitTest`
Expected: PASS en todas.

- [ ] **Paso 5: probar en el Xiaomi**

Expected: al abrir una foto suena una de las dos canciones; Enter pausa la presentación y la música; Atrás la detiene. Sin la carpeta `musica/` (moverla un momento), la presentación pasa en silencio.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(pantallas): música de fondo en la presentación de fotos (paso 3.3)"
```

---

### Paso 3.4: lectura de la USB del huésped

**Rama:** `feat/datos-usb`

**Archivos:**
- Modificar: `sistema/usb/VolumenesUsb.kt`, `datos/biblioteca/FuenteArchivos.kt`, `datos/biblioteca/Biblioteca.kt`, `AndroidManifest.xml` y `tool/tv.sh`.
- Crear: `datos/usb/UsbDelHuesped.kt`.
- Prueba: `test/.../datos/biblioteca/CarpetaUsbTest.kt`, `test/.../datos/usb/UsbDelHuespedTest.kt` y `test/.../sistema/usb/DoblesUsb.kt` (`VolumenesFalsos`).

**Interfaces:**
- Produce:
  - `interface Volumenes { fun actuales(): List<VolumenUsb>; fun observar(alCambiar: (List<VolumenUsb>) -> Unit): AutoCloseable }`, que `VolumenesUsb` implementa. `VolumenUsb` suma `val estado: String` (el de `StorageVolume.getState()`);
  - `ArchivoLocal` suma `val deUsb: Boolean = false` (último parámetro);
  - `class CarpetaUsb(raiz: File) : FuenteArchivos`, con `deUsb = true`, que salta lo de «Restricciones globales» y lanza `IOException` si `raiz.listFiles()` da `null` (sin permiso);
  - `fun ContenidoBiblioteca.paraLaUsb(): List<Elemento>`: videos y fotos, del más nuevo al más viejo por `modificadoMs`, y por título si empatan;
  - `sealed interface EstadoUsb`: `SinUsb`, `Leyendo(volumen)`, `Lista(volumen, elementos: List<Elemento>, rechazados: Int)` y `NoSeLee(volumen, motivo: MotivoUsb)`, con `enum class MotivoUsb { FORMATO, SIN_PERMISO, ERROR }`;
  - `class UsbDelHuesped(volumenes: Volumenes, lector: LectorMetadatos, permiso: () -> Boolean, alRetirar: (VolumenUsb) -> Unit, registrarArchivos: Boolean = BuildConfig.DEBUG, despachador: CoroutineDispatcher = Dispatchers.IO)` con `val estado: StateFlow<EstadoUsb>` y `fun empezar(alcance: CoroutineScope): AutoCloseable`, que revisa los volúmenes actuales y escucha los cambios.
- Reglas de `UsbDelHuesped`: toma el primer volumen extraíble. Montado con raíz y permiso: `Leyendo` y luego `Lista` (con `Biblioteca(CarpetaUsb(raiz), lector, despachador).cargar().paraLaUsb()`); montado sin permiso: `NoSeLee(SIN_PERMISO)`; presente pero en `unmountable` o `nofs`: `NoSeLee(FORMATO)`; la lectura lanza: `NoSeLee(ERROR)`; ningún volumen presente (o `removed`, `bad_removal`, `ejecting`, `unmounted`): `SinUsb`. Al pasar a `SinUsb` desde otro estado, cancela la lectura en curso y llama a `alRetirar` con el volumen anterior.
- Registro `Usb` (para la prueba a mano, D-18): cada cambio de volumen (`id`, `estado`, `montado`, `raiz`), el resultado (`N fotos y videos, M rechazados en X ms` o el motivo) y, si `registrarArchivos`, una línea por archivo con su tipo o su motivo de rechazo.
- Permisos: `READ_MEDIA_IMAGES` y `READ_MEDIA_VIDEO` en el manifiesto. `tool/tv.sh instalar` los da después de instalar con `adb shell pm grant pe.inkaterra.tv android.permission.READ_MEDIA_IMAGES` (y `READ_MEDIA_VIDEO`).

- [ ] **Paso 1: escribir las pruebas que fallan**

`CarpetaUsbTest` (con `TemporaryFolder`):
- `ignoraCarpetasDelSistemaYArchivosOcultos`: con `DCIM/foto.jpg`, `DCIM/._foto.jpg`, `.Trashes/x.jpg`, `Android/data/y.jpg`, `LOST.DIR/z.jpg`, `System Volume Information/w.jpg` y `$RECYCLE.BIN/v.jpg`, `listar()` da solo `foto.jpg`, con `deUsb == true`.
- `sinPermisoLanza`: una raíz que no es carpeta lanza `IOException`.

`UsbDelHuespedTest` (con `VolumenesFalsos`, `LectorFalso` y `StandardTestDispatcher`):
- `alConectarListaFotosYVideosRecientesPrimero`: `vieja.jpg` (modificada en 1 000), `nueva.mp4` (3 000) y `media.heic` (2 000) dan `Lista` con `nueva`, `media` y `vieja`; una canción de la USB no aparece.
- `cuentaLosRechazados`: un video de 7680 × 4320 da `rechazados == 1`.
- `alRetirarAvisaYVuelveASinUsb`: conectar y retirar llama a `alRetirar` una vez con el volumen y deja `SinUsb`.
- `sinPermisoNoLee`: con `permiso = { false }` el estado es `NoSeLee(SIN_PERMISO)`.
- `formatoQueNoLeeLoDice`: un volumen con `estado = "unmountable"` da `NoSeLee(FORMATO)`.
- `retirarMientrasLeeNoDejaLaFila`: con un lector que espera un `CompletableDeferred`, retirar durante la lectura deja `SinUsb`, y liberar el lector después no lo cambia.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*CarpetaUsbTest' --tests '*UsbDelHuespedTest'`
Expected: FAIL (no compila).

- [ ] **Paso 3: implementar la interfaz, `CarpetaUsb`, `paraLaUsb`, `UsbDelHuesped`, los permisos y el `pm grant`.**

- [ ] **Paso 4: correr todas**

Run: `./gradlew testDebugUnitTest`
Expected: PASS en todas.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(datos): leer la usb del huésped sin copiar nada (paso 3.4)"
```

---

### Paso 3.5: fila «Tu USB» en el inicio

**Rama:** `feat/pantallas-fila-usb`

**Archivos:**
- Modificar: `pantallas/inicio/Filas.kt`, `InicioModelo.kt`, `InicioPantalla.kt`, `AreaDestacada.kt`, `TarjetaContenido.kt`, `pantallas/comun/Miniatura.kt`, `datos/miniaturas/Miniaturas.kt`, `GeneradorMiniatura.kt`, `dominio/Reproduccion.kt`, `app/Contenedor.kt`, `app/Navegacion.kt`, `app/MainActivity.kt` y los `strings.xml`.
- Prueba: `test/.../pantallas/inicio/FilasTest.kt`, `InicioModeloTest.kt`, `test/.../pantallas/NavegacionTest.kt`, `test/.../dominio/ReproduccionTest.kt` y `test/.../datos/miniaturas/MiniaturasTest.kt`.

**Interfaces:**
- Consume: `UsbDelHuesped`, `EstadoUsb` y `ArchivoLocal.deUsb` (paso 3.4).
- Produce:
  - `TipoFila.USB` (después de `DESTACADOS` en el enum) y `enum class AvisoFila { LEYENDO, VACIA, NO_SE_LEE }`; `Fila` suma `val aviso: AvisoFila? = null` y `val rechazados: Int = 0`;
  - `fun filasDe(contenido: ContenidoBiblioteca, usb: EstadoUsb = EstadoUsb.SinUsb): List<Fila>`, en el orden Destacados, Tu USB, Videos, Fotos y Música. `Leyendo` da el aviso `LEYENDO`; `Lista` sin elementos, `VACIA`; `NoSeLee`, `NO_SE_LEE`. Una fila con aviso se muestra solo si otra fila tiene tarjetas;
  - `InicioModelo(biblioteca: Biblioteca, usb: UsbDelHuesped)`: arranca `usb.empezar(viewModelScope)` (lo cierra en `onCleared`) y rearma las filas en cada cambio. Si el foco estaba en una tarjeta que sigue estando, `foco` pasa a su nueva posición (se busca por tipo de fila y ruta); si no, a `Foco(0, 0)`;
  - `InicioPantalla`: la fila con aviso muestra su texto (`testTag("aviso-USB")`) en vez de tarjetas y no recibe el foco; si `rechazados > 0`, debajo del título va `usb_rechazados`. La tarjeta inicial del área destacada es la primera de la primera fila con tarjetas. El `LaunchedEffect` que pide el foco usa como clave la lista de tipos de fila, para volver a enfocar `foco` cuando una fila aparece o se va;
  - `interface FuenteMiniaturas { suspend fun de(elemento: Elemento): Any? }` (lo que Coil sabe mostrar: un `File` o un `Bitmap`), que `Miniaturas` implementa; `class MiniaturasEnMemoria(generador: GeneradorMiniatura, despachador: CoroutineDispatcher = Dispatchers.IO) : FuenteMiniaturas` con un `LruCache` de 48 MB medido en bytes, de a dos a la vez, y `fun limpiar()`; `class MiniaturasApp(hotel: Miniaturas, usb: MiniaturasEnMemoria) : FuenteMiniaturas`, que elige por `elemento.archivo.deUsb`. `Miniatura`, `TarjetaContenido`, `AreaDestacada`, `InicioPantalla`, `ReproductorMusicaPantalla` y `Navegacion` reciben `FuenteMiniaturas`;
  - `GeneradorMiniaturaAndroid(ancho: Int = ANCHO_MINIATURA, alto: Int = ALTO_MINIATURA)`; la USB usa 640 × 360;
  - `PosicionesVideo.olvidarDentroDe(carpeta: String)`: borra las posiciones de las rutas que empiezan con `carpeta` + `/`;
  - `Contenedor` suma `val usb: UsbDelHuesped`, una por proceso, con `permiso` que comprueba los dos permisos y `alRetirar` que llama a `posiciones.olvidarDentroDe(raiz)`, `miniaturasUsb.limpiar()` y `SingletonImageLoader.get(context).memoryCache?.clear()`. `Navegacion(biblioteca, miniaturas, posiciones, idioma, usb)`;
  - textos (es / en): `fila_usb` «Tu USB» / «Your USB»; `usb_leyendo` «Leyendo tu memoria…» / «Reading your drive…»; `usb_vacia` «No hay fotos ni videos en esta memoria» / «There are no photos or videos on this drive»; `usb_no_se_lee` «Esta memoria no se puede leer en esta TV» / «This drive can’t be read on this TV»; `usb_rechazados` (plural) «%d archivo no se puede ver en esta TV» / «%d archivos no se pueden ver en esta TV» y «%d file can’t be shown on this TV» / «%d files can’t be shown on this TV».
- Al retirar la USB con su foto o video abierto, `DeLaFila` ya vuelve al inicio porque la fila desaparece (índice fuera de la lista).

- [ ] **Paso 1: escribir las pruebas que fallan**

`FilasTest`:
- `laUsbVaDespuesDeDestacados`: con destacados, videos y `Lista` de 2 elementos, los tipos son `DESTACADOS, USB, VIDEOS`.
- `usbIlegibleMuestraAviso`: `NoSeLee(FORMATO)` da la fila `USB` con `aviso == NO_SE_LEE` y sin elementos.
- `avisoSinOtrasFilasNoSeMuestra`: contenido vacío y `NoSeLee(FORMATO)` dan una lista vacía.

`InicioModeloTest`:
- `alConectarLaUsbElFocoSigueEnSuTarjeta`: con foco en «Río Madre de Dios» (`Foco(1, 1)`), conectar una USB deja `foco == Foco(2, 1)`.
- `alRetirarLaUsbElFocoVaALaPrimera`: con foco en la fila `USB`, retirarla deja `Foco(0, 0)`.

`ReproduccionTest.olvidarDentroDeBorraSoloEsaCarpeta`: con posiciones en `/storage/AB12-34CD/v.mp4`, `/storage/AB12-34CDE/w.mp4` y `/contenido/x.mp4`, `olvidarDentroDe("/storage/AB12-34CD")` deja en 0 solo la primera.

`MiniaturasTest.enMemoriaNoEscribeEnDisco`: `MiniaturasEnMemoria` con un generador que da un `Bitmap` de 4 × 4 devuelve ese `Bitmap`, la segunda vez no vuelve a generarlo, y después de `limpiar()` sí.

`NavegacionTest` (con `VolumenesFalsos` y una carpeta temporal como USB, permiso concedido):
- `alConectarLaUsbApareceSuFila`: conectar muestra `fila-USB` y `tarjeta-USB-0`.
- `alRetirarLaUsbElVisorVuelveAlInicio`: Enter en `tarjeta-USB-0` (una foto) y retirar dejan el inicio sin `fila-USB` y con el foco en `tarjeta-DESTACADOS-0`.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*FilasTest' --tests '*InicioModeloTest' --tests '*ReproduccionTest' --tests '*MiniaturasTest' --tests '*NavegacionTest'`
Expected: FAIL en las 10 nuevas.

- [ ] **Paso 3: implementar la fila, los avisos, el foco, las miniaturas en memoria, `olvidarDentroDe` y el `Contenedor`.** Ajustar `IdiomaTest`, `DoblesPantallas` (suma `usbSinMemoria()`) y las pruebas existentes a las firmas nuevas.

- [ ] **Paso 4: correr todas**

Run: `./gradlew testDebugUnitTest`
Expected: PASS en todas.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(pantallas): fila tu usb en el inicio (paso 3.5)"
```

---

### Paso 3.6: prueba integral, prueba de la USB a mano, informe y PR

**Rama:** `test/flujo-sprint3`

**Archivos:**
- Prueba en el equipo: `androidTest/.../FlujoSprint3Test.kt`.
- Crear: `docs/pruebas/usb-a-mano.md` y `docs/informes/sprint-3.md`.
- Modificar: `CHANGELOG.md`, `docs/arquitectura.md` (música y USB), `docs/decisiones.md` (D-18 con el resultado) y `docs/plan-de-desarrollo.md` (próximos pasos).

**Interfaces:**
- Consume: todo lo anterior y, como `FlujoSprint2Test`, la carpeta `contenido-prueba/` pasada con `MainActivity.EXTRA_RAIZ_CONTENIDO`, con el idioma fijado en español.

- [ ] **Paso 1: escribir la prueba en el equipo**

`FlujoSprint3Test.musicaYFondo`, con `Selva.mp3` y `Lluvia en la Amazonía.m4a` copiadas de `contenido/musica/` (las deja `tool/copiar-contenido.sh`) y 2 JPEG escritos por la prueba en `contenido-prueba/fotos/`:
1. Enter en `tarjeta-MUSICA-0`: `video-titulo` dice «Lluvia en la Amazonía» y, en 3 s, `AudioManager.isMusicActive` es verdadero.
2. `KEYCODE_MEDIA_NEXT`: `video-titulo` dice «Selva».
3. Atrás: foco en `tarjeta-MUSICA-1` y, en 2 s, `isMusicActive` es falso.
4. Enter en `tarjeta-FOTOS-0`: en 3 s, `isMusicActive` es verdadero. Enter (pausa): en 2 s es falso. Atrás.

- [ ] **Paso 2: correr todo lo del CI y las pruebas en el Xiaomi**

Run: `(cd apps/tv && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug) && EXCLUIR=pe.inkaterra.tv.sistema.VolumenesUsbTest tool/tv.sh probar`
Expected: BUILD SUCCESSFUL y todas en verde.

- [ ] **Paso 3: escribir `docs/pruebas/usb-a-mano.md`** (sirve también para la TCL):
- **Antes:** instalar con `tool/tv.sh instalar` (da los permisos). Preparar memorias en FAT32, exFAT y, si hay, NTFS, con fotos de celular (JPG y HEIC), un video de celular (MP4 o MOV), un video de más de 4K, y una carpeta copiada desde una Mac o con `System Volume Information`.
- **En la TV, con la depuración por USB y la inalámbrica apagadas (D-18):** abrir la app; conectar la memoria; anotar si aparece «Tu USB» o qué aviso sale, si se ven las miniaturas, si abren las fotos (también HEIC) y los videos, si suena la música de fondo, y si el aviso de archivos que no se pueden ver cuenta bien. Retirar la memoria con una foto abierta: la app vuelve al inicio sin la fila. Repetir con cada formato.
- **Después:** encender la depuración inalámbrica, `tool/tv.sh conectar`, y guardar `adb logcat -d -s Usb:* Fondo:* Reproductor:* Contenido:*` en `docs/pruebas/`. Hacerlo enseguida: el registro de Android es circular y se pierde con el tiempo.

- [ ] **Paso 4: Adrián hace la prueba a mano; leer el registro con él y anotar el resultado en D-18** (qué formatos lee el Xiaomi, si los permisos alcanzan y cuánto tarda en leer la memoria).

- [ ] **Paso 5: escribir el informe y el CHANGELOG, hacer el commit, unir y abrir el PR** de `sprint-3/integracion` hacia `main` (con el visto bueno de Adrián).

```bash
git commit -m "test(app): flujo del sprint 3 en el Xiaomi, prueba de la usb e informe (paso 3.6)"
```

**Criterio de aceptación del sprint:** en el Xiaomi suena una lista completa con repetir y orden aleatorio, la presentación pasa con música de fondo, y en la prueba a mano una memoria con fotos de celular aparece como «Tu USB» al conectarla y desaparece sin dejar nada al retirarla.
