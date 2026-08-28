# Herbert's Zoomies — Game Design Document (Milestone 1)

## 1. Overview & Fantasy
*Herbert's Zoomies* is an arcade runner/steering mobile game starring **Herbert**, a real chaotic kitten.
Herbert has suddenly caught the Zoomies and is tearing through the living room at breakneck speed.

Tone: Adorable, stupid, chaotic, cozy, funny, never violent.

## 2. Platform & Target Specs
- **Platform**: Android (Target SDK 35, Min SDK 29).
- **Target Device**: Samsung Galaxy S24 Ultra (1080x2340 / high-DPI AMOLED 120Hz/60Hz).
- **Orientation**: **Landscape** (gives maximum horizontal visibility down the room while steering Herbert).
- **Framerate Target**: Solid 60 FPS (scalable fixed-timestep loop on dedicated SurfaceView rendering thread).

## 3. Core Gameplay
1. **Movement**:
   - Herbert runs forward automatically at increasing speed.
   - Player drags / moves finger horizontally to steer Herbert left/right.
   - Player taps or flicks upward to Jump / Pounce over floor obstacles or onto cushions.
2. **Collectibles & Pickups**:
   - **Toy Mouse / Yarn Balls**: +Score, fills Zoomie Meter.
   - **Cat Treats / Kibble**: +Score, fills Zoomie Meter.
3. **Obstacles & Living Room Elements**:
   - **Cardboard Boxes & Cat Tunnels**: Enter to slide through with bonus speed/points or jump over.
   - **Slippers, Sock piles, Rug corners**: Low obstacles that can be jumped over or dodged.
   - **Couch Cushions & Stools**: Bounce off or leap onto.
   - **Table/Chair Legs**: Solid obstacles requiring swift steering.
4. **The Spot (Scratch Patch)**:
   - A small **discoloured, worn patch of floorboard** occasionally appears on the floor. Based on real Herbert, who cannot walk past his one.
   - Herbert breaks off his lane, **laps around the patch twice** and **rakes at it with his front paws in circles**, then rejoins the run.
   - The room slows to 15% for the ~1.6s so it reads as a deliberate detour, and Herbert is collision-immune throughout - the bit can never cost a run.
   - Reward: 500 points (combo / Max Zoomies multipliers apply) + 45 zoomie energy, enough to tip the meter into Maximum Zoomies.
   - Deliberately rare: never before 10s, one on screen at a time, 9s minimum gap. It is a treat, not a staple.
5. **The Zoomie Meter & Maximum Zoomies**:
   - Meter charges from pickups, near misses, and uninterrupted running.
   - At 100%, **MAXIMUM ZOOMIES** activates:
     - Herbert's eyes dilate huge with wild blue intensity.
     - Speed increases by 40% with animated speed trail lines.
     - Score multiplier (3x).
     - Herbert is invincible/blows through light obstacles (e.g. harmlessly scatters socks/slippers).
     - Lasts 6 seconds, then drains back to standard run.
6. **Fail State (Wholesome / Funny)**:
   - Hitting a hard obstacle (when not in Maximum Zoomies) ends the run safely.
   - Failure states include: Flopping onto pink belly with dazed cute eyes, diving into a box and loafing, abruptly stopping to lick paw.
   - No injury, blood, or distress.

## 4. Scoring & Persistence
- Score = (Distance Travelled) + (Toys/Treats * 100) + (Near Misses * 50) * Combo Multiplier.
- Local High Score saved via Android `SharedPreferences`. No network/cloud required.

## 5. Technology Architecture
- **Language**: Kotlin 2.0.21 on Java 21 / JVM 17.
- **Architecture**:
  - `game-core`: Pure Kotlin game engine logic (zero Android dependencies) containing `HerbertModel`, `WorldState`, `ObstacleManager`, `PickupManager`, `ZoomieMeter`, `CollisionSystem`, `ScoringEngine`. 100% unit-testable via standard JUnit 5.
  - `app`: Android native SurfaceView game loop with hardware-accelerated canvas, audio playback (SoundPool), haptics (Vibrator), and lifecycle-aware state machine.
