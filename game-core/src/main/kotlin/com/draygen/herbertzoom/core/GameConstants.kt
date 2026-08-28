package com.draygen.herbertzoom.core

object GameConstants {
    // Virtual Coordinate Space
    const val WORLD_WIDTH = 1920f
    const val WORLD_HEIGHT = 1080f

    // Herbert Physical Specs
    const val HERBERT_WIDTH = 140f
    const val HERBERT_HEIGHT = 100f
    const val HERBERT_HITBOX_RADIUS = 45f

    // Movement speeds (world units per second)
    const val BASE_SPEED = 500f
    const val MAX_NORMAL_SPEED = 950f
    const val SPEED_ACCELERATION = 20f
    const val MAX_ZOOMIE_SPEED_BOOST = 1.45f
    const val LATERAL_STEER_SPEED = 1400f

    // Jump Physics
    const val JUMP_DURATION_SEC = 0.55f
    const val JUMP_MAX_HEIGHT = 130f

    // Zoomie Meter
    const val ZOOMIE_METER_MAX = 100f
    const val ZOOMIE_PER_TREAT = 20f
    const val ZOOMIE_PER_TOY = 25f
    const val ZOOMIE_PER_NEAR_MISS = 15f
    const val MAX_ZOOMIE_DURATION_SEC = 5.5f
    const val NORMAL_ZOOMIE_DECAY_RATE = 2.0f // per second if idle

    // Scoring
    const val POINTS_PER_TREAT = 100
    const val POINTS_PER_TOY = 200
    const val POINTS_PER_NEAR_MISS = 150
    const val POINTS_PER_METER_RUN = 10
}
