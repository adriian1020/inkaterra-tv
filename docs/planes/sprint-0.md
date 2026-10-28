# Sprint 0 — Preparación en el Xiaomi: plan de implementación

> **Para agentes:** SUB-SKILL REQUERIDA: usar superpowers:subagent-driven-development (recomendada) o superpowers:executing-plans para implementar este plan paso a paso. Los pasos usan casillas (`- [ ]`) para el seguimiento.

**Objetivo:** dejar el repositorio listo, con su CI, y una app mínima instalada en el Xiaomi. Con ella se averigua qué decodifica la TV, si el 4K va fluido y qué permite Google TV 14 (pantalla principal, arranque, protector de pantalla, suspensión y USB).

**Arquitectura:** un proyecto Gradle en `apps/tv` con un solo módulo `app`, paquete `pe.inkaterra.tv` y las capas `app/ dominio/ datos/ sistema/ pantallas/`. Lo que se averigua con pruebas en el Xiaomi queda escrito como decisiones (D-13 en adelante) en `docs/decisiones.md`; el código de exploración que no se adopte se borra antes del PR.

**Tecnología:**
- Gradle 9.3.1 y AGP 9.1.0 con el Kotlin integrado de AGP (Kotlin 2.4.0). Es la combinación que ya compila en esta PC con asistencia-movil.
- JDK 21, con bytecode para Java 17.
- `compileSdk` y `targetSdk` 36; `minSdk` 30 (Android 11, por las TCL más antiguas con Google TV).
- Compose BOM 2026.09.00, `androidx.tv:tv-material` 1.1.0 y Media3 1.11.1.
- Pruebas: JUnit 4.13.2, Robolectric 4.17 y `androidx.test` (ext-junit 1.3.0, runner 1.7.0).
- Formato con ktlint (plugin `org.jlleitschuh.gradle.ktlint` 14.2.0) y análisis con Android Lint.

**Spec:** [docs/plan-de-desarrollo.md](../plan-de-desarrollo.md), Fase 0.

## Restricciones globales

- Todo en español: nombres en el código, comentarios, commits, CHANGELOG y docs. Las excepciones son los nombres que impone Android (`MainActivity`, `AndroidManifest.xml`).
- Cada archivo empieza con un comentario breve de qué hace y qué reglas sigue. Los comentarios explican el porqué.
- Commits con el formato `tipo(ámbito): resumen (paso 0.N)`, en minúscula, en presente y de hasta 72 caracteres. Sin firma de Claude.
- Ramas: cada paso en `feat/<capa>-<tema>` (o `chore/`, `docs/`), que sale de `sprint-0/integracion` y vuelve con `git merge --no-ff`.
- Las dependencias van con versión exacta en `apps/tv/gradle/libs.versions.toml`. Subir una es un PR propio.
- Equipo de pruebas: Xiaomi en `192.168.31.242:5555` (`adb connect`). Toda prueba en el equipo usa `-s 192.168.31.242:5555`.
- Los videos y fotos de prueba no se versionan: viven en `muestras/`, ignorada por git.
- Máximo 4K: 3840 × 2160. Formatos de video aceptados: H.264 y HEVC (spec, «Reglas de contenido»).
- Antes de cada commit, lo mismo que el CI, desde `apps/tv`: `./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug`.

## Foco de revisión

1. **El Xiaomi se desconecta de adb** (cambió de IP o se reinició): `tool/tv.sh` debe decir en palabras simples que no encuentra el equipo y cómo reconectarlo, no fallar con un error de adb crudo. Lo cubre la verificación del paso 0.1.
2. **Un video de prueba que el Xiaomi no puede decodificar** (por ejemplo HEVC de 10 bits a 120 Mbps): la prueba de 4K debe registrar «no reproduce» para ese caso y seguir con los demás, no colgarse. Lo cubre la prueba `videoImposibleSeRegistraYSigue` del paso 0.4.
3. **La memoria USB se retira mientras la app la lee:** la exploración del paso 0.7 debe registrar qué evento llega y que la app no se cierra.
4. **La TV se reinicia durante una prueba larga** (suspensión, protector de pantalla): las mediciones del paso 0.6 se anotan con la hora, para no confundir un reinicio con un fallo de la app.
5. **El CI sin permisos para subir el APK, o un secreto faltante:** el workflow no usa secretos en el sprint 0, y el artefacto se sube con `actions/upload-artifact` (paso 0.2).

