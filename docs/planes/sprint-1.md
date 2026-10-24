# Sprint 1 — Base y navegación: plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendada) o superpowers:executing-plans para implementar este plan paso a paso. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** que el huésped elija su idioma y recorra con el control remoto un inicio con la imagen de Inkaterra, con secciones Videos, Fotos y Música que listan el contenido de la carpeta del hotel ya validado con las reglas de contenido.

**Arquitectura:**
- Las reglas de contenido (tipo por extensión, máximo 4K, códecs, tasa de D-13) son Kotlin puro en `dominio/`.
- La biblioteca (`datos/`) lee la carpeta del hotel detrás de dos interfaces, `FuenteArchivos` y `LectorMetadatos`, con dobles para las pruebas.
- Las pantallas (`pantallas/`) usan Compose for TV y navigation-compose, y reciben todo de un `Contenedor` manual en `app/`, sin Hilt.

**Tecnología:**
- Lo del sprint 0, más navigation-compose 2.10.2, lifecycle-viewmodel-compose 2.11.0, appcompat 1.8.0 (idioma por app) y kotlinx-coroutines 1.11.0.
- Pruebas de pantallas con `ui-test-junit4` (BOM) sobre Robolectric 4.17, en la PC.

**Spec:** [docs/plan-de-desarrollo.md](../plan-de-desarrollo.md): Fase 1, sprint 1, «Reglas de contenido», «Imagen de marca» y CU-01.

**Depende del sprint 0:** el proyecto, el CI y `tool/onn.sh` (paso 0.1), la tasa máxima D-13 (paso 0.4) y `soporta4k` (paso 0.3).

## Restricciones globales

- Las mismas del sprint 0 (idioma, comentarios, commits, ramas, versiones exactas y chequeos del CI antes de cada commit). Las ramas salen de `sprint-1/integracion`, que sale de `sprint-0/integracion`.
- Carpeta del hotel: `getExternalFilesDir("contenido")`, con las subcarpetas `videos/`, `fotos/`, `musica/` y `promociones/`. Así `adb push` puede escribir sin root: `/sdcard/Android/data/pe.inkaterra.tv/files/contenido/`.
- Máximo 3840 × 2160. Se compara el lado mayor con 3840 y el menor con 2160, para que los videos verticales de 2160 × 3840 también entren.
- Video: extensiones `mp4`, `mkv` y `mov`; códecs `video/avc` y `video/hevc`. Foto: `jpg`, `jpeg`, `png`, `webp` y `heic`. Música: `mp3`, `m4a`, `aac` y `flac`. Las extensiones se comparan sin importar mayúsculas.
- Tasa máxima de video: `TASA_MAXIMA_BPS` = el valor de D-13. Si el sprint 0 no lo fijó, 40 000 000.
- Colores: verde `#5A6A40`, verde secundario `#626A45`, crema `#F3F2EE`, texto `#424242`, fondo de la TV `#1E2416`. Letras: Josefin Sans para el texto y Cinzel para los títulos, dentro de la app con su licencia OFL en `assets/licencias/`.
- Idiomas: español (por omisión, en `values/`) e inglés (`values-en/`). Ningún texto visible va escrito en el código.
- Datos de prueba inventados: «Atardecer en Machu Picchu.mp4», «Reserva Amazónica 01.jpg» y similares.

## Foco de revisión

