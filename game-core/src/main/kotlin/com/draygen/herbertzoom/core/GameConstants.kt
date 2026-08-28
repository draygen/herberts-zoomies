package com.draygen.herbertzoom.core

object GameConstants {
    // Virtual Coordinate Space
    const val WORLD_WIDTH = 1920f
    const val WORLD_HEIGHT = 1080f

    // Herbert Physical Specs - slightly larger for readability, generous forgiving hitbox
    const val HERBERT_WIDTH = 160f
    const val HERBERT_HEIGHT = 115f
    const val HERBERT_HITBOX_RADIUS = 36f // Softened hitbox for forgiving near misses

    // Kitty Physical Specs - compact, fat, round older cat
    const val KITTY_WIDTH = 150f
    const val KITTY_HEIGHT = 110f
    const val KITTY_HITBOX_RADIUS = 45f

    // Movement speeds (world units per second) - gentle entry & gradual ramp
    const val BASE_SPEED = 360f // reduced from 500f for comfortable reaction time
    const val MAX_NORMAL_SPEED = 780f // reduced from 950f
    const val SPEED_ACCELERATION = 9f // slow, smooth ramp
    const val MAX_ZOOMIE_SPEED_BOOST = 1.35f
    const val LATERAL_STEER_SPEED = 1600f // snappy steering response

    // Jump Physics - generous airborne hang time
    const val JUMP_DURATION_SEC = 0.62f
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

    // Scoring
    const val POINTS_PER_TREAT = 100
    const val POINTS_PER_TOY = 250
    const val POINTS_PER_NEAR_MISS = 150
    const val POINTS_PER_METER_RUN = 10
}