---

### Paso 0.1: proyecto base e instalación en el Xiaomi

**Rama:** `chore/tv-base`

**Archivos:**
- Crear:
  - `.gitignore` (Gradle, `build/`, `.idea/`, `local.properties`, `muestras/`, `*.apk` fuera de `build/`) y `.gitattributes` (`* text=auto eol=lf`, `*.bat eol=crlf`, `*.ttf binary`).
  - `README.md` (qué es, cómo arrancar y enlaces a `docs/`), que reemplaza al README del commit inicial.
  - `CHANGELOG.md` con la sección «## [Sin publicar]».
  - `docs/desarrollo.md` (entorno, comandos, convenciones) y `docs/arquitectura.md` (capas, con el diagrama de la spec).
  - `docs/decisiones.md`, con D-01 a D-12 copiadas de la spec.
  - En `apps/tv/`: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml` y el wrapper de Gradle 9.3.1.
  - En `apps/tv/app/`: `build.gradle.kts` y `src/main/AndroidManifest.xml`.
  - En `apps/tv/app/src/main/java/pe/inkaterra/tv/`: `app/MainActivity.kt` y `pantallas/inicio/InicioPantalla.kt`.
  - En `apps/tv/app/src/main/res/`: `drawable/banner.xml` (banner de TV de 320 × 180 con el texto «Inkaterra TV» sobre `#5A6A40`), `values/strings.xml` y `values-en/strings.xml`.
  - `tool/tv.sh`.
- Prueba: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/AppArrancaTest.kt`.

**Interfaces:**
- Produce:
  - `tool/tv.sh conectar | instalar | logs`, que usan los pasos siguientes;
  - `InicioPantalla()` (composable sin parámetros por ahora), que el sprint 1 reemplaza;
  - el catálogo `libs` con los alias `androidx-tv-material`, `androidx-compose-bom`, `media3-exoplayer`, `junit`, `robolectric`, `androidx-test-ext-junit` y `androidx-test-runner`.

- [ ] **Paso 1: escribir la prueba en el equipo que falla**

```kotlin
@RunWith(AndroidJUnit4::class)
class AppArrancaTest {
    @get:Rule val regla = createAndroidComposeRule<MainActivity>()

    @Test fun abreElInicioConElNombreDeLaApp() {
        regla.onNodeWithText("Inkaterra TV").assertIsDisplayed()
    }
}
```

- [ ] **Paso 2: crear el proyecto Gradle y la app mínima**
  - `namespace` y `applicationId`: `pe.inkaterra.tv`; `versionName` "0.1.0" y `versionCode` 1.
  - Manifiesto:
    - `uses-feature android.software.leanback required="true"` y `android.hardware.touchscreen required="false"`;
    - `android:banner="@drawable/banner"`;
    - `MainActivity` con `MAIN` y `LEANBACK_LAUNCHER`.
  - `MainActivity` extiende `ComponentActivity` y hace `setContent { InicioPantalla() }`. La pantalla muestra «Inkaterra TV» y la versión, con `androidx.tv.material3.Text` sobre fondo `#1E2416`.
  - Kotlin: el integrado de AGP 9 (sin el plugin `org.jetbrains.kotlin.android`); se aplica solo `org.jetbrains.kotlin.plugin.compose` 2.4.0. Si la sincronización falla por el Kotlin integrado, poner `android.builtInKotlin=false` en `gradle.properties`, aplicar `org.jetbrains.kotlin.android` 2.4.0 y anotarlo en `docs/decisiones.md`.

- [ ] **Paso 3: escribir `tool/tv.sh`**
  - Lee el equipo de la variable `TV` (por omisión `192.168.31.242:5555`).
  - `conectar` hace `adb connect` y comprueba que `adb -s $TV get-state` responda `device`. Si no responde, imprime: «No encuentro la TV en $TV. Revise que esté encendido, en la misma red y con la depuración por red activa; si cambió de IP, use TV=ip:5555».
  - `instalar` compila con `./gradlew :app:installDebug` y pasa `-s` a adb mediante `ANDROID_SERIAL=$TV`.
  - `logs` filtra logcat por el paquete.

