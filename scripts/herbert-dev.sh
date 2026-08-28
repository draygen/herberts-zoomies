#!/usr/bin/env bash
#
# herbert-dev.sh - emulator-first development loop for Herbert's Zoomies.
#
# Every adb call made by this script passes an explicit `-s <serial>`, and the
# serial defaults to a running *emulator*. The physical Galaxy S24 Ultra is
# never targeted unless you ask for it by hand (see "Real device" below).
#
#   ./scripts/herbert-dev.sh help
#
# Real device: pass --device <serial> together with HERBERT_ALLOW_PHYSICAL=1,
# e.g.
#   HERBERT_ALLOW_PHYSICAL=1 ./scripts/herbert-dev.sh --device 192.168.0.71:5555 install
#
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# --- machine-local defaults (this workstation: Windows 11 host + WSL2) --------
# Override any of these in scripts/dev.env or via the environment.
HERBERT_JAVA_HOME="${HERBERT_JAVA_HOME:-/home/draygen/.local/toolchains/jdk-21.0.12.1+1}"
HERBERT_WSL_SDK="${HERBERT_WSL_SDK:-/home/draygen/.local/toolchains/android-sdk}"
HERBERT_WIN_SDK="${HERBERT_WIN_SDK:-C:\\Users\\Administrator\\AppData\\Local\\Android\\Sdk}"
HERBERT_WIN_SDK_WSL="${HERBERT_WIN_SDK_WSL:-/mnt/c/Users/Administrator/AppData/Local/Android/Sdk}"
HERBERT_WIN_AVD_HOME="${HERBERT_WIN_AVD_HOME:-/mnt/c/Users/Administrator/.android/avd}"
HERBERT_AVD="${HERBERT_AVD:-Herbert_S24U_Dev}"
# -----------------------------------------------------------------------------

[ -f "$REPO_ROOT/scripts/dev.env" ] && . "$REPO_ROOT/scripts/dev.env"

PKG=com.draygen.herbertzoom
ACTIVITY="$PKG/.MainActivity"
APK="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"
SHOTS="$REPO_ROOT/.devout/screenshots"
LOGS="$REPO_ROOT/.devout/logs"

ADB="${ADB:-adb}"
TARGET="${HERBERT_TARGET:-}"

die() { printf '\033[31merror:\033[0m %s\n' "$*" >&2; exit 1; }
info() { printf '\033[36m==>\033[0m %s\n' "$*"; }

# --- device selection --------------------------------------------------------

list_devices() {
    "$ADB" devices | awk 'NR>1 && $2=="device" {print $1}'
}

# Resolve the serial we will operate on. Emulators only, unless explicitly
# overridden with --device + HERBERT_ALLOW_PHYSICAL=1.
resolve_target() {
    if [ -n "$TARGET" ]; then
        case "$TARGET" in
            emulator-*) ;;
            *)
                [ "${HERBERT_ALLOW_PHYSICAL:-0}" = "1" ] || die \
"refusing to target non-emulator device '$TARGET'.
       Re-run with HERBERT_ALLOW_PHYSICAL=1 if you really mean the physical phone."
                printf '\033[33m!! targeting PHYSICAL device %s\033[0m\n' "$TARGET" >&2
                ;;
        esac
        echo "$TARGET"; return
    fi

    local emus
    emus=$(list_devices | grep '^emulator-' || true)
    [ -n "$emus" ] || die \
"no emulator is running.
       Start one with:  ./scripts/herbert-dev.sh boot
       (Physical devices are ignored on purpose. See --device in 'help'.)"

    local count; count=$(printf '%s\n' "$emus" | wc -l)
    if [ "$count" -gt 1 ]; then
        die "more than one emulator is running:
$(printf '%s\n' "$emus" | sed 's/^/         /')
       Pick one with --device <serial>."
    fi
    printf '%s\n' "$emus"
}

T() { [ -n "${_T:-}" ] || _T="$(resolve_target)" || exit 1; echo "$_T"; }
a() { "$ADB" -s "$(T)" "$@"; }

# --- windows helpers ---------------------------------------------------------

win_run() {  # run a windows exe detached, from a non-UNC cwd
    ( cd /mnt/c && cmd.exe /c "$@" ) 2>&1 | grep -v -e 'UNC path' -e 'CMD.EXE was started' -e '^..wsl.localhost' || true
}

