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

    fun resume() {
        isRunning = true
        gameThread = Thread(this, "GameLoopThread").apply { start() }
    }

    fun pause() {
        isRunning = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
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

            // Update game physics & state
            world.update(dt)
            particleManager.update(dt)

            // Running paw dust & skid effects
            if (world.state == GamePlayState.PLAYING && !world.herbert.isJumping) {
                dustTimer += dt
                val dustRate = if (world.zoomieMeter.isMaxZoomies) 0.06f else 0.12f
                if (dustTimer >= dustRate) {
                    dustTimer = 0f
                    particleManager.spawnDust(world.herbert.x - 45f, world.herbert.y + 35f, count = if (world.zoomieMeter.isMaxZoomies) 3 else 1)
                }
            }

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

            // 2. Obstacles & Pickups
            worldRenderer.renderObstacles(canvas, world.obstacles)
            worldRenderer.renderPickups(canvas, world.pickups, world.herbert.animTimer)

            // 3. Particles / Dust / Sparkles
            particleManager.render(canvas)

            // 4. Herbert
            herbertRenderer.render(canvas, world.herbert)

            // 5. State UI overlays
            when (world.state) {
                GamePlayState.TITLE -> hudRenderer.renderTitleScreen(canvas, world)
                GamePlayState.PLAYING -> hudRenderer.renderHud(canvas, world)
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

                when (world.state) {
                    GamePlayState.TITLE -> {
                        val w = GameConstants.WORLD_WIDTH
                        val h = GameConstants.WORLD_HEIGHT
                        if (virtX in (w / 2f - 220f)..(w / 2f + 220f) && virtY in (h / 2f + 60f)..(h / 2f + 240f)) {
                            world.startNewRun()
                            particleManager.clear()
                            soundEffects.playJump()
                        }
                    }
                    GamePlayState.GAME_OVER -> {
                        val w = GameConstants.WORLD_WIDTH
                        val h = GameConstants.WORLD_HEIGHT
                        if (virtX in (w / 2f - 220f)..(w / 2f + 220f) && virtY in (h / 2f + 140f)..(h / 2f + 320f)) {
                            world.startNewRun()
                            particleManager.clear()
                            soundEffects.playJump()
                        }
                    }
                    GamePlayState.PLAYING -> {
                        world.herbert.steerTo(virtY / GameConstants.WORLD_HEIGHT)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (world.state == GamePlayState.PLAYING) {
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
                if (world.state == GamePlayState.PLAYING) {
                    val dy = virtY - touchStartY
                    if (kotlin.math.abs(dy) < 20f && virtX > GameConstants.WORLD_WIDTH * 0.4f) {
                        world.herbert.jump()
                        soundEffects.playJump()
                    }
                }
            }
        }
        return true
    }
}
