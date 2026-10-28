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
