# Emulator Development Environment

Fast, emulator-first development loop for *Herbert's Zoomies*, so day-to-day
iteration never has to commandeer the real Galaxy S24 Ultra.

> **Local configuration.** The absolute paths below are for **this workstation**
> (Windows 11 host + WSL2 Ubuntu, Windows user `Administrator`). They are
> defaults inside `scripts/herbert-dev.sh` and can all be overridden from
> `scripts/dev.env` (gitignored — see `scripts/dev.env.example`).

---

## 1. Where things live

| Component | Version | Location |
|---|---|---|
| Android Studio | `2026.1.3.7` (build `AI-261.26222.65.2613.15948027`) | `C:\Program Files\Android\Android Studio` |
| Android SDK (emulator side, **Windows**) | — | `C:\Users\Administrator\AppData\Local\Android\Sdk` |
| Android SDK (build side, **WSL**) | — | `/home/draygen/.local/toolchains/android-sdk` |
| SDK Command-line Tools | `23.0.0` | `…\Sdk\cmdline-tools\latest` |
| Platform-Tools (`adb`) | `37.0.1` | `…\Sdk\platform-tools` |
| Emulator | `37.1.11` (build `15917651`) | `…\Sdk\emulator` |
| Platform | `android-35` (matches `compileSdk`/`targetSdk` 35) | `…\Sdk\platforms\android-35` |
| Build-Tools | `35.0.0` | `…\Sdk\build-tools\35.0.0` |
| System image | `system-images;android-35;google_apis;x86_64` (rev 9) | `…\Sdk\system-images\android-35` |
| JDK (Windows, for SDK tools) | Microsoft OpenJDK `21.0.12.8` | `C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot` |
| JDK (WSL, for Gradle) | Temurin `21.0.12.1+1` | `/home/draygen/.local/toolchains/jdk-21.0.12.1+1` |

**Two SDKs on purpose.** The Gradle build keeps using the existing WSL SDK
(shared with `draygen/smstext`); the Windows SDK exists only to host the GUI,
the emulator and the AVDs. Neither project's `local.properties`, JDK, Gradle or
SDK versions were changed.

Hardware acceleration: **WHPX (Windows Hypervisor Platform)**. The
`HypervisorPlatform` Windows feature was enabled during setup;
`emulator -accel-check` reports *"WHPX(10.0.26200) is installed and usable."*

## 2. The AVDs

| AVD | Purpose | Display | Density | RAM | Cores | Storage |
|---|---|---|---|---|---|---|
| `Herbert_S24U_Dev` | main dev target, S24 Ultra-class | 1440 × 3120 (19.5:9) | 560 dpi | 6144 MB | 4 | 10 G data + 1 G sdcard |
| `Herbert_Midrange_Dev` | "does it still run on a normal phone?" | 1080 × 2400 (20:9) | 420 dpi | 3072 MB | 2 | 6 G data + 512 M sdcard |

Both run API 35 / Google APIs / x86_64, `hw.gpu.mode=host`, no device frame.
AVD definitions live in `C:\Users\Administrator\.android\avd` (never committed).

The game must keep using its virtual 1920 × 1080 coordinate space with
aspect-fill scaling — **nothing is hardcoded to these resolutions.**

## 3. Daily loop

Everything goes through `scripts/herbert-dev.sh`, run from the repo root in WSL.

```bash
./scripts/herbert-dev.sh boot        # start Herbert_S24U_Dev, wait for boot
./scripts/herbert-dev.sh run         # build + install + launch   <- the loop
./scripts/herbert-dev.sh cycle       # run + screenshot
./scripts/herbert-dev.sh shot title  # screenshot -> .devout/screenshots/
./scripts/herbert-dev.sh logcat      # this app's recent log
./scripts/herbert-dev.sh fps         # real SurfaceView frame rate
./scripts/herbert-dev.sh pauseresume # lifecycle check + screenshot
./scripts/herbert-dev.sh kill        # shut the emulator down
./scripts/herbert-dev.sh help        # everything else
```

### The raw commands it wraps