- [ ] **Paso 4: verificar en el Xiaomi**

Run: `tool/tv.sh probar`
Expected: `AppArrancaTest` PASS. En la TV aparece «Inkaterra TV» en la fila de apps de Google TV, con su banner.

Comprobar también el foco de revisión 1: con `TV=192.168.31.250:5555 tool/tv.sh conectar`, aparece el mensaje en palabras simples y la salida es 1.

- [ ] **Paso 5: commit y unión**

```bash
git add .gitignore .gitattributes README.md CHANGELOG.md docs/desarrollo.md docs/arquitectura.md docs/decisiones.md apps/tv tool/tv.sh
git commit -m "chore(tv): proyecto base que se instala en el Xiaomi (paso 0.1)"
git checkout sprint-0/integracion && git merge --no-ff chore/tv-base
```

---

### Paso 0.2: CI de la app de TV

**Rama:** `chore/tv-ci`

**Archivos:**
- Crear: `.github/workflows/tv.yml`, `apps/tv/.editorconfig` y una prueba unitaria `apps/tv/app/src/test/java/pe/inkaterra/tv/dominio/VersionTest.kt`.
- Modificar: `apps/tv/app/build.gradle.kts` (plugin de ktlint, `lint { warningsAsErrors = true; abortOnError = true }`).
- Crear: `apps/tv/app/src/main/java/pe/inkaterra/tv/dominio/Version.kt`.

**Interfaces:**
- Produce: `fun textoVersion(nombre: String, codigo: Int): String`, que devuelve `"v$nombre ($codigo)"`; la usa `InicioPantalla`. Sirve para que la primera prueba unitaria del CI pruebe algo real.

- [ ] **Paso 1: escribir la prueba que falla**

```kotlin
class VersionTest {
    @Test fun formateaNombreYCodigo() {
        assertEquals("v0.1.0 (1)", textoVersion("0.1.0", 1))
    }
}
```

- [ ] **Paso 2: correrla y ver que falla**

Run: `cd apps/tv && ./gradlew testDebugUnitTest --tests '*VersionTest'`
Expected: FAIL, la función no existe.

- [ ] **Paso 3: implementar `textoVersion` en `dominio/Version.kt`** y usarla en `InicioPantalla` con `BuildConfig.VERSION_NAME` y `BuildConfig.VERSION_CODE` (activar `buildFeatures { buildConfig = true }`).

- [ ] **Paso 4: escribir el workflow `tv.yml`**
  - Corre en `pull_request` y en `push` a `main`, solo con cambios en `apps/tv/**` o en `.github/workflows/tv.yml`, con `concurrency` por ref.
  - Un job en `ubuntu-latest` con `actions/checkout@v4`, `actions/setup-java@v4` (temurin 21) y `gradle/actions/setup-gradle@v4`.
  - Comando, en `apps/tv`: `./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug`.
  - Sube `app/build/outputs/apk/debug/app-debug.apk` como artefacto `apk-tv`, con 7 días de retención.

- [ ] **Paso 5: verificar en local**

Run: `cd apps/tv && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, con `VersionTest` en verde.

- [ ] **Paso 6: commit, unión y subida** (subir solo con el visto bueno de Adrián)

```bash
git add .github/workflows/tv.yml apps/tv
git commit -m "chore(ci): formato, lint, pruebas y apk de la app de tv (paso 0.2)"
git checkout sprint-0/integracion && git merge --no-ff chore/tv-ci
git push -u origin sprint-0/integracion
```

Expected: el workflow corre en GitHub y termina en verde; el artefacto `apk-tv` se puede descargar.

---

### Paso 0.3: ficha del equipo y decodificadores

**Rama:** `feat/sistema-capacidades`

**Archivos:**
- Crear: `apps/tv/app/src/main/java/pe/inkaterra/tv/sistema/capacidades/CapacidadesEquipo.kt` y `apps/tv/app/src/main/java/pe/inkaterra/tv/dominio/Decodificador.kt`.
- Prueba unitaria: `apps/tv/app/src/test/java/pe/inkaterra/tv/dominio/DecodificadorTest.kt`.
- Prueba en el equipo: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/sistema/CapacidadesEquipoTest.kt`.
- Crear: `docs/informes/equipo-xiaomi.md`.

