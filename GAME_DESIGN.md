# Herbert's Zoomies — Game Design Document (Milestone 1)

## 1. Overview & Fantasy
*Herbert's Zoomies* is an arcade runner/steering mobile game starring **Herbert**, a real chaotic kitten.
Herbert has suddenly caught the Zoomies and is tearing through the living room at breakneck speed.

Tone: Adorable, stupid, chaotic, cozy, funny, never violent.

## 2. Platform & Target Specs
- **Platform**: Android (Target SDK 35, Min SDK 29).
- **Target Device**: Samsung Galaxy S24 Ultra (1080x2340 / high-DPI AMOLED 120Hz/60Hz).
- **Orientation**: **Landscape** (gives maximum horizontal visibility down the room while steering Herbert).
- **Framerate Target**: Solid 60 FPS (scalable fixed-timestep loop on **one** dedicated SurfaceView rendering thread - see the speed note in `HANDOFF.md`; a duplicate loop thread silently ran the whole simulation at 2x for most of the project's life).
- **Speed Tuning**: the movement block at the top of `GameConstants` is the difficulty knob. Herbert starts at 720 world units/s and ramps to 1500 over ~45s.

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

7. **The Kitty Boss Encounter**:
   - At **200 toys in a single run**, Kitty has finally had enough. The encounter
     is a contained detour *layered over* the runner, never a replacement for it:
     `RunMode` walks `RUNNING -> BOSS_INTRO -> BOSS_FIGHT -> BOSS_VICTORY ->
     RUNNING`, all inside `GamePlayState.PLAYING`. Score, combo, high score and
     the zoomie meter carry straight through, and the trigger latches for the run
     so toys 201+ can never re-summon her.
   - **Kitty**: enormous, seated at the right of the arena, tracking Herbert with
     her eyes, ears flattening further the more fed up she gets. She is **never
     hurt** — the *Grump Meter* is her patience, and emptying it just means she
     gives up and leaves.
   - **Herbert's answer**: *Blue Zoomie Energy* orbs drift in; three of them
     charge one pair of ridiculous cyan eye beams, fired with the **ZAP!** button.
     Four landed beams empty her patience. A beam always interrupts whatever she
     was winding up, so firing is never punished.
   - **Her attacks**: giant paw swat, double swat (two paws around a guaranteed
     corridor), tail sweep (jump it or stay high), tracking stare, yarn barrage,
     and — after every third attack — she stops to **wash a paw**, which doubles
     the energy rate and hands the player a free charging window.
   - **Fairness is structural.** Patterns are *built around* a guaranteed safe
     corridor rather than rolled and checked, so an unfair pattern is not
     something the RNG can produce. Every attack opens on a ≥0.7s telegraph,
     danger exists only during the strike, and recovery never drops below 1.0s.
   - **Readability contract (renderer)**: telegraph bands are hazard-yellow with
     marching stripes and the safe corridor is tinted green; a band only turns
     red once it is genuinely live. The giant paw **slams onto the band and
     stays** rather than sweeping across it, because the whole row is dangerous
     for the whole strike and a travelling paw would misrepresent that.
   - **Failure keeps the cozy contract**: the fight grants a deeper stumble
     buffer (3 bonks), a hit costs a stumble plus the combo plus one charge of
     energy, and only repeated mistakes reach the normal wholesome flop.
   - **Victory**: blast → a comedic puff of loose fur → she is revealed
     completely unharmed and deeply unimpressed, then leaves in one of three
     ways (waddles off, becomes a loaf, or grooms and ignores you). Worth
     **20,000 points**, and the run resumes straight into Maximum Zoomies.

## 4. Scoring & Persistence
- Score = (Distance Travelled) + (Toys/Treats * 100) + (Near Misses * 50) * Combo Multiplier.
- Local High Score saved via Android `SharedPreferences`. No network/cloud required.

## 5. Technology Architecture
- **Language**: Kotlin 2.0.21 on Java 21 / JVM 17.
- **Architecture**:
  - `game-core`: Pure Kotlin game engine logic (zero Android dependencies) containing `HerbertModel`, `WorldState`, `ObstacleManager`, `PickupManager`, `ZoomieMeter`, `CollisionSystem`, `ScoringEngine`. 100% unit-testable via standard JUnit 5.
  - `app`: Android native SurfaceView game loop with hardware-accelerated canvas, audio playback (SoundPool), haptics (Vibrator), and lifecycle-aware state machine.