```bash
# build (WSL, unchanged toolchain)
export JAVA_HOME=/home/draygen/.local/toolchains/jdk-21.0.12.1+1
export PATH=$JAVA_HOME/bin:$PATH
./gradlew test assembleDebug
#   -> app/build/outputs/apk/debug/app-debug.apk

# boot the AVD (Windows binary, launched from WSL)
cd /mnt/c && cmd.exe /c start "" \
  "C:\Users\Administrator\AppData\Local\Android\Sdk\emulator\emulator.exe" \
  -avd Herbert_S24U_Dev -gpu host -no-boot-anim

adb devices -l                                             # list devices
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am force-stop com.draygen.herbertzoom
adb -s emulator-5554 shell am start -n com.draygen.herbertzoom/.MainActivity
adb -s emulator-5554 exec-out screencap -p > shot.png
adb -s emulator-5554 logcat -d --pid=$(adb -s emulator-5554 shell pidof com.draygen.herbertzoom)
adb -s emulator-5554 emu kill                              # shut down
```

## 4. Real-phone safety

WSL runs with `networkingMode=mirrored`, so **WSL and Windows share one adb
server** — the physical S24 Ultra (`192.168.0.71:5555`) and the emulator are
both visible from either side. `herbert-dev.sh` therefore:

* only ever resolves to a serial matching `emulator-*`;
* refuses to act if two emulators are running (pick one with `--device`);
* refuses a non-emulator serial unless you *also* set `HERBERT_ALLOW_PHYSICAL=1`,
  and prints a warning when you do;
* passes an explicit `-s <serial>` on every single adb call.

Milestone verification on the real phone, when you actually want it:

```bash
HERBERT_ALLOW_PHYSICAL=1 ./scripts/herbert-dev.sh --device 192.168.0.71:5555 install
HERBERT_ALLOW_PHYSICAL=1 ./scripts/herbert-dev.sh --device 192.168.0.71:5555 launch
```

## 5. Gotchas worth remembering

**The emulator window is "held" in portrait.** `MainActivity` is locked to
landscape, so on a freshly booted AVD the game renders *sideways* inside a tall
portrait window. That is faithful device behaviour (you'd physically turn a real
phone), not a bug. `boot` fixes it automatically; otherwise:

```bash
./scripts/herbert-dev.sh spin      # rotates until the device reads landscape
```

`spin` reads the emulator's accelerometer over the console and rotates until
gravity is along +x, so it is self-correcting rather than a fixed number of
turns. It needs the console auth token in `$HOME`:
`cp /mnt/c/Users/Administrator/.emulator_console_auth_token ~/` (done by `boot`).

**Touch coordinates are screenshot coordinates.** Verified on this setup:
`adb shell input tap/swipe` uses the logical display space, the same space
`screencap` returns — so numbers read straight off a screenshot work unchanged,
even though the panel is physically portrait. If a tap seems to do nothing, use
`./scripts/herbert-dev.sh touchdebug 1` to see where it actually landed.

**`dumpsys gfxinfo` is useless here.** The game draws on a SurfaceView render
thread, so gfxinfo reports ~11 frames. `herbert-dev.sh fps` samples
SurfaceFlinger's latency buffer for the game's own layer instead.

**A one-time "Viewing full screen" system dialog** covers the title screen on a
fresh AVD. `boot` suppresses it
(`settings put secure immersive_mode_confirmations confirmed`).

**The new `android` CLI exits non-zero on success.** `sdkmanager` is deprecated
in cmdline-tools 23; the replacement `android sdk install` downloads and
installs correctly but returns `0xC0000409`. Check the filesystem, not the exit
code. Also note `sdkmanager.bat` mangles `;`-separated package names when
invoked from PowerShell — use `android sdk install platforms/android-35`
(slashes) or a `.bat` wrapper.

## 6. What the emulator does *not* tell you

Real-device testing on the S24 Ultra stays authoritative for:

* **Samsung One UI** behaviour — the AVD is stock AOSP + Google APIs.
* **API level.** The AVD is API 35 (matching the project's `targetSdk`); the
  real phone is on **Android 16 / API 36**.
* **Panel resolution.** The AVD runs the S24 Ultra's native 1440 × 3120 @ 560 dpi;
  the real phone ships at FHD+ **1080 × 2340 @ 450 dpi** (override 470) unless
  its display setting is changed. So the emulator is a *higher*-resolution target.
* **120 Hz.** The AVD is capped at 60 Hz.
* **Real GPU/thermal performance, HDR/AMOLED colour, haptics, and audio
  latency.**

To add an API 36 image later:

```
android sdk install system-images/android-36/google_apis/x86_64
```