**Interfaces:**
- Produce:
  - `data class Decodificador(val nombre: String, val mime: String, val porHardware: Boolean, val anchoMax: Int, val altoMax: Int, val tasaMaxBps: Int)`;
  - `fun soporta4k(d: Decodificador): Boolean` (en `dominio/Decodificador.kt`): verdadero si decodifica 3840 × 2160 por hardware;
  - `object CapacidadesEquipo { fun decodificadoresVideo(): List<Decodificador>; fun espacioLibreBytes(dir: File): Long; fun memoriaTotalBytes(context: Context): Long }`. Usa `MediaCodecList(REGULAR_CODECS)`, `StatFs` y `ActivityManager.MemoryInfo`.
- El sprint 1 usa `soporta4k` y los límites medidos aquí.

- [ ] **Paso 1: escribir la prueba unitaria que falla**

```kotlin
class DecodificadorTest {
    private fun d(ancho: Int, alto: Int, hw: Boolean = true) =
        Decodificador("c2.prueba.hevc.decoder", "video/hevc", hw, ancho, alto, 100_000_000)

    @Test fun cuatroKPorHardware() = assertTrue(soporta4k(d(3840, 2160)))
    @Test fun soloFullHdNoAlcanza() = assertFalse(soporta4k(d(1920, 1080)))
    @Test fun porSoftwareNoCuenta() = assertFalse(soporta4k(d(3840, 2160, hw = false)))
    @Test fun verticalTambienCuenta() = assertTrue(soporta4k(d(2160, 3840)))
}
```

`verticalTambienCuenta`: un decodificador que declara 2160 × 3840 sirve también para video horizontal; se compara el lado mayor con 3840 y el menor con 2160.

- [ ] **Paso 2: correrla y ver que falla**

Run: `./gradlew testDebugUnitTest --tests '*DecodificadorTest'`
Expected: FAIL.

- [ ] **Paso 3: implementar `Decodificador`, `soporta4k` y `CapacidadesEquipo`.** Por hardware, `MediaCodecInfo.isHardwareAccelerated`. Los límites, de `videoCapabilities.supportedWidths.upper`, `supportedHeights.upper` y `bitrateRange.upper`.

- [ ] **Paso 4: correr la prueba unitaria y ver que pasa**

Run: `./gradlew testDebugUnitTest --tests '*DecodificadorTest'`
Expected: PASS.

- [ ] **Paso 5: escribir y correr la prueba en el Xiaomi**
  - `CapacidadesEquipoTest.registraLaFicha` imprime con `Log.i("Ficha", …)` cada decodificador de video, el espacio libre de `getExternalFilesDir(null)` y de `filesDir`, y la memoria total.
  - Afirma que hay al menos un decodificador `video/avc` y uno `video/hevc` con `soporta4k`.

Run: `tool/tv.sh probar pe.inkaterra.tv.sistema.CapacidadesEquipoTest && adb -s 192.168.31.242:5555 logcat -d -s Ficha`
Expected: PASS. La ficha aparece en logcat; se esperan `c2.amlogic.avc/hevc/vp9/av1.decoder` y ~24 GB libres.

- [ ] **Paso 6: escribir `docs/informes/equipo-xiaomi.md`** con la tabla de decodificadores, el espacio, la RAM y la versión de Android.

- [ ] **Paso 7: commit y unión**

```bash
git commit -m "feat(sistema): ficha del equipo y decodificadores 4k (paso 0.3)"
```

---

### Paso 0.4: prueba de 4K con Media3

**Rama:** `feat/sistema-prueba-4k`