emulator_exe() { echo "$HERBERT_WIN_SDK_WSL/emulator/emulator.exe"; }

# --- orientation + input ----------------------------------------------------
#
# Verified on this setup: `adb shell input tap/swipe` uses the *logical display*
# coordinate space, which is the same space `screencap` returns. So coordinates
# read straight off a screenshot can be passed through unchanged, even though
# the panel is physically portrait (1440x3120) and the game is landscape
# (3120x1440). Use `touchdebug 1` to see where a tap actually lands.

# echoes: <natural_w> <natural_h> <rotation 0|90|180|270>
display_geometry() {
    a shell dumpsys window displays 2>/dev/null | tr -d '\r' | awk '
        /init=/ && !done_init {
            match($0, /init=[0-9]+x[0-9]+/); g=substr($0, RSTART+5, RLENGTH-5)
            split(g, d, "x"); nw=d[1]; nh=d[2]; done_init=1
        }
        /mDisplayRotation=ROTATION_/ && !done_rot {
            match($0, /mDisplayRotation=ROTATION_[0-9]+/)
            r=substr($0, RSTART+26, RLENGTH-26); done_rot=1
        }
        END { if (r=="") r=0; print nw, nh, r }'
}

cmd_tap() {
    [ $# -ge 2 ] || die "usage: tap <x> <y>   (coordinates read straight off a screenshot)"
    info "tap ($1,$2)"
    a shell input tap "$1" "$2"
}

cmd_swipe() {
    [ $# -ge 4 ] || die "usage: swipe <x1> <y1> <x2> <y2> [ms]"
    info "swipe ($1,$2) -> ($3,$4) over ${5:-250}ms"
    a shell input swipe "$1" "$2" "$3" "$4" "${5:-250}"
}

# Show/hide the touch overlay - handy when a tap "does nothing" and you need to
# see where it actually landed.
cmd_touchdebug() {
    local on="${1:-1}"
    a shell settings put system show_touches "$on"
    a shell settings put system pointer_location "$on"
    info "touch overlay = $on"
}

# The emulator *host window* is rotated independently of the guest's app
# orientation, so a landscape-locked game can render sideways in a portrait
# window. Read the emulator's accelerometer to find the device orientation and
# rotate until it is landscape-left (gravity along +x).
cmd_spin() {
    local i accel x
    for i in 1 2 3 4 5; do
        accel=$(a emu sensor get acceleration 2>/dev/null | tr -d '\r' | sed -n 's/^acceleration = //p')
        if [ -z "$accel" ]; then
            info "emulator console unavailable; copy the console auth token with:"
            info "  cp /mnt/c/Users/\$WINUSER/.emulator_console_auth_token ~/"
            return 1
        fi
        x=${accel%%:*}
        if [ "${x%%.*}" -ge 9 ] 2>/dev/null; then
            info "device is landscape (acceleration $accel)"
            return 0
        fi
        info "rotating emulator window (acceleration $accel)"
        a emu rotate >/dev/null
        sleep 2
    done
    info "gave up after 5 rotations - rotate manually with Ctrl+Left/Ctrl+Right"
    return 1
}

# --- commands ----------------------------------------------------------------

cmd_help() {
    sed -n '3,13p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    cat <<'EOF'
Commands:
  boot [avd]        start an emulator (default: $HERBERT_AVD) and wait for boot
  avds              list available AVDs
  devices           list adb devices, flagged emulator / PHYSICAL
  target            print the serial this script would use
  info              print toolchain + emulator environment summary

  test              ./gradlew test          (JVM unit tests, no device needed)
  build             ./gradlew assembleDebug
  install           adb install -r the debug APK onto the emulator
  launch            start MainActivity
  stop              force-stop the app
  restart           stop + launch
  run               build + install + launch          <- the main dev loop
  cycle             build + install + launch + shot   <- loop + screenshot

  shot [name]       screenshot -> .devout/screenshots/
  logcat [n]        last n lines (default 200) of this app's log
  logcat-follow     live tail of this app's log
  crash             dump the most recent crash buffer
  clearlog          clear the device log buffer

  pause             send the app to background (HOME)
  resume            bring the app back to the foreground
  pauseresume       pause, wait, resume, screenshot   <- lifecycle check
  spin              rotate the emulator WINDOW until the device is landscape
                    (a landscape-locked game renders sideways otherwise)
  tap <x> <y>       tap at screenshot coordinates
  swipe <x1> <y1> <x2> <y2> [ms]
  touchdebug [0|1]  show/hide the on-screen touch position overlay
  geometry          natural WxH + current rotation
  rotate <n>        force guest user_rotation 0|1|2|3 (0=portrait, 1=landscape)
  rotate auto       restore automatic rotation
  gfx               raw dumpsys gfxinfo for the app
  fps [secs]        real SurfaceView frame rate (default 5s sample)

  kill              shut the emulator down cleanly
  wipe              cold-boot the emulator (wipe user data)

Flags:
  --device <serial> operate on a specific serial (emulator, or a physical
                    device when HERBERT_ALLOW_PHYSICAL=1)
EOF
}

cmd_avds() {
    local ini name cfg
    for ini in "$HERBERT_WIN_AVD_HOME"/*.ini; do
        [ -e "$ini" ] || { echo "  (no AVDs found in $HERBERT_WIN_AVD_HOME)"; return; }
        name=$(basename "$ini" .ini)
        cfg="$HERBERT_WIN_AVD_HOME/$name.avd/config.ini"
        printf '  %-22s %s\n' "$name" "$(
            [ -f "$cfg" ] && tr -d '\r' < "$cfg" | awk -F= '
                /^hw\.lcd\.width=/{w=$2} /^hw\.lcd\.height=/{h=$2}
                /^hw\.lcd\.density=/{d=$2} /^hw\.ramSize=/{r=$2}
                /^hw\.cpu\.ncore=/{c=$2} /^target=/{t=$2}
                END{printf "%sx%s @%sdpi  %sMB RAM  %s cores  %s", w, h, d, r, c, t}')"
    done
}

cmd_boot() {
    local avd="${1:-$HERBERT_AVD}"
    if list_devices | grep -q '^emulator-'; then
        info "emulator already running: $(list_devices | grep '^emulator-' | tr '\n' ' ')"
        return 0
    fi
    info "booting AVD $avd (host GPU, hardware accelerated)"
    win_run start "" "$HERBERT_WIN_SDK\\emulator\\emulator.exe" -avd "$avd" -gpu host -no-boot-anim
    info "waiting for boot..."
    local i serial=""
    for i in $(seq 1 90); do
        serial=$(list_devices | grep '^emulator-' | head -1)
        if [ -n "$serial" ]; then
            [ "$("$ADB" -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r\n')" = "1" ] && break
        fi
        sleep 2
    done
    [ -n "$serial" ] || die "emulator did not appear in adb"
    info "booted: $serial"
    # one-time emulator conveniences (emulator only, never a real phone)
    "$ADB" -s "$serial" shell settings put secure immersive_mode_confirmations confirmed >/dev/null 2>&1
    "$ADB" -s "$serial" shell settings put global window_animation_scale 0.5 >/dev/null 2>&1
    "$ADB" -s "$serial" shell settings put global transition_animation_scale 0.5 >/dev/null 2>&1
    # the emulator console needs its auth token in $HOME to accept `adb emu`
    if [ ! -f "$HOME/.emulator_console_auth_token" ] && [ -f "$HERBERT_WIN_AVD_HOME/../.emulator_console_auth_token" ]; then
        cp "$HERBERT_WIN_AVD_HOME/../.emulator_console_auth_token" "$HOME/.emulator_console_auth_token" 2>/dev/null
        chmod 600 "$HOME/.emulator_console_auth_token" 2>/dev/null
    fi
    _T="$serial" cmd_spin || true
}

cmd_devices() {
    local d
    for d in $(list_devices); do
        case "$d" in
            emulator-*) printf '  %-24s emulator   (%s)\n' "$d" "$("$ADB" -s "$d" shell getprop ro.boot.qemu.avd_name 2>/dev/null | tr -d '\r')" ;;
            *)          printf '  %-24s \033[33mPHYSICAL\033[0m   (%s)\n' "$d" "$("$ADB" -s "$d" shell getprop ro.product.model 2>/dev/null | tr -d '\r')" ;;
        esac
    done
    [ -n "$(list_devices)" ] || echo "  (none)"
}

cmd_target() { T; }

cmd_info() {
    echo "repo             : $REPO_ROOT"
    echo "JDK (build)      : $HERBERT_JAVA_HOME"
    echo "SDK (build, WSL) : $HERBERT_WSL_SDK"
    echo "SDK (emu, Win)   : $HERBERT_WIN_SDK"
    echo "default AVD      : $HERBERT_AVD"
    echo "adb              : $("$ADB" version | head -2 | tail -1)"
    echo "emulator         : $(win_run "$HERBERT_WIN_SDK\\emulator\\emulator.exe" -version 2>/dev/null | head -1)"
    echo "devices:"; cmd_devices
}

gradle() {
    ( cd "$REPO_ROOT" && JAVA_HOME="$HERBERT_JAVA_HOME" PATH="$HERBERT_JAVA_HOME/bin:$PATH" ./gradlew "$@" )
}

cmd_test()  { info "gradle test";          gradle test; }
cmd_build() { info "gradle assembleDebug"; gradle assembleDebug || die "build failed"; ls -lh "$APK"; }

cmd_install() {
    [ -f "$APK" ] || die "no APK at $APK - run 'build' first"
    info "installing to $(T)"
    a install -r "$APK" || die "install failed"
}

cmd_launch()  { info "launching $ACTIVITY on $(T)"; a shell am start -n "$ACTIVITY" >/dev/null && echo "started"; }
cmd_stop()    { info "force-stopping $PKG on $(T)"; a shell am force-stop "$PKG"; }
cmd_restart() { cmd_stop; sleep 1; cmd_launch; }
cmd_run()     { cmd_build && cmd_install && cmd_launch; }
cmd_cycle()   { cmd_run && sleep 4 && cmd_shot; }

cmd_shot() {
    mkdir -p "$SHOTS"
    local name="${1:-shot}"
    local out="$SHOTS/$(date +%Y%m%d-%H%M%S)-$name.png"
    a exec-out screencap -p > "$out" || die "screencap failed"
    [ -s "$out" ] || die "screenshot was empty"
    info "$out  ($(file -b "$out" | cut -d, -f2 | xargs))"
    echo "$out"
}

app_pid() { a shell pidof "$PKG" 2>/dev/null | tr -d '\r\n'; }

cmd_logcat() {
    local n="${1:-200}" pid; pid=$(app_pid)
    if [ -n "$pid" ]; then a logcat -d --pid="$pid" -t "$n"
    else info "app not running; showing tagged lines"; a logcat -d -t "$n" | grep -i -e herbert -e zoomies -e AndroidRuntime || true
    fi
}
cmd_logcat_follow() { local pid; pid=$(app_pid); [ -n "$pid" ] || die "app not running"; a logcat --pid="$pid"; }
cmd_crash()    { a logcat -d -b crash -t 200; }
cmd_clearlog() { a logcat -c && info "log buffer cleared"; }

cmd_pause()  { info "sending app to background"; a shell input keyevent KEYCODE_HOME; }
cmd_resume() { info "resuming app"; a shell am start -n "$ACTIVITY" >/dev/null && echo resumed; }
cmd_pauseresume() {
    cmd_clearlog >/dev/null
    cmd_pause; sleep 3
    local pid_bg; pid_bg=$(app_pid)
    cmd_resume; sleep 4
    local pid_fg; pid_fg=$(app_pid)
    echo "pid while backgrounded: ${pid_bg:-<process gone>}"
    echo "pid after resume      : ${pid_fg:-<process gone>}"
    if [ -n "$pid_bg" ] && [ "$pid_bg" = "$pid_fg" ]; then
        info "process survived background/foreground (true pause/resume)"
    else
        info "process was recreated - check onPause/onResume state handling"
    fi
    cmd_shot resume
}

cmd_rotate() {
    local r="${1:-}"
    case "$r" in
        auto) a shell settings put system accelerometer_rotation 1; info "auto-rotate on" ;;
        0|1|2|3)
            a shell settings put system accelerometer_rotation 0
            a shell settings put system user_rotation "$r"
            info "user_rotation=$r" ;;
        *) die "usage: rotate <0|1|2|3|auto>" ;;
    esac
}

cmd_gfx() { a shell dumpsys gfxinfo "$PKG"; }

# The game draws on a SurfaceView render thread, so `dumpsys gfxinfo` sees
# almost nothing. Real frame times come from SurfaceFlinger's latency buffer
# for the game's own layer.
cmd_fps() {
    local secs="${1:-5}" layer
    layer=$(a shell dumpsys SurfaceFlinger --list 2>/dev/null | tr -d '\r' \
            | grep -F "SurfaceView[$PKG" | grep -F '(BLAST)' | head -1 \
            | sed 's/^RequestedLayerState{//; s/ parentId=.*$//; s/}$//')
    [ -n "$layer" ] || die "no SurfaceView layer for $PKG - is the game running?"
    info "sampling '$layer' for ${secs}s"
    a shell dumpsys SurfaceFlinger --latency-clear >/dev/null 2>&1
    sleep "$secs"
    a shell dumpsys SurfaceFlinger --latency "\"$layer\"" 2>/dev/null | tr -d '\r' | awk '
        NR>1 && NF==3 && $2+0>0 { t[NR]=$2 }
        END {
            n=0
            for (i in t) v[++n]=t[i]
            if (n<3) { print "not enough frames sampled"; exit }
            for (i=1;i<n;i++) for (j=1;j<n-i+1;j++) if (v[j]>v[j+1]) { tmp=v[j]; v[j]=v[j+1]; v[j+1]=tmp }
            m=0
            for (i=1;i<n;i++) { d=(v[i+1]-v[i])/1e6; if (d>0 && d<500) dt[++m]=d }
            if (m<2) { print "not enough frames sampled"; exit }
            for (i=1;i<m;i++) for (j=1;j<m-i+1;j++) if (dt[j]>dt[j+1]) { tmp=dt[j]; dt[j]=dt[j+1]; dt[j+1]=tmp }
            s=0; for (i=1;i<=m;i++) s+=dt[i]
            printf "frames sampled : %d\n", m+1
            printf "mean           : %.2f ms  (%.1f FPS)\n", s/m, 1000/(s/m)
            printf "median         : %.2f ms  (%.1f FPS)\n", dt[int(m/2)+1], 1000/dt[int(m/2)+1]
            printf "p90 / p99      : %.2f ms / %.2f ms\n", dt[int(m*0.90)+1], dt[int(m*0.99)]
            printf "worst          : %.2f ms\n", dt[m]
        }'
}

cmd_kill() { info "shutting down $(T)"; a emu kill; }
cmd_wipe() {
    local avd="${1:-$HERBERT_AVD}"
    list_devices | grep -q '^emulator-' && { info "killing running emulator first"; a emu kill; sleep 4; }
    info "cold-booting $avd with wiped user data"
    win_run start "" "$HERBERT_WIN_SDK\\emulator\\emulator.exe" -avd "$avd" -gpu host -no-boot-anim -wipe-data
}

# --- dispatch ----------------------------------------------------------------

while [ $# -gt 0 ]; do
    case "$1" in
        --device) TARGET="${2:?--device needs a serial}"; shift 2 ;;
        *) break ;;
    esac
done

cmd="${1:-help}"; shift || true
case "$cmd" in
    help|-h|--help) cmd_help ;;
    boot)           cmd_boot "$@" ;;
    avds)           cmd_avds ;;
    devices)        cmd_devices ;;
    target)         cmd_target ;;
    info)           cmd_info ;;
    test)           cmd_test ;;
    build)          cmd_build ;;
    install)        cmd_install ;;
    launch)         cmd_launch ;;
    stop)           cmd_stop ;;
    restart)        cmd_restart ;;
    run)            cmd_run ;;
    cycle)          cmd_cycle ;;
    shot)           cmd_shot "$@" ;;
    logcat)         cmd_logcat "$@" ;;
    logcat-follow)  cmd_logcat_follow ;;
    crash)          cmd_crash ;;
    clearlog)       cmd_clearlog ;;
    pause)          cmd_pause ;;
    resume)         cmd_resume ;;
    pauseresume)    cmd_pauseresume ;;
    rotate)         cmd_rotate "$@" ;;
    spin)           cmd_spin ;;
    tap)            cmd_tap "$@" ;;
    swipe)          cmd_swipe "$@" ;;
    touchdebug)     cmd_touchdebug "$@" ;;
    geometry)       display_geometry ;;
    gfx)            cmd_gfx ;;
    fps)            cmd_fps "$@" ;;
    kill)           cmd_kill ;;
    wipe)           cmd_wipe "$@" ;;
    *)              die "unknown command '$cmd' (try: help)" ;;
esac
