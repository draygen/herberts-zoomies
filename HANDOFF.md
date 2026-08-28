# Herbert's Zoomies — Handoff to Claude

## 1. Project Context & Environment
- **Project Root**: `/home/draygen/herbertzoom`
- **Current Branch**: `main`
- **JDK Location**: `/home/draygen/.local/toolchains/jdk-21.0.12.1+1` (Temurin OpenJDK 21.0.12.1)
- **Android SDK**: `/home/draygen/.local/toolchains/android-sdk` (compileSdk 35, minSdk 29, targetSdk 35)
- **Target Physical Hardware**: Samsung Galaxy S24 Ultra (`SM-S928U`, 1080x2340 AMOLED, connected via ADB at `192.168.0.71:5555`)

## 2. Technology & Architecture Choices
- **Engine**: Pure Kotlin Native SurfaceView Game Engine with dual-module separation:
  - `game-core`: Clean, deterministic game simulation engine with zero Android runtime dependencies. Encapsulates `Herbert`, `Kitty`, `LivingRoomWorld`, `Obstacle` & `Pickup` management, `ZoomieMeter`, `ScoreRecord`, collision detection, jump parabolic physics, and combo multipliers. Fully unit tested via JUnit 5.
  - `app`: Android SurfaceView fixed-timestep render loop (~60 FPS target with fullscreen fill scaling on Samsung's ~19.5:9 aspect ratio), custom high-performance 2D Canvas vector rendering (`HerbertRenderer`, `KittyRenderer`, `WorldRenderer`, `GameHudRenderer`), dynamic synthesized sound effects (`SoundEffects`), particle juice system (`ParticleSystem`), and lifecycle management (`MainActivity`).

## 3. What Was Changed in This Pass

### A. Real-Herbert Mascot Overhaul (`HerbertRenderer.kt`):
- **Fur & Markings**: Switched to mostly cream-white body with authentic dark seal/brown ear tips, dark eye mask encircling the eyes, white forehead-to-muzzle blaze, and a solid dark tail with expressive kitten whip animations.
- **Eyes**: Large pale ice-blue eyes with royal blue inner iris ring, deep black pupils, and bright glints (dilating huge during Maximum Zoomies).
- **Pink Accents**: Bubblegum pink nose and 3 pink toe beans on paws. Visible pink belly during jumps, skids, and flopping.

### B. Added Kitty as a Character Hazard (`KittyRenderer.kt` & `WorldEntities.kt`):
- **Visuals**: Plump, short, compact 9-year-old brown/gray mackerel tabby with forehead "M" stripes, cream muzzle/chest, and round gold-green eyes with heavy unamused eyelids ("sick of Herbert's nonsense").
- **Gameplay Behaviors**:
  - `KITTY_LOAF`: Loafing on the floor judging Herbert.
  - `KITTY_SLEEPING`: Sleeping with curved eyes and floating "Zzz"s.
  - `KITTY_WADDLE`: Waddling slowly across the room.
  - `SWAT ATTACK`: Extends paw and claws with cartoon swipe when Herbert zooms nearby.
  - *Interaction*: Herbert can cleanly leap/pounce over Kitty (+near miss points) or bounce safely past in Maximum Zoomies.

### C. Difficulty, Accessibility & Fun Rebalance:
- **Base Speed**: Lowered starting speed from `500f` to `360f` with a much smoother acceleration ramp (`9f/s`).
- **Pacing**: First 25 seconds space obstacles gently (`1.8s` spawn intervals) to build player confidence.
- **Forgiving Hitboxes & Pickups**: Herbert's hitbox softened to `36f` (from `45f`) and pickup radius enlarged to `55f`.
- **Life / Stumble Buffer**: First collision triggers a forgiving cartoon stumble recovery with 1.5s invulnerability and temporary slowdown rather than instant run-ending game over.
- **Generous Zoomies**: Zoomie meter charges faster (`25-35%` per pickup) and decays at half the previous rate.

## 4. Commands

### Run Unit Tests:
```bash
export JAVA_HOME=/home/draygen/.local/toolchains/jdk-21.0.12.1+1
export PATH=$JAVA_HOME/bin:$PATH
./gradlew test
```

### Build Debug APK:
```bash
./gradlew assembleDebug
```
APK Path: `app/build/outputs/apk/debug/app-debug.apk`

### Install & Launch on Real S24 Ultra:
```bash
adb -s 192.168.0.71:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.0.71:5555 shell am start -n com.draygen.herbertzoom/.MainActivity
```

## 5. Assets & Licenses
Documented in `docs/ASSETS.md`. All visual and audio rendering is 100% original programmatic / synthesized vector & PCM sound code under project license (MIT / Proprietary). No third-party copyright assets are loaded.

## 6. What Claude Should Do Next (Recommended Tasks)
1. **Herbert Visual Variations / Polish**:
   - Add open cardboard box dive animation state when landing inside open cardboard boxes.
2. **Audio / Haptic Settings**:
   - Add a subtle gear / audio toggle icon on Title screen to mute sound/vibration.
3. **Subsequent Environments (Post Milestone 1)**:
   - Level 2: *Kitchen Tile Drift* (low friction sliding mechanics).
   - Level 3: *Midnight Hallway Sprint*.

## 7. Decisions Not to Casually Reverse
- Keep `game-core` 100% decoupled from Android SDK so physics/rules can be tested instantaneously on JVM without emulator/device overhead.
- Keep the coordinate system virtualized (1920x1080) with dynamic aspect ratio fill to preserve crisp rendering on any phone.
- Maintain wholesome / no-harm policy for Herbert and Kitty (flops, loafs, and comical stumbles only).
