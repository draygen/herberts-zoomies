# Herbert's Zoomies — Handoff to Claude

## 1. Project Context & Environment
- **Project Root**: `/home/draygen/herbertzoom`
- **Current Branch**: `main`
- **JDK Location**: `/home/draygen/.local/toolchains/jdk-21.0.12.1+1` (Temurin OpenJDK 21.0.12.1)
- **Android SDK**: `/home/draygen/.local/toolchains/android-sdk` (compileSdk 35, minSdk 29, targetSdk 35)
- **Target Physical Hardware**: Samsung Galaxy S24 Ultra (`SM-S928U`, Android 16 / API 36, running at FHD+ 1080x2340 @ 450dpi, connected via ADB at `192.168.0.71:5555`) — *milestone verification only*
- **Day-to-day Dev Target**: `Herbert_S24U_Dev` Android emulator on the Windows host (API 35, 1440x3120 @ 560dpi, WHPX accelerated). Setup, AVD specs, commands and gotchas: **`docs/EMULATOR.md`**

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

### Build an *installable* Release APK
`./gradlew assembleRelease` produces **`app-release-unsigned.apk`** - there is no
`signingConfig` on the release build type, and an unsigned APK cannot be
installed. Sign it with the **debug keystore**, which matters for a reason
beyond convenience: it is the same certificate the debug build uses, so the
release APK installs *over* an existing debug install with `-r` and the saved
high score survives. Signing with a fresh key would force an uninstall and wipe
`herbert_zoomies_prefs`.

```bash
BT=/home/draygen/.local/toolchains/android-sdk/build-tools/35.0.0
OUT=app/build/outputs/apk/release
./gradlew assembleRelease
$BT/zipalign -p -f 4 $OUT/app-release-unsigned.apk $OUT/app-release-aligned.apk
$BT/apksigner sign \
  --ks ~/.android/debug.keystore --ks-pass pass:android --key-pass pass:android \
  --ks-key-alias androiddebugkey \
  --out $OUT/app-release-signed.apk $OUT/app-release-aligned.apk
```

Use the release build to play the **real** pacing: `BuildConfig.DEBUG` is false,
so Kitty arrives at the true 200 toys and both dev hotspots are unreachable.
Verified on device by tapping the summon hotspot repeatedly with no effect.
This is a locally-signed build for side-loading only - a real store release
would need its own keystore, which does not exist yet.

### Emulator Dev Loop (default):
```bash
./scripts/herbert-dev.sh boot     # boot Herbert_S24U_Dev and wait
./scripts/herbert-dev.sh run      # build + install + launch
./scripts/herbert-dev.sh cycle    # ... + screenshot
./scripts/herbert-dev.sh logcat   # this app's log
./scripts/herbert-dev.sh fps      # real SurfaceView frame rate
./scripts/herbert-dev.sh kill     # shut the emulator down
```
`herbert-dev.sh` always resolves to an `emulator-*` serial and passes an explicit
`-s`, so it cannot accidentally deploy to the real phone.

### Install & Launch on Real S24 Ultra (milestone verification only):

**The port is no longer 5555.** The phone is now on Android's *Wireless
debugging* (Developer options), which listens on a **random port** that changes
whenever wireless debugging is toggled - port 5555 is refused. Discover the
current one, which also auto-connects if the phone is already paired:

```bash
adb mdns services          # -> adb-R5CX14Q3PMY-ExDWEw  _adb-tls-connect._tcp  192.168.0.71:<port>
adb devices -l             # the phone shows up as model:SM_S928U
```

Then, with the serial that printed (e.g. `192.168.0.71:34233`):

```bash
HERBERT_ALLOW_PHYSICAL=1 ./scripts/herbert-dev.sh --device 192.168.0.71:<port> install
HERBERT_ALLOW_PHYSICAL=1 ./scripts/herbert-dev.sh --device 192.168.0.71:<port> launch
```

If `adb mdns services` lists nothing, wireless debugging is off on the phone -
turn it back on under Developer options. The pairing itself survives, so no
re-pairing is normally needed.

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

## 7. The Spot (added this pass)
Herbert's real habit of circling and scratching a discoloured patch of floor is
now a gameplay beat. `ScratchSpot` (game-core) spawns rarely; on contact Herbert
enters the new `SCRATCHING` animation state, orbits the patch for ~1.6s with his
front paws windmilling, and is collision-immune while the world scroll drops to
15%. Worth 500 pts + 45 zoomie energy. Tuning lives in `GameConstants`
(`SCRATCH_*`), visuals in `WorldRenderer.renderScratchSpots` and the
`SCRATCHING` branches of `HerbertRenderer`.

Fixed alongside it: `GameSurfaceView.resume()` was starting a second game-loop
thread (both `onResume()` and `surfaceCreated()` call it), which raced the
entity lists into a `ConcurrentModificationException`. `resume()` is now
idempotent.

**That bug had been silently doubling the game speed.** Two loops each advanced
the world every frame, so the sim ran at ~2x real time and every tuning value
behaved as if doubled. Measured from screen recordings: 675 world units/s with
the bug vs 338 after the fix. All prior difficulty tuning was authored by feel
against that 2x clock, so the speed constants were re-based to the real
one-thread numbers that reproduce the pace the game always actually played at
(BASE_SPEED 360 -> 720, MAX_NORMAL_SPEED 780 -> 1500, SPEED_ACCELERATION 9 ->
18, LATERAL_STEER_SPEED 1600 -> 3200, JUMP_DURATION_SEC 0.62 -> 0.34, spawn
intervals halved). Verified back at 720 units/s on the emulator.
The deliberately generous timers (6s Maximum Zoomies, 4s combo window, 1.5s
stumble invulnerability, slow zoomie decay) were left at their real values, so
they are now genuinely twice as forgiving as they used to play.

