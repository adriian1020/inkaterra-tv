# Sprint 1 — Base y navegación: plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendada) o superpowers:executing-plans para implementar este plan paso a paso. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** que el huésped elija su idioma y recorra con el control remoto un inicio estilo Netflix con la imagen de Inkaterra: un área destacada que sigue al foco y filas Destacados, Videos, Fotos y Música con la miniatura de cada archivo de la carpeta del hotel, ya validado con las reglas de contenido.

**Arquitectura:**
- Las reglas de contenido (tipo por extensión, máximo 4K, códecs, tasa de D-13) son Kotlin puro en `dominio/`.
- La biblioteca (`datos/biblioteca/`) lee la carpeta del hotel detrás de dos interfaces, `FuenteArchivos` y `LectorMetadatos`, con dobles para las pruebas. Las miniaturas (`datos/miniaturas/`) se generan detrás de `GeneradorMiniatura` y se guardan en la caché de la app.
- Las pantallas (`pantallas/`) usan Compose for TV y navigation-compose, y reciben todo de un `Contenedor` manual en `app/`, sin Hilt.

**Tecnología:**
- Lo del sprint 0, más navigation-compose 2.10.2, lifecycle-viewmodel-compose 2.11.0, appcompat 1.8.0 (idioma por app), kotlinx-coroutines 1.11.0 y Coil 3.3.0 (`io.coil-kt.coil3:coil-compose`, para mostrar las miniaturas con caché en memoria).
- Pruebas de pantallas con `ui-test-junit4` (BOM) sobre Robolectric 4.17, en la PC.

**Spec:** [docs/plan-de-desarrollo.md](../plan-de-desarrollo.md): Fase 1, sprint 1, «Reglas de contenido», «Imagen de marca» y CU-01; y [docs/decisiones.md](../decisiones.md), D-20 (inicio estilo Netflix).

**Depende del sprint 0:** el proyecto, el CI y `tool/tv.sh` (paso 0.1), la tasa máxima D-13 (paso 0.4), `soporta4k` (paso 0.3) y los videos de `tool/videos-prueba.sh` en la TV (paso 0.4).

## Restricciones globales

- Las mismas del sprint 0 (idioma, comentarios, commits, ramas, versiones exactas y chequeos del CI antes de cada commit). Las ramas salen de `sprint-1/integracion`, que sale de `sprint-0/integracion`.
- Carpeta del hotel: `getExternalFilesDir("contenido")`, con las subcarpetas `videos/`, `fotos/`, `musica/` y `promociones/`. Así `adb push` puede escribir sin root: `/sdcard/Android/data/pe.inkaterra.tv/files/contenido/`.
- Máximo 3840 × 2160. Se compara el lado mayor con 3840 y el menor con 2160, para que los videos verticales de 2160 × 3840 también entren.
- Video: extensiones `mp4`, `mkv`, `mov` y `webm`; códecs `video/avc`, `video/hevc` y `video/x-vnd.on2.vp9` (D-19). Foto: `jpg`, `jpeg`, `png`, `webp` y `heic`. Música: `mp3`, `m4a`, `aac` y `flac`. Las extensiones se comparan sin importar mayúsculas.
- Tasa máxima de video: `TASA_MAXIMA_BPS = 60_000_000` (D-13).
- Colores: verde `#5A6A40`, verde secundario `#626A45`, crema `#F3F2EE`, texto `#424242`, fondo de la TV `#1E2416`. Letras: Josefin Sans para el texto y Cinzel para los títulos, dentro de la app con su licencia OFL en `assets/licencias/`.
- Inicio (D-20): filas en el orden Destacados, Videos, Fotos y Música; una fila vacía no se muestra. Destacados son las fotos y videos de `promociones/`. Tarjetas 16:9 que con el foco crecen a 1,1 y llevan un borde verde `#5A6A40` de 3 dp. El área destacada cambia cuando el foco lleva 500 ms en una tarjeta.
- Miniaturas (D-20): una por archivo, JPEG de calidad 85 que cubre 1280 × 720 sin deformar (el recorte lo hace la tarjeta), guardada en `cacheDir/miniaturas/`. Como máximo dos se generan a la vez.
- Idiomas: español (por omisión, en `values/`) e inglés (`values-en/`). Ningún texto visible va escrito en el código.
- Datos de prueba inventados: «Atardecer en Machu Picchu.mp4», «Reserva Amazónica 01.jpg» y similares.

## Foco de revisión

