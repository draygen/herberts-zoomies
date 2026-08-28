package com.draygen.herbertzoom

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.draygen.herbertzoom.core.*

class GameSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isRunning = false

    private val world = LivingRoomWorld()
    private val herbertRenderer = HerbertRenderer()
    private val worldRenderer = WorldRenderer()
    private val hudRenderer = GameHudRenderer()
    private val soundEffects = SoundEffects(context)

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
    private var isDragging = false

    init {
        holder.addCallback(this)
        isFocusable = true

        // Load persisted high score
        world.score.highScore = prefs.getLong("high_score", 0L)

        // Bind callbacks
        world.onPickupCollected = {
            soundEffects.playPickup()
            vibrateSubtle(20)
        }

        world.onNearMiss = {
            soundEffects.playNearMiss()
        }

        world.onMaxZoomiesStart = {
            soundEffects.playZoomie()
            vibrateSubtle(70)
        }

        world.onFlop = {
            soundEffects.playFlop()
            vibrateSubtle(40)
            // Save high score
            if (world.score.currentScore >= world.score.highScore) {
                prefs.edit().putLong("high_score", world.score.highScore).apply()
            }
        }
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
            // Scale and letterbox/pillarbox to 1920x1080 virtual canvas
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            val scaleX = viewWidth / GameConstants.WORLD_WIDTH
            val scaleY = viewHeight / GameConstants.WORLD_HEIGHT
            val scale = minOf(scaleX, scaleY)

            val offsetX = (viewWidth - (GameConstants.WORLD_WIDTH * scale)) / 2f
            val offsetY = (viewHeight - (GameConstants.WORLD_HEIGHT * scale)) / 2f

            canvas.drawColor(android.graphics.Color.BLACK)

            canvas.save()
            canvas.translate(offsetX, offsetY)
            canvas.scale(scale, scale)

            // 1. Living room background
            worldRenderer.renderBackground(canvas, world)

            // 2. Obstacles & Pickups
            worldRenderer.renderObstacles(canvas, world.obstacles)
            worldRenderer.renderPickups(canvas, world.pickups, world.herbert.animTimer)

            // 3. Herbert
            herbertRenderer.render(canvas, world.herbert)

            // 4. State UI overlays
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
        // Transform screen coords to virtual 1920x1080 coords
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scale = minOf(viewWidth / GameConstants.WORLD_WIDTH, viewHeight / GameConstants.WORLD_HEIGHT)
        val offsetX = (viewWidth - (GameConstants.WORLD_WIDTH * scale)) / 2f
        val offsetY = (viewHeight - (GameConstants.WORLD_HEIGHT * scale)) / 2f

        val virtX = (event.x - offsetX) / scale
        val virtY = (event.y - offsetY) / scale

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartY = virtY
                isDragging = false

                when (world.state) {
                    GamePlayState.TITLE -> {
                        // Check if touched Play button
                        val w = GameConstants.WORLD_WIDTH
                        val h = GameConstants.WORLD_HEIGHT
                        if (virtX in (w / 2f - 200f)..(w / 2f + 200f) && virtY in (h / 2f + 50f)..(h / 2f + 220f)) {
                            world.startNewRun()
                            soundEffects.playJump()
                        }
                    }
                    GamePlayState.GAME_OVER -> {
                        // Check if touched Again button
                        val w = GameConstants.WORLD_WIDTH
                        val h = GameConstants.WORLD_HEIGHT
                        if (virtX in (w / 2f - 200f)..(w / 2f + 200f) && virtY in (h / 2f + 160f)..(h / 2f + 300f)) {
                            world.startNewRun()
                            soundEffects.playJump()
                        }
                    }
                    GamePlayState.PLAYING -> {
                        // Steer directly toward touch Y
                        world.herbert.steerTo(virtY / GameConstants.WORLD_HEIGHT)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (world.state == GamePlayState.PLAYING) {
                    val dy = virtY - touchStartY
                    if (dy < -60f && !world.herbert.isJumping) {
                        // Upward swipe = Jump/Pounce!
                        world.herbert.jump()
                        soundEffects.playJump()
                    } else {
                        // Drag steer
                        world.herbert.steerTo(virtY / GameConstants.WORLD_HEIGHT)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (world.state == GamePlayState.PLAYING) {
                    // A quick tap also causes jump if not dragged far
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
