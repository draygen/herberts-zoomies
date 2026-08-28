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
    SOCK_PILE
}

data class Pickup(
    val id: Long,
    var x: Float,
    var y: Float,
    val type: PickupType,
    val radius: Float = 35f,
    var collected: Boolean = false
)

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
    var nearMissTriggered: Boolean = false
) {
    val left: Float get() = x - width / 2f
    val right: Float get() = x + width / 2f
    val top: Float get() = y - height / 2f
    val bottom: Float get() = y + height / 2f
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
        comboTimer = 3.5f // 3.5 seconds to keep combo alive
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
