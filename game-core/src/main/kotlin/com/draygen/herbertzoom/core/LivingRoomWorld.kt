package com.draygen.herbertzoom.core

import java.util.Random

enum class GamePlayState {
    TITLE,
    PLAYING,
    GAME_OVER
}

data class NearMissEvent(val obstacle: Obstacle)
data class PickupEvent(val pickup: Pickup)
data class FlopEvent(val obstacle: Obstacle)
data class MaxZoomiesActivatedEvent(val duration: Float)

class LivingRoomWorld(
    private val rng: Random = Random()
) {
    val herbert = Herbert()
    val zoomieMeter = ZoomieMeter()
    val score = ScoreRecord()
    var state: GamePlayState = GamePlayState.TITLE

    val obstacles = mutableListOf<Obstacle>()
    val pickups = mutableListOf<Pickup>()

    private var nextEntityId = 1L
    private var spawnTimer = 0f
    private var spawnInterval = 1.2f
    private var distanceMeterAccumulator = 0f

    // Callbacks for sound / haptics
    var onPickupCollected: ((PickupEvent) -> Unit)? = null
    var onNearMiss: ((NearMissEvent) -> Unit)? = null
    var onMaxZoomiesStart: ((MaxZoomiesActivatedEvent) -> Unit)? = null
    var onFlop: ((FlopEvent) -> Unit)? = null

    fun startNewRun() {
        herbert.reset()
        zoomieMeter.reset()
        score.reset()
        obstacles.clear()
        pickups.clear()
        spawnTimer = 0f
        spawnInterval = 1.2f
        distanceMeterAccumulator = 0f
        state = GamePlayState.PLAYING
    }

    fun update(dt: Float) {
        if (state != GamePlayState.PLAYING) return

        score.update(dt)
        val wasMax = zoomieMeter.isMaxZoomies
        zoomieMeter.update(dt)
        if (!wasMax && zoomieMeter.isMaxZoomies) {
            onMaxZoomiesStart?.invoke(MaxZoomiesActivatedEvent(GameConstants.MAX_ZOOMIE_DURATION_SEC))
        }

        herbert.update(dt, zoomieMeter.isMaxZoomies)

        val effectiveSpeed = if (zoomieMeter.isMaxZoomies) {
            herbert.currentSpeed * GameConstants.MAX_ZOOMIE_SPEED_BOOST
        } else {
            herbert.currentSpeed
        }

        // Advance world distance
        val stepDistance = effectiveSpeed * dt
        score.distanceRun += stepDistance
        distanceMeterAccumulator += stepDistance
        if (distanceMeterAccumulator >= 100f) {
            score.addPoints(GameConstants.POINTS_PER_METER_RUN.toLong(), zoomieMeter.isMaxZoomies)
            distanceMeterAccumulator -= 100f
        }

        // Move active obstacles & pickups leftward relative to Herbert
        val iterObs = obstacles.iterator()
        while (iterObs.hasNext()) {
            val obs = iterObs.next()
            obs.x -= stepDistance
            if (obs.x + obs.width < -100f) {
                iterObs.remove()
            }
        }

        val iterPick = pickups.iterator()
        while (iterPick.hasNext()) {
            val pick = iterPick.next()
            pick.x -= stepDistance
            if (pick.x + pick.radius < -100f) {
                iterPick.remove()
            }
        }

        // Spawning
        spawnTimer += dt
        val currentSpawnRate = if (zoomieMeter.isMaxZoomies) 0.6f else (spawnInterval - (herbert.currentSpeed / 4000f)).coerceAtLeast(0.55f)
        if (spawnTimer >= currentSpawnRate) {
            spawnTimer = 0f
            spawnWave()
        }

        // Check Collisions
        checkCollisions()
    }

    private fun spawnWave() {
        val spawnX = GameConstants.WORLD_WIDTH + 150f
        val laneY = (0.2f + rng.nextFloat() * 0.65f) * GameConstants.WORLD_HEIGHT

        // 60% chance to spawn obstacle, 70% chance to spawn pickup (often alongside)
        if (rng.nextFloat() < 0.7f) {
            val type = when (rng.nextInt(5)) {
                0 -> ObstacleType.SLIPPER
                1 -> ObstacleType.SOCK_PILE
                2 -> ObstacleType.CARDBOARD_BOX
                3 -> ObstacleType.COUCH_CUSHION
                else -> ObstacleType.TABLE_LEG
            }
            val (w, h, canJump, isSoft) = when (type) {
                ObstacleType.SLIPPER -> Quad(90f, 60f, true, true)
                ObstacleType.SOCK_PILE -> Quad(80f, 50f, true, true)
                ObstacleType.CARDBOARD_BOX -> Quad(120f, 100f, false, false)
                ObstacleType.COUCH_CUSHION -> Quad(150f, 80f, true, true)
                ObstacleType.TABLE_LEG -> Quad(60f, 180f, false, false)
            }
            obstacles.add(
                Obstacle(
                    id = nextEntityId++,
                    x = spawnX,
                    y = laneY,
                    type = type,
                    width = w,
                    height = h,
                    canJumpOver = canJump,
                    isSoft = isSoft
                )
            )
        }

        // Spawn pickup (offset or in open space)
        if (rng.nextFloat() < 0.75f) {
            val pickupType = when (rng.nextInt(3)) {
                0 -> PickupType.TREAT
                1 -> PickupType.TOY_MOUSE
                else -> PickupType.YARN_BALL
            }
            val pickupY = (0.2f + rng.nextFloat() * 0.65f) * GameConstants.WORLD_HEIGHT
            pickups.add(
                Pickup(
                    id = nextEntityId++,
                    x = spawnX + (rng.nextFloat() * 100f),
                    y = pickupY,
                    type = pickupType
                )
            )
        }
    }

    private data class Quad(val w: Float, val h: Float, val jump: Boolean, val soft: Boolean)

    private fun checkCollisions() {
        val hx = herbert.x
        val hy = herbert.y
        val hRadius = GameConstants.HERBERT_HITBOX_RADIUS
        val isJumping = herbert.isJumping
        val jumpH = herbert.jumpHeight

        // Pickups
        val pickIter = pickups.iterator()
        while (pickIter.hasNext()) {
            val pick = pickIter.next()
            if (pick.collected) continue
            val dx = hx - pick.x
            val dy = hy - pick.y
            val distSq = dx * dx + dy * dy
            val touchDist = hRadius + pick.radius
            if (distSq <= touchDist * touchDist) {
                pick.collected = true
                val (pts, zoomieGain) = when (pick.type) {
                    PickupType.TREAT -> Pair(GameConstants.POINTS_PER_TREAT, GameConstants.ZOOMIE_PER_TREAT)
                    PickupType.TOY_MOUSE -> Pair(GameConstants.POINTS_PER_TOY, GameConstants.ZOOMIE_PER_TOY)
                    PickupType.YARN_BALL -> Pair(GameConstants.POINTS_PER_TOY, GameConstants.ZOOMIE_PER_TOY)
                }
                score.addPoints(pts.toLong(), zoomieMeter.isMaxZoomies)
                if (pick.type == PickupType.TREAT) score.treatsCollected++ else score.toysCollected++
                score.incrementCombo()

                val activated = zoomieMeter.addEnergy(zoomieGain)
                if (activated) {
                    onMaxZoomiesStart?.invoke(MaxZoomiesActivatedEvent(GameConstants.MAX_ZOOMIE_DURATION_SEC))
                }
                onPickupCollected?.invoke(PickupEvent(pick))
                pickIter.remove()
            }
        }

        // Obstacles
        for (obs in obstacles) {
            if (obs.hit) continue

            val dx = kotlin.math.abs(hx - obs.x)
            val dy = kotlin.math.abs(hy - obs.y)

            val collidesX = dx < (obs.width / 2f + hRadius * 0.7f)
            val collidesY = dy < (obs.height / 2f + hRadius * 0.7f)

            // Near miss check (passed close without hitting)
            if (!obs.nearMissTriggered && !collidesX && hx > obs.right && hx < obs.right + 60f && dy < (obs.height / 2f + 90f)) {
                obs.nearMissTriggered = true
                score.nearMissCount++
                score.addPoints(GameConstants.POINTS_PER_NEAR_MISS.toLong(), zoomieMeter.isMaxZoomies)
                score.incrementCombo()
                val activated = zoomieMeter.addEnergy(GameConstants.ZOOMIE_PER_NEAR_MISS)
                if (activated) {
                    onMaxZoomiesStart?.invoke(MaxZoomiesActivatedEvent(GameConstants.MAX_ZOOMIE_DURATION_SEC))
                }
                onNearMiss?.invoke(NearMissEvent(obs))
            }

            if (collidesX && collidesY) {
                // If Herbert is high enough in a jump and obstacle is jumpable
                if (obs.canJumpOver && isJumping && jumpH > 40f) {
                    // Safe jump!
                    if (!obs.nearMissTriggered) {
                        obs.nearMissTriggered = true
                        score.addPoints(GameConstants.POINTS_PER_NEAR_MISS.toLong(), zoomieMeter.isMaxZoomies)
                        score.incrementCombo()
                        onNearMiss?.invoke(NearMissEvent(obs))
                    }
                    continue
                }

                // If in Maximum Zoomies and obstacle is soft/scatterable, Herbert knocks it away!
                if (zoomieMeter.isMaxZoomies && obs.isSoft) {
                    obs.hit = true
                    score.addPoints(100L, true)
                    score.incrementCombo()
                    continue
                }

                // Otherwise, wholesome flop!
                obs.hit = true
                herbert.flop()
                state = GamePlayState.GAME_OVER
                onFlop?.invoke(FlopEvent(obs))
                break
            }
        }
    }
}
