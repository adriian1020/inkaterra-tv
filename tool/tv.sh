#!/usr/bin/env bash
# Conecta, instala y lee los registros de la app en la TV de pruebas, por la red.
# El equipo sale de la variable TV (por omisión el Xiaomi TV Box S de Adrián).
#   tool/tv.sh conectar | instalar | probar [clase] | logs
set -euo pipefail

TV=${TV:-192.168.31.242:5555}
raiz="$(cd "$(dirname "$0")/.." && pwd)"

conectar() {
  adb connect "$TV" >/dev/null 2>&1 || true
  if [ "$(adb -s "$TV" get-state 2>/dev/null)" != "device" ]; then
    echo "No encuentro la TV en $TV. Revise que esté encendida, en la misma red y con la" >&2
    echo "depuración activa en Opciones de desarrollador; si cambió de IP, use TV=ip:5555." >&2
    echo "Si dice «unauthorized», acepte en la TV el aviso «¿Permitir depuración?»." >&2
    exit 1
  fi
  echo "Conectado a $TV"
}

case "${1:-}" in
  conectar) conectar ;;
  instalar)
    conectar
    cd "$raiz/apps/tv"
    ANDROID_SERIAL="$TV" ./gradlew :app:installDebug --console=plain -q
    adb -s "$TV" shell am start -n pe.inkaterra.tv/.app.MainActivity >/dev/null
    echo "Instalada y abierta en $TV"
    ;;
  probar)
    # Pruebas en el equipo con am instrument directo: connectedDebugAndroidTest de AGP 9.4
    # marca la tarea como fallida con la TV conectada por red aunque todas las pruebas pasen.
    #   tool/tv.sh probar [pe.inkaterra.tv.Clase]
    conectar
    cd "$raiz/apps/tv"
    ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain -q
    adb -s "$TV" install -r -t app/build/outputs/apk/debug/app-debug.apk >/dev/null
    adb -s "$TV" install -r -t -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null
    # Con la TV suspendida las pruebas corren pero no se dibuja nada: despertarla primero.
    adb -s "$TV" shell input keyevent KEYCODE_WAKEUP
    # EXCLUIR=clase salta una prueba que necesita algo que no está (por ejemplo, una USB).
    filtro=()
    [ -n "${2:-}" ] && filtro=(-e class "$2")
    [ -n "${EXCLUIR:-}" ] && filtro+=(-e notClass "$EXCLUIR")
    # Lo que la prueba alcanzó a imprimir se muestra siempre, aunque adb termine con error.
    codigo=0
    salida=$(adb -s "$TV" shell am instrument -w "${filtro[@]}" pe.inkaterra.tv.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tr -d '\r') || codigo=$?
    echo "$salida"
    if [ "$codigo" -ne 0 ] && [ "$(adb -s "$TV" get-state 2>/dev/null)" != "device" ]; then
      echo "$(date +%H:%M:%S) Se perdió la conexión con la TV durante las pruebas (¿se reinició o se" >&2
      echo "suspendió?). No es un fallo de la app. Reconéctela con tool/tv.sh conectar y repita." >&2
      exit 1
    fi
    grep -q '^OK (' <<<"$salida"
    ;;
  logs)
    conectar
    adb -s "$TV" logcat --pid="$(adb -s "$TV" shell pidof pe.inkaterra.tv)"
    ;;
  *)
    echo "uso: tool/tv.sh conectar | instalar | probar [clase] | logs" >&2
    exit 2
    ;;
esac