1. **Primera instalación, con la carpeta del hotel inexistente o vacía:** la biblioteca vuelve vacía sin error, y el inicio muestra «Todavía no hay contenido» en vez de una pantalla en blanco o filas vacías. Pruebas `carpetaInexistenteDaBibliotecaVacia` (paso 1.2) y `sinContenidoMuestraMensaje` (paso 1.5).
2. **Un archivo dañado:** el lector de metadatos lanza una excepción y el archivo va a «rechazados» con `ArchivoIlegible` mientras el resto se carga; si lo que falla es la miniatura, la tarjeta queda con su recuadro verde y la app sigue. Pruebas `archivoIlegibleNoRompeLaCarga` (paso 1.2) y `errorDelGeneradorDaNulo` (paso 1.3).
3. **Muchos videos 4K la primera vez:** las miniaturas se generan de a dos en segundo plano, sin trabar el control ni agotar los decodificadores del equipo. Prueba `generaComoMaximoDosALaVez` (paso 1.3) y la medición en el Xiaomi del paso 1.3.
4. **Nombres con tildes, ñ, espacios o muy largos:** se muestran sin la extensión, se ordenan como en español («Árbol» junto a la A, no al final) y, si no entran, se cortan con «…» en una línea (dos en el área destacada). Prueba `ordenaComoEnEspanol` (paso 1.2) y la revisión en el Xiaomi del paso 1.5.
5. **Atrás:** desde lo abierto, el foco regresa a la tarjeta de origen; en el inicio, fuera de la primera fila, sube al primer destacado; en la primera fila, sale de la app. Pruebas `atrasDevuelveElFocoALaTarjeta`, `atrasFueraDeLaPrimeraFilaSubeAlInicio` y `atrasEnLaPrimeraFilaSaleDeLaApp` (paso 1.5).

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
  - `const val ANCHO_MAXIMO = 3840`, `const val ALTO_MAXIMO = 2160` y `const val TASA_MAXIMA_BPS = 60_000_000`.

- [ ] **Paso 1: escribir las pruebas que fallan**