**Archivos:**
- Crear: `tool/videos-prueba.sh`.
- Prueba en el equipo: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/sistema/Reproduccion4kTest.kt`.
- Modificar:
  - `apps/tv/app/build.gradle.kts` (`androidTestImplementation(libs.media3.exoplayer)`);
  - `docs/informes/equipo-xiaomi.md`;
  - `docs/decisiones.md` (D-13: tasa máxima de video).

**Interfaces:**
- Produce el valor de **D-13**, `TASA_MAXIMA_BPS`, que el sprint 1 usa en las reglas de contenido.

- [ ] **Paso 1: instalar ffmpeg y escribir `tool/videos-prueba.sh`**
  - Instalación: `winget install --id Gyan.FFmpeg -e`.
  - El script genera en `muestras/prueba-4k/` videos de 30 s de 3840 × 2160 a 30 fps con `testsrc2` y un tono de audio AAC, cada uno con `-b:v`, `-maxrate` y `-bufsize` fijos:
    - `h264-20m.mp4`, `h264-40m.mp4` y `h264-60m.mp4`;
    - `hevc-20m.mp4`, `hevc-40m.mp4` y `hevc-60m.mp4`;
    - `hevc-60fps-40m.mp4` (60 fps);
    - `hevc10-120m.mkv` (HEVC de 10 bits a 120 Mbps, el caso que puede fallar).
  - Al final los copia con `adb push` a `/sdcard/Android/data/pe.inkaterra.tv/files/prueba-4k/`.

- [ ] **Paso 2: escribir la prueba en el equipo**
  - `Reproduccion4kTest.reproduceCadaVideo` recorre `getExternalFilesDir("prueba-4k")`. Por cada archivo crea un `ExoPlayer` con un `AnalyticsListener` y lo reproduce 20 s en el hilo principal (`InstrumentationRegistry.getInstrumentation().runOnMainSync` y espera con `CountDownLatch`).
  - Registra con `Log.i("Prueba4k", …)`: el nombre, el decodificador usado (`onVideoDecoderInitialized`), los cuadros perdidos (`DecoderCounters.droppedBufferCount`), los cuadros mostrados (`renderedOutputBufferCount`) y el error si lo hubo.
  - `videoImposibleSeRegistraYSigue`: si `onPlayerError` llega en un archivo, se anota «no reproduce» y la prueba sigue con el siguiente. Al final afirma que los archivos `h264-20m.mp4` y `hevc-20m.mp4` tienen 0 errores.

- [ ] **Paso 3: correr la prueba en el Xiaomi**

Run: `tool/videos-prueba.sh && tool/tv.sh probar pe.inkaterra.tv.sistema.Reproduccion4kTest && adb -s 192.168.31.242:5555 logcat -d -s Prueba4k`
Expected: PASS, con una línea por video.

- [ ] **Paso 4: fijar D-13 y anotar los resultados**
  - La tasa máxima es la más alta en H.264 **y** en HEVC con menos del 1 % de cuadros perdidos.
  - Se anota en `docs/decisiones.md` como `D-13. Tasa máxima de video: N Mbps`, con la tabla de resultados en `docs/informes/equipo-xiaomi.md`.
  - Si ninguna tasa pasa, D-13 queda en 20 Mbps y se avisa a Adrián.

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "test(sistema): reproducción 4k en el Xiaomi y tasa máxima (paso 0.4)"
```

---

### Paso 0.5: pantalla principal y arranque al encender

**Rama:** `feat/sistema-arranque`

**Archivos:**
- Crear: `apps/tv/app/src/main/java/pe/inkaterra/tv/sistema/arranque/ReceptorArranque.kt`.
- Modificar: `AndroidManifest.xml` (receptor `BOOT_COMPLETED` y permiso `RECEIVE_BOOT_COMPLETED`) y `docs/decisiones.md` (D-14 y D-15).

**Interfaces:**
- Produce: `class ReceptorArranque : BroadcastReceiver`, que abre `MainActivity` con `FLAG_ACTIVITY_NEW_TASK`. Se queda solo si D-15 dice que funciona.

- [ ] **Paso 1: arranque al encender**
  - Implementar `ReceptorArranque` e instalar.
  - Reiniciar con `adb -s 192.168.31.242:5555 reboot`, esperar con `adb wait-for-device` y revisar con `adb shell dumpsys activity activities | grep -m1 ResumedActivity`.
  - Expected: anotar si queda `pe.inkaterra.tv/.app.MainActivity` en primer plano o si Android lo bloqueó (por las restricciones a abrir pantallas desde segundo plano). Si lo bloqueó, probar con el permiso `SYSTEM_ALERT_WINDOW` concedido por `adb shell appops set pe.inkaterra.tv SYSTEM_ALERT_WINDOW allow` y anotarlo.

