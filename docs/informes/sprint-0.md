# Informe del sprint 0 — App de TV de Inkaterra

**Proyecto:** `inkaterra-tv` (Kotlin, Compose for TV, Media3) · **Rama:** `sprint-0/integracion`
**Periodo:** 23 de octubre de 2026 (plan) y 28 de octubre (pasos 0.1 a 0.8) · **Responsable:** Adrián
**Equipo de pruebas:** Xiaomi TV Box S (Google TV, Android 14), por la red en `192.168.31.242`
**Estado:** sprint 0 completo, salvo la prueba de la memoria USB en la TV (el Xiaomi no reconoció
la memoria). Falta subir la rama y abrir el PR hacia `main`.

---

## 1. Resumen

El sprint 0 deja **el repositorio, el CI y una app mínima que se instala en Google TV**, y
responde con pruebas en la TV real lo que el resto del proyecto necesitaba saber:

- **4K sin trabas:** el Xiaomi reprodujo H.264, HEVC y VP9 en 4K a 20, 40 y 60 Mbps, HEVC a
  60 fps y HEVC de 10 bits a 120 Mbps **sin perder un solo cuadro**. La tasa máxima queda en
  60 Mbps (D-13) y se aceptan también videos WEBM con VP9 (D-19).
- **Protector de pantalla propio:** funciona. Las promociones pueden salir aunque el huésped
  esté en YouTube, configurando cada TV por adb (D-16).
- **Suspensión:** dentro de la app no ocurre; fuera de ella hay que desactivarla en cada TV para
  que las promociones sigan sin fin (D-17).
- **Pantalla principal y arranque al encender:** Google TV 14 no se los da a una app normal.
  Quedan tres caminos para decidir antes del sprint 5 (D-14, D-15).
- **Memoria USB:** el código está, pero el Xiaomi no reconoció la memoria de prueba. Queda
  pendiente para el sprint 3 (D-18).

| Pregunta del sprint | Respuesta |
|---|---|
| ¿Reproduce 4K fluido? | ✅ Sí, H.264, HEVC y VP9 hasta 60 Mbps (y más) |
| ¿Puede ser la pantalla principal? | ❌ No como app normal · ⏳ decidir entre las 3 opciones de D-14 |
| ¿Abre sola al encender? | ❌ No como app normal (D-15) |
| ¿Protector de pantalla propio? | ✅ Sí, configurado por adb (D-16) |
| ¿Se evita la suspensión? | ✅ En la app · ⚠️ fuera de ella, desactivándola por adb (D-17) |
| ¿Lee memorias USB? | ⏳ Pendiente: el Xiaomi no reconoció la memoria (D-18) |

---

## 2. Lo realizado, paso a paso

### 0.1 Proyecto base e instalación en la TV
- **Qué:** proyecto Gradle en `apps/tv` (paquete `pe.inkaterra.tv`) con las capas `app/`,
  `dominio/`, `datos/`, `sistema/` y `pantallas/`; la app aparece en Google TV con su banner y
  abre «Inkaterra TV» sobre fondo oscuro. `tool/tv.sh` conecta, instala, corre las pruebas y lee
  los registros de la TV por la red. Documentos base: README, CHANGELOG, `desarrollo.md`,
  `arquitectura.md` y `decisiones.md`.
- **Archivos:** `apps/tv/**`, `tool/tv.sh`, `docs/*.md`
- **Commit:** `2caf353` · **Prueba en la TV:** `AppArrancaTest`
- **Verificado:** la app abre en el Xiaomi; sin la TV, `tool/tv.sh` lo dice en palabras simples.

### 0.2 CI
- **Qué:** workflow `.github/workflows/tv.yml` con ktlint, lint estricto, pruebas y APK.
  `textoVersion` muestra la versión en el inicio.
- **Commit:** `d6d40ea` · **Pruebas:** 1
- **Verificado:** en la PC. En GitHub todavía no, porque la rama no está subida.

### 0.3 Ficha del equipo y decodificadores
- **Qué:** `CapacidadesEquipo` lee los decodificadores, el espacio y la memoria; `soporta4k`
  decide si un decodificador sirve para 4K por hardware.