```kotlin
class ReglasContenidoTest {
    private fun v(ancho: Int, alto: Int, mime: String? = "video/hevc", tasa: Int? = 20_000_000) =
        MetadatosVideo(ancho, alto, mime, tasa)

    @Test fun clasificaPorExtension() {
        assertEquals(TipoContenido.VIDEO, clasificar("Atardecer en Machu Picchu.mp4"))
        assertEquals(TipoContenido.FOTO, clasificar("Reserva Amazónica 01.heic"))
        assertEquals(TipoContenido.MUSICA, clasificar("Selva.flac"))
        assertEquals(TipoContenido.VIDEO, clasificar("Celular.webm"))
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
    @Test fun aceptaVp9() = assertNull(evaluarVideo(v(3840, 2160, "video/x-vnd.on2.vp9")))
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
Expected: PASS, 12 pruebas.

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
- Prueba:
  - `apps/tv/app/src/test/java/pe/inkaterra/tv/datos/biblioteca/BibliotecaTest.kt`;
  - `apps/tv/app/src/test/java/pe/inkaterra/tv/datos/biblioteca/DoblesBiblioteca.kt`, con los dobles, que también usan las pruebas de pantallas.

**Interfaces:**
- Consume: `clasificar`, `evaluarVideo`, `MetadatosVideo`, `MotivoRechazo` y `TipoContenido` (paso 1.1).
- Produce:
  - `data class ArchivoLocal(val ruta: String, val nombre: String, val tamanoBytes: Long, val modificadoMs: Long, val esPromocion: Boolean)`;
  - `interface FuenteArchivos { fun listar(): List<ArchivoLocal> }`. `CarpetaContenido(raiz: File)` lista recursivamente, marca `esPromocion = true` lo que está dentro de `raiz/promociones/` y devuelve una lista vacía si `raiz` no existe;
  - `interface LectorMetadatos { fun video(ruta: String): MetadatosVideo }`, que lanza una excepción si el archivo es ilegible;
  - `data class Elemento(val archivo: ArchivoLocal, val tipo: TipoContenido, val titulo: String)`, donde `titulo` es el nombre sin la extensión;
  - `data class Rechazado(val archivo: ArchivoLocal, val motivo: MotivoRechazo)`;
  - `data class ContenidoBiblioteca(val destacados: List<Elemento>, val videos: List<Elemento>, val fotos: List<Elemento>, val musica: List<Elemento>, val rechazados: List<Rechazado>)`, con `companion object { val VACIO }`;
  - `class Biblioteca(private val fuente: FuenteArchivos, private val lector: LectorMetadatos, private val despachador: CoroutineDispatcher = Dispatchers.IO) { suspend fun cargar(): ContenidoBiblioteca }`;
  - en `DoblesBiblioteca.kt`: `class FuenteFalsa(archivos: List<ArchivoLocal>)`, `class LectorFalso(metadatos: Map<String, MetadatosVideo>, danados: Set<String> = emptySet())`, que lanza `IllegalStateException` para las rutas de `danados`, y `fun archivo(nombre: String, esPromocion: Boolean = false, modificadoMs: Long = 0): ArchivoLocal`.

- [ ] **Paso 1: escribir las pruebas que fallan**

- `separaPorTipo`: dos mp4, un jpg y un mp3 dan 2 videos, 1 foto y 1 canción, y ningún destacado.
- `promocionesVanADestacados`: un mp4, un jpg y un mp3 con `esPromocion = true` dan 2 destacados (el video y la foto) y nada en videos, fotos ni música. La música en `promociones/` se ignora en silencio: las promociones son fotos y videos (CU-05).
- `rechazaVideoOchoK`: un mp4 de 7680 × 4320 queda en `rechazados` con `ResolucionExcedida(7680, 4320)` y no en `videos`. Lo mismo vale para un destacado.
- `archivoIlegibleNoRompeLaCarga`: con un video dañado y uno bueno, se carga el bueno y el dañado queda con `ArchivoIlegible`.
- `ignoraLoQueNoEsContenido`: `notas.pdf` no aparece en ningún lado.
- `ordenaComoEnEspanol`: «Zorro.jpg», «Árbol.jpg» y «Cóndor.jpg» dan los títulos `["Árbol", "Cóndor", "Zorro"]`. Se ordena con `java.text.Collator.getInstance(Locale("es"))` y `strength = PRIMARY`, en cada lista.
- `carpetaInexistenteDaBibliotecaVacia`: `CarpetaContenido(File("no-existe"))` y un lector cualquiera dan `ContenidoBiblioteca.VACIO`.
- `carpetaMarcaPromociones`: con un `TemporaryFolder` que tiene `promociones/Oferta spa.jpg` y `fotos/Reserva Amazónica 01.jpg`, `CarpetaContenido.listar()` marca la primera como promoción y la segunda no, y trae su fecha de modificación.

Las fotos y la música no pasan por `LectorMetadatos` en este sprint: se aceptan por su extensión. La reducción de fotos de más de 4K se hace al mostrarlas, en el sprint 2.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*BibliotecaTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `FuenteArchivos`/`CarpetaContenido`, `LectorMetadatos` y `Biblioteca`**
  - `LectorMetadatosAndroid` lee `METADATA_KEY_VIDEO_WIDTH`, `VIDEO_HEIGHT` y `BITRATE`, y siempre llama a `release()` en un `finally`.
  - El mime del video se lee con `MediaExtractor` (el formato de la pista de video), porque `METADATA_KEY_MIMETYPE` devuelve el del contenedor (`video/mp4`).

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*BibliotecaTest'`
Expected: PASS, 8 pruebas.

- [ ] **Paso 5: escribir `tool/copiar-contenido.sh`**
  - Copia `muestras/contenido/{videos,fotos,musica,promociones}` a `/sdcard/Android/data/pe.inkaterra.tv/files/contenido/` con `adb -s $TV push`, con `MSYS_NO_PATHCONV=1` y las rutas locales pasadas por `cygpath -w`, como `tool/videos-prueba.sh`.
  - Antes, preparar la carpeta `muestras/contenido/` con fotos y videos de inkaterra.com (los que Adrián autorizó para pruebas), dos o tres en `promociones/`, una canción con carátula y otra sin ella (con ffmpeg, `-i portada.jpg -map 0 -map 1 -disposition:v attached_pic`), y un video 8K y uno AV1 generados con ffmpeg, para ver los rechazos.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(datos): biblioteca local con destacados y reglas de contenido (paso 1.2)"
```

---

### Paso 1.3: miniaturas

**Rama:** `feat/datos-miniaturas`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/datos/miniaturas/GeneradorMiniatura.kt` (interfaz e implementación `GeneradorMiniaturaAndroid`);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/datos/miniaturas/Miniaturas.kt`.
- Prueba:
  - `apps/tv/app/src/test/java/pe/inkaterra/tv/datos/miniaturas/MiniaturasTest.kt` (Robolectric, con `@GraphicsMode(GraphicsMode.Mode.NATIVE)` para que `Bitmap.compress` escriba un JPEG real);
  - en el equipo: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/sistema/MiniaturasEquipoTest.kt`.

