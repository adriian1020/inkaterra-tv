# Ficha del equipo de pruebas: Xiaomi TV Box S

Leída el 28 de octubre de 2026 con `CapacidadesEquipoTest` (paso 0.3) y adb.

| Dato | Valor |
|---|---|
| Modelo | Xiaomi MiTV-AFMU0 (TV Box S), dispositivo `twilight` |
| Sistema | Google TV, Android 14 (SDK 34) |
| Chip | Amlogic (plataforma `s7d`) |
| Memoria | 1,87 GB |
| Espacio libre | 23,5 GB (el de la app y el interno son el mismo volumen) |
| Pantalla conectada | 1080p a 59,94 Hz, sin HDR (el equipo puede sacar 3840 × 2160 con una TV 4K) |
| USB | Sí (`android.hardware.usb.host`) |
| Protector de pantalla | Ninguno configurado (`screensaver_components` = `null`) |
| Apagado de pantalla y suspensión | 10 y 20 minutos sin uso |

## Decodificadores de video para la app

Solo los que importan para las reglas de contenido. La lista completa queda en el registro de
la prueba.

| Formato | Decodificador | Por hardware | Máximo | Tasa máxima declarada | 4K |
|---|---|---|---|---|---|
| H.264 (`video/avc`) | `c2.amlogic.avc.decoder` | Sí | 4096 × 4096 | 300 Mbps | Sí |
| HEVC (`video/hevc`) | `c2.amlogic.hevc.decoder` | Sí | 4096 × 4096 | 160 Mbps | Sí |
| VP9 | `c2.amlogic.vp9.decoder` | Sí | 4096 × 4096 | 120 Mbps | Sí |
| AV1 | `c2.amlogic.av1.decoder` | Sí | 3840 × 3840 | 40 Mbps | Sí |
| Dolby Vision | `c2.amlogic.dolby-vision.*` | Sí | 4096 × 4096 | 300 Mbps | Sí |

Los decodificadores por software de Android (`c2.android.*`, `OMX.google.*`) llegan como mucho a
2048 × 2048: si el hardware fallara, el 4K no se podría reproducir.

La tasa declarada es lo que el decodificador acepta, no lo que reproduce fluido. Lo fluido se
mide en el paso 0.4.

## Reproducción 4K (paso 0.4)

`Reproduccion4kTest`, 28 de octubre: cada video de `tool/videos-prueba.sh` (3840 × 2160, 25 s,
con ruido para que la tasa real sea la pedida) se reprodujo 20 s en pantalla con Media3.

| Video | Decodificador | Cuadros mostrados | Perdidos |
|---|---|---|---|
| H.264, 20 Mbps | `c2.amlogic.avc.decoder` | 556 | 0 |
| H.264, 40 Mbps | `c2.amlogic.avc.decoder` | 588 | 0 |
| H.264, 60 Mbps | `c2.amlogic.avc.decoder` | 587 | 0 |
| HEVC, 20 Mbps | `c2.amlogic.hevc.decoder` | 587 | 0 |
| HEVC, 40 Mbps | `c2.amlogic.hevc.decoder` | 588 | 0 |
| HEVC, 60 Mbps | `c2.amlogic.hevc.decoder` | 588 | 0 |
| HEVC, 60 fps, 40 Mbps | `c2.amlogic.hevc.decoder` | 1177 | 0 |
| HEVC 10 bits, 120 Mbps (MKV) | `c2.amlogic.hevc.decoder` | 584 | 0 |
| VP9, 20 Mbps (WEBM) | `c2.amlogic.vp9.decoder` | 589 | 0 |
| VP9, 40 Mbps (WEBM) | `c2.amlogic.vp9.decoder` | 590 | 0 |
| VP9, 60 Mbps (WEBM) | `c2.amlogic.vp9.decoder` | 589 | 0 |

Un archivo dañado se registra como «no reproduce» y la prueba sigue
(`videoImposibleSeRegistraYSigue`). La pantalla conectada es 1080p: Android decodifica el 4K y lo
escala. Con esto se fija D-13 en 60 Mbps. Las filas de VP9 se midieron después, para D-19.
