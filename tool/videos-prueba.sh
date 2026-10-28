#!/usr/bin/env bash
# Genera videos 4K de prueba a distintas tasas y los copia a la TV, para medir con
# Reproduccion4kTest qué reproduce fluido. Cada video lleva ruido para que el codificador no
# pueda comprimirlo de más y la tasa real se acerque a la pedida.
#   tool/videos-prueba.sh            genera lo que falte y copia todo a la TV
set -euo pipefail
# Git Bash convierte las rutas que empiezan con / en rutas de Windows; las de la TV no deben cambiar.
export MSYS_NO_PATHCONV=1

TV=${TV:-192.168.31.242:5555}
raiz="$(cd "$(dirname "$0")/.." && pwd)"
salida="$raiz/muestras/prueba-4k"
destino=/sdcard/Android/data/pe.inkaterra.tv/files/prueba-4k
segundos=25

ffmpeg=$(command -v ffmpeg || echo "$LOCALAPPDATA/Microsoft/WinGet/Links/ffmpeg.exe")
[ -x "$ffmpeg" ] || { echo "Falta ffmpeg: winget install --id Gyan.FFmpeg -e" >&2; exit 1; }
mkdir -p "$salida"

# ffmpeg y adb son programas de Windows: las rutas locales van en formato de Windows.
ruta_local() { cygpath -w "$1" 2>/dev/null || echo "$1"; }

# nombre  códec  tasa(Mbps)  fps  formato de píxel
casos=(
  "h264-20m.mp4    libx264 20  30 yuv420p"
  "h264-40m.mp4    libx264 40  30 yuv420p"
  "h264-60m.mp4    libx264 60  30 yuv420p"
  "hevc-20m.mp4    libx265 20  30 yuv420p"
  "hevc-40m.mp4    libx265 40  30 yuv420p"
  "hevc-60m.mp4    libx265 60  30 yuv420p"
  "hevc-60fps-40m.mp4 libx265 40 60 yuv420p"
  "hevc10-120m.mkv libx265 120 30 yuv420p10le"
  "vp9-20m.webm    libvpx-vp9 20 30 yuv420p"
  "vp9-40m.webm    libvpx-vp9 40 30 yuv420p"
  "vp9-60m.webm    libvpx-vp9 60 30 yuv420p"
)

for caso in "${casos[@]}"; do
  read -r nombre codec tasa fps pixel <<<"$caso"
  [ -f "$salida/$nombre" ] && continue
  echo "Generando $nombre…"
  # Cada códec tiene sus propias opciones de velocidad; WEBM no admite audio AAC, lleva Opus.
  case "$codec" in
    libx265) opciones=(-preset ultrafast -tag:v hvc1) ;;
    libvpx-vp9) opciones=(-deadline realtime -cpu-used 8 -row-mt 1) ;;
    *) opciones=(-preset ultrafast) ;;
  esac
  audio=(-c:a aac -b:a 128k)
  [ "${nombre##*.}" = webm ] && audio=(-c:a libopus -b:a 128k)
  "$ffmpeg" -hide_banner -loglevel error -y \
    -f lavfi -i "testsrc2=size=3840x2160:rate=$fps,noise=alls=12:allf=t" \
    -f lavfi -i "sine=frequency=440:sample_rate=48000" \
    -t "$segundos" -c:v "$codec" "${opciones[@]}" -pix_fmt "$pixel" \
    -b:v "${tasa}M" -minrate "${tasa}M" -maxrate "${tasa}M" -bufsize "$((tasa * 2))M" \
    "${audio[@]}" "$(ruta_local "$salida/$nombre")"
done

# Solo audio: así es como queda un video que la TV no puede decodificar (Reproduccion4kTest).
if [ ! -f "$salida/solo-audio.m4a" ]; then
  "$ffmpeg" -hide_banner -loglevel error -y -f lavfi -i "sine=frequency=440:sample_rate=48000" \
    -t "$segundos" -c:a aac -b:a 128k "$(ruta_local "$salida/solo-audio.m4a")"
fi

adb connect "$TV" >/dev/null 2>&1 || true
adb -s "$TV" shell mkdir -p "$destino"
for archivo in "$salida"/*; do
  adb -s "$TV" push "$(ruta_local "$archivo")" "$destino/" >/dev/null
  echo "Copiado $(basename "$archivo")"
done
