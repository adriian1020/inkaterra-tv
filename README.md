# Inkaterra TV

App de Android TV / Google TV para las habitaciones de Inkaterra. El huésped ve videos, fotos y
música del hotel (y los de su propia memoria USB) con el control remoto, y cuando nadie usa la
TV pasan las promociones del hotel. Más adelante: panel web para subir el contenido e IPTV.

## Arrancar

Requisitos: JDK 21, Android SDK con la plataforma 37 y adb. En Windows, los scripts de `tool/`
corren en Git Bash.

```bash
tool/tv.sh conectar          # la TV de pruebas, por la red (TV=ip:5555 si cambió)
tool/tv.sh instalar          # compila, instala y abre la app en la TV
cd apps/tv && ./gradlew testDebugUnitTest
tool/tv.sh probar            # pruebas en la TV
```

## Estructura

| Carpeta | Qué hay |
|---|---|
| `apps/tv/` | App de Android TV (Kotlin, Compose for TV, Media3) |
| `apps/panel/`, `services/api/` | Panel web y API (desde la Fase 2) |
| `tool/` | Scripts para la TV de pruebas |
| `docs/` | Plan, decisiones, arquitectura, planes e informes de cada sprint |

## Documentación

- [docs/plan-de-desarrollo.md](docs/plan-de-desarrollo.md): fases, sprints y alcance.
- [docs/desarrollo.md](docs/desarrollo.md): entorno, comandos, pruebas y convenciones.
- [docs/arquitectura.md](docs/arquitectura.md): capas y flujos.
- [docs/decisiones.md](docs/decisiones.md): decisiones y lo que se averiguó de Google TV.
- [docs/planes/](docs/planes/) e [docs/informes/](docs/informes/): pasos e informe de cada sprint.
- [CHANGELOG.md](CHANGELOG.md).
