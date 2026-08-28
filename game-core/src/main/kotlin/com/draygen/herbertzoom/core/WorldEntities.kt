package com.draygen.herbertzoom.core

enum class PickupType {
    TREAT,
    TOY_MOUSE,
    YARN_BALL
}

enum class ObstacleType {
    SLIPPER,
    CARDBOARD_BOX,
    COUCH_CUSHION,
    TABLE_LEG,
    SOCK_PILE,
    CAT_TUNNEL,
    SCRATCHING_POST,
    COUCH_SECTION,
    KITTY_LOAF,
    KITTY_SLEEPING,
    KITTY_WADDLE
}

data class Pickup(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: PickupType,
    val radius: Float = 55f, // Large forgiving radius
    var collected: Boolean = false
)

/**
 * A small discoloured, worn patch of floorboard. It is scenery, not a hazard:
 * Herbert simply cannot walk past one without stopping to lap around it and
 * scratch at it with both front paws.
 */
data class ScratchSpot(
    val id: Long,
    var x: Float,
    var y: Float,
    val radiusX: Float = GameConstants.SCRATCH_SPOT_RADIUS_X,
    val radiusY: Float = GameConstants.SCRATCH_SPOT_RADIUS_Y,
    var used: Boolean = false,
    /** 0..1 while Herbert works on it - drives how scuffed the patch looks. */
    var scratchProgress: Float = 0f
) {
    /** Generous elliptical trigger area; Herbert is easily tempted. */
    fun contains(px: Float, py: Float, pad: Float = 0f): Boolean {
        val nx = (px - x) / (radiusX + pad)
        val ny = (py - y) / (radiusY + pad)
        return nx * nx + ny * ny <= 1f
    }
}

data class Obstacle(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: ObstacleType,
    val width: Float,
    val height: Float,
    val canJumpOver: Boolean,
    val isSoft: Boolean = false, // soft obstacles scatter/bounce when hit in max zoomies
    var hit: Boolean = false,
    var nearMissTriggered: Boolean = false,
    var animTimer: Float = 0f,
    var vy: Float = 0f, // vertical movement for waddling Kitty
    var isKittySwatting: Boolean = false,
    var kittySwatTimer: Float = 0f
) {
    val left: Float get() = x - width / 2f
    val right: Float get() = x + width / 2f
    val top: Float get() = y - height / 2f
    val bottom: Float get() = y + height / 2f
    val isKitty: Boolean get() = type == ObstacleType.KITTY_LOAF || type == ObstacleType.KITTY_SLEEPING || type == ObstacleType.KITTY_WADDLE

    fun updateKitty(dt: Float, herbertX: Float, herbertY: Float) {
        animTimer += dt
        if (type == ObstacleType.KITTY_WADDLE) {
            y += vy * dt
            if (y < 0.22f * GameConstants.WORLD_HEIGHT) {
                y = 0.22f * GameConstants.WORLD_HEIGHT
                vy = -vy
            } else if (y > 0.88f * GameConstants.WORLD_HEIGHT) {
                y = 0.88f * GameConstants.WORLD_HEIGHT
                vy = -vy
            }
        }

        // Kitty warns/swats when Herbert zooms nearby
        val distSq = (herbertX - x) * (herbertX - x) + (herbertY - y) * (herbertY - y)
        if (distSq < 220f * 220f && !isKittySwatting) {
            isKittySwatting = true
            kittySwatTimer = 0.6f
        }

        if (kittySwatTimer > 0f) {
            kittySwatTimer -= dt
            if (kittySwatTimer <= 0f) {
                isKittySwatting = false
            }
        }
    }
}

data class ScoreRecord(
    var currentScore: Long = 0L,
    var highScore: Long = 0L,
    var treatsCollected: Int = 0,
    var toysCollected: Int = 0,
    var nearMissCount: Int = 0,
    var comboMultiplier: Int = 1,
    var comboTimer: Float = 0f,
    var distanceRun: Float = 0f
) {
    fun addPoints(points: Long, isMaxZoomies: Boolean) {
        val mult = comboMultiplier * (if (isMaxZoomies) 3 else 1)
        currentScore += points * mult
        if (currentScore > highScore) {
            highScore = currentScore
        }
    }

    fun incrementCombo() {
        if (comboMultiplier < 5) {
            comboMultiplier++
        }
        comboTimer = 4.0f // generous 4 seconds
    }

    fun update(dt: Float) {
        if (comboTimer > 0f) {
            comboTimer -= dt
            if (comboTimer <= 0f) {
                comboMultiplier = 1
            }
        }
    }

    fun reset() {
        currentScore = 0L
        treatsCollected = 0
        toysCollected = 0
        nearMissCount = 0
        comboMultiplier = 1
        comboTimer = 0f
        distanceRun = 0f
    }
}

class ZoomieMeter {
    var value: Float = 0f
        private set
    var isMaxZoomies: Boolean = false
        private set
    var maxZoomieTimeRemaining: Float = 0f
        private set

    fun addEnergy(amount: Float): Boolean {
        if (isMaxZoomies) return false
        value = (value + amount).coerceIn(0f, GameConstants.ZOOMIE_METER_MAX)
        if (value >= GameConstants.ZOOMIE_METER_MAX) {
            activateMaxZoomies()
            return true
        }
        return false
    }

    private fun activateMaxZoomies() {
        isMaxZoomies = true
        maxZoomieTimeRemaining = GameConstants.MAX_ZOOMIE_DURATION_SEC
        value = GameConstants.ZOOMIE_METER_MAX
    }

    fun update(dt: Float) {
        if (isMaxZoomies) {
            maxZoomieTimeRemaining -= dt
            value = (maxZoomieTimeRemaining / GameConstants.MAX_ZOOMIE_DURATION_SEC) * GameConstants.ZOOMIE_METER_MAX
            if (maxZoomieTimeRemaining <= 0f) {
                isMaxZoomies = false
                value = 0f
                maxZoomieTimeRemaining = 0f
            }
        } else {
            if (value > 0f) {
                value = (value - GameConstants.NORMAL_ZOOMIE_DECAY_RATE * dt).coerceAtLeast(0f)
            }
        }
    }

    fun reset() {
        value = 0f
        isMaxZoomies = false
        maxZoomieTimeRemaining = 0f
    }
}