1. **Extensiones en mayúsculas o archivos sin extensión** (`VIDEO.MP4`, `foto.JPEG`, `LEEME`): los primeros se clasifican igual que en minúsculas; los sin extensión se ignoran en silencio. Pruebas `extensionEnMayusculas` y `sinExtensionSeIgnora` del paso 1.1.
2. **Primera instalación, con la carpeta del hotel inexistente o vacía:** la biblioteca vuelve vacía sin error, y cada sección muestra «Todavía no hay videos» (o fotos, o música) en vez de una pantalla en blanco. Pruebas `carpetaInexistenteDaBibliotecaVacia` (paso 1.2) y `seccionVaciaMuestraMensaje` (paso 1.4).
3. **Un archivo dañado** (el lector de metadatos lanza una excepción): va a «rechazados» con `ARCHIVO_ILEGIBLE` y el resto se carga. Prueba `archivoIlegibleNoRompeLaCarga` del paso 1.2.
4. **Nombres con tildes, ñ y espacios:** se muestran sin la extensión y se ordenan como en español («Árbol» junto a la A, no al final). Prueba `ordenaComoEnEspanol` del paso 1.2.
5. **Volver de una sección con Atrás:** el foco regresa a la tarjeta de la que se salió, no a la primera ni a ninguna. Prueba `atrasDevuelveElFocoALaTarjeta` del paso 1.4.

---

### Paso 1.1: reglas de contenido

**Rama:** `feat/dominio-reglas-contenido`

**Archivos:**
- Crear: `apps/tv/app/src/main/java/pe/inkaterra/tv/dominio/ReglasContenido.kt`.
- Prueba: `apps/tv/app/src/test/java/pe/inkaterra/tv/dominio/ReglasContenidoTest.kt`.

**Interfaces:**
- Produce:
  - `enum class TipoContenido { VIDEO, FOTO, MUSICA }`;
  - `fun clasificar(nombreArchivo: String): TipoContenido?`, que devuelve `null` si la extensión no es de ningún tipo;
  - `data class MetadatosVideo(val ancho: Int, val alto: Int, val mime: String?, val tasaBps: Int?)`;
  - `sealed interface MotivoRechazo`, con `ResolucionExcedida(val ancho: Int, val alto: Int)`, `CodecNoSoportado(val mime: String?)`, `TasaExcedida(val tasaBps: Int)` y `data object ArchivoIlegible`;
  - `fun evaluarVideo(m: MetadatosVideo): MotivoRechazo?`, que devuelve `null` si el video se acepta;
  - `const val ANCHO_MAXIMO = 3840`, `const val ALTO_MAXIMO = 2160` y `const val TASA_MAXIMA_BPS = <D-13>`.

- [ ] **Paso 1: escribir las pruebas que fallan**

```kotlin
class ReglasContenidoTest {
    private fun v(ancho: Int, alto: Int, mime: String? = "video/hevc", tasa: Int? = 20_000_000) =
        MetadatosVideo(ancho, alto, mime, tasa)

    @Test fun clasificaPorExtension() {
        assertEquals(TipoContenido.VIDEO, clasificar("Atardecer en Machu Picchu.mp4"))
        assertEquals(TipoContenido.FOTO, clasificar("Reserva Amazónica 01.heic"))
        assertEquals(TipoContenido.MUSICA, clasificar("Selva.flac"))
        assertNull(clasificar("documento.pdf"))
    }
    @Test fun extensionEnMayusculas() = assertEquals(TipoContenido.VIDEO, clasificar("VIDEO.MP4"))
    @Test fun sinExtensionSeIgnora() = assertNull(clasificar("LEEME"))
    @Test fun aceptaCuatroK() = assertNull(evaluarVideo(v(3840, 2160)))
    @Test fun aceptaCuatroKVertical() = assertNull(evaluarVideo(v(2160, 3840)))
    @Test fun rechazaOchoK() = assertEquals(MotivoRechazo.ResolucionExcedida(7680, 4320), evaluarVideo(v(7680, 4320)))
    @Test fun rechazaCuatroKDeCine() = assertEquals(MotivoRechazo.ResolucionExcedida(4096, 2160), evaluarVideo(v(4096, 2160)))
    @Test fun rechazaCodecNoSoportado() = assertEquals(MotivoRechazo.CodecNoSoportado("video/av01"), evaluarVideo(v(1920, 1080, "video/av01")))
    @Test fun aceptaH264() = assertNull(evaluarVideo(v(1920, 1080, "video/avc")))
    @Test fun rechazaTasaExcedida() = assertEquals(MotivoRechazo.TasaExcedida(TASA_MAXIMA_BPS + 1), evaluarVideo(v(3840, 2160, tasa = TASA_MAXIMA_BPS + 1)))
    @Test fun tasaDesconocidaSeAcepta() = assertNull(evaluarVideo(v(3840, 2160, tasa = null)))
}
```

