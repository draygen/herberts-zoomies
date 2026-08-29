package com.draygen.herbertzoom.core

import java.util.Random

enum class GamePlayState {
    TITLE,
    PLAYING,
    GAME_OVER
}

data class NearMissEvent(val obstacle: Obstacle)
data class PickupEvent(val pickup: Pickup, val points: Long)
data class StumbleEvent(val obstacle: Obstacle)
data class FlopEvent(val obstacle: Obstacle)
data class MaxZoomiesActivatedEvent(val duration: Float)
data class ScratchSpotEvent(val spot: ScratchSpot, val points: Long)

class LivingRoomWorld(
    private val rng: Random = Random()
) {
    val herbert = Herbert()
    val zoomieMeter = ZoomieMeter()
    val score = ScoreRecord()
    var state: GamePlayState = GamePlayState.TITLE

    // --- Kitty boss encounter -------------------------------------------------
    // The boss is a contained detour inside PLAYING: the normal runner is left
    // completely intact and is handed straight back when the encounter ends.
    val boss = KittyBoss(rng)
    var runMode: RunMode = RunMode.RUNNING
        private set

    /** Latched for the whole run so the fight can never trigger twice. */
    var bossTriggered: Boolean = false
        private set

    /** Seconds elapsed inside the current boss mode (intro / victory beats). */
    var bossModeTimer: Float = 0f
        private set

    /**
     * Toys required to summon Kitty. Overridable ONLY by debug builds so the
     * encounter can be reached in seconds during development.
     */
    var bossToyThreshold: Int = GameConstants.BOSS_TRIGGER_TOYS

    val isBossActive: Boolean get() = runMode != RunMode.RUNNING

    val obstacles = mutableListOf<Obstacle>()
    val pickups = mutableListOf<Pickup>()
    val scratchSpots = mutableListOf<ScratchSpot>()

    private var nextEntityId = 1L
    private var spawnTimer = 0f
    private var spawnInterval = 0.85f // Generous spacing between waves
    private var distanceMeterAccumulator = 0f
    private var runTime = 0f
    private var lastScratchSpotTime = -999f
    private var activeScratchSpotId: Long? = null

    // Callbacks for sound / haptics / particles
    var onPickupCollected: ((PickupEvent) -> Unit)? = null
    var onNearMiss: ((NearMissEvent) -> Unit)? = null
    var onMaxZoomiesStart: ((MaxZoomiesActivatedEvent) -> Unit)? = null
    var onStumble: ((StumbleEvent) -> Unit)? = null
    var onFlop: ((FlopEvent) -> Unit)? = null
    var onScratchSpot: ((ScratchSpotEvent) -> Unit)? = null
    var onBossIntro: ((BossIntroEvent) -> Unit)? = null
    var onBossFightStart: (() -> Unit)? = null
    var onBossHit: ((BossHitEvent) -> Unit)? = null
    var onBossVictory: ((BossVictoryEvent) -> Unit)? = null
    var onBossEnded: ((BossEndedEvent) -> Unit)? = null

    fun startNewRun() {
        herbert.reset()
        zoomieMeter.reset()
        score.reset()
        obstacles.clear()
        pickups.clear()
        scratchSpots.clear()
        spawnTimer = 0f
        spawnInterval = 0.85f
        distanceMeterAccumulator = 0f
        runTime = 0f
        lastScratchSpotTime = -999f
        activeScratchSpotId = null
        // The encounter is per-run state only. Nothing about it is persisted, so
        // a new run always starts with Kitty un-summoned.
        boss.reset()
        runMode = RunMode.RUNNING
        bossTriggered = false
        bossModeTimer = 0f
        state = GamePlayState.PLAYING
    }

    fun update(dt: Float) {
        if (state != GamePlayState.PLAYING) return

        runTime += dt
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

        // While Herbert is lapping The Spot the room almost stops, so the moment
        // reads as a deliberate detour rather than obstacles sliding into him.
        // The boss encounter borrows the same trick: the room keeps drifting so
        // the scene stays alive, but slowly enough to read as "the run paused".
        val scrollScale = when {
            herbert.isScratching -> GameConstants.SCRATCH_WORLD_SLOWDOWN
            runMode == RunMode.BOSS_INTRO -> {
                // Ease the world down to a crawl over the dramatic pause.
                val p = (bossModeTimer / GameConstants.BOSS_INTRO_PAUSE_SEC).coerceIn(0f, 1f)
                1f + (GameConstants.BOSS_INTRO_SCROLL - 1f) * p
            }
            runMode != RunMode.RUNNING -> GameConstants.BOSS_FIGHT_SCROLL
            else -> 1f
        }

        // Advance world distance
        val stepDistance = effectiveSpeed * dt * scrollScale
        score.distanceRun += stepDistance
        distanceMeterAccumulator += stepDistance
        if (distanceMeterAccumulator >= 100f) {
            score.addPoints(GameConstants.POINTS_PER_METER_RUN.toLong(), zoomieMeter.isMaxZoomies)
            distanceMeterAccumulator -= 100f
        }

        // Update active obstacles & Kitty behaviors
        val iterObs = obstacles.iterator()
        while (iterObs.hasNext()) {
            val obs = iterObs.next()
            obs.x -= stepDistance
            if (obs.isKitty) {
                obs.updateKitty(dt, herbert.x, herbert.y)
            }
            if (obs.x + obs.width < -200f) {
                iterObs.remove()
            }
        }

        val iterPick = pickups.iterator()
        while (iterPick.hasNext()) {
            val pick = iterPick.next()
            pick.x -= stepDistance
            if (pick.x + pick.radius < -200f) {
                iterPick.remove()
            }
        }

        val iterSpot = scratchSpots.iterator()
        while (iterSpot.hasNext()) {
            val spot = iterSpot.next()
            spot.x -= stepDistance
            if (spot.id == activeScratchSpotId) {
                if (herbert.isScratching) {
                    // Herbert orbits the patch, so his centre must track it as it drifts
                    herbert.scratchCenterX = spot.x
                    herbert.scratchCenterY = spot.y
                    spot.scratchProgress = herbert.scratchProgress
                } else {
                    // Finished: leave the patch fully scuffed up
                    spot.scratchProgress = 1f
                    activeScratchSpotId = null
                }
            }
            if (spot.x + spot.radiusX < -200f) {
                iterSpot.remove()
            }
        }

        if (runMode != RunMode.RUNNING) {
            // Kitty owns the screen. No normal spawns, no normal collisions -
            // the runner is frozen in place, not torn down.
            updateBoss(dt)
            return
        }

        // Spawning with early-game gentle pacing
        spawnTimer += dt
        // In the first 25 seconds, space things out gently
        val baseRate = if (runTime < 20f) 0.95f else 0.75f
        val currentSpawnRate = if (zoomieMeter.isMaxZoomies) 0.45f else (baseRate - (herbert.currentSpeed / 12000f)).coerceAtLeast(0.5f)
        if (spawnTimer >= currentSpawnRate) {
            spawnTimer = 0f
            spawnWave()
        }

        // Check Collisions
        checkCollisions()

        // ...and finally: has Herbert pushed Kitty too far?
        maybeTriggerBoss()
    }

    // =========================================================================
    // KITTY BOSS ENCOUNTER
    //
    // A self-contained state machine layered over the normal run:
    //   RUNNING -> BOSS_INTRO -> BOSS_FIGHT -> BOSS_VICTORY -> RUNNING
    // Nothing here replaces the runner; it only pauses it.
    // =========================================================================

    private fun maybeTriggerBoss() {
        if (bossTriggered) return
        if (score.toysCollected < bossToyThreshold) return
        beginBossIntro()
    }

    /** Development-only shortcut. Never reachable from release gameplay. */
    fun debugForceBoss() {
        if (state == GamePlayState.PLAYING && !bossTriggered) beginBossIntro()
    }

    private fun beginBossIntro() {
        // Latch immediately: toy 201, 202... must never re-summon her.
        bossTriggered = true
        runMode = RunMode.BOSS_INTRO
        bossModeTimer = 0f
        boss.reset()

        // Clear the stage so the reveal is clean and nothing can hit Herbert
        // mid-cutscene. Score, combo and the zoomie meter are all untouched.
        obstacles.clear()
        pickups.clear()
        scratchSpots.clear()
        activeScratchSpotId = null
        herbert.scratchTimer = 0f
        if (herbert.animState == AnimationState.SCRATCHING) {
            herbert.animState = AnimationState.RUNNING
            herbert.x = GameConstants.HERBERT_HOME_X
        }

        onBossIntro?.invoke(BossIntroEvent(score.toysCollected))
    }

    private fun updateBoss(dt: Float) {
        bossModeTimer += dt
        when (runMode) {
            RunMode.BOSS_INTRO -> {
                boss.tickAnimationOnly(dt, herbert.y)
                if (bossModeTimer >= GameConstants.BOSS_INTRO_DURATION) {
                    runMode = RunMode.BOSS_FIGHT
                    bossModeTimer = 0f
                    // A boss hit must never end a run outright: Herbert gets a
                    // deeper stumble buffer for the duration of the fight.
                    herbert.lives = GameConstants.BOSS_STUMBLE_BUFFER
                    boss.beginFight()
                    onBossFightStart?.invoke()
                }
            }

            RunMode.BOSS_FIGHT -> {
                boss.update(dt, herbert)
                boss.collectEnergy(herbert)

                if (!herbert.isInvulnerable && boss.isHerbertStruck(herbert)) {
                    applyBossHit()
                }

                if (boss.isDefeated) beginBossVictory()
            }

            RunMode.BOSS_VICTORY -> {
                boss.tickAnimationOnly(dt, herbert.y)
                if (bossModeTimer >= GameConstants.BOSS_VICTORY_DURATION) {
                    endBossEncounter()
                }
            }

            RunMode.RUNNING -> {}
        }
    }

    /**
     * Kitty never lands a run-ending blow on her own. A hit costs a stumble, the
     * combo, and a charge of Blue Eye Energy - the same forgiving currency the
     * rest of the game trades in.
     */
    private fun applyBossHit() {
        val energyLost = boss.loseEnergyOnHit()
        score.comboMultiplier = 1
        score.comboTimer = 0f

        if (herbert.lives > 0) {
            herbert.lives--
            herbert.stumble()
            onBossHit?.invoke(BossHitEvent(herbertStumbled = true, energyLost = energyLost))
            return
        }

        // Out of buffer: the normal wholesome flop, routed through the existing
        // flop path so high-score saving and the usual juice all still happen.
        herbert.flop()
        state = GamePlayState.GAME_OVER
        onBossHit?.invoke(BossHitEvent(herbertStumbled = false, energyLost = energyLost))
        onFlop?.invoke(FlopEvent(bossFlopStandIn()))
        onBossEnded?.invoke(BossEndedEvent(victory = false))
    }

    private fun bossFlopStandIn() = Obstacle(
        id = -1L,
        x = GameConstants.WORLD_WIDTH * 0.78f,
        y = GameConstants.WORLD_HEIGHT * 0.5f,
        type = ObstacleType.KITTY_LOAF,
        width = GameConstants.KITTY_WIDTH,
        height = GameConstants.KITTY_HEIGHT,
        canJumpOver = false,
        isSoft = true
    )

    private fun beginBossVictory() {
        runMode = RunMode.BOSS_VICTORY
        bossModeTimer = 0f
        boss.chooseExit()
        boss.yarn.clear()
        boss.orbs.clear()
        score.addFlatBonus(GameConstants.BOSS_VICTORY_BONUS)
        onBossVictory?.invoke(BossVictoryEvent(GameConstants.BOSS_VICTORY_BONUS))
    }

    private fun endBossEncounter() {
        runMode = RunMode.RUNNING
        bossModeTimer = 0f
        boss.yarn.clear()
        boss.orbs.clear()

        // Hand the run straight back, mid-zoomies, with a breather before the
        // first obstacle so the handover never feels like an ambush.
        herbert.lives = 1
        if (herbert.animState == AnimationState.STUMBLING) {
            herbert.animState = AnimationState.RUNNING
        }
        spawnTimer = -1.2f
        val activated = zoomieMeter.addEnergy(GameConstants.ZOOMIE_METER_MAX)
        if (activated) {
            onMaxZoomiesStart?.invoke(MaxZoomiesActivatedEvent(GameConstants.MAX_ZOOMIE_DURATION_SEC))
        }
        onBossEnded?.invoke(BossEndedEvent(victory = true))
    }

    private fun spawnWave() {
        val spawnX = GameConstants.WORLD_WIDTH + 200f
        val laneY = (0.28f + rng.nextFloat() * 0.54f) * GameConstants.WORLD_HEIGHT

        // Kitty Spawn (25% chance after 8 seconds of play)
        if (runTime > 8f && rng.nextFloat() < 0.28f) {
            val kittyType = when (rng.nextInt(3)) {
                0 -> ObstacleType.KITTY_LOAF
                1 -> ObstacleType.KITTY_SLEEPING
                else -> ObstacleType.KITTY_WADDLE
            }
            val vy = if (kittyType == ObstacleType.KITTY_WADDLE) (if (rng.nextBoolean()) 90f else -90f) else 0f
            obstacles.add(
                Obstacle(
                    id = nextEntityId++,
                    x = spawnX,
                    y = laneY,
                    type = kittyType,
                    width = GameConstants.KITTY_WIDTH,
                    height = GameConstants.KITTY_HEIGHT,
                    canJumpOver = true, // Herbert can pounce over Kitty!
                    isSoft = true, // Soft bounce / swat
                    vy = vy
                )
            )
        } else if (rng.nextFloat() < 0.65f) {
            // General furniture obstacles
            val type = when (rng.nextInt(6)) {
                0 -> ObstacleType.SLIPPER
                1 -> ObstacleType.SOCK_PILE
                2 -> ObstacleType.CARDBOARD_BOX
                3 -> ObstacleType.COUCH_CUSHION
                4 -> ObstacleType.CAT_TUNNEL
                else -> ObstacleType.SCRATCHING_POST
            }
            val (w, h, canJump, isSoft) = when (type) {
                ObstacleType.SLIPPER -> Quad(95f, 60f, true, true)
                ObstacleType.SOCK_PILE -> Quad(85f, 55f, true, true)
                ObstacleType.CARDBOARD_BOX -> Quad(125f, 95f, false, false)
                ObstacleType.COUCH_CUSHION -> Quad(145f, 80f, true, true)
                ObstacleType.CAT_TUNNEL -> Quad(170f, 90f, true, true)
                ObstacleType.SCRATCHING_POST -> Quad(75f, 150f, false, false)
                else -> Quad(100f, 60f, true, true)
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

        // The Spot: rare, never crowded, and never while he's already at one.
        val canSpawnSpot = runTime > 10f &&
            (runTime - lastScratchSpotTime) > GameConstants.SCRATCH_SPOT_MIN_INTERVAL_SEC &&
            scratchSpots.none { !it.used } &&
            !herbert.isScratching
        if (canSpawnSpot && rng.nextFloat() < 0.22f) {
            lastScratchSpotTime = runTime
            // Keep it clear of the very top/bottom so the orbit stays on screen
            val spotY = (0.34f + rng.nextFloat() * 0.42f) * GameConstants.WORLD_HEIGHT
            scratchSpots.add(
                ScratchSpot(
                    id = nextEntityId++,
                    x = spawnX + 260f,
                    y = spotY
                )
            )
        }

        // Pickups spawn frequently in rewarding lines
        val pickupCount = if (rng.nextFloat() < 0.4f) 2 else 1
        for (i in 0 until pickupCount) {
            val pickupType = when (rng.nextInt(3)) {
                0 -> PickupType.TREAT
                1 -> PickupType.TOY_MOUSE
                else -> PickupType.YARN_BALL
            }
            val offsetLaneY = (0.26f + rng.nextFloat() * 0.58f) * GameConstants.WORLD_HEIGHT
            pickups.add(
                Pickup(
                    id = nextEntityId++,
                    x = spawnX + (i * 140f),
                    y = offsetLaneY,
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
                val totalPoints = (pts * score.comboMultiplier * (if (zoomieMeter.isMaxZoomies) 3 else 1)).toLong()
                score.addPoints(pts.toLong(), zoomieMeter.isMaxZoomies)
                if (pick.type == PickupType.TREAT) score.treatsCollected++ else score.toysCollected++
                score.incrementCombo()

                val activated = zoomieMeter.addEnergy(zoomieGain)
                if (activated) {
                    onMaxZoomiesStart?.invoke(MaxZoomiesActivatedEvent(GameConstants.MAX_ZOOMIE_DURATION_SEC))
                }
                onPickupCollected?.invoke(PickupEvent(pick, totalPoints))
                pickIter.remove()
            }
        }

        // The Spot - Herbert cannot resist a worn patch of floorboard
        if (!herbert.isScratching && !isJumping && herbert.animState != AnimationState.FLOPPED) {
            for (spot in scratchSpots) {
                if (spot.used) continue
                if (!spot.contains(hx, hy, pad = hRadius)) continue

                spot.used = true
                activeScratchSpotId = spot.id
                herbert.beginScratch(spot.x, spot.y)

                val points = GameConstants.POINTS_PER_SCRATCH_SPOT
                val totalPoints = (points * score.comboMultiplier *
                    (if (zoomieMeter.isMaxZoomies) 3 else 1)).toLong()
                score.addPoints(points.toLong(), zoomieMeter.isMaxZoomies)
                score.incrementCombo()

                val activated = zoomieMeter.addEnergy(GameConstants.ZOOMIE_PER_SCRATCH_SPOT)
                if (activated) {
                    onMaxZoomiesStart?.invoke(MaxZoomiesActivatedEvent(GameConstants.MAX_ZOOMIE_DURATION_SEC))
                }
                onScratchSpot?.invoke(ScratchSpotEvent(spot, totalPoints))
                break
            }
        }

        // Obstacles
        for (obs in obstacles) {
            if (obs.hit) continue

            val dx = kotlin.math.abs(hx - obs.x)
            val dy = kotlin.math.abs(hy - obs.y)

            val collidesX = dx < (obs.width / 2f + hRadius * 0.65f)
            val collidesY = dy < (obs.height / 2f + hRadius * 0.65f)

            // Near miss check (passed close without hitting)
            if (!obs.nearMissTriggered && !collidesX && hx > obs.right && hx < obs.right + 80f && dy < (obs.height / 2f + 95f)) {
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
                // If invulnerable from a recent stumble, pass safely
                if (herbert.isInvulnerable) continue

                // If Herbert is high enough in a jump and obstacle is jumpable (or jumping over Kitty!)
                if (obs.canJumpOver && isJumping && jumpH > 30f) {
                    // Safe jump!
                    if (!obs.nearMissTriggered) {
                        obs.nearMissTriggered = true
                        score.addPoints(GameConstants.POINTS_PER_NEAR_MISS.toLong(), zoomieMeter.isMaxZoomies)
                        score.incrementCombo()
                        onNearMiss?.invoke(NearMissEvent(obs))
                    }
                    continue
                }

                // If in Maximum Zoomies and obstacle is soft (or Kitty swat), harmlessly scatter / bounce past
                if (zoomieMeter.isMaxZoomies) {
                    obs.hit = true
                    score.addPoints(100L, true)
                    score.incrementCombo()
                    continue
                }

                // If Herbert still has a life buffer, trigger Stumble (forgiving recovery instead of instant loss)
                if (herbert.lives > 0) {
                    obs.hit = true
                    herbert.lives--
                    herbert.stumble()
                    onStumble?.invoke(StumbleEvent(obs))
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
