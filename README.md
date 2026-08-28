# Herbert's Zoomies 🐾⚡

*Herbert's Zoomies* is an arcade Android game starring **Herbert**, a chaotic kitten who suddenly gets the Zoomies and tears across the living room at absurd speed.

## Target Device & Environment
- **Primary Target**: Samsung Galaxy S24 Ultra (`SM-S928U` / Android 14+ / SDK 35 / Min SDK 29)
- **Orientation**: Landscape (immersive full-screen)
- **Engine / Architecture**:
  - `game-core`: Pure Kotlin modular game simulation (zero Android framework dependencies, fully unit tested via JUnit 5).
  - `app`: Native Android SurfaceView 60 FPS hardware canvas renderer, synthesized SoundPool audio engine, and subtle haptics.
- **Java / Toolchain**: OpenJDK 21 / JVM 17 (`/home/draygen/.local/toolchains/jdk-21.0.12.1+1`), Gradle 8.11.1.

## Build & Test Commands

### 1. Run Pure Game Core Unit Tests
```bash
export JAVA_HOME=/home/draygen/.local/toolchains/jdk-21.0.12.1+1
export PATH=$JAVA_HOME/bin:$PATH
./gradlew :game-core:test
```

### 2. Assemble Debug APK
```bash
./gradlew assembleDebug
```
APK output location:
`app/build/outputs/apk/debug/app-debug.apk`

### 3. Deploy and Launch on Galaxy S24 Ultra
```bash
adb -s 192.168.0.71:5555 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s 192.168.0.71:5555 shell am start -n com.draygen.herbertzoom/.MainActivity
```

## Gameplay Controls
- **Drag / Hold Finger (Vertical)**: Steer Herbert smoothly up and down across the room.
- **Swipe Up / Tap**: Jump / Pounce over low obstacles (slippers, cushions, socks).
- **Collectibles**: Pick up fish treats and toy mice to charge the Zoomie Meter.
- **Maximum Zoomies**: Fills meter to 100% to trigger wild speed boost, dilated blue eyes, speed trails, 3x score multiplier, and soft-obstacle scatter.
- **Fail State**: Wholesome crash flopping onto pink belly with dazed cute eyes. Tap "AGAIN!" to replay. High scores persist locally.