La tasa desconocida se acepta porque algunos MKV no la declaran; si el video se traba, lo dice el reproductor del sprint 2. El orden de evaluación es resolución, luego códec y luego tasa: se informa el primer motivo.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `cd apps/tv && ./gradlew testDebugUnitTest --tests '*ReglasContenidoTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `ReglasContenido.kt`** (sin imports de Android).

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*ReglasContenidoTest'`
Expected: PASS, 11 pruebas.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(dominio): reglas de contenido: tipos, 4k, códecs y tasa (paso 1.1)"
git checkout sprint-1/integracion && git merge --no-ff feat/dominio-reglas-contenido
```

---

### Paso 1.2: biblioteca local

**Rama:** `feat/datos-biblioteca`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/datos/biblioteca/FuenteArchivos.kt` (interfaz e implementación `CarpetaContenido`);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/datos/biblioteca/LectorMetadatos.kt` (interfaz e implementación `LectorMetadatosAndroid`, con `MediaMetadataRetriever`);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/datos/biblioteca/Biblioteca.kt`;
  - `tool/copiar-contenido.sh`.
- Prueba: `apps/tv/app/src/test/java/pe/inkaterra/tv/datos/biblioteca/BibliotecaTest.kt`, con dobles en el mismo archivo.

**Interfaces:**
- Consume: `clasificar`, `evaluarVideo`, `MetadatosVideo`, `MotivoRechazo` y `TipoContenido` (paso 1.1).
- Produce:
  - `data class ArchivoLocal(val ruta: String, val nombre: String, val tamanoBytes: Long)`;
  - `interface FuenteArchivos { fun listar(): List<ArchivoLocal> }`. `CarpetaContenido(raiz: File)` lista recursivamente y devuelve una lista vacía si `raiz` no existe;
  - `interface LectorMetadatos { fun video(ruta: String): MetadatosVideo }`, que lanza una excepción si el archivo es ilegible;
  - `data class Elemento(val archivo: ArchivoLocal, val tipo: TipoContenido, val titulo: String)`, donde `titulo` es el nombre sin la extensión;
  - `data class Rechazado(val archivo: ArchivoLocal, val motivo: MotivoRechazo)`;
  - `data class ContenidoBiblioteca(val videos: List<Elemento>, val fotos: List<Elemento>, val musica: List<Elemento>, val rechazados: List<Rechazado>)`, con `companion object { val VACIO }`;
  - `class Biblioteca(private val fuente: FuenteArchivos, private val lector: LectorMetadatos, private val despachador: CoroutineDispatcher = Dispatchers.IO) { suspend fun cargar(): ContenidoBiblioteca }`.

- [ ] **Paso 1: escribir las pruebas que fallan**

Escribir las pruebas con dobles: `FuenteFalsa(listOf(...))` y `LectorFalso(mapOf(ruta to MetadatosVideo))`, que lanza `IllegalStateException` para las rutas marcadas como dañadas.

- `separaPorTipo`: dos mp4, un jpg y un mp3 dan 2 videos, 1 foto y 1 canción.
- `rechazaVideoOchoK`: un mp4 de 7680 × 4320 queda en `rechazados` con `ResolucionExcedida(7680, 4320)` y no en `videos`.
- `archivoIlegibleNoRompeLaCarga`: con un video dañado y uno bueno, se carga el bueno y el dañado queda con `ArchivoIlegible`.
- `ignoraLoQueNoEsContenido`: `notas.pdf` no aparece en ningún lado.
- `ordenaComoEnEspanol`: «Zorro.jpg», «Árbol.jpg» y «Cóndor.jpg» dan los títulos `["Árbol", "Cóndor", "Zorro"]`. Se ordena con `java.text.Collator.getInstance(Locale("es"))` y `strength = PRIMARY`.
- `carpetaInexistenteDaBibliotecaVacia`: `CarpetaContenido(File("no-existe"))` y un lector cualquiera dan `ContenidoBiblioteca.VACIO`.

