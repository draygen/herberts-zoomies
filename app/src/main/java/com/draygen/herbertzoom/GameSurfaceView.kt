package com.draygen.herbertzoom

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.draygen.herbertzoom.core.*
import kotlin.math.sin
import java.util.Random

class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    private val world = LivingRoomWorld()
    private val herbertRenderer = HerbertRenderer()
    private val worldRenderer = WorldRenderer()
    private val hudRenderer = GameHudRenderer()
    private val bossRenderer = KittyBossRenderer()
    private val soundEffects = SoundEffects(context)
    private val particleManager = ParticleManager()
    private val rng = Random()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("herbert_zoomies_prefs", Context.MODE_PRIVATE)

    // Touch handling state
    private var touchStartY = 0f

    // Screen Shake juice
    private var shakeTimer = 0f
    private var shakeMagnitude = 0f
    private var dustTimer = 0f

    // Boss encounter presentation state
    private var lastIntroBeat: IntroBeat? = null
    private var lastVictoryBeat: VictoryBeat? = null
    /** Set when a touch lands on the ZAP button so ACTION_UP can't also jump. */
    private var touchConsumedByFire = false

    // --- Input intents, handed from the touch thread to the game thread ------
    //
    // Anything that mutates the world has to run ON the game loop. These actions
    // all raise callbacks that spawn particles, and spawning into the particle
    // list while the loop is iterating it to render throws
    // ConcurrentModificationException on GameLoopThread - the same crash the
    // duplicate-loop-thread bug used to produce. So the touch thread only ever
    // records an intent here, and run() consumes it between frames.
    @Volatile private var pendingFireBeam = false
    @Volatile private var pendingDebugSummon = false
    @Volatile private var pendingDebugCharge = false
    @Volatile private var pendingNewRun = false

    init {
        holder.addCallback(this)
        isFocusable = true

        // Load persisted high score
        world.score.highScore = prefs.getLong("high_score", 0L)

        // Bind callbacks for particles & juice
        world.onPickupCollected = { event ->
            soundEffects.playPickup()
            vibrateSubtle(20)

            // Spawn juicy feedback
            val colorHex = when (event.pickup.type) {
                PickupType.TREAT -> "#FFB300"
                PickupType.TOY_MOUSE -> "#00E5FF"
                PickupType.YARN_BALL -> "#FF4081"
            }
            particleManager.spawnSparkles(event.pickup.x, event.pickup.y, count = 12, colorHex = colorHex)
            particleManager.spawnFloatingText(event.pickup.x, event.pickup.y - 20f, "+${event.points}", colorHex)
        }

        world.onNearMiss = { event ->
            soundEffects.playNearMiss()
            particleManager.spawnSparkles(world.herbert.x, world.herbert.y - 40f, count = 6, colorHex = "#FFEB3B")
            particleManager.spawnFloatingText(world.herbert.x, world.herbert.y - 50f, "CLOSE!", "#FF9800")
        }

        world.onMaxZoomiesStart = {
            soundEffects.playZoomie()
            vibrateSubtle(75)
            triggerScreenShake(durationSec = 0.4f, magnitude = 12f)
            particleManager.spawnSparkles(world.herbert.x, world.herbert.y, count = 25, colorHex = "#00E5FF")
            particleManager.spawnHearts(world.herbert.x, world.herbert.y, count = 6)
        }

        world.onScratchSpot = { event ->
            soundEffects.playScratch()
            vibrateSubtle(35)
            particleManager.spawnFloatingText(event.spot.x, event.spot.y - 90f, "SCRITCH SCRITCH!", "#8D6E63")
            particleManager.spawnFloatingText(event.spot.x, event.spot.y - 150f, "+${event.points}", "#FFB300")
            particleManager.spawnSparkles(event.spot.x, event.spot.y, count = 10, colorHex = "#D7B98A")
        }

        world.onStumble = { event ->
            soundEffects.playNearMiss()
            vibrateSubtle(30)
            triggerScreenShake(durationSec = 0.2f, magnitude = 8f)
            particleManager.spawnDust(world.herbert.x, world.herbert.y + 15f, count = 5)
            val tag = if (event.obstacle.isKitty) "OOF! KITTY!" else "WHOOPS!"
            particleManager.spawnFloatingText(world.herbert.x, world.herbert.y - 40f, tag, "#FF7043")
        }

        world.onFlop = {
            soundEffects.playFlop()
            vibrateSubtle(45)
            triggerScreenShake(durationSec = 0.35f, magnitude = 16f)
            particleManager.spawnDust(world.herbert.x, world.herbert.y + 20f, count = 10)
            particleManager.spawnHearts(world.herbert.x, world.herbert.y - 30f, count = 5)

            // Save high score
            if (world.score.currentScore >= world.score.highScore) {
                prefs.edit().putLong("high_score", world.score.highScore).apply()
            }
        }

        bindBossCallbacks()

        // Reaching 200 toys legitimately takes a long run, which makes the
        // encounter impractical to iterate on. Debug builds summon her early.
        if (BuildConfig.DEBUG) {
            world.bossToyThreshold = DEBUG_BOSS_TOY_THRESHOLD
        }
    }

    /**
     * Sound, haptics and particles for the encounter. The simulation raises
     * every one of these; nothing here drives gameplay.
     */
    private fun bindBossCallbacks() {
        world.onBossIntro = {
            particleManager.clear()
            lastIntroBeat = null
            lastVictoryBeat = null
        }

        world.onBossFightStart = {
            triggerScreenShake(durationSec = 0.3f, magnitude = 10f)
        }

        world.onBossHit = { event ->
            soundEffects.playFlop()
            vibrateSubtle(40)
            triggerScreenShake(durationSec = 0.28f, magnitude = 14f)
            particleManager.spawnDust(world.herbert.x, world.herbert.y + 15f, count = 8)
            if (event.herbertStumbled) {
                particleManager.spawnFloatingText(
                    world.herbert.x, world.herbert.y - 50f, "BONK!", "#FF7043"
                )
                if (event.energyLost > 0) {
                    particleManager.spawnFloatingText(
                        world.herbert.x, world.herbert.y - 110f, "-1 ENERGY", "#4FC3F7"
                    )
                }
            }
        }

        world.onBossVictory = { event ->
            soundEffects.playBossVictory()
            vibrateSubtle(90)
            triggerScreenShake(durationSec = 0.5f, magnitude = 18f)
            particleManager.spawnSparkles(
                KittyBossRenderer.BOSS_X, KittyBossRenderer.BOSS_Y, count = 30, colorHex = "#00E5FF"
            )
            particleManager.spawnHearts(KittyBossRenderer.BOSS_X, KittyBossRenderer.BOSS_Y, count = 8)
            particleManager.spawnFloatingText(
                GameConstants.WORLD_WIDTH / 2f, GameConstants.WORLD_HEIGHT / 2f,
                "+${event.bonus}", "#FFB300"
            )
        }

        world.onBossEnded = { event ->
            if (event.victory) {
                // The run resumes straight into Maximum Zoomies; onMaxZoomiesStart
                // already fires its own juice, so just clear the stage.
                particleManager.spawnHearts(world.herbert.x, world.herbert.y - 40f, count = 5)
            }
        }

        val boss = world.boss

        boss.onTelegraph = { event ->
            if (event.attack.type != BossAttackType.GROOMING) {
                soundEffects.playTelegraph()
            }
        }

        boss.onStrike = { event ->
            soundEffects.playBossSlam()
            vibrateSubtle(25)
            val heavy = event.attack.type == BossAttackType.GIANT_PAW_SWAT ||
                event.attack.type == BossAttackType.DOUBLE_SWAT
            triggerScreenShake(durationSec = if (heavy) 0.3f else 0.16f, magnitude = if (heavy) 16f else 8f)
            for (zone in event.attack.zones) {
                particleManager.spawnDust(
                    GameConstants.WORLD_WIDTH * 0.5f, zone.bottom, count = if (heavy) 6 else 3
                )
            }
        }

        boss.onEnergyCollected = { event ->
            soundEffects.playPickup()
            vibrateSubtle(18)
            particleManager.spawnSparkles(event.orb.x, event.orb.y, count = 14, colorHex = "#00E5FF")
            if (event.ready) {
                particleManager.spawnFloatingText(
                    world.herbert.x, world.herbert.y - 90f, "EYES CHARGED! TAP ZAP!", "#00E5FF"
                )
            } else {
                particleManager.spawnFloatingText(
                    event.orb.x, event.orb.y - 30f, "${event.energy}/${GameConstants.BOSS_ENERGY_PER_BEAM}", "#4FC3F7"
                )
            }
        }

        boss.onBeamFired = { event ->
            soundEffects.playBeam()
            soundEffects.playKittyShock()
            vibrateSubtle(70)
            triggerScreenShake(durationSec = 0.45f, magnitude = 20f)
            particleManager.spawnSparkles(
                KittyBossRenderer.BOSS_X - 150f, world.boss.gazeY, count = 26, colorHex = "#00E5FF"
            )
            val tag = if (event.finishing) "SHE'S HAD IT!" else "ZAP!"
            particleManager.spawnFloatingText(
                KittyBossRenderer.BOSS_X - 220f, world.boss.gazeY - 120f, tag, "#00E5FF"
            )
        }

        boss.onPhaseChanged = { event ->
            soundEffects.playKittyShock()
            vibrateSubtle(45)
            triggerScreenShake(durationSec = 0.3f, magnitude = 12f)
            val caption = when (event.phase) {
                BossPhase.MILDLY_IRRITATED -> "UNIMPRESSED"
                BossPhase.VERY_ANNOYED -> "VERY ANNOYED!"
                BossPhase.HAD_ENOUGH -> "ABSOLUTELY DONE!"
            }
            particleManager.spawnFloatingText(
                GameConstants.WORLD_WIDTH / 2f, 300f, caption, "#FF5252"
            )
        }
    }

    private fun triggerScreenShake(durationSec: Float, magnitude: Float) {
        shakeTimer = durationSec
        shakeMagnitude = magnitude
    }

    private fun vibrateSubtle(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    /**
     * Idempotent on purpose: both Activity.onResume() and surfaceCreated() call
     * this, and on a cold start they both fire. Starting a second thread would
     * leave two game loops mutating and iterating the same entity lists, which
     * shows up as a ConcurrentModificationException in the renderer.
     */
    fun resume() {
        if (isRunning && gameThread?.isAlive == true) return
        isRunning = true
        gameThread = Thread(this, "GameLoopThread").apply { start() }
    }

    fun pause() {
        isRunning = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        gameThread = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        resume()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pause()
    }

    override fun run() {
        var lastTime = System.nanoTime()
        val targetFrameTimeNs = 16_666_666L // ~60 FPS

        while (isRunning) {
            val now = System.nanoTime()
            val elapsedNs = now - lastTime
            lastTime = now

            val dt = (elapsedNs / 1_000_000_000f).coerceIn(0.001f, 0.05f)

            // Anything the player asked for since the last frame, applied here
            // on the game thread rather than on the thread that took the touch.
            consumePendingInput()

            // Update game physics & state
            world.update(dt)
            particleManager.update(dt)

            // Running paw dust & skid effects
            if (world.state == GamePlayState.PLAYING && !world.herbert.isJumping) {
                dustTimer += dt
                if (world.herbert.isScratching) {
                    // Wood shavings flicking off the patch as he rakes at it
                    if (dustTimer >= 0.05f) {
                        dustTimer = 0f
                        particleManager.spawnDust(world.herbert.scratchCenterX, world.herbert.scratchCenterY, count = 2)
                        particleManager.spawnSparkles(
                            world.herbert.scratchCenterX, world.herbert.scratchCenterY,
                            count = 2, colorHex = "#C9A227"
                        )
                    }
                } else {
                    val dustRate = if (world.zoomieMeter.isMaxZoomies) 0.06f else 0.12f
                    if (dustTimer >= dustRate) {
                        dustTimer = 0f
                        particleManager.spawnDust(world.herbert.x - 45f, world.herbert.y + 35f, count = if (world.zoomieMeter.isMaxZoomies) 3 else 1)
                    }
                }
            }

            // Boss cutscene audio beats
            updateBossCutsceneAudio()

            // Screen shake update
            if (shakeTimer > 0f) {
                shakeTimer -= dt
            }

            // Draw frame
            drawFrame()

            // Frame limiter
            val frameExecutionTimeNs = System.nanoTime() - now
            val sleepNs = targetFrameTimeNs - frameExecutionTimeNs
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (sleepNs % 1_000_000L).toInt())
                } catch (_: InterruptedException) {}
            }
        }
    }

    private fun drawFrame() {
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            // Calculate Fill Scale to prevent black bars on ultra-wide screens (Galaxy S24 Ultra is ~19.5:9)
            val scale = maxOf(viewWidth / GameConstants.WORLD_WIDTH, viewHeight / GameConstants.WORLD_HEIGHT)
            val offsetX = (viewWidth - (GameConstants.WORLD_WIDTH * scale)) / 2f
            val offsetY = (viewHeight - (GameConstants.WORLD_HEIGHT * scale)) / 2f

            // Clean background fill matching wallpaper color
            canvas.drawColor(Color.parseColor("#FFF3E0"))

            canvas.save()

            // Apply screen shake
            if (shakeTimer > 0f) {
                val shakeX = (rng.nextFloat() - 0.5f) * 2f * shakeMagnitude
                val shakeY = (rng.nextFloat() - 0.5f) * 2f * shakeMagnitude
                canvas.translate(shakeX, shakeY)
            }

            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            // Extend room walls/floors left and right to fill entire visible viewport bounds seamlessly
            val extraMargin = (viewWidth / scale - GameConstants.WORLD_WIDTH) / 2f + 100f
            val clipRect = canvas.clipBounds

            // 1. Living room cozy layered background
            worldRenderer.renderBackground(canvas, world)

            if (world.isBossActive) {
                // Kitty owns the screen. The runner's entity lists are empty for
                // the duration, so the arena replaces them rather than stacking
                // on top of them.
                //
                // Order matters. Yarn and energy orbs go in first so they emerge
                // from *behind* Kitty rather than sliding across her face. Her
                // attacks go over her, because the limb delivering them reaches
                // toward the player. Nothing is drawn on top of a danger band
                // that could hide it, and Herbert's beams go last so they read
                // as leaving his face and landing on hers.
                bossRenderer.renderArena(canvas, world)
                bossRenderer.renderProjectiles(canvas, world)
                bossRenderer.renderKitty(canvas, world)
                bossRenderer.renderAttack(canvas, world)
                particleManager.render(canvas)
                herbertRenderer.render(canvas, world.herbert)
                bossRenderer.renderEyeBeams(canvas, world)
            } else {
                // 2. Floor decals (The Spot), then obstacles & pickups on top
                worldRenderer.renderScratchSpots(canvas, world.scratchSpots, world.herbert.animTimer)
                worldRenderer.renderObstacles(canvas, world.obstacles)
                worldRenderer.renderPickups(canvas, world.pickups, world.herbert.animTimer)

                // 3. Particles / Dust / Sparkles
                particleManager.render(canvas)

                // 4. Herbert
                herbertRenderer.render(canvas, world.herbert)
            }

            // 5. State UI overlays
            when (world.state) {
                GamePlayState.TITLE -> hudRenderer.renderTitleScreen(canvas, world)
                GamePlayState.PLAYING -> {
                    hudRenderer.renderHud(canvas, world)
                    if (world.isBossActive) {
                        hudRenderer.renderBossHud(canvas, world)
                        hudRenderer.renderBossCutscene(canvas, world)
                    }
                }
                GamePlayState.GAME_OVER -> {
                    hudRenderer.renderHud(canvas, world)
                    hudRenderer.renderGameOver(canvas, world)
                }
            }

            canvas.restore()
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    /**
     * The intro rumble and the victory beats are timed off the simulation's own
     * clock rather than fired once at the top, so the sound lands on the beat
     * the renderer is drawing.
     */
    private fun updateBossCutsceneAudio() {
        when (world.runMode) {
            RunMode.BOSS_INTRO -> {
                val beat = KittyBoss.introBeatAt(world.bossModeTimer)
                if (beat != lastIntroBeat) {
                    when (beat) {
                        IntroBeat.RUMBLE -> {
                            soundEffects.playBossRumble()
                            vibrateSubtle(120)
                            triggerScreenShake(durationSec = GameConstants.BOSS_INTRO_RUMBLE_SEC, magnitude = 9f)
                        }
                        IntroBeat.TITLE -> {
                            soundEffects.playBossSlam()
                            vibrateSubtle(80)
                            triggerScreenShake(durationSec = 0.35f, magnitude = 20f)
                        }
                        IntroBeat.HOLD -> {}
                    }
                    lastIntroBeat = beat
                }
            }

            RunMode.BOSS_VICTORY -> {
                val beat = KittyBoss.victoryBeatAt(world.bossModeTimer)
                if (beat != lastVictoryBeat) {
                    when (beat) {
                        VictoryBeat.SMOKE -> {
                            // A puff of loose fur, and a beat of silence for the gag.
                            particleManager.spawnDust(
                                KittyBossRenderer.BOSS_X, KittyBossRenderer.BOSS_Y, count = 14
                            )
                        }
                        VictoryBeat.REVEAL -> {
                            soundEffects.playKittyShock()
                            particleManager.spawnHearts(
                                KittyBossRenderer.BOSS_X, KittyBossRenderer.BOSS_Y - 120f, count = 6
                            )
                        }
                        VictoryBeat.BLAST -> {}
                    }
                    lastVictoryBeat = beat
                }
            }

            else -> {
                lastIntroBeat = null
                lastVictoryBeat = null
            }
        }
    }

    /**
     * Applies the touch thread's queued intents. Called only from run(), so
     * every world mutation and every callback it fires happens on the game
     * thread, where the entity and particle lists are safe to mutate.
     */
    private fun consumePendingInput() {
        if (pendingNewRun) {
            pendingNewRun = false
            world.startNewRun()
            particleManager.clear()
        }
        if (pendingDebugSummon) {
            pendingDebugSummon = false
            world.debugForceBoss()
        }
        if (pendingDebugCharge) {
            pendingDebugCharge = false
            world.boss.debugChargeEyes()
        }
        if (pendingFireBeam) {
            pendingFireBeam = false
            // The sim decides whether it lands; onBeamFired supplies the juice.
            world.boss.fireEyeBeam()
        }
    }

    /** True if (x, y) is on the ZAP button and it is live. */
    private fun isOnFireButton(x: Float, y: Float): Boolean {
        if (world.runMode != RunMode.BOSS_FIGHT || !world.boss.beamReady) return false
        val dx = x - GameHudRenderer.FIRE_BUTTON_X
        val dy = y - GameHudRenderer.FIRE_BUTTON_Y
        // A little larger than it is drawn, because it is the fight's one verb.
        val r = GameHudRenderer.FIRE_BUTTON_RADIUS * 1.25f
        return dx * dx + dy * dy <= r * r
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Transform screen coords using the responsive fill scale
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scale = maxOf(viewWidth / GameConstants.WORLD_WIDTH, viewHeight / GameConstants.WORLD_HEIGHT)
        val offsetX = (viewWidth - (GameConstants.WORLD_WIDTH * scale)) / 2f
        val offsetY = (viewHeight - (GameConstants.WORLD_HEIGHT * scale)) / 2f

        val virtX = (event.x - offsetX) / scale
        val virtY = (event.y - offsetY) / scale

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = virtY
                touchConsumedByFire = false

                // The ZAP button is checked before anything else so firing can
                // never be mistaken for steering.
                if (world.state == GamePlayState.PLAYING && isOnFireButton(virtX, virtY)) {
                    pendingFireBeam = true
                    // Pressing the button must never also count as a jump.
                    touchConsumedByFire = true
                    return true
                }

                // Debug-only: summon Kitty on the spot. The strip between the
                // combo badge and the zoomie meter is empty in normal play, and
                // unlike the very corners it survives the 19.5:9 crop. Never
                // present in a release build.
                if (BuildConfig.DEBUG && world.state == GamePlayState.PLAYING &&
                    virtX in 1000f..1200f && virtY in 100f..180f
                ) {
                    pendingDebugSummon = true
                    return true
                }

                // Debug-only: charge Herbert's eyes on the spot, so the beam and
                // the victory sequence can be exercised without farming orbs.
                if (BuildConfig.DEBUG && world.runMode == RunMode.BOSS_FIGHT &&
                    virtX in 1230f..1430f && virtY in 100f..180f
                ) {
                    pendingDebugCharge = true
                    return true
                }

                when (world.state) {
                    GamePlayState.TITLE -> {
                        val w = GameConstants.WORLD_WIDTH
                        val h = GameConstants.WORLD_HEIGHT
                        if (virtX in (w / 2f - 220f)..(w / 2f + 220f) && virtY in (h / 2f + 60f)..(h / 2f + 240f)) {
                            pendingNewRun = true
                            soundEffects.playJump()
                        }
                    }
                    GamePlayState.GAME_OVER -> {
                        val w = GameConstants.WORLD_WIDTH
                        val h = GameConstants.WORLD_HEIGHT
                        if (virtX in (w / 2f - 220f)..(w / 2f + 220f) && virtY in (h / 2f + 140f)..(h / 2f + 320f)) {
                            pendingNewRun = true
                            soundEffects.playJump()
                        }
                    }
                    GamePlayState.PLAYING -> {
                        world.herbert.steerTo(virtY / GameConstants.WORLD_HEIGHT)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (world.state == GamePlayState.PLAYING && !touchConsumedByFire) {
                    val dy = virtY - touchStartY
                    if (dy < -60f && !world.herbert.isJumping) {
                        world.herbert.jump()
                        soundEffects.playJump()
                    } else {
                        world.herbert.steerTo(virtY / GameConstants.WORLD_HEIGHT)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (world.state == GamePlayState.PLAYING && !touchConsumedByFire) {
                    val dy = virtY - touchStartY
                    if (kotlin.math.abs(dy) < 20f && virtX > GameConstants.WORLD_WIDTH * 0.4f) {
                        world.herbert.jump()
                        soundEffects.playJump()
                    }
                }
                touchConsumedByFire = false
            }
        }
        return true
    }

    companion object {
        /**
         * Debug-build toy count that summons Kitty. Release keeps the real 200
         * from [GameConstants.BOSS_TRIGGER_TOYS]; this only exists so the fight
         * is reachable in a normal dev session.
         */
        private const val DEBUG_BOSS_TOY_THRESHOLD = 12
    }
}
