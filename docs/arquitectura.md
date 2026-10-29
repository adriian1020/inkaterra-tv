# Arquitectura

La app de TV reproduce siempre desde la memoria de la TV; en la Fase 2 la nube solo decide qué
debe tener cada TV y se lo hace descargar.

```mermaid
flowchart LR
  pantallas --> datos
  pantallas --> dominio
  datos --> dominio
  datos --> sistema
  sistema -- APIs --> android[Android TV]
```

| Capa | Paquete | Para qué |
|---|---|---|
| `app/` | `pe.inkaterra.tv.app` | Arranque, tema, navegación e idioma |
| `dominio/` | `pe.inkaterra.tv.dominio` | Kotlin puro, sin Android: reglas de contenido, inactividad, campañas |
| `datos/` | `pe.inkaterra.tv.datos` | Biblioteca local, ajustes y, en la Fase 2, red y sincronización |
| `sistema/` | `pe.inkaterra.tv.sistema` | Lo que toca Android TV: decodificadores, USB, protector de pantalla, arranque |
| `pantallas/` | `pe.inkaterra.tv.pantallas` | Lo que ve el huésped (Compose for TV); no toca archivos ni red |

- `dominio/` no importa Android: se prueba en la PC.
- Lo que toca el sistema va detrás de una interfaz con un doble para las pruebas.

## Navegación, biblioteca y miniaturas (sprint 1)

```mermaid
flowchart LR
  idioma[IdiomaPantalla] --> inicio[InicioPantalla]
  inicio -- Enter --> abrir[AbrirPantalla]
  abrir -- Atrás --> inicio
  inicio -. lee .-> modelo[InicioModelo]
  modelo --> biblioteca[Biblioteca]
  biblioteca --> carpeta[CarpetaContenido]
  biblioteca --> lector[LectorMetadatos]
  biblioteca --> reglas[reglas de contenido]
  inicio -. imágenes .-> miniaturas[Miniaturas]
  miniaturas --> generador[GeneradorMiniatura]
```

- `MainActivity` arma un `Contenedor` (sin Hilt) con la biblioteca, las miniaturas y el ajuste
  de idioma, y se los pasa a `Navegacion`. Las pruebas pasan dobles en su lugar.
- **Biblioteca** (`datos/biblioteca/`): lee `getExternalFilesDir("contenido")`, clasifica por
  extensión, valida cada video con las reglas de contenido y aparta lo de `promociones/` como
  destacados. Un archivo ilegible va a «rechazados» y no corta la carga.
- **Miniaturas** (`datos/miniaturas/`): un JPEG de 1280 × 720 por archivo en
  `cacheDir/miniaturas/`, con la ruta, el tamaño y la fecha como clave; se generan de a dos.
- **Inicio** (`pantallas/inicio/`): área destacada y filas (D-20). `InicioModelo` carga la
  biblioteca una vez; el foco y Atrás viven en `InicioPantalla`.
- **Idioma** (`sistema/idioma/`): idioma por app de appcompat; desde Android 13 lo guarda el
  sistema.
- `AbrirPantalla` es provisional: desde el sprint 2 solo la usa la música, hasta el sprint 3.

## Video y fotos (sprint 2)

```mermaid
flowchart LR
  inicio[InicioPantalla] -- Enter en un video --> video[ReproductorPantalla]
  inicio -- Enter en una foto --> fotos[VisorFotosPantalla]
  inicio -- Enter en una canción --> abrir[AbrirPantalla]
  video -- Atrás o fin de la fila --> inicio
  fotos -- Atrás --> inicio
  video -. filas y foco .-> modelo[InicioModelo]
  fotos -. filas y foco .-> modelo
  video --> posiciones[PosicionesVideo]
  video --> exo[ExoPlayer + SurfaceView]
  fotos --> coil[Coil]
```

- **Rutas:** `video/{fila}/{indice}` y `fotos/{fila}/{indice}`. Cada pantalla recibe los
  videos o las fotos de su fila (en Destacados, solo los de su tipo), que lee del `InicioModelo`
  de la entrada `inicio`. Al salir le dice al modelo qué fue lo último que se vio, y el
  inicio devuelve el foco a esa tarjeta (D-21).
- **Reproductor** (`pantallas/video/`): un ExoPlayer por pantalla sobre un `SurfaceView`, la
  única superficie que mantiene el 4K sin perder cuadros. Las reglas (retomar, saltos de 10 s,
  siguiente video) son Kotlin puro en `dominio/Reproduccion.kt` y `ListaVideos.kt`. Cada video
  deja en el registro (`Reproductor`) cuántos cuadros mostró y cuántos perdió.
- **Posiciones para retomar:** `PosicionesVideo`, una sola por proceso en el `Contenedor`; nunca
  se guardan en disco.
- **Visor** (`pantallas/fotos/`): Coil reduce cada foto al tamaño de la pantalla; la
  presentación es un temporizador de 8 s que se reinicia con cada cambio.
- **Reglas de contenido:** desde el sprint 2, un video también se rechaza si la TV no tiene un
  decodificador para su perfil (`MediaCodecList`).

Pendiente de los sprints: música y USB del huésped (sprint 3) y promociones (sprint 4).