Las fotos y la música no pasan por `LectorMetadatos` en este sprint: se aceptan por su extensión. La reducción de fotos de más de 4K se hace al mostrarlas, en el sprint 2.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*BibliotecaTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `FuenteArchivos`/`CarpetaContenido`, `LectorMetadatos` y `Biblioteca`**
  - `LectorMetadatosAndroid` lee `METADATA_KEY_VIDEO_WIDTH`, `VIDEO_HEIGHT`, `MIMETYPE` y `BITRATE`, y siempre llama a `release()` en un `finally`.
  - El mime del video se lee con `MediaExtractor` (el formato de la pista de video), porque `METADATA_KEY_MIMETYPE` devuelve el del contenedor (`video/mp4`).

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*BibliotecaTest'`
Expected: PASS, 6 pruebas.

- [ ] **Paso 5: escribir `tool/copiar-contenido.sh`**
  - Copia `muestras/contenido/{videos,fotos,musica,promociones}` a `/sdcard/Android/data/pe.inkaterra.tv/files/contenido/` con `adb -s $ONN push`.
  - Antes, preparar la carpeta `muestras/contenido/` con fotos y videos de inkaterra.com (los que Adrián autorizó para pruebas), más un video 8K y uno AV1 generados con `tool/videos-prueba.sh`, para ver los rechazos.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(datos): biblioteca local con las reglas de contenido (paso 1.2)"
```

---

### Paso 1.3: tema de Inkaterra

**Rama:** `feat/app-tema`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/res/font/`: `josefin_sans.ttf` y `cinzel.ttf`, variables, de `github.com/google/fonts` en `ofl/josefinsans` y `ofl/cinzel`;
  - `apps/tv/app/src/main/assets/licencias/OFL-josefin-sans.txt` y `OFL-cinzel.txt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/app/Tema.kt`.
- Prueba: `apps/tv/app/src/test/java/pe/inkaterra/tv/app/TemaTest.kt` (Robolectric).

**Interfaces:**
- Produce:
  - `object ColoresInkaterra { val verde = Color(0xFF5A6A40); val verdeSecundario = Color(0xFF626A45); val crema = Color(0xFFF3F2EE); val texto = Color(0xFF424242); val fondo = Color(0xFF1E2416) }`;
  - `val familiaTexto: FontFamily` (Josefin Sans) y `val familiaTitulos: FontFamily` (Cinzel);
  - `@Composable fun TemaInkaterra(content: @Composable () -> Unit)`: `androidx.tv.material3.MaterialTheme` con `darkColorScheme(primary = verde, secondary = verdeSecundario, background = fondo, surface = fondo, onPrimary = blanco, onBackground = crema, onSurface = crema)` y una tipografía con `familiaTitulos` en `display*`/`headline*` y `familiaTexto` en el resto.

- [ ] **Paso 1: escribir la prueba que falla**

```kotlin
@RunWith(AndroidJUnit4::class)
class TemaTest {
    @get:Rule val regla = createComposeRule()

    @Test fun usaLosColoresYLetrasDeInkaterra() {
        lateinit var esquema: ColorScheme
        lateinit var tipos: Typography
        regla.setContent { TemaInkaterra { esquema = MaterialTheme.colorScheme; tipos = MaterialTheme.typography } }
        assertEquals(Color(0xFF5A6A40), esquema.primary)
        assertEquals(Color(0xFF1E2416), esquema.background)
        assertEquals(familiaTitulos, tipos.headlineLarge.fontFamily)
        assertEquals(familiaTexto, tipos.bodyLarge.fontFamily)
    }
}
```

