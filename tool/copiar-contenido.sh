#!/usr/bin/env bash
# Copia el contenido de muestra a la carpeta del hotel en la TV, para probar el inicio con la
# biblioteca local. Si muestras/contenido/ no existe, primero genera con ffmpeg un juego inventado:
# videos, fotos (una vertical y una de nombre largo), una canción con carátula y otra sin ella,
# dos promociones, y un video 8K y uno AV1 que la biblioteca debe rechazar. Las fotos y videos
# reales de Inkaterra se pueden poner en las mismas carpetas.
#   tool/copiar-contenido.sh
set -euo pipefail
# Git Bash convierte las rutas que empiezan con / en rutas de Windows; las de la TV no deben cambiar.
export MSYS_NO_PATHCONV=1

TV=${TV:-192.168.31.242:5555}
raiz="$(cd "$(dirname "$0")/.." && pwd)"
muestras="$raiz/muestras/contenido"
destino=/sdcard/Android/data/pe.inkaterra.tv/files/contenido

# ffmpeg y adb son programas de Windows: las rutas locales van en formato de Windows.
ruta_local() { cygpath -w "$1" 2>/dev/null || echo "$1"; }

generar() {
  local ffmpeg
  ffmpeg=$(command -v ffmpeg || echo "$LOCALAPPDATA/Microsoft/WinGet/Links/ffmpeg.exe")
  [ -x "$ffmpeg" ] || { echo "Falta ffmpeg: winget install --id Gyan.FFmpeg -e" >&2; exit 1; }
  ff() { "$ffmpeg" -hide_banner -loglevel error -y "$@"; }
  mkdir -p "$muestras"/{videos,fotos,musica,promociones}
  local m="$muestras"
  echo "Generando el contenido de muestra en $m…"

  # Fotos: un cuadro de distintos patrones. Una vertical y una con un nombre muy largo.
  ff -f lavfi -i "mandelbrot=size=3840x2160" -frames:v 1 "$(ruta_local "$m/fotos/Reserva Amazónica 01.jpg")"
  ff -f lavfi -i "cellauto=size=1920x1080:rule=110" -frames:v 1 "$(ruta_local "$m/fotos/Árbol de la vida.png")"
  ff -f lavfi -i "life=size=1080x1920:mold=10:ratio=0.2" -frames:v 1 "$(ruta_local "$m/fotos/Cóndor andino.jpg")"
  ff -f lavfi -i "mandelbrot=size=1920x1080:start_scale=0.5" -frames:v 1 \
    "$(ruta_local "$m/fotos/Vista panorámica del Valle Sagrado de los Incas desde la terraza del hotel.jpg")"

  # Videos que se aceptan: H.264 en Full HD y HEVC en 4K, de 10 segundos. Siempre yuv420p: con
  # una fuente RGB ffmpeg elige 4:4:4, que el decodificador de la TV no lee.
  ff -f lavfi -i "testsrc2=size=1920x1080:rate=30" -f lavfi -i "sine=frequency=330" -t 10 \
    -c:v libx264 -preset ultrafast -pix_fmt yuv420p -c:a aac "$(ruta_local "$m/videos/Atardecer en Machu Picchu.mp4")"
  ff -f lavfi -i "smptehdbars=size=3840x2160:rate=30" -f lavfi -i "sine=frequency=440" -t 10 \
    -c:v libx265 -preset ultrafast -pix_fmt yuv420p -tag:v hvc1 -c:a aac "$(ruta_local "$m/videos/Río Madre de Dios.mp4")"
  # Videos que se rechazan: 8K y AV1.
  ff -f lavfi -i "testsrc2=size=7680x4320:rate=30" -t 2 -c:v libx265 -preset ultrafast -pix_fmt yuv420p -tag:v hvc1 \
    "$(ruta_local "$m/videos/Ocho K.mp4")"
  ff -f lavfi -i "testsrc2=size=1920x1080:rate=30" -t 2 -c:v libsvtav1 -pix_fmt yuv420p "$(ruta_local "$m/videos/Selva en AV1.mkv")"

  # Música: una con la carátula incrustada y otra sin ella.
  ff -f lavfi -i "sine=frequency=262:duration=20" -i "$(ruta_local "$m/fotos/Reserva Amazónica 01.jpg")" \
    -map 0 -map 1 -c:a libmp3lame -c:v mjpeg -disposition:v attached_pic "$(ruta_local "$m/musica/Selva.mp3")"
  ff -f lavfi -i "sine=frequency=196:duration=20" -c:a aac "$(ruta_local "$m/musica/Lluvia en la Amazonía.m4a")"

  # Promociones: una foto y un video.
  ff -f lavfi -i "mandelbrot=size=1920x1080:start_scale=1.5" -frames:v 1 "$(ruta_local "$m/promociones/Oferta spa.jpg")"
  ff -f lavfi -i "rgbtestsrc=size=1920x1080:rate=30" -f lavfi -i "sine=frequency=523" -t 10 \
    -c:v libx264 -preset ultrafast -pix_fmt yuv420p -c:a aac "$(ruta_local "$m/promociones/Cena en la selva.mp4")"
}

[ -d "$muestras" ] || generar

adb connect "$TV" >/dev/null 2>&1 || true
for carpeta in videos fotos musica promociones; do
  [ -d "$muestras/$carpeta" ] || continue
  # Si adb push crea la subcarpeta dentro de Android/data, a veces falla con «secure_mkdirs
  # failed: Operation not permitted»; creada antes, no.
  adb -s "$TV" shell mkdir -p "$destino/$carpeta"
  adb -s "$TV" push "$(ruta_local "$muestras/$carpeta")" "$destino/" >/dev/null
  echo "Copiado $carpeta/ ($(find "$muestras/$carpeta" -type f | wc -l) archivos)"
done