- **Archivos:** `dominio/Decodificador.kt`, `sistema/capacidades/CapacidadesEquipo.kt`,
  `docs/informes/equipo-xiaomi.md`
- **Commits:** `802cb90`, `13df9c3` · **Pruebas:** 4 + prueba en la TV
- **Verificado:** H.264 y HEVC en 4K por hardware; 1,87 GB de RAM y 23,5 GB libres.

### 0.4 Prueba de 4K con Media3
- **Qué:** `tool/videos-prueba.sh` genera 11 videos 4K de prueba (con ruido, para que la tasa
  real sea la pedida) y los copia a la TV; `Reproduccion4kTest` reproduce cada uno 20 s y mide
  los cuadros perdidos.
- **Commits:** `37246a7` (y `499491b` para VP9) · **Pruebas:** 2 en la TV
- **Resultado:** 0 cuadros perdidos en los 11 videos. D-13: 60 Mbps.

### 0.5 Pantalla principal y arranque al encender
- **Qué se probó:** un receptor de `BOOT_COMPLETED` (bloqueado por Android, también con el
  permiso de mostrarse sobre otras apps) y la app como pantalla principal con
  `set-home-activity` (Google TV lo ignora). El código de prueba se borró.
- **Commit:** `e5d166d` · **Decisiones:** D-14 y D-15

### 0.6 Protector de pantalla y suspensión
- **Qué:** `ProtectorPantalla` (un `DreamService`) y `mantenerPantallaEncendida`.
- **Commit:** `825d61e` · **Decisiones:** D-16 y D-17
- **Verificado:** el protector salió a 1 minuto sin uso; la app al frente no dejó suspender la
  TV; con la suspensión en «nunca» el protector siguió en pantalla. Los ajustes del Xiaomi se
  restauraron.

### 0.7 Memorias USB
- **Qué:** `VolumenesUsb` lista las memorias y avisa al conectarlas o retirarlas; lee sin copiar.
- **Commit:** `34afb26` · **Prueba en la TV:** `VolumenesUsbTest` (pendiente)
- **Resultado:** la memoria Dahua U156 (FAT32, que la PC lee) no apareció en el puerto del
  Xiaomi. D-18 deja la prueba para el sprint 3.

### Extra: WEBM con VP9 (D-19)
- A pedido de Adrián, después de ver que la memoria de prueba traía un video WEBM: se midió VP9
  4K en la TV y se agregó a las reglas de contenido del sprint 1.

---

## 3. Calidad y verificación

| Chequeo (igual que el CI) | Resultado |
|---|---|
| Formato (`ktlintCheck`) | ✅ Sin problemas |
| Análisis (`lintDebug`, avisos como errores) | ✅ Sin problemas |
| Pruebas en la PC (`testDebugUnitTest`) | ✅ 5 de 5 |
| APK (`assembleDebug`) | ✅ |
| Pruebas en el Xiaomi (`tool/tv.sh probar`) | ✅ 4 de 4 (`VolumenesUsbTest` excluida, sin USB) |
| CI en GitHub | ⏳ Al subir la rama |

Forma de trabajo en cada paso: rama propia → prueba primero → código → chequeos del CI → prueba
en la TV → commit → unión a `sprint-0/integracion`.

**Tamaño del cambio frente a `main`:** 42 archivos, unas 3 000 líneas, la mayoría
documentación (plan, planes de los sprints 0 y 1, decisiones e informes).

---

## 4. Decisiones tomadas

| Decisión | Motivo |
|---|---|
| AGP 9.4.1, Gradle 9.7.1 y Android 37 para compilar | Compose BOM 2026.09.00 lo exige; AGP 9.1 llegaba hasta 36 |
| Pruebas en la TV con `tool/tv.sh probar` y no `connectedDebugAndroidTest` | Con la TV por red, esa tarea de AGP marca error aunque las pruebas pasen |
| Lint estricto, salvo avisos de versiones nuevas y la carpeta del banner | Subir versiones es un PR propio; el banner de TV va solo en xhdpi |
| D-13: 60 Mbps | La tasa más alta probada en H.264 y HEVC; deja margen para la TCL |
| D-14 y D-15: no hay pantalla principal ni arranque como app normal | Google TV 14 los bloquea; se decide antes del sprint 5 |
| D-16 y D-17: protector y suspensión se configuran por adb en cada TV | Google TV no deja elegir el protector en sus ajustes |
| D-19: WEBM con VP9 | Formato común de celulares y descargas; el Xiaomi lo reproduce perfecto |

