package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.sin

class GameHudRenderer {

    // HUD Text Paints
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF6F00")
        textSize = 90f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(8f, 4f, 4f, Color.argb(100, 0, 0, 0))
    }

    private val titleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 90f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 14f
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#263238")
        textSize = 46f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val comboPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E91E63")
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val maxZoomieTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD600")
        textSize = 58f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, Color.parseColor("#FF6D00"))
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 52f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#37474F")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    // Zoomie Meter Bar
    private val meterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CFD8DC")
        style = Paint.Style.FILL
    }

    private val meterFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        style = Paint.Style.FILL
    }

    private val maxMeterFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }

    fun renderHud(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT

        // 1. Top Left Score & Treats Count
        canvas.drawText("SCORE: ${world.score.currentScore}", 50f, 75f, scorePaint)
        canvas.drawText("BEST: ${world.score.highScore}", 50f, 130f, Paint(scorePaint).apply { textSize = 32f; color = Color.parseColor("#78909C") })

        // Combo Multiplier indicator
        if (world.score.comboMultiplier > 1) {
            canvas.drawText("${world.score.comboMultiplier}x COMBO!", 420f, 75f, comboPaint)
        }

        // 2. Zoomie Meter (Top Right)
        val meterWidth = 380f
        val meterHeight = 36f
        val meterRight = w - 50f
        val meterLeft = meterRight - meterWidth
        val meterTop = 45f
        val meterBottom = meterTop + meterHeight

        val meterRect = RectF(meterLeft, meterTop, meterRight, meterBottom)
        canvas.drawRoundRect(meterRect, 18f, 18f, meterBgPaint)

        // Fill proportion
        val pct = (world.zoomieMeter.value / GameConstants.ZOOMIE_METER_MAX).coerceIn(0f, 1f)
        if (pct > 0f) {
            val fillRect = RectF(meterLeft, meterTop, meterLeft + meterWidth * pct, meterBottom)
            val fillP = if (world.zoomieMeter.isMaxZoomies) maxMeterFillPaint else meterFillPaint
            canvas.drawRoundRect(fillRect, 18f, 18f, fillP)
        }
        canvas.drawRoundRect(meterRect, 18f, 18f, outlinePaint.apply { strokeWidth = 4f })

        // Meter Label
        val meterLabel = if (world.zoomieMeter.isMaxZoomies) "⚡ MAX ZOOMIES ⚡" else "ZOOMIE METER"
        val labelPaint = Paint(scorePaint).apply {
            textSize = 24f
            color = if (world.zoomieMeter.isMaxZoomies) Color.parseColor("#D50000") else Color.parseColor("#37474F")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(meterLabel, meterLeft + meterWidth / 2f, meterTop + 27f, labelPaint)

        // Banner during MAX ZOOMIES
        if (world.zoomieMeter.isMaxZoomies) {
            val pulse = 1f + sin(world.herbert.animTimer * 16f).toFloat() * 0.08f
            canvas.save()
            canvas.scale(pulse, pulse, w / 2f, 160f)
            canvas.drawText("⚡ MAXIMUM ZOOMIES! (3x SCORE) ⚡", w / 2f, 160f, maxZoomieTextPaint)
            canvas.restore()
        }
    }

    fun renderTitleScreen(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT

        // Dark cozy tint
        canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.argb(100, 0, 0, 0); style = Paint.Style.FILL })

        // Title Box
        val titleY = 280f
        canvas.drawText("HERBERT'S ZOOMIES", w / 2f, titleY, titleShadowPaint)
        canvas.drawText("HERBERT'S ZOOMIES", w / 2f, titleY, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("The Living Room Panic", w / 2f, titleY + 60f, subPaint)

        // Best Score Banner
        if (world.score.highScore > 0L) {
            val bestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFD54F")
                textSize = 38f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("BEST SCORE: ${world.score.highScore}", w / 2f, titleY + 140f, bestPaint)
        }

        // Play Button
        val btnRect = RectF(w / 2f - 180f, h / 2f + 80f, w / 2f + 180f, h / 2f + 200f)
        canvas.drawRoundRect(btnRect, 30f, 30f, buttonPaint)
        canvas.drawRoundRect(btnRect, 30f, 30f, outlinePaint)
        canvas.drawText("PLAY", w / 2f, h / 2f + 160f, buttonTextPaint)

        // How to play hint
        val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E0E0")
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🐾 Drag to steer • Tap/Swipe up to Jump • Collect treats & go wild!", w / 2f, h - 80f, hintPaint)
    }

    fun renderGameOver(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT

        canvas.drawRect(0f, 0f, w, h, overlayPaint)

        // Modal Panel
        val panel = RectF(w / 2f - 380f, h / 2f - 260f, w / 2f + 380f, h / 2f + 280f)
        canvas.drawRoundRect(panel, 35f, 35f, panelPaint)
        canvas.drawRoundRect(panel, 35f, 35f, outlinePaint)

        // Title
        val goTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D81B60")
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ZOOMIES COMPLETE!", w / 2f, panel.top + 80f, goTitlePaint)

        // Funny status description
        val fluffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#546E7A")
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Herbert has flopped onto his pink belly!", w / 2f, panel.top + 130f, fluffPaint)

        // Score summary
        val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#263238")
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val leftCol = panel.left + 80f
        canvas.drawText("Final Score:", leftCol, panel.top + 210f, statPaint)
        canvas.drawText("${world.score.currentScore}", panel.right - 80f, panel.top + 210f, Paint(statPaint).apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#FF6F00") })

        canvas.drawText("Best Score:", leftCol, panel.top + 270f, statPaint)
        canvas.drawText("${world.score.highScore}", panel.right - 80f, panel.top + 270f, Paint(statPaint).apply { textAlign = Paint.Align.RIGHT })

        canvas.drawText("Treats & Toys:", leftCol, panel.top + 330f, statPaint)
        canvas.drawText("${world.score.treatsCollected + world.score.toysCollected}", panel.right - 80f, panel.top + 330f, Paint(statPaint).apply { textAlign = Paint.Align.RIGHT })

        // Again button
        val againBtn = RectF(w / 2f - 180f, panel.bottom - 110f, w / 2f + 180f, panel.bottom - 20f)
        canvas.drawRoundRect(againBtn, 25f, 25f, buttonPaint)
        canvas.drawRoundRect(againBtn, 25f, 25f, outlinePaint)
        canvas.drawText("AGAIN!", w / 2f, panel.bottom - 48f, buttonTextPaint)
    }
}