**Interfaces:**
- Consume: `Elemento`, `ArchivoLocal` y `TipoContenido` (pasos 1.1 y 1.2).
- Produce:
  - `interface GeneradorMiniatura { fun generar(elemento: Elemento): Bitmap? }`: `null` si el archivo no trae imagen (una canción sin carátula); puede lanzar una excepción si el archivo está dañado;
  - `class GeneradorMiniaturaAndroid : GeneradorMiniatura`:
    - video: `MediaMetadataRetriever.getScaledFrameAtTime(duración / 10 en µs, OPTION_CLOSEST_SYNC, 1280, 720)`, para no tomar el negro del primer cuadro;
    - foto: `ImageDecoder` con `setTargetSize` para cubrir 1280 × 720 sin deformar (lee HEIC);
    - música: `embeddedPicture` decodificada y escalada igual;
    - siempre `release()` en un `finally`;
  - `class Miniaturas(private val generador: GeneradorMiniatura, private val carpeta: File, private val despachador: CoroutineDispatcher = Dispatchers.IO) { suspend fun de(elemento: Elemento): File? }`. Devuelve el JPEG guardado o `null` si no hay imagen o si generarla falló. La clave del archivo es un SHA-1 en hexadecimal de `"$ruta|$tamanoBytes|$modificadoMs"`, con la extensión `.jpg`.

- [ ] **Paso 1: escribir las pruebas que fallan**

Con un `GeneradorFalso` que cuenta sus llamadas y devuelve `Bitmap.createBitmap(16, 9, ARGB_8888)`, y la carpeta en un `TemporaryFolder`:

- `generaUnaVezYReutiliza`: dos llamadas a `de()` con el mismo elemento llaman al generador una sola vez, y devuelven el mismo archivo, que existe y no está vacío.
- `archivoCambiadoGeneraOtra`: el mismo elemento con otro `modificadoMs` vuelve a llamar al generador y da otro archivo.
- `sinImagenDaNulo`: si el generador devuelve `null`, `de()` devuelve `null` y la carpeta queda vacía.
- `errorDelGeneradorDaNulo`: si el generador lanza una excepción, `de()` devuelve `null`, no la propaga y no deja archivos a medias en la carpeta.
- `generaComoMaximoDosALaVez`: con un generador que se bloquea en un `CountDownLatch` y anota cuántos corren a la vez, cinco llamadas simultáneas a `de()` (con `async` sobre `Dispatchers.IO`) no pasan de dos al mismo tiempo.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*MiniaturasTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `GeneradorMiniatura`, `GeneradorMiniaturaAndroid` y `Miniaturas`**
  - Se limita a dos generaciones a la vez con un `Semaphore(2)` de kotlinx-coroutines; dentro del permiso se vuelve a mirar si el archivo ya existe, por si otra llamada lo generó mientras tanto.
  - Se escribe primero en `<clave>.tmp` y luego se renombra, para que una caída a mitad no deje un JPEG roto. Si algo falla, se borra el `.tmp`.

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*MiniaturasTest'`
Expected: PASS, 5 pruebas.

- [ ] **Paso 5: probar el generador real en el Xiaomi**

`MiniaturasEquipoTest` usa los videos que `tool/videos-prueba.sh` dejó en `getExternalFilesDir("prueba-4k")`:
- `cuadroDeVideoHevcYVp9`: `hevc-20m.mp4` y `vp9-20m.webm` dan un `Bitmap` de 1280 de ancho o 720 de alto, como mínimo. Anota en el registro, con la etiqueta «Miniaturas», cuánto tardó cada uno.
- `fotoReducida`: una foto de 3840 × 2160 escrita por la prueba en `cacheDir` da un `Bitmap` de 1280 × 720.
- `audioSinCaratulaDaNulo`: `solo-audio.m4a` da `null`.

Run: `tool/tv.sh probar pe.inkaterra.tv.sistema.MiniaturasEquipoTest`
Expected: `OK (3 tests)`. Anotar en el informe cuánto tarda una miniatura de un video 4K.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(datos): miniaturas de videos, fotos y música en caché (paso 1.3)"
```

---

### Paso 1.4: tema de Inkaterra

**Rama:** `feat/app-tema`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/res/font/`: `josefin_sans.ttf` y `cinzel.ttf`, variables, de `github.com/google/fonts` en `ofl/josefinsans` y `ofl/cinzel`;
  - `apps/tv/app/src/main/assets/licencias/OFL-josefin-sans.txt` y `OFL-cinzel.txt`;
  - `apps/tv/app/src/main/res/drawable/sin_caratula.xml`: vector de 16:9 con fondo verde `#5A6A40` y una nota musical crema `#F3F2EE` al centro, para la música sin carátula;
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

