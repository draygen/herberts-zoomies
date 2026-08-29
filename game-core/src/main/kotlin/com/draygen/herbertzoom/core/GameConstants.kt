package com.draygen.herbertzoom.core

object GameConstants {
    // Virtual Coordinate Space
    const val WORLD_WIDTH = 1920f
    const val WORLD_HEIGHT = 1080f

    // Herbert Physical Specs - slightly larger for readability, generous forgiving hitbox
    const val HERBERT_HOME_X = 300f // Herbert's anchored running lane on screen
    const val HERBERT_WIDTH = 160f
    const val HERBERT_HEIGHT = 115f
    const val HERBERT_HITBOX_RADIUS = 36f // Softened hitbox for forgiving near misses

    // Kitty Physical Specs - compact, fat, round older cat
    const val KITTY_WIDTH = 150f
    const val KITTY_HEIGHT = 110f
    const val KITTY_HITBOX_RADIUS = 45f

    // Movement speeds (world units per second) - gentle entry & gradual ramp
    // NOTE: until the double game-loop-thread bug was fixed, the world was
    // advanced twice per frame, so every value here behaved as if doubled. These
    // are the real one-thread numbers that reproduce the pace the game has
    // always actually played at. This block is the difficulty knob.
    const val BASE_SPEED = 720f
    const val MAX_NORMAL_SPEED = 1500f
    const val SPEED_ACCELERATION = 18f // reaches top speed in ~45s
    const val MAX_ZOOMIE_SPEED_BOOST = 1.35f
    const val LATERAL_STEER_SPEED = 3200f // snappy steering response

    // Jump Physics - generous airborne hang time
    const val JUMP_DURATION_SEC = 0.34f
    const val JUMP_MAX_HEIGHT = 150f

    // Zoomie Meter - fills quickly to reward player
    const val ZOOMIE_METER_MAX = 100f
    const val ZOOMIE_PER_TREAT = 25f
    const val ZOOMIE_PER_TOY = 35f
    const val ZOOMIE_PER_NEAR_MISS = 20f
    const val MAX_ZOOMIE_DURATION_SEC = 6.0f
    const val NORMAL_ZOOMIE_DECAY_RATE = 1.0f // very slow decay

    // Stumble / Invulnerability recovery
    const val STUMBLE_INVULNERABILITY_SEC = 1.5f

    // The Spot - a worn, discoloured patch of floorboard that Herbert cannot resist.
    // He breaks off his run, laps around it and scratches at it in circles.
    const val SCRATCH_DURATION_SEC = 1.15f
    const val SCRATCH_ORBIT_TURNS = 2f // laps around the patch during one scratch
    const val SCRATCH_ORBIT_RADIUS_X = 108f
    const val SCRATCH_ORBIT_RADIUS_Y = 54f
    const val SCRATCH_WORLD_SLOWDOWN = 0.2f // the room nearly stops while he's busy
    const val SCRATCH_SPOT_RADIUS_X = 96f
    const val SCRATCH_SPOT_RADIUS_Y = 48f
    const val SCRATCH_SPOT_MIN_INTERVAL_SEC = 9f // don't let spots crowd each other
    // radians/sec needed to complete SCRATCH_ORBIT_TURNS laps within SCRATCH_DURATION_SEC
    val SCRATCH_ORBIT_SPEED = (2f * kotlin.math.PI.toFloat()) * SCRATCH_ORBIT_TURNS / SCRATCH_DURATION_SEC

    // Scoring
    const val POINTS_PER_TREAT = 100
    const val POINTS_PER_TOY = 250
    const val POINTS_PER_NEAR_MISS = 150
    const val POINTS_PER_METER_RUN = 10
    const val POINTS_PER_SCRATCH_SPOT = 500
    const val ZOOMIE_PER_SCRATCH_SPOT = 45f

