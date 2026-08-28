# Herbert's Zoomies — Handoff to Claude

## 1. Project Context & Environment
- **Project Root**: `/home/draygen/herbertzoom`
- **Current Branch**: `main`
- **JDK Location**: `/home/draygen/.local/toolchains/jdk-21.0.12.1+1` (Temurin OpenJDK 21.0.12.1)
- **Android SDK**: `/home/draygen/.local/toolchains/android-sdk` (compileSdk 35, minSdk 29, targetSdk 35)
- **Target Physical Hardware**: Samsung Galaxy S24 Ultra (`SM-S928U`, 1080x2340 AMOLED, connected via ADB at `192.168.0.71:5555`)

## 2. Technology & Architecture Choices
- **Engine**: Pure Kotlin Native SurfaceView Game Engine with dual-module separation:
  - `game-core`: Clean, deterministic game simulation engine with zero Android runtime dependencies. Encapsulates `Herbert`, `LivingRoomWorld`, `Obstacle` & `Pickup` management, `ZoomieMeter`, `ScoreRecord`, collision detection, jump parabolic physics, and combo multipliers. Fully unit tested via JUnit 5.
  - `app`: Android SurfaceView fixed-timestep render loop (~60 FPS target with fullscreen fill scaling on Samsung's ~19.5:9 aspect ratio), custom high-performance 2D Canvas vector rendering (`HerbertRenderer`, `WorldRenderer`, `GameHudRenderer`), dynamic synthesized sound effects (`SoundEffects`), particle juice system (`ParticleSystem`), and lifecycle management (`MainActivity`).
- **Why this stack**: Zero bloated third-party engines, instantaneous compile times (sub-second incremental builds), predictable memory allocations without GC stalls, zero network permissions, and native compatibility on Galaxy S24 Ultra.

## 3. Commands

### Run Unit Tests:
```bash
export JAVA_HOME=/home/draygen/.local/toolchains/jdk-21.0.12.1+1
export PATH=$JAVA_HOME/bin:$PATH
./gradlew test
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
- [x] **Cozy Layered Living Room Art Pass**:
  - Warm parquet hardwood floor with scrolling plank joints.
  - Pastel wallpaper wall with vertical stripes, picture frames (cat portrait), and windows with soft outdoor lighting.
  - Parallax teal living room area rugs with decorative borders and tassels.
- [x] **Expressive Herbert Mascot**:
  - Striking oversized blue eyes with dark blue iris shading, gleams, and dilated pupils during Zoomies.
  - Distinct black ear tips, black forehead spot, and black back/tail markings matching real Herbert.
  - Exposed pink belly during jumps, skids, and flopping.
  - Rosy blush cheeks and cute :3 mouth.
  - Dynamic animated tail whip with black tip.
  - Dynamic scaling floor drop shadow that responds to jump height.
- [x] **Living Room Props & Obstacles**:
  - Slippers (with fluffy inner lining).
  - Amazon delivery cardboard boxes with open flaps and doodle markings.
  - Sofa cushions (teal with button tufts).
  - Sisal scratching posts with rope grooves and heavy wooden base.
  - Turned mahogany table legs.
  - Crinkle cat tunnels with fabric hoops.
  - Sock piles with colorful stripes.
  - Couch corner armrests with cushions.
- [x] **Popping Pickups**:
  - Golden fish treats with glint highlights and outer glow.
  - Plush mouse toys with pink ears and spring tails.
  - Vibrant magenta yarn balls with woven thread loops and trailing string.
- [x] **Juice & Game Feel**:
  - Running paw dust puffs and skidding clouds.
  - Star sparkles on treat/toy collection.
  - Floating score multipliers (`+100`, `+300`, `CLOSE!`).
  - Screen shake on crashes and Maximum Zoomies activation.
  - Floating hearts during flop / max zoomies.
- [x] **Polished HUD & Fullscreen Viewport**:
  - Rounded semi-translucent status chips positioned cleanly within safe insets.
  - Responsive fill-scaling eliminating black letterboxing on ultra-wide screens.
  - Animated combo multiplier badge (`🔥 2x COMBO!`).
  - Styled capsule Zoomie Energy meter.
- [x] **Real S24 Ultra Hardware Verification (`SM-S928U`)**:
  - Verified 60 FPS silky smooth performance.
  - Touch drag steering and swipe jumping confirmed responsive.
  - Verified pause/resume backgrounding without crashing or memory leaks.

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
- Maintain wholesome / no-harm policy for Herbert (flops and loaves only).
