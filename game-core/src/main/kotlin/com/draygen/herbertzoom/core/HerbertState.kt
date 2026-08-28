package com.draygen.herbertzoom.core

enum class AnimationState {
    IDLE,
    RUNNING,
    JUMPING,
    MAX_ZOOM_RUNNING,
    SKIDDING,
    STUMBLING,
    FLOPPED,
    BOX_DIVE
}

data class Herbert(
    var x: Float = 300f,
    var y: Float = GameConstants.WORLD_HEIGHT / 2f,
    var targetY: Float = GameConstants.WORLD_HEIGHT / 2f,
    var jumpProgress: Float = 0f, // 0..1
    var isJumping: Boolean = false,
    var animState: AnimationState = AnimationState.IDLE,
    var currentSpeed: Float = GameConstants.BASE_SPEED,
    var isMaxZoomies: Boolean = false,
    var animTimer: Float = 0f,
    var eyeWideness: Float = 1.0f,
    var skidAngle: Float = 0f,
    var invulnerabilityTimer: Float = 0f,
    var lives: Int = 1 // 1 life buffer for forgiving stumbles
) {
    val isInvulnerable: Boolean get() = invulnerabilityTimer > 0f

    val jumpHeight: Float
        get() {
            if (!isJumping) return 0f
            // Parabolic arc: 4 * h * p * (1 - p)
            return 4f * GameConstants.JUMP_MAX_HEIGHT * jumpProgress * (1f - jumpProgress)
        }

    fun jump() {
        if (!isJumping && animState != AnimationState.FLOPPED) {
            isJumping = true
            jumpProgress = 0f
        }
    }

    fun steerTo(normalizedY: Float) {
        val clamped = normalizedY.coerceIn(0.18f, 0.88f)
        targetY = clamped * GameConstants.WORLD_HEIGHT
    }

    fun stumble() {
        invulnerabilityTimer = GameConstants.STUMBLE_INVULNERABILITY_SEC
        animState = AnimationState.STUMBLING
        // Brief slowdown
        currentSpeed = (currentSpeed * 0.7f).coerceAtLeast(GameConstants.BASE_SPEED)
    }

    fun update(dt: Float, maxZoomiesActive: Boolean) {
        if (animState == AnimationState.FLOPPED) return

        animTimer += dt
        isMaxZoomies = maxZoomiesActive

        if (invulnerabilityTimer > 0f) {
            invulnerabilityTimer -= dt
            if (invulnerabilityTimer <= 0f && animState == AnimationState.STUMBLING) {
                animState = AnimationState.RUNNING
            }
        }

        // Lateral steering smoothing (snappy & forgiving)
        val dy = targetY - y
        val moveStep = GameConstants.LATERAL_STEER_SPEED * dt
        if (kotlin.math.abs(dy) <= moveStep) {
            y = targetY
            skidAngle = 0f
        } else {
            val sign = if (dy > 0) 1f else -1f
            y += sign * moveStep
            skidAngle = sign * 14f // playful kitten tilt
        }

        // Jump progression
        if (isJumping) {
            jumpProgress += dt / GameConstants.JUMP_DURATION_SEC
            if (jumpProgress >= 1f) {
                isJumping = false
                jumpProgress = 0f
            }
        }

        // Speed ramp (gentle acceleration)
        if (currentSpeed < GameConstants.MAX_NORMAL_SPEED) {
            currentSpeed += GameConstants.SPEED_ACCELERATION * dt
        }

        // Update animation state
        if (animState != AnimationState.STUMBLING) {
            animState = when {
                isJumping -> AnimationState.JUMPING
                isMaxZoomies -> AnimationState.MAX_ZOOM_RUNNING
                kotlin.math.abs(dy) > 120f -> AnimationState.SKIDDING
                else -> AnimationState.RUNNING
            }
        }

        eyeWideness = if (isMaxZoomies) 1.8f else 1.0f
    }

    fun flop() {
        animState = AnimationState.FLOPPED
        isJumping = false
    }

    fun reset() {
        x = 300f
        y = GameConstants.WORLD_HEIGHT / 2f
        targetY = y
        jumpProgress = 0f
        isJumping = false
        animState = AnimationState.IDLE
        currentSpeed = GameConstants.BASE_SPEED
        isMaxZoomies = false
        animTimer = 0f
        eyeWideness = 1.0f
        skidAngle = 0f
        invulnerabilityTimer = 0f
        lives = 1
    }
}