- [ ] **Paso 3: descargar las fuentes con sus licencias e implementar `Tema.kt` y `sin_caratula.xml`.** `MainActivity` envuelve su contenido en `TemaInkaterra`. En `app/build.gradle.kts` agregar `testOptions { unitTests.isIncludeAndroidResources = true }`, que Robolectric necesita para leer las fuentes y las cadenas.

- [ ] **Paso 4: correrla y ver que pasa, y mirarla en el Xiaomi**

Run: `./gradlew testDebugUnitTest --tests '*TemaTest' && cd ../.. && tool/tv.sh instalar`
Expected: PASS. En la TV, «Inkaterra TV» sale en Cinzel sobre el fondo `#1E2416`.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(app): tema de inkaterra: verde, josefin sans y cinzel (paso 1.4)"
```

---

### Paso 1.5: inicio estilo Netflix y navegación con el control

**Rama:** `feat/pantallas-inicio`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/app/Contenedor.kt` y `app/Navegacion.kt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/inicio/Filas.kt` (modelo de filas, sin Compose);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/inicio/InicioModelo.kt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/inicio/InicioPantalla.kt` (reemplaza a la del paso 0.1: filas, foco y Atrás);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/inicio/AreaDestacada.kt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/inicio/TarjetaContenido.kt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/comun/Miniatura.kt`;
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/abrir/AbrirPantalla.kt` (provisional hasta el sprint 2).
- Modificar: `app/MainActivity.kt`, `gradle/libs.versions.toml` y `app/build.gradle.kts` (Coil), `res/values/strings.xml` y `res/values-en/strings.xml`.
- Prueba:
  - `apps/tv/app/src/test/java/pe/inkaterra/tv/pantallas/inicio/FilasTest.kt` (JUnit, sin Robolectric);
  - `apps/tv/app/src/test/java/pe/inkaterra/tv/pantallas/NavegacionTest.kt` (Robolectric, con los dobles del paso 1.2 y un generador de miniaturas que devuelve `null`).

**Interfaces:**
- Consume: `Biblioteca`, `ContenidoBiblioteca`, `Elemento` y `TipoContenido` (pasos 1.1 y 1.2), `Miniaturas` (paso 1.3), y `TemaInkaterra`, `ColoresInkaterra` y `R.drawable.sin_caratula` (paso 1.4).
- Produce:
  - `enum class TipoFila { DESTACADOS, VIDEOS, FOTOS, MUSICA }` y `data class Fila(val tipo: TipoFila, val elementos: List<Elemento>)`;
  - `fun filasDe(contenido: ContenidoBiblioteca): List<Fila>`, en el orden del enum y sin las filas vacías. En el sprint 3 se agrega `USB` después de `DESTACADOS`;
  - `sealed interface EstadoInicio { data object Cargando : EstadoInicio; data class Listo(val filas: List<Fila>) : EstadoInicio }`;
  - `class InicioModelo(biblioteca: Biblioteca) : ViewModel { val estado: StateFlow<EstadoInicio> }`, que carga en `init` con `viewModelScope`;
  - `class Contenedor(context: Context, raiz: File = requireNotNull(context.getExternalFilesDir("contenido"))) { val biblioteca: Biblioteca; val miniaturas: Miniaturas }`, con las miniaturas en `File(context.cacheDir, "miniaturas")`. Se crea en `MainActivity`;
  - `@Composable fun Navegacion(biblioteca: Biblioteca, miniaturas: Miniaturas)`, con un `NavHost` y las rutas `"inicio"` y `"abrir/{ruta}"` (la ruta del archivo con `Uri.encode`);
  - `@Composable fun InicioPantalla(estado: EstadoInicio, miniaturas: Miniaturas, alElegir: (Elemento) -> Unit)`:
    - arriba, `AreaDestacada` en la mitad superior: la miniatura a la derecha, que se funde con el fondo `#1E2416` con un degradado de izquierda a derecha, y a la izquierda el título en `headlineLarge` (Cinzel, hasta dos líneas con «…») y el tipo. Lleva `testTag("destacado-titulo")` en el título;
    - debajo, un `LazyColumn` de filas con su título (`testTag("fila-<TipoFila>")`) y un `LazyRow` de `TarjetaContenido` por fila;
    - con `EstadoInicio.Listo(emptyList())` solo muestra el texto `inicio_vacio`;
  - `@Composable fun TarjetaContenido(elemento: Elemento, miniaturas: Miniaturas, alElegir: () -> Unit, modifier: Modifier)`: `androidx.tv.material3.Card` de 200 × 113 dp con `CardDefaults.scale(focusedScale = 1.1f)`, `CardDefaults.border(focusedBorder = Border(BorderStroke(3.dp, ColoresInkaterra.verde)))`, la miniatura y el título en una línea con «…». Lleva `testTag("tarjeta-<TipoFila>-<índice>")`;
  - `@Composable fun Miniatura(elemento: Elemento, miniaturas: Miniaturas, modifier: Modifier)`: pide el archivo con `produceState` y lo muestra con `coil3.compose.AsyncImage` y `ContentScale.Crop`. Mientras no hay archivo, o si es `null`, se ve un recuadro `ColoresInkaterra.verdeSecundario`; para la música sin carátula, `R.drawable.sin_caratula`;
  - `@Composable fun AbrirPantalla(titulo: String)`: el título en grande con `testTag("abrir-titulo")`. El sprint 2 la reemplaza por el reproductor y el visor.