- [ ] **Paso 2: correrla y ver que falla**

Run: `./gradlew testDebugUnitTest --tests '*TemaTest'`
Expected: FAIL.

- [ ] **Paso 3: descargar las fuentes con sus licencias e implementar `Tema.kt`.** `MainActivity` envuelve su contenido en `TemaInkaterra`. En `app/build.gradle.kts` agregar `testOptions { unitTests.isIncludeAndroidResources = true }`, que Robolectric necesita para leer las fuentes y las cadenas.

- [ ] **Paso 4: correrla y ver que pasa, y mirarla en el onn**

Run: `./gradlew testDebugUnitTest --tests '*TemaTest' && cd ../.. && tool/onn.sh instalar`
Expected: PASS. En la TV, «Inkaterra TV» sale en Cinzel sobre el fondo `#1E2416`.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(app): tema de inkaterra: verde, josefin sans y cinzel (paso 1.3)"
```

---

### Paso 1.4: inicio, secciones y navegación con el control

**Rama:** `feat/pantallas-inicio`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/app/Contenedor.kt` y `app/Navegacion.kt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/inicio/InicioPantalla.kt` (reemplaza a la del paso 0.1);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/seccion/SeccionPantalla.kt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/seccion/SeccionModelo.kt`.
- Modificar: `app/MainActivity.kt`, `res/values/strings.xml` y `res/values-en/strings.xml`.
- Prueba: `apps/tv/app/src/test/java/pe/inkaterra/tv/pantallas/NavegacionTest.kt` (Robolectric, con una `Biblioteca` falsa).

**Interfaces:**
- Consume: `Biblioteca`, `ContenidoBiblioteca`, `Elemento` y `TipoContenido` (paso 1.2), y `TemaInkaterra` (paso 1.3).
- Produce:
  - `class Contenedor(context: Context) { val biblioteca: Biblioteca }`, creado en `MainActivity`. Las pruebas pasan su propio `Biblioteca` a `Navegacion`;
  - `@Composable fun Navegacion(biblioteca: Biblioteca)`, con un `NavHost` y las rutas `"inicio"` y `"seccion/{tipo}"`, donde `tipo` es el `name` de un `TipoContenido`;
  - `@Composable fun InicioPantalla(alElegir: (TipoContenido) -> Unit)`: tres `androidx.tv.material3.Card` en fila, «Videos», «Fotos» y «Música», con `Modifier.testTag("tarjeta-VIDEO")` (y `FOTO`, `MUSICA`). La primera recibe el foco al abrir con un `FocusRequester`;
  - `class SeccionModelo(biblioteca: Biblioteca) : ViewModel { val estado: StateFlow<EstadoSeccion> }`, con `sealed interface EstadoSeccion { Cargando; Lista(val elementos: List<Elemento>) }`;
  - `@Composable fun SeccionPantalla(tipo: TipoContenido, estado: EstadoSeccion)`, que lista los títulos con un `LazyColumn` de elementos enfocables (`androidx.tv.material3.ListItem`). Si la lista está vacía, muestra el texto `seccion_vacia_<tipo>`.

Textos (en `values/` y `values-en/`):

| Clave | Español | Inglés |
| --- | --- | --- |
| `seccion_videos` | Videos | Videos |
| `seccion_fotos` | Fotos | Photos |
| `seccion_musica` | Música | Music |
| `seccion_vacia_video` | Todavía no hay videos | No videos yet |
| `seccion_vacia_foto` | Todavía no hay fotos | No photos yet |
| `seccion_vacia_musica` | Todavía no hay música | No music yet |

- [ ] **Paso 1: escribir las pruebas que fallan**

Todas con `regla.setContent { TemaInkaterra { Navegacion(bibliotecaFalsa) } }` y teclas con `onRoot().performKeyInput { pressKey(Key.DirectionRight) }`.

- `alAbrirElFocoEstaEnVideos`: `onNodeWithTag("tarjeta-VIDEO").assertIsFocused()`.
- `laFlechaDerechaPasaAFotos`: después de `DirectionRight`, `tarjeta-FOTO` tiene el foco.
- `entrarAVideosListaLosTitulos`: con la biblioteca falsa que trae «Atardecer en Machu Picchu» y «Río Madre de Dios», `Enter` en Videos muestra los dos títulos.
- `seccionVaciaMuestraMensaje`: con `ContenidoBiblioteca.VACIO`, entrar a Fotos muestra «Todavía no hay fotos».
- `atrasDevuelveElFocoALaTarjeta`: Derecha, Enter (Fotos) y `Key.Back` dejan el foco en `tarjeta-FOTO`. Se logra guardando el último tipo elegido con `rememberSaveable` en `Navegacion` y pidiendo el foco a su `FocusRequester` al volver.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*NavegacionTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `Contenedor`, `Navegacion`, `InicioPantalla`, `SeccionModelo` y `SeccionPantalla`**
  - El modelo carga en `init` con `viewModelScope` y la biblioteca recibida, y filtra por el `tipo` de la ruta.
  - Tarjetas de 56 dp de alto como mínimo para el texto, con borde `ColoresInkaterra.verde` al tener el foco (el `CardDefaults.border` de tv-material).

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*NavegacionTest'`
Expected: PASS, 5 pruebas.

