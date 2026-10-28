# Guía de desarrollo

La app sigue las convenciones de asistencia-movil y asistencia-vip: todo en español, commits
`tipo(ámbito): resumen`, ramas y PR, SemVer y CHANGELOG.

## Entorno

- JDK 21 (el bytecode sale para Java 17) y Android SDK con la plataforma 37.
- Gradle 9.7.1 (wrapper en `apps/tv/`), AGP 9.4.1 con su Kotlin integrado y el plugin de
  Compose de Kotlin 2.4.0.
- `apps/tv/local.properties` con `sdk.dir=C\:/Users/<usuario>/AppData/Local/Android/Sdk` (los
  dos puntos van escapados; git lo ignora).
- Las dependencias van con versión exacta en `apps/tv/gradle/libs.versions.toml`. Subir una es
  un PR propio.

## TV de pruebas

El Xiaomi TV Box S de Adrián, por la red: `192.168.31.242:5555`. Para otra TV, `TV=ip:5555`
antes de cada comando.

```bash
tool/tv.sh conectar          # comprueba que la TV responda
tool/tv.sh instalar          # compila, instala y abre la app
tool/tv.sh probar [clase]    # pruebas en la TV (src/androidTest)
tool/tv.sh logs              # registros de la app
```

La primera vez que se conecta una PC, la TV muestra «¿Permitir depuración?»: aceptar con
«Permitir siempre desde este equipo».

Las pruebas en la TV se corren con `tool/tv.sh probar` y no con `connectedDebugAndroidTest`:
con la TV conectada por red, esa tarea de AGP 9.4 termina en error aunque las pruebas pasen.

## Pruebas

Lo mismo que el CI, desde `apps/tv`:

```bash
./gradlew ktlintCheck lintDebug testDebugUnitTest assembleDebug
```

- `src/test/`: lógica y pantallas, en la PC (JUnit y Robolectric). Espejo de `src/main/`.
- `src/androidTest/`: lo que necesita la TV real (reproductor, USB, sistema).
- Datos inventados siempre: «Atardecer en Machu Picchu.mp4», «Reserva Amazónica 01.jpg».

## Convenciones

- Nombres en español en el código; solo se mantiene lo que impone Android (`MainActivity`).
- Cada archivo empieza con un comentario breve de qué hace y qué reglas sigue. Los comentarios
  explican el porqué.
- Commits `tipo(ámbito): resumen (paso N.M)`, en minúscula, en presente y de hasta 72
  caracteres.
- Cada paso del sprint en su rama (`feat/<capa>-<tema>`), que sale de `sprint-N/integracion` y
  vuelve con `git merge --no-ff`. Al cerrar el sprint, PR hacia `main`.
- Lo visible para el usuario va en «## [Sin publicar]» del CHANGELOG.
- Videos y fotos de muestra en `muestras/`, que git ignora.