- [ ] **Paso 2: pantalla principal (sin cambiar código)**
  - Agregar temporalmente a `MainActivity` el filtro `HOME` + `DEFAULT`.
  - Probar `adb shell cmd package set-home-activity pe.inkaterra.tv/.app.MainActivity` y presionar la tecla Inicio del control (`adb shell input keyevent KEYCODE_HOME`).
  - Anotar si Google TV respeta el cambio o vuelve a `launcherx`.
  - Anotar el plan B sin ejecutarlo: «device owner» con `dpm set-device-owner` exige un equipo sin cuentas, o sea restablecido de fábrica. **No restablecer el Xiaomi**: solo documentarlo.
  - Quitar el filtro `HOME` antes del commit.

- [ ] **Paso 3: escribir D-14 (pantalla principal: qué permite Google TV y qué opción queda para decidir antes del sprint 5) y D-15 (arranque al encender: sí o no y cómo)**, con los comandos usados.

- [ ] **Paso 4: commit y unión** (con `ReceptorArranque` solo si D-15 lo adopta)

```bash
git commit -m "feat(sistema): arranque al encender y límites de pantalla principal (paso 0.5)"
```

---

### Paso 0.6: protector de pantalla y suspensión

**Rama:** `feat/sistema-protector`

**Archivos:**
- Crear: `apps/tv/app/src/main/java/pe/inkaterra/tv/sistema/protector/ProtectorPantalla.kt`.
- Modificar:
  - `AndroidManifest.xml` (servicio con `android.service.dreams.DreamService` y permiso `BIND_DREAM_SERVICE`);
  - `app/MainActivity.kt` (mantener la pantalla encendida);
  - `docs/decisiones.md` (D-16 y D-17).

**Interfaces:**
- Produce:
  - `class ProtectorPantalla : DreamService`, que por ahora muestra «Inkaterra TV» sobre `#5A6A40`; el sprint 4 le pone las promociones;
  - `fun mantenerPantallaEncendida(activity: Activity, encendida: Boolean)` en `sistema/protector/`, que pone o quita `FLAG_KEEP_SCREEN_ON`.

- [ ] **Paso 1: protector de pantalla**
  - Implementar `ProtectorPantalla` (en `onAttachedToWindow`, `isInteractive = false` y `isFullscreen = true`) e instalar.
  - Guardar los valores actuales: `settings get secure screensaver_components` (hoy `null`: ninguno), `screensaver_enabled` y `system screen_off_timeout` (hoy 600000).
  - Activar el nuestro con `adb shell settings put secure screensaver_components pe.inkaterra.tv/.sistema.protector.ProtectorPantalla` y `adb shell settings put system screen_off_timeout 60000`, salir a otra app y esperar 1 minuto.
  - Expected: anotar si aparece nuestro protector. **Al terminar, restaurar los valores guardados.**

- [ ] **Paso 2: suspensión**
  - Guardar `settings get secure sleep_timeout` (hoy 1200000) y bajarlo a 120000.
  - Dejar la app en primer plano con `mantenerPantallaEncendida(true)` durante 5 minutos.
  - Expected: anotar con la hora si la TV se suspendió. Restaurar el valor.

- [ ] **Paso 3: escribir D-16 (protector de pantalla: si se puede y cómo se configura en cada TV) y D-17 (cómo se evita la suspensión durante las promociones)**, con los comandos de configuración que el personal o TI tendrá que correr en cada TCL.

- [ ] **Paso 4: commit y unión**

```bash
git commit -m "feat(sistema): protector de pantalla y pantalla encendida (paso 0.6)"
```

---

### Paso 0.7: memoria USB

**Rama:** `feat/sistema-usb`

**Archivos:**
- Crear: `apps/tv/app/src/main/java/pe/inkaterra/tv/sistema/usb/VolumenesUsb.kt`.
- Prueba en el equipo: `apps/tv/app/src/androidTest/java/pe/inkaterra/tv/sistema/VolumenesUsbTest.kt`.
- Modificar: `docs/decisiones.md` (D-18).