## 8. Emulator Environment (added this pass)
Android Studio `2026.1.3.7`, SDK Platform 35, Emulator `37.1.11` and the
`android-35 google_apis x86_64` system image are installed on the **Windows**
host; the Gradle build still uses the untouched WSL SDK/JDK shared with
`draygen/smstext`. Two AVDs exist: `Herbert_S24U_Dev` (1440x3120, 6 GB, main
target) and `Herbert_Midrange_Dev` (1080x2400, 3 GB, 2 cores, floor check).
Both hold a solid **60 FPS** with the game running. Full details, including the
landscape-window `spin` gotcha and real-vs-emulated differences, are in
`docs/EMULATOR.md`.

## 9. The Kitty Boss Encounter (completed this pass)

The simulation had already landed in `game-core` (commit `3b4cc7f`); this pass
built the entire **presentation layer**, which was the unfinished half. Before
it, the app module contained zero references to the boss - the fight ran
correctly in tests and was completely invisible on device.

Added:
- **`KittyBossRenderer`** (app): giant Kitty in Kitty's exact palette, with
  gaze-tracking pupils, progressively flattening ears, blown-pupil shock on a
  beam hit, the grooming lick, telegraph/strike bands, the slamming giant paw,
  tail sweep, stare reticle, yarn lane markers, energy orbs, Herbert's twin eye
  beams, and all three victory exits.
- **Boss HUD** (`GameHudRenderer.renderBossHud` / `renderBossCutscene`): the
  Grump Meter with per-beam tick marks, the remaining stumble buffer as paw
  pips, Blue Eye Energy pips, the ZAP button, the intro title card and the
  victory banner.
- **Boss audio** (`SoundEffects`): rumble, telegraph, slam, beam, Kitty's
  startled chirp, and the victory fanfare - all synthesized, as before.
- **`IntroBeat` + cutscene beat helpers** in `KittyBoss`'s companion, so the
  renderer and the audio layer read the same clock. 5 new tests; 53 pass.

### Reading the fight
The one rule the visuals owe the player: **an attack must be readable before it
can hurt.** Telegraph bands are hazard-yellow with marching stripes, the safe
corridor is tinted green, and a band only turns red once it is genuinely live.
The giant paw deliberately **slams onto the band and stays** rather than
sweeping across it - the sim makes the whole row dangerous for the whole strike,
so a travelling paw would lie about where the danger is.

### Two things worth knowing
- **The 19.5:9 safe area.** The 1920x1080 virtual space is drawn with *fill*
  scaling, so on both the S24 Ultra and the dev emulator only world y in roughly
  **97..983** is actually on screen. The first HUD pass put the ZAP button and
  the coaching line outside it. `GameHudRenderer.SAFE_TOP` / `SAFE_BOTTOM` record
  this; anything the player must see or touch has to live inside it. (The title
  screen's tutorial pill still sits slightly under the bottom edge - pre-existing.)
- **Input must not mutate the world.** Firing the beam from `onTouchEvent`
  crashed the renderer with `ConcurrentModificationException` on GameLoopThread:
  the callback spawned particles while the loop was iterating the particle list
  to draw it. Touch handlers now only set `pending*` intent flags, and
  `consumePendingInput()` applies them on the game thread. The two restart
  buttons were routed through the same queue, since `startNewRun()` clears the
  entity lists the render pass walks.

### Verified on the real S24 Ultra
Installed and played through on `SM-S928U` at 2340x1080 landscape. The boss
fight holds a **locked 60 FPS on device** - mean 16.91ms, median 16.65ms, and
p90 *and* p99 both 16.65ms, i.e. no dropped frames at all across the sample.
(The emulator is the pessimistic case here at a 59.8 FPS median with p90
21.3ms.) Every HUD element lands inside the safe area on the real panel, and
the existing high score survived the upgrade untouched.

### Dev shortcuts (debug builds only, all behind `BuildConfig.DEBUG`)
Reaching the fight legitimately needs 200 toys, which is impractical to iterate
on, so debug builds lower `bossToyThreshold` to 12 and add two tap hotspots
during play, both in the empty strip between the combo badge and the zoomie
meter:

| Hotspot (world coords)      | Effect                                  |
|-----------------------------|-----------------------------------------|
| x 1000-1200, y 100-180      | `debugForceBoss()` - summon Kitty now   |
| x 1230-1430, y 100-180      | `debugChargeEyes()` - fill the eye beam |

None of this exists in a release build.

## 10. Decisions Not to Casually Reverse
- Keep `game-core` 100% decoupled from Android SDK so physics/rules can be tested instantaneously on JVM without emulator/device overhead.
- Keep the coordinate system virtualized (1920x1080) with dynamic aspect ratio fill to preserve crisp rendering on any phone.
- Maintain wholesome / no-harm policy for Herbert and Kitty (flops, loafs, and comical stumbles only).
- Keep normal iteration on the emulator. The real S24 Ultra is reserved for agreed milestone verification, and `herbert-dev.sh` enforces this.
- Do not hardcode anything to the emulator's 1440x3120 panel; the virtual 1920x1080 space with aspect-fill scaling stays authoritative.
- Keep The Spot rare and always safe. It is a joke about Herbert, not a scoring staple or a hazard.
- Keep `GameSurfaceView.resume()` idempotent; two game-loop threads crash the renderer.
- Never mutate the world (or spawn particles) from `onTouchEvent`. Set a
  `pending*` flag and let `consumePendingInput()` apply it on the game thread.
- Keep the boss's danger bands honest: telegraph before danger, green corridor
  always visible, and no decoration drawn on top of a live band.
- Keep Kitty unhurt. The Grump Meter is patience, not health, and the encounter
  always hands the run back.