    // ===================================================================
    // KITTY BOSS ENCOUNTER
    // A contained, ridiculous set-piece that interrupts the run at 200 toys
    // and hands it straight back. Every number here is tuned to stay inside
    // the game's cozy contract: telegraphed, forgiving, never punishing.
    // ===================================================================

    /** Toys collected in a single run that summon the encounter. Once per run. */
    const val BOSS_TRIGGER_TOYS = 200

    // Arena: the vertical band Herbert may dodge within during the fight.
    // Matches Herbert's normal steering clamp so the fight feels like the run.
    const val BOSS_ARENA_TOP = 0.18f * WORLD_HEIGHT
    const val BOSS_ARENA_BOTTOM = 0.88f * WORLD_HEIGHT
    val BOSS_ARENA_HEIGHT = BOSS_ARENA_BOTTOM - BOSS_ARENA_TOP

    /** Paw / tail / stare patterns always leave a corridor at least this tall. */
    const val BOSS_MIN_SAFE_GAP = 230f

    /**
     * Yarn lanes are narrower bands than a paw slam, so they get their own (still
     * generous) corridor guarantee: over twice Herbert's 72-unit hitbox width,
     * and yarn can be jumped as well as dodged.
     */
    const val BOSS_MIN_YARN_CORRIDOR = 150f

    // Intro beats (seconds). Total reveal is deliberately unhurried.
    const val BOSS_INTRO_PAUSE_SEC = 0.7f      // the world holds its breath
    const val BOSS_INTRO_RUMBLE_SEC = 1.5f     // rumble + Kitty rises into frame
    const val BOSS_INTRO_TITLE_SEC = 1.9f      // "KITTY HAS HAD ENOUGH"
    val BOSS_INTRO_DURATION = BOSS_INTRO_PAUSE_SEC + BOSS_INTRO_RUMBLE_SEC + BOSS_INTRO_TITLE_SEC

    // World scroll scale while the encounter owns the screen.
    const val BOSS_INTRO_SCROLL = 0.22f
    const val BOSS_FIGHT_SCROLL = 0.30f

    // Grump Meter - Kitty's patience, NOT health. Empty = she is over it.
    const val BOSS_GRUMP_MAX = 100f
    const val BOSS_GRUMP_PER_BEAM = 30f        // -> 4 eye beams to win
    const val BOSS_PHASE2_GRUMP = 70f          // at/below this she is Very Annoyed
    const val BOSS_PHASE3_GRUMP = 35f          // at/below this she has Had Enough

    // Blue Eye Energy: 3 orbs charge one beam.
    const val BOSS_ENERGY_PER_BEAM = 3
    const val BOSS_ENERGY_ORB_RADIUS = 52f     // generous, like every pickup
    const val BOSS_ENERGY_ORB_SPEED = 420f
    const val BOSS_MAX_ENERGY_ORBS = 4         // bounded for frame budget

    // Eye beam firing
    const val BOSS_BEAM_DURATION = 0.85f
    const val BOSS_BEAM_HALF_HEIGHT = 44f

    // Yarn barrage
    const val BOSS_YARN_RADIUS = 46f
    const val BOSS_MAX_YARN = 5                // hard cap on live projectiles

    // Boss failure buffer: a boss hit must never end a run outright.
    const val BOSS_STUMBLE_BUFFER = 2          // 3 boss hits before the cute flop

    // Victory sequence beats (seconds)
    const val BOSS_VICTORY_BLAST_SEC = 1.2f    // she takes the full beam
    const val BOSS_VICTORY_SMOKE_SEC = 1.0f    // comedic pause behind the fluff
    const val BOSS_VICTORY_REVEAL_SEC = 2.2f   // unharmed, deeply unimpressed
    val BOSS_VICTORY_DURATION = BOSS_VICTORY_BLAST_SEC + BOSS_VICTORY_SMOKE_SEC + BOSS_VICTORY_REVEAL_SEC

    const val BOSS_VICTORY_BONUS = 20_000L
}
