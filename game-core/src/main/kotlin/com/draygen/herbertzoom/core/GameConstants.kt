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
}