---

## 5. Problemas encontrados y cómo se resolvieron

| Problema | Solución |
|---|---|
| El onn. 4K tenía ~1 GB libre y luego no respondió en la red | Se pasó al Xiaomi TV Box S (~24 GB libres) |
| `local.properties` con la ruta mal escapada rompía Gradle y lint | `sdk.dir=C\:/Users/...`, anotado en `desarrollo.md` |
| `connectedDebugAndroidTest` fallaba con las pruebas en verde | `tool/tv.sh probar` con `am instrument` |
| Un commit entró con errores de ktlint porque el comando no cortó | Se corrigió en el commit siguiente; desde entonces el CI local corre con `pipefail` |
| Git Bash cambiaba las rutas de la TV (`/sdcard/...`) y adb no entendía las de Git Bash | `MSYS_NO_PATHCONV=1` y rutas locales con `cygpath -w` |
| La medición 4K dio 0 cuadros: la TV estaba suspendida | `tool/tv.sh probar` despierta la TV antes de las pruebas |
| La prueba de suspensión dejó el Xiaomi apagado y sin red con los ajustes de prueba | Adrián lo despertó y se restauró; los scripts reconectan antes de restaurar |
| YouTube bloqueó la descarga de un video de Inkaterra (verificación anti-robots) | No se forzó; los videos reales se piden a marketing o se usan videos con licencia libre |

---

## 6. Pendientes y riesgos

- **Subir la rama y abrir el PR** de `sprint-0/integracion` hacia `main`, y ver el CI en verde en
  GitHub.
- **Memoria USB (D-18):** repetir con otra memoria o un hub con alimentación antes del sprint 3.
- **Decidir antes del sprint 5** si la app es una app normal, un equipo administrado o
  reemplaza el inicio de Google TV (D-14).
- **TCL:** confirmar modelo y espacio, y repetir en ella las pruebas de 4K, VP9, protector y
  suspensión (piloto del sprint 5).
- **Pantalla 4K:** las mediciones se hicieron con una pantalla 1080p; falta ver la salida en 4K.
- **Contenido real:** pedir a marketing los videos originales (más calidad que los de YouTube).

---

## 7. Próximos pasos — Sprint 1: base y navegación

Plan completo en [docs/planes/sprint-1.md](../planes/sprint-1.md):

1. Reglas de contenido (4K, códecs, 60 Mbps, WEBM con VP9).
2. Biblioteca de la carpeta del hotel.
3. Tema de Inkaterra (verde, Josefin Sans y Cinzel).
4. Inicio y secciones con el control remoto.
5. Elegir el idioma.
6. Prueba completa en la TV e informe.

---

## 8. Cómo probarlo

```bash
tool/tv.sh conectar                                   # TV=ip:5555 si es otra TV
tool/tv.sh instalar
(cd apps/tv && ./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug)
tool/videos-prueba.sh                                 # videos 4K de prueba (necesita ffmpeg)
EXCLUIR=pe.inkaterra.tv.sistema.VolumenesUsbTest tool/tv.sh probar
```

---

## 9. Glosario

| Término | Significado |
|---|---|
| **Decodificador por hardware** | Parte del chip que descomprime el video; sin él, el 4K se traba |
| **Cuadro perdido** | Imagen del video que la TV no alcanzó a mostrar a tiempo |
| **DreamService** | Forma en que Android TV muestra un protector de pantalla |
| **Suspensión** | La TV apaga la imagen y la red después de un tiempo sin uso |
| **adb** | Herramienta para instalar, probar y configurar la TV desde la PC |
| **VP9 / WEBM** | Formato de video de Google y el archivo que lo contiene |
