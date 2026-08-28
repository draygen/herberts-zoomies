# Herbert's Zoomies — Handoff to Claude

## 1. Project Context & Environment
- **Project Root**: `/home/draygen/herbertzoom`
- **Current Branch**: `main`
- **JDK Location**: `/home/draygen/.local/toolchains/jdk-21.0.12.1+1` (Temurin OpenJDK 21.0.12.1)
- **Android SDK**: `/home/draygen/.local/toolchains/android-sdk` (compileSdk 35, minSdk 29, targetSdk 35)
- **Target Physical Hardware**: Samsung Galaxy S24 Ultra (`SM-S928U`, 1080x2340 AMOLED, connected via ADB at `192.168.0.71:5555`)

## 2. Technology & Architecture Choices
- **Engine**: Pure Kotlin Native SurfaceView Game Engine with dual-module separation:
  - `game-core`: Clean, deterministic game simulation engine with zero Android runtime dependencies. Encapsulates `Herbert`, `LivingRoomWorld`, `ObstacleManager`, `ZoomieMeter`, `ScoreRecord`, collision detection, jump parabolic physics, and combo multipliers. Fully unit tested via JUnit 5.
  - `app`: Android SurfaceView fixed-timestep render loop (~60 FPS target with letterbox/pillarbox virtual 1920x1080 canvas), custom high-performance 2D Canvas vector rendering (`HerbertRenderer`, `WorldRenderer`, `GameHudRenderer`), dynamic synthesized sound effects (`SoundEffects`), and lifecycle management (`MainActivity`).
- **Why this stack**: Zero bloated third-party engines, instantaneous compile times (1-3 seconds), predictable memory allocations without GC stalls, zero network permissions, and native compatibility on Galaxy S24 Ultra.

## 3. Commands

### Run Unit Tests:
```bash
export JAVA_HOME=/home/draygen/.local/toolchains/jdk-21.0.12.1+1
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :game-core:test
```

### Build APK:
```bash
./gradlew assembleDebug
```
APK Path: `app/build/outputs/apk/debug/app-debug.apk`

### Install & Launch on Real S24 Ultra:
```bash
adb -s 192.168.0.71:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.0.71:5555 shell am start -n com.draygen.herbertzoom/.MainActivity
```

## 4. What Is Implemented & Verified on Hardware
- [x] Full standalone Android Gradle repository (`game-core` + `app`).
- [x] Procedural/vector stylized Herbert rendering featuring:
  - Striking blue eyes with dilated pupils during Zoomies
  - Dark ear tips and dark spot markings
  - Pink nose and mouth
  - Pink belly exposed during jumps, skids, and flopping
  - Dynamic animated tail whip and galloping paws
- [x] Living Room Panic environment with parallax wood floor planks, baseboard, dynamic living room rug, shadows, and props.
- [x] Obstacles: Slippers, cardboard boxes, couch cushions, table legs, sock piles.
- [x] Pickups: Golden fish treats, plush toy mice, yarn balls with string trails.
- [x] Collision detection: Jump clearance over low obstacles, soft obstacle scatter during Maximum Zoomies, near-miss bonus points.
- [x] Wholesome fail state: Flopped cat on pink belly with wholesome sound cue and game over dialog.
- [x] Zoomie Meter & Maximum Zoomies (speed boost, visual trails, dilated eyes, 3x score multiplier).
- [x] Local high score persistence using `SharedPreferences`.
- [x] Synthesized audio engine for blips, jump boings, near-miss swooshes, zoomie fanfares, and flop thuds (zero external audio file dependencies).
- [x] Full real-device testing on Galaxy S24 Ultra (`192.168.0.71:5555`):
  - Verified 60 FPS smooth rendering on 1080x2340 screen.
  - Verified touch drag steering and swipe-up jumps.
  - Verified pause/resume backgrounding without crashing or memory leaks.
  - Verified zero sensitive permissions in AndroidManifest.

## 5. Assets & Licenses
Documented in `docs/ASSETS.md`. All visual and audio rendering is 100% original programmatic / synthesized vector & PCM sound code under project license (MIT / Proprietary). No third-party copyright assets are loaded.

## 6. What Claude Should Do Next (Recommended Tasks)
1. **Herbert Visual Variations / Polish**:
   - Add box dive animation state when Herbert lands inside an open cardboard box.
   - Add extra particle dust clouds under paws when hard steering / skidding.
2. **Additional Living Room Props**:
   - Cat tunnel interactive sliding.
   - Harmless knocked-over lightweight objects (e.g. newspaper rolls, catnip toys).
3. **Audio / Haptic Customization**:
   - Add simple toggle in title/pause screen for sound and vibration mute.
4. **Subsequent Environments (Post Milestone 1)**:
   - Level 2: *Kitchen Tile Drift* (low friction sliding mechanics).
   - Level 3: *Midnight Hallway Sprint*.

## 7. Decisions Not to Casually Reverse
- Keep `game-core` 100% decoupled from Android SDK so physics/rules can be tested instantaneously on JVM without emulator/device overhead.
- Keep the coordinate system virtualized (1920x1080) to preserve crisp aspect ratio scaling on any screen.
- Maintain wholesome / no-harm policy for Herbert (flops and loaves only).
