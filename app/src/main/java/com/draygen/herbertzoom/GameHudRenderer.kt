package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.sin

class GameHudRenderer {

    // HUD Text Paints
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5722")
        textSize = 96f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 4f, 6f, Color.argb(120, 0, 0, 0))
    }

    private val titleShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 96f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeWidth = 16f
    }

    private val hudScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E1C14")
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val hudBestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8D6E63")
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val comboBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E91E63")
        style = Paint.Style.FILL
    }

    private val comboBadgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val maxZoomieTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFEB3B")
        textSize = 54f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(14f, 0f, 0f, Color.parseColor("#FF3D00"))
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
    }

    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 54f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val panelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFDF9")
        style = Paint.Style.FILL
    }

    private val hudChipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(240, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E1C14")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 20, 10, 5)
        style = Paint.Style.FILL
    }

    // Zoomie Meter Bar
    private val meterTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0D8D0")
        style = Paint.Style.FILL
    }

    private val meterNormalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9800")
        style = Paint.Style.FILL
    }

    private val meterMaxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }

    fun renderHud(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH

        // 1. Top Left Score Badge Chip (Placed cleanly down within the wallpaper banner)
        val scoreChipRect = RectF(60f, 95f, 430f, 185f)
        canvas.drawRoundRect(scoreChipRect, 24f, 24f, hudChipPaint)
        canvas.drawRoundRect(scoreChipRect, 24f, 24f, outlinePaint.apply { strokeWidth = 3.5f })
        outlinePaint.strokeWidth = 6f

        // Cute Paw icon
        val iconX = 95f
        val iconY = 132f
        canvas.drawCircle(iconX, iconY + 7f, 11f, Paint().apply { color = Color.parseColor("#FFA000"); style = Paint.Style.FILL })
        canvas.drawCircle(iconX - 8f, iconY - 6f, 4.8f, Paint().apply { color = Color.parseColor("#FFA000"); style = Paint.Style.FILL })
        canvas.drawCircle(iconX, iconY - 10f, 5.2f, Paint().apply { color = Color.parseColor("#FFA000"); style = Paint.Style.FILL })
        canvas.drawCircle(iconX + 8f, iconY - 6f, 4.8f, Paint().apply { color = Color.parseColor("#FFA000"); style = Paint.Style.FILL })

        canvas.drawText("SCORE: ${world.score.currentScore}", 132f, 134f, hudScorePaint)
        canvas.drawText("BEST: ${world.score.highScore}", 132f, 168f, hudBestPaint)

        // Combo Multiplier Pop
        if (world.score.comboMultiplier > 1) {
            val comboRect = RectF(450f, 102f, 650f, 178f)
            val popScale = 1f + (sin(world.herbert.animTimer * 14f).toFloat() * 0.06f)
            canvas.save()
            canvas.scale(popScale, popScale, 550f, 140f)
            canvas.drawRoundRect(comboRect, 18f, 18f, comboBadgePaint)
            canvas.drawRoundRect(comboRect, 18f, 18f, outlinePaint.apply { strokeWidth = 3f })
            canvas.drawText("🔥 ${world.score.comboMultiplier}x COMBO!", 550f, 150f, comboBadgeTextPaint)
            canvas.restore()
        }

        // 2. Zoomie Meter (Top Right Styled Capsule in wallpaper band)
        val meterWidth = 380f
        val meterHeight = 42f
        val meterRight = w - 60f
        val meterLeft = meterRight - meterWidth
        val meterTop = 110f
        val meterBottom = meterTop + meterHeight

        val chipBg = RectF(meterLeft - 14f, meterTop - 10f, meterRight + 14f, meterBottom + 10f)
        canvas.drawRoundRect(chipBg, 28f, 28f, hudChipPaint)
        canvas.drawRoundRect(chipBg, 28f, 28f, outlinePaint.apply { strokeWidth = 3.5f })

        val trackRect = RectF(meterLeft, meterTop, meterRight, meterBottom)
        canvas.drawRoundRect(trackRect, 21f, 21f, meterTrackPaint)

        val pct = (world.zoomieMeter.value / GameConstants.ZOOMIE_METER_MAX).coerceIn(0f, 1f)
        if (pct > 0f) {
            val fillRect = RectF(meterLeft, meterTop, meterLeft + meterWidth * pct, meterBottom)
            val fillPaint = if (world.zoomieMeter.isMaxZoomies) meterMaxPaint else meterNormalPaint
            canvas.drawRoundRect(fillRect, 21f, 21f, fillPaint)
        }
        canvas.drawRoundRect(trackRect, 21f, 21f, outlinePaint.apply { strokeWidth = 3f })

        // Meter Label
        val meterLabel = if (world.zoomieMeter.isMaxZoomies) "⚡ MAXIMUM ZOOMIES ⚡" else "🐾 ZOOMIE ENERGY 🐾"
        val labelPaint = Paint(hudScorePaint).apply {
            textSize = 21f
            color = if (world.zoomieMeter.isMaxZoomies) Color.parseColor("#006064") else Color.parseColor("#4E342E")
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(meterLabel, meterLeft + meterWidth / 2f, meterTop + 29f, labelPaint)

        // Banner during MAX ZOOMIES
        if (world.zoomieMeter.isMaxZoomies) {
            val pulse = 1f + sin(world.herbert.animTimer * 16f).toFloat() * 0.08f
            canvas.save()
            canvas.scale(pulse, pulse, w / 2f, 210f)
            canvas.drawText("⚡ MAXIMUM ZOOMIES! (3x SCORE) ⚡", w / 2f, 210f, maxZoomieTextPaint)
            canvas.restore()
        }
    }

    fun renderTitleScreen(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT

        canvas.drawRect(0f, 0f, w, h, overlayPaint)

        // Title Plaque
        val titleY = 270f
        canvas.drawText("HERBERT'S ZOOMIES", w / 2f, titleY, titleShadowPaint)
        canvas.drawText("HERBERT'S ZOOMIES", w / 2f, titleY, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFF8E1")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Living Room Panic!", w / 2f, titleY + 65f, subPaint)

        // Best Score Card
        if (world.score.highScore > 0L) {
            val bestCard = RectF(w / 2f - 240f, titleY + 110f, w / 2f + 240f, titleY + 180f)
            canvas.drawRoundRect(bestCard, 25f, 25f, hudChipPaint)
            canvas.drawRoundRect(bestCard, 25f, 25f, outlinePaint.apply { strokeWidth = 4f })

            val bestPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6F00")
                textSize = 34f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("👑 BEST SCORE: ${world.score.highScore}", w / 2f, titleY + 158f, bestPaint)
        }

        // Play Button with shadow
        val btnRect = RectF(w / 2f - 190f, h / 2f + 85f, w / 2f + 190f, h / 2f + 210f)
        val btnShadow = RectF(btnRect.left + 6f, btnRect.top + 10f, btnRect.right + 6f, btnRect.bottom + 10f)
        canvas.drawRoundRect(btnShadow, 35f, 35f, outlinePaint.apply { style = Paint.Style.FILL; color = Color.parseColor("#2E1C14") })
        canvas.drawRoundRect(btnRect, 35f, 35f, buttonPaint)
        canvas.drawRoundRect(btnRect, 35f, 35f, outlinePaint.apply { style = Paint.Style.STROKE; strokeWidth = 6f })
        canvas.drawText("PLAY 🐾", w / 2f, h / 2f + 168f, buttonTextPaint)

        // Tutorial hint pill
        val hintCard = RectF(w / 2f - 480f, h - 140f, w / 2f + 480f, h - 75f)
        canvas.drawRoundRect(hintCard, 25f, 25f, hudChipPaint)
        canvas.drawRoundRect(hintCard, 25f, 25f, outlinePaint.apply { strokeWidth = 3.5f })
        val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3E2723")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✨ Drag to Steer • Tap / Swipe Up to Jump • Collect Treats & Go Wild!", w / 2f, h - 98f, hintPaint)
    }

    fun renderGameOver(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT

        canvas.drawRect(0f, 0f, w, h, overlayPaint)

        // Modal Plaque
        val panel = RectF(w / 2f - 420f, h / 2f - 270f, w / 2f + 420f, h / 2f + 290f)
        val panelShadow = RectF(panel.left + 8f, panel.top + 12f, panel.right + 8f, panel.bottom + 12f)
        canvas.drawRoundRect(panelShadow, 42f, 42f, outlinePaint.apply { style = Paint.Style.FILL; color = Color.argb(120, 0, 0, 0) })
        canvas.drawRoundRect(panel, 42f, 42f, panelBgPaint)
        canvas.drawRoundRect(panel, 42f, 42f, outlinePaint.apply { style = Paint.Style.STROKE; strokeWidth = 6f })

        // Title
        val goTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D81B60")
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ZOOMIES COMPLETE!", w / 2f, panel.top + 80f, goTitlePaint)

        // Wholesome status description
        val fluffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#795548")
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText("Herbert has flopped onto his pink belly!", w / 2f, panel.top + 135f, fluffPaint)

        // Score summary
        val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4E342E")
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val leftCol = panel.left + 80f
        canvas.drawText("Final Score:", leftCol, panel.top + 215f, statLabelPaint)
        canvas.drawText("${world.score.currentScore}", panel.right - 80f, panel.top + 215f, Paint(statLabelPaint).apply { textAlign = Paint.Align.RIGHT; color = Color.parseColor("#FF6F00") })

        canvas.drawText("Best Score:", leftCol, panel.top + 275f, statLabelPaint)
        canvas.drawText("${world.score.highScore}", panel.right - 80f, panel.top + 275f, Paint(statLabelPaint).apply { textAlign = Paint.Align.RIGHT })

        canvas.drawText("Treats & Toys:", leftCol, panel.top + 335f, statLabelPaint)
        canvas.drawText("${world.score.treatsCollected + world.score.toysCollected}", panel.right - 80f, panel.top + 335f, Paint(statLabelPaint).apply { textAlign = Paint.Align.RIGHT })

        // Play Again button
        val againBtn = RectF(w / 2f - 190f, panel.bottom - 115f, w / 2f + 190f, panel.bottom - 25f)
        val againShadow = RectF(againBtn.left + 4f, againBtn.top + 6f, againBtn.right + 4f, againBtn.bottom + 6f)
        canvas.drawRoundRect(againShadow, 30f, 30f, outlinePaint.apply { style = Paint.Style.FILL; color = Color.parseColor("#2E1C14") })
        canvas.drawRoundRect(againBtn, 30f, 30f, buttonPaint)
        canvas.drawRoundRect(againBtn, 30f, 30f, outlinePaint.apply { style = Paint.Style.STROKE; strokeWidth = 6f })
        canvas.drawText("AGAIN! 🐾", w / 2f, panel.bottom - 52f, buttonTextPaint)
    }
}