- [ ] **Paso 5: probar en el onn con contenido real**

Run: `tool/copiar-contenido.sh && tool/onn.sh instalar`
Expected: con el control del onn se recorren las tres tarjetas, se entra a cada sección, se ven los títulos de las muestras y no aparecen el video 8K ni el AV1. Atrás vuelve a la tarjeta de origen.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(pantallas): inicio y secciones con el control remoto (paso 1.4)"
```

---

### Paso 1.5: elegir el idioma

**Rama:** `feat/pantallas-idioma`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/sistema/idioma/AjusteIdioma.kt` (interfaz e implementación `AjusteIdiomaAndroid`);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/idioma/IdiomaPantalla.kt`.
- Modificar:
  - `app/MainActivity.kt` (extiende `AppCompatActivity`);
  - `app/Navegacion.kt` (ruta `"idioma"` como inicio si no hay idioma elegido);
  - `app/Contenedor.kt`;
  - `AndroidManifest.xml` (servicio `androidx.appcompat.app.AppLocalesMetadataHolderService` con `autoStoreLocales=true`, `android:localeConfig="@xml/idiomas"` y `android:theme="@style/Theme.AppCompat.NoActionBar"` en la app, que `AppCompatActivity` exige);
  - `res/xml/idiomas.xml` (es, en) y las cadenas.
- Prueba: `apps/tv/app/src/test/java/pe/inkaterra/tv/pantallas/IdiomaTest.kt`.

**Interfaces:**
- Produce:
  - `enum class Idioma(val etiqueta: String) { ESPANOL("es"), INGLES("en") }`;
  - `interface AjusteIdioma { fun actual(): Idioma?; fun cambiar(idioma: Idioma) }`. La implementación usa `AppCompatDelegate.getApplicationLocales()` y `setApplicationLocales(LocaleListCompat.forLanguageTags(etiqueta))`, y `actual()` devuelve `null` si nunca se eligió;
  - `@Composable fun IdiomaPantalla(alElegir: (Idioma) -> Unit)`, con dos botones fijos en su propio idioma, «Español» e «English», y el título bilingüe «Bienvenido · Welcome»;
  - `Navegacion(biblioteca: Biblioteca, idioma: AjusteIdioma)`: la firma crece con este parámetro, y la prueba del paso 1.4 pasa un doble que devuelve `ESPANOL`.
- También: un acceso en el inicio para cambiar de idioma más tarde, el botón «English» (o «Español», según el actual) en la esquina superior, con `testTag("cambiar-idioma")`.

- [ ] **Paso 1: escribir las pruebas que fallan**

Las pruebas usan un `AjusteIdiomaFalso`.

- `primeraVezPreguntaElIdioma`: si `actual()` es `null`, se ve «Bienvenido · Welcome» y el foco está en «Español».
- `elegirInglesGuardaYPasaAlInicio`: Derecha, Enter llama a `cambiar(INGLES)` una vez y se ve la tarjeta de videos.
- `conIdiomaElegidoVaDirectoAlInicio`: con `actual()` igual a `ESPANOL`, no aparece la pantalla de idioma.
- `cambiarIdiomaDesdeElInicio`: el foco en `cambiar-idioma` más Enter llama a `cambiar(INGLES)`.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*IdiomaTest' --tests '*NavegacionTest'`
Expected: FAIL en `IdiomaTest`.

