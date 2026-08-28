package com.draygen.herbertzoom.core

enum class AnimationState {
    IDLE,
    RUNNING,
    JUMPING,
    MAX_ZOOM_RUNNING,
    SKIDDING,
    STUMBLING,
    FLOPPED,
    BOX_DIVE,
    SCRATCHING
}

data class Herbert(
    var x: Float = GameConstants.HERBERT_HOME_X,
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
    var lives: Int = 1, // 1 life buffer for forgiving stumbles
    // "The Spot" - lapping around a worn patch of floorboard, scratching at it
    var scratchTimer: Float = 0f,
    var scratchAngle: Float = 0f,
    var scratchCenterX: Float = 0f,
    var scratchCenterY: Float = 0f
) {
    /** Collision immunity from any source (stumble recovery, or busy scratching). */
    val isInvulnerable: Boolean get() = invulnerabilityTimer > 0f || isScratching

    /** Only the post-stumble recovery blinks Herbert; scratching must not. */
    val isStumbleInvulnerable: Boolean get() = invulnerabilityTimer > 0f

    val isScratching: Boolean get() = scratchTimer > 0f

    /** 0..1 through the current scratch, for renderers and particle pacing. */
    val scratchProgress: Float
        get() = if (!isScratching) 0f
                else (1f - scratchTimer / GameConstants.SCRATCH_DURATION_SEC).coerceIn(0f, 1f)

    val jumpHeight: Float
        get() {
            if (!isJumping) return 0f
            // Parabolic arc: 4 * h * p * (1 - p)
            return 4f * GameConstants.JUMP_MAX_HEIGHT * jumpProgress * (1f - jumpProgress)
        }

    /**
     * Herbert spots the patch and commits. He abandons his lane, orbits the
     * patch and goes at it with both front paws.
     */
    fun beginScratch(centerX: Float, centerY: Float) {
        if (isScratching || animState == AnimationState.FLOPPED) return
        scratchTimer = GameConstants.SCRATCH_DURATION_SEC
        // Start the lap on the near side so he visibly circles round the patch
        scratchAngle = kotlin.math.PI.toFloat()
        scratchCenterX = centerX
        scratchCenterY = centerY
        isJumping = false
        jumpProgress = 0f
        animState = AnimationState.SCRATCHING
    }

    fun jump() {
        if (isScratching) return
        if (!isJumping && animState != AnimationState.FLOPPED) {
            isJumping = true
            jumpProgress = 0f
        }
    }

    fun steerTo(normalizedY: Float) {
        if (isScratching) return // he is busy; steering resumes when he's done
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

        // The Spot takes priority over everything except flopping: while Herbert
        // is scratching he orbits the patch instead of running his normal lane.
        if (isScratching) {
            scratchTimer -= dt
            scratchAngle += GameConstants.SCRATCH_ORBIT_SPEED * dt
            x = scratchCenterX + kotlin.math.cos(scratchAngle) * GameConstants.SCRATCH_ORBIT_RADIUS_X
            y = scratchCenterY + kotlin.math.sin(scratchAngle) * GameConstants.SCRATCH_ORBIT_RADIUS_Y
            targetY = y
            skidAngle = 0f
            animState = AnimationState.SCRATCHING
            // Delighted, slightly unhinged eyes while he works
            eyeWideness = if (maxZoomiesActive) 1.8f else 1.35f
            if (scratchTimer <= 0f) {
                scratchTimer = 0f
                x = GameConstants.HERBERT_HOME_X
                targetY = y
                animState = AnimationState.RUNNING
            }
            return
        }

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
        scratchTimer = 0f
    }

    fun reset() {
        x = GameConstants.HERBERT_HOME_X
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
        scratchTimer = 0f
        scratchAngle = 0f
        scratchCenterX = 0f
        scratchCenterY = 0f
    }
}