Comportamiento del foco y de Atrás:
- Al abrir, el foco está en la primera tarjeta de la primera fila.
- El área destacada muestra la primera tarjeta al abrir y cambia cuando el foco lleva 500 ms en otra (`LaunchedEffect(enfocado) { delay(500); destacado = enfocado }`).
- Se guarda con `rememberSaveable` la tarjeta enfocada (fila y posición); al volver de `abrir/` se le pide el foco a esa tarjeta. Los `LazyListState` de las filas se recuerdan para que siga visible.
- Atrás en el inicio: con el foco fuera de la primera fila (otra fila o el botón de idioma del paso 1.6), lo lleva a la primera tarjeta de la primera fila (`BackHandler` activo solo en ese caso); en la primera fila no se intercepta y la app se cierra, como cualquier app de Google TV.

Textos (en `values/` y `values-en/`):

| Clave | Español | Inglés |
| --- | --- | --- |
| `fila_destacados` | Destacados | Featured |
| `fila_videos` | Videos | Videos |
| `fila_fotos` | Fotos | Photos |
| `fila_musica` | Música | Music |
| `tipo_video` | Video | Video |
| `tipo_foto` | Foto | Photo |
| `tipo_musica` | Música | Music |
| `inicio_vacio` | Todavía no hay contenido | No content yet |

- [ ] **Paso 1: escribir las pruebas que fallan**

`FilasTest`:
- `filasEnOrdenSinLasVacias`: un contenido con 0 destacados, 2 videos, 0 fotos y 1 canción da las filas `[VIDEOS, MUSICA]`.
- `todoVacioDaSinFilas`: `filasDe(ContenidoBiblioteca.VACIO)` es una lista vacía.

`NavegacionTest`, todas con `@get:Rule val regla = createAndroidComposeRule<ComponentActivity>()` y `regla.setContent { TemaInkaterra { Navegacion(biblioteca, miniaturas) } }`, una biblioteca de `FuenteFalsa` con 2 destacados («Oferta spa.jpg» y «Reserva Amazónica 01.jpg» en promociones), 2 videos («Atardecer en Machu Picchu.mp4» y «Río Madre de Dios.mp4») y 1 canción («Selva.mp3»), y teclas con `onRoot().performKeyInput { pressKey(Key.DirectionRight) }`:
- `alAbrirElFocoEstaEnElPrimerDestacado`: `tarjeta-DESTACADOS-0` tiene el foco y `destacado-titulo` dice «Oferta spa».
- `laFlechaDerechaRecorreLaFila`: después de `DirectionRight`, `tarjeta-DESTACADOS-1` tiene el foco.
- `laFlechaAbajoBajaDeFila`: después de `DirectionDown`, `tarjeta-VIDEOS-0` tiene el foco.
- `areaDestacadaSigueAlFocoConDemora`: con `regla.mainClock.autoAdvance = false`, después de `DirectionDown` y 100 ms, `destacado-titulo` sigue diciendo «Oferta spa»; a los 600 ms dice «Atardecer en Machu Picchu».
- `sinContenidoMuestraMensaje`: con una fuente vacía se ve «Todavía no hay contenido» y no existe ningún `fila-*`.
- `atrasDevuelveElFocoALaTarjeta`: Abajo, Derecha y Enter muestran `abrir-titulo` con «Río Madre de Dios»; `Key.Back` vuelve al inicio con el foco en `tarjeta-VIDEOS-1`.
- `atrasFueraDeLaPrimeraFilaSubeAlInicio`: Abajo, Abajo (Música) y `Key.Back` dejan el foco en `tarjeta-DESTACADOS-0`.
- `atrasEnLaPrimeraFilaSaleDeLaApp`: `Key.Back` en la primera fila deja `regla.activity.isFinishing` en `true`.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*FilasTest' --tests '*NavegacionTest'`
Expected: FAIL.