- [ ] **Paso 3: implementar `Idioma`, `AjusteIdioma`, `IdiomaPantalla`, la ruta `"idioma"` y el acceso para cambiar el idioma.** Ajustar `NavegacionTest` a la nueva firma.

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest`
Expected: PASS en todas.

- [ ] **Paso 5: probar en el onn**

Run: `adb -s 192.168.31.112:5555 shell pm clear pe.inkaterra.tv && tool/onn.sh instalar`
Expected: aparece la pantalla de idioma. Con «English» el inicio dice «Photos» y «Music». Al cerrar y abrir la app ya no pregunta, y desde el inicio se vuelve a español.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(pantallas): elegir el idioma de la app (paso 1.5)"
```

---

### Paso 1.6: prueba integral en el onn, informe y PR

**Rama:** `test/flujo-sprint1`

**Archivos:**
- Prueba en el equipo: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/FlujoSprint1Test.kt`.
- Modificar: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/AppArrancaTest.kt`, que ahora espera la pantalla de idioma o el inicio.
- Crear: `docs/informes/sprint-1.md`.
- Modificar: `CHANGELOG.md`, `docs/arquitectura.md` (navegación y biblioteca) y `docs/plan-de-desarrollo.md` (estado del sprint).

**Interfaces:**
- Consume: todo lo anterior. La prueba usa `createAndroidComposeRule<MainActivity>()` y escribe sus propios archivos falsos (archivos vacíos con nombre `.jpg` y `.mp3`) en una subcarpeta temporal `contenido-prueba/` de `getExternalFilesDir`. Para eso el `Contenedor` acepta la raíz por un extra del `Intent` solo en builds de depuración (`BuildConfig.DEBUG`).

- [ ] **Paso 1: escribir la prueba en el equipo**
  - `FlujoSprint1Test.recorreIdiomaInicioYSecciones`: limpia el idioma, elige Español, va a Fotos con Derecha y Enter, comprueba que se ve la foto falsa, vuelve con Atrás y comprueba el foco en Fotos.
  - Las teclas se envían con `InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)`, como el control real.

- [ ] **Paso 2: correr todo lo del CI y las pruebas en el onn**

Run: `cd apps/tv && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug && ANDROID_SERIAL=192.168.31.112:5555 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=pe.inkaterra.tv.sistema.VolumenesUsbTest`
Expected: BUILD SUCCESSFUL y todas en verde.

- [ ] **Paso 3: escribir el informe y el CHANGELOG, hacer el commit, unir y abrir el PR** de `sprint-1/integracion` hacia `main` después del PR del sprint 0 (solo con el visto bueno de Adrián)

```bash
git commit -m "test(app): flujo del sprint 1 en el onn e informe (paso 1.6)"
```

**Criterio de aceptación del sprint:** con el control del onn se elige el idioma, se recorre todo el inicio y cada sección lista los archivos de la carpeta del hotel, sin los que no cumplen las reglas de contenido.