**Interfaces:**
- Produce:
  - `data class VolumenUsb(val id: String, val etiqueta: String?, val raiz: File?, val montado: Boolean)`;
  - `class VolumenesUsb(context: Context) { fun actuales(): List<VolumenUsb>; fun observar(alCambiar: (List<VolumenUsb>) -> Unit): AutoCloseable }`, con `StorageManager.storageVolumes` (solo `isRemovable`), `StorageVolume.directory` y el registro de `StorageManager.StorageVolumeCallback`.
  - El sprint 3 lo usa para la USB del huésped.

Requiere de Adrián: tres memorias USB (o una formateada tres veces) en FAT32, exFAT y NTFS, cada una con una foto JPG, una HEIC y un video MOV de celular. El Xiaomi tiene un puerto USB: la memoria se conecta directo.

- [ ] **Paso 1: escribir la prueba en el equipo**
  - `VolumenesUsbTest.listaLaUsbConectada` afirma que `actuales()` trae al menos un volumen montado con `raiz` legible.
  - Lista con `raiz.walkTopDown()` los archivos e imprime `Log.i("Usb", …)` con el nombre, el tamaño y si `canRead()`.
  - Si la lectura directa falla, registra el error e intenta por `MediaStore` (`MediaStore.getExternalVolumeNames` + consulta de `Images` y `Video`), anotando qué permiso pidió: `READ_MEDIA_IMAGES` y `READ_MEDIA_VIDEO` en el SDK 33 o superior.

- [ ] **Paso 2: correrla con cada formato**

Run: conectar la USB y `tool/tv.sh probar pe.inkaterra.tv.sistema.VolumenesUsbTest && adb -s 192.168.31.242:5555 logcat -d -s Usb`
Expected: por cada formato, anotar si monta, si se lee directo o solo por MediaStore, y si la foto HEIC se decodifica (`ImageDecoder.decodeBitmap`).

- [ ] **Paso 3: retirar la USB durante la lectura** (foco de revisión 3)
  - Una app de prueba temporal registra `observar` y deja un bucle de lectura de 30 s.
  - Retirar la USB a mitad.
  - Expected: anotar el evento que llega y confirmar en `tool/tv.sh logs` que no hay cierre de la app.

- [ ] **Paso 4: escribir D-18 (USB: formatos que lee el Xiaomi, forma de lectura y permisos)**

- [ ] **Paso 5: commit y unión**

```bash
git commit -m "feat(sistema): detectar y leer memorias usb (paso 0.7)"
```

---

### Paso 0.8: informe del sprint 0 y PR

**Archivos:**
- Crear: `docs/informes/sprint-0.md` (misma plantilla que los informes de asistencia-movil: resumen, lo realizado paso a paso, calidad, decisiones, problemas, pendientes, próximos pasos, cómo probarlo y glosario).
- Modificar: `CHANGELOG.md` (en «Sin publicar», la app mínima instalable en Google TV) y `docs/plan-de-desarrollo.md`. En el plan: marcar las decisiones cerradas, ajustar «Reglas de contenido» con D-13 y anotar si el sprint 1 cambia.

- [ ] **Paso 1: correr todo lo del CI y las pruebas en el Xiaomi**

Run: `(cd apps/tv && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug) && tool/tv.sh probar`
Expected: BUILD SUCCESSFUL y todas las pruebas en verde. `VolumenesUsbTest` necesita una USB conectada; si no hay, se corre con `-Pandroid.testInstrumentationRunnerArguments.notClass=pe.inkaterra.tv.sistema.VolumenesUsbTest` y se dice en el informe.

- [ ] **Paso 2: borrar el código de exploración que ninguna decisión adoptó** y repetir el paso 1.

- [ ] **Paso 3: escribir el informe y el CHANGELOG, hacer el commit y abrir el PR** de `sprint-0/integracion` hacia `main` (solo con el visto bueno de Adrián)

```bash
git commit -m "docs: informe del sprint 0 (paso 0.8)"
git push
```

**Criterio de aceptación del sprint:** la app mínima generada por el CI se instala en el Xiaomi por la red y reproduce un video 4K sin perder cuadros, y `docs/decisiones.md` dice qué se puede y qué no en Google TV (D-13 a D-18).