- [ ] **Paso 3: agregar Coil e implementar `Filas`, `InicioModelo`, `Contenedor`, `Navegacion`, `InicioPantalla`, `AreaDestacada`, `TarjetaContenido`, `Miniatura` y `AbrirPantalla`**

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest --tests '*FilasTest' --tests '*NavegacionTest'`
Expected: PASS, 10 pruebas.

- [ ] **Paso 5: probar en el Xiaomi con contenido real**

Run: `tool/copiar-contenido.sh && tool/tv.sh instalar`
Expected: con el control del Xiaomi:
- se recorren las filas y cada tarjeta muestra su miniatura (la canción sin carátula, la imagen de la marca), sin el video 8K ni el AV1;
- el área destacada cambia al detenerse en una tarjeta;
- un título largo se corta con «…»;
- Atrás vuelve a la tarjeta de origen y luego al primer destacado.

La primera vez, anotar cuánto tardan en aparecer todas las miniaturas; la segunda, deben estar al abrir.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(pantallas): inicio estilo netflix con filas y miniaturas (paso 1.5)"
```

---

### Paso 1.6: elegir el idioma

**Rama:** `feat/pantallas-idioma`

**Archivos:**
- Crear:
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/sistema/idioma/AjusteIdioma.kt` (interfaz e implementación `AjusteIdiomaAndroid`);
  - `apps/tv/app/src/main/java/pe/inkaterra/tv/pantallas/idioma/IdiomaPantalla.kt`.
- Modificar:
  - `app/MainActivity.kt` (extiende `AppCompatActivity`);
  - `app/Navegacion.kt` (ruta `"idioma"` como inicio si no hay idioma elegido);
  - `app/Contenedor.kt`;
  - `pantallas/inicio/InicioPantalla.kt` (botón para cambiar el idioma);
  - `AndroidManifest.xml` (servicio `androidx.appcompat.app.AppLocalesMetadataHolderService` con `autoStoreLocales=true`, `android:localeConfig="@xml/idiomas"` y `android:theme="@style/Theme.AppCompat.NoActionBar"` en la app, que `AppCompatActivity` exige);
  - `res/xml/idiomas.xml` (es, en) y las cadenas.
- Prueba: `apps/tv/app/src/test/java/pe/inkaterra/tv/pantallas/IdiomaTest.kt`.

**Interfaces:**
- Consume: `Navegacion`, `InicioPantalla` y los dobles de `NavegacionTest` (paso 1.5).
- Produce:
  - `enum class Idioma(val etiqueta: String) { ESPANOL("es"), INGLES("en") }`;
  - `interface AjusteIdioma { fun actual(): Idioma?; fun cambiar(idioma: Idioma) }`. La implementación usa `AppCompatDelegate.getApplicationLocales()` y `setApplicationLocales(LocaleListCompat.forLanguageTags(etiqueta))`, y `actual()` devuelve `null` si nunca se eligió;
  - `@Composable fun IdiomaPantalla(alElegir: (Idioma) -> Unit)`, con dos botones fijos en su propio idioma, «Español» e «English», y el título bilingüe «Bienvenido · Welcome»;
  - `Navegacion(biblioteca: Biblioteca, miniaturas: Miniaturas, idioma: AjusteIdioma)`: la firma crece con este parámetro, y `NavegacionTest` pasa un doble que devuelve `ESPANOL`.
- También: en la esquina superior derecha del inicio, sobre el área destacada, el botón «English» (o «Español», según el actual) con `testTag("cambiar-idioma")`. Se llega con Arriba desde la primera fila, y Atrás desde él lleva al primer destacado (la regla del paso 1.5).

- [ ] **Paso 1: escribir las pruebas que fallan**

Las pruebas usan un `AjusteIdiomaFalso`.

- `primeraVezPreguntaElIdioma`: si `actual()` es `null`, se ve «Bienvenido · Welcome» y el foco está en «Español».
- `elegirInglesGuardaYPasaAlInicio`: Derecha, Enter llama a `cambiar(INGLES)` una vez y se ve `tarjeta-DESTACADOS-0`.
- `conIdiomaElegidoVaDirectoAlInicio`: con `actual()` igual a `ESPANOL`, no aparece la pantalla de idioma.
- `cambiarIdiomaDesdeElInicio`: Arriba y Enter, con el foco en `cambiar-idioma`, llaman a `cambiar(INGLES)`.

- [ ] **Paso 2: correrlas y ver que fallan**

Run: `./gradlew testDebugUnitTest --tests '*IdiomaTest' --tests '*NavegacionTest'`
Expected: FAIL en `IdiomaTest`.

- [ ] **Paso 3: implementar `Idioma`, `AjusteIdioma`, `IdiomaPantalla`, la ruta `"idioma"` y el botón para cambiar el idioma.** Ajustar `NavegacionTest` a la nueva firma.

- [ ] **Paso 4: correrlas y ver que pasan**

Run: `./gradlew testDebugUnitTest`
Expected: PASS en todas.

- [ ] **Paso 5: probar en el Xiaomi**

Run: `adb -s 192.168.31.242:5555 shell pm clear pe.inkaterra.tv && tool/copiar-contenido.sh && tool/tv.sh instalar`
Expected: aparece la pantalla de idioma. Con «English» las filas dicen «Featured», «Photos» y «Music». Al cerrar y abrir la app ya no pregunta, y desde el inicio se vuelve a español. `pm clear` borra también el contenido copiado y las miniaturas; por eso se vuelve a copiar.

- [ ] **Paso 6: commit y unión**

```bash
git commit -m "feat(pantallas): elegir el idioma de la app (paso 1.6)"
```

---

### Paso 1.7: prueba integral en el Xiaomi, informe y PR

**Rama:** `test/flujo-sprint1`

**Archivos:**
- Prueba en el equipo: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/FlujoSprint1Test.kt`.
- Modificar: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/AppArrancaTest.kt`, que ahora espera la pantalla de idioma o el inicio.
- Crear: `docs/informes/sprint-1.md`.
- Modificar: `CHANGELOG.md`, `docs/arquitectura.md` (navegación, biblioteca y miniaturas) y `docs/plan-de-desarrollo.md` (estado del sprint).

**Interfaces:**
- Consume: todo lo anterior. La prueba usa `createAndroidComposeRule<MainActivity>()` y escribe sus propios archivos falsos (archivos vacíos con nombre `.jpg` y `.mp3`, en `fotos/` y `musica/`) en una subcarpeta temporal `contenido-prueba/` de `getExternalFilesDir`. Para eso el `Contenedor` acepta la raíz por un extra del `Intent` solo en builds de depuración (`BuildConfig.DEBUG`). Como los archivos están vacíos, sus miniaturas fallan y se ven los recuadros verdes: así se prueba también ese caso.

- [ ] **Paso 1: escribir la prueba en el equipo**
  - `FlujoSprint1Test.recorreIdiomaInicioYAbre`: limpia el idioma, elige Español, comprueba que el foco está en `tarjeta-FOTOS-0` (no hay destacados ni videos), baja a Música, la abre con Enter, comprueba `abrir-titulo`, vuelve con Atrás y comprueba el foco en `tarjeta-MUSICA-0`.
  - Las teclas se envían con `InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN)`, como el control real.

- [ ] **Paso 2: correr todo lo del CI y las pruebas en el Xiaomi**

Run: `(cd apps/tv && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug) && EXCLUIR=pe.inkaterra.tv.sistema.VolumenesUsbTest tool/tv.sh probar`
Expected: BUILD SUCCESSFUL y todas en verde.

- [ ] **Paso 3: escribir el informe y el CHANGELOG, hacer el commit, unir y abrir el PR** de `sprint-1/integracion` hacia `main` después del PR del sprint 0 (solo con el visto bueno de Adrián). El informe incluye cuánto tardan las miniaturas de los videos 4K (pasos 1.3 y 1.5).

```bash
git commit -m "test(app): flujo del sprint 1 en el Xiaomi e informe (paso 1.7)"
```

**Criterio de aceptación del sprint:** con el control del Xiaomi se elige el idioma y se recorre todo el inicio: cada archivo de la carpeta del hotel que cumple las reglas de contenido sale en su fila con su miniatura, el área destacada sigue al foco y Atrás vuelve a la tarjeta de origen.
