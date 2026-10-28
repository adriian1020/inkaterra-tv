# Decisiones

Las D-01 a D-12 vienen del plan de desarrollo ([plan-de-desarrollo.md](plan-de-desarrollo.md)).
Desde la D-13 se anota lo que se averigua en la TV durante el sprint 0.

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
  hasta 36. `targetSdk` sigue en 36 y `minSdk` en 30.
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
