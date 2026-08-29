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

    // =========================================================================
    // KITTY BOSS ENCOUNTER
    //
    // Three things the player must be able to read at a glance mid-fight:
    // how much patience Kitty has left, how charged Herbert's eyes are, and
    // how much of his stumble buffer is still there. Everything else is flavour.
    // =========================================================================

    private val grumpTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4E342E"); style = Paint.Style.FILL
    }
    private val energyPipEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B0BEC5"); style = Paint.Style.FILL
    }
    private val energyPipFullPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF"); style = Paint.Style.FILL
    }
    private val fireButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00B8D4"); style = Paint.Style.FILL
    }
    private val fireGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 0, 229, 255); style = Paint.Style.FILL
    }

    // Reused every frame - see the note in KittyBossRenderer: allocating these
    // per frame cost roughly a dropped frame in every ten during the fight.
    private val grumpCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4E342E")
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val grumpFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val grumpTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val bufferPawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bossHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val pipShinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3F5FF")
    }
    private val catEyeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F7FA")
    }
    private val catEyeSlitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#102027")
    }
    private val zapLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    /** The in-fight HUD. Drawn on top of the normal HUD, never instead of it. */
    fun renderBossHud(canvas: Canvas, world: LivingRoomWorld) {
        if (world.runMode != RunMode.BOSS_FIGHT) return
        val boss = world.boss
        val w = GameConstants.WORLD_WIDTH

        // --- Grump Meter: Kitty's patience, draining toward zero -------------
        val barW = 660f
        val barH = 46f
        val left = w / 2f - barW / 2f
        // Sits below SAFE_TOP so the caption above the bar is never cropped.
        val top = 152f
        val right = left + barW
        val bottom = top + barH

        val chip = RectF(left - 16f, top - 34f, right + 16f, bottom + 14f)
        canvas.drawRoundRect(chip, 26f, 26f, hudChipPaint)
        canvas.drawRoundRect(chip, 26f, 26f, outlinePaint.apply { strokeWidth = 3.5f })

        canvas.drawText(
            "😾 KITTY'S PATIENCE — ${boss.grumpLabel.caption}", w / 2f, top - 8f, grumpCaptionPaint
        )

        canvas.drawRoundRect(RectF(left, top, right, bottom), 23f, 23f, grumpTrackPaint)
        val frac = boss.grumpFraction
        if (frac > 0f) {
            // Drains from the right, so the empty side reads as progress made.
            grumpFillPaint.color = when (boss.phase) {
                BossPhase.MILDLY_IRRITATED -> Color.parseColor("#8BC34A")
                BossPhase.VERY_ANNOYED -> Color.parseColor("#FFB300")
                BossPhase.HAD_ENOUGH -> Color.parseColor("#EF5350")
            }
            canvas.drawRoundRect(RectF(left, top, left + barW * frac, bottom), 23f, 23f, grumpFillPaint)
        }
        canvas.drawRoundRect(RectF(left, top, right, bottom), 23f, 23f,
            outlinePaint.apply { strokeWidth = 4f })

        // Beam ticks: four notches, one per beam it takes to empty her.
        val beamsToWin = (GameConstants.BOSS_GRUMP_MAX / GameConstants.BOSS_GRUMP_PER_BEAM).toInt()
        val tickPaint = grumpTickPaint
        for (i in 1 until beamsToWin + 1) {
            val tx = left + barW * (i * GameConstants.BOSS_GRUMP_PER_BEAM / GameConstants.BOSS_GRUMP_MAX)
            if (tx < right - 2f) canvas.drawLine(tx, top + 6f, tx, bottom - 6f, tickPaint)
        }

        // --- Stumble buffer: how many more bonks the fight will absorb -------
        val pawPaint = bufferPawPaint
        for (i in 0 until GameConstants.BOSS_STUMBLE_BUFFER + 1) {
            val px = left + 18f + i * 46f
            val py = bottom + 44f
            pawPaint.color = if (i <= world.herbert.lives) Color.parseColor("#FF7043")
                             else Color.parseColor("#BCAAA4")
            canvas.drawCircle(px, py + 5f, 11f, pawPaint)
            canvas.drawCircle(px - 8f, py - 7f, 5f, pawPaint)
            canvas.drawCircle(px, py - 11f, 5.4f, pawPaint)
            canvas.drawCircle(px + 8f, py - 7f, 5f, pawPaint)
        }

        // --- Blue Eye Energy + the ZAP button --------------------------------
        renderEnergyAndFireButton(canvas, world)

        // --- First-few-seconds coaching --------------------------------------
        if (world.bossModeTimer < 5.5f) {
            val fade = (1f - ((world.bossModeTimer - 4.0f) / 1.5f)).coerceIn(0f, 1f)
            val hintPaint = bossHintPaint
            hintPaint.color = Color.argb((235 * fade).toInt(), 255, 253, 231)
            hintPaint.setShadowLayer(8f, 2f, 3f, Color.argb((160 * fade).toInt(), 0, 0, 0))
            canvas.drawText(
                "Grab 🔵 BLUE ZOOMIE ENERGY  •  Dodge into the GREEN gap  •  Tap ZAP! to blast",
                GameConstants.WORLD_WIDTH / 2f, SAFE_BOTTOM - 18f, hintPaint
            )
        }
    }

    private fun renderEnergyAndFireButton(canvas: Canvas, world: LivingRoomWorld) {
        val boss = world.boss

        // Three pips above the button, one per orb needed for a beam.
        val pipY = FIRE_BUTTON_Y - FIRE_BUTTON_RADIUS - 46f
        val total = GameConstants.BOSS_ENERGY_PER_BEAM
        val spacing = 54f
        val startX = FIRE_BUTTON_X - (total - 1) * spacing / 2f
        for (i in 0 until total) {
            val px = startX + i * spacing
            val filled = i < boss.eyeEnergy
            canvas.drawCircle(px, pipY, 19f, if (filled) energyPipFullPaint else energyPipEmptyPaint)
            canvas.drawCircle(px, pipY, 19f, outlinePaint.apply { strokeWidth = 3.5f })
            if (filled) canvas.drawCircle(px - 5f, pipY - 5f, 6f, pipShinePaint)
        }

        if (!boss.beamReady) return

        // Charged: a pulsing ZAP button. Deliberately large and unmissable.
        val pulse = 1f + sin(world.herbert.animTimer * 9f) * 0.07f
        canvas.drawCircle(FIRE_BUTTON_X, FIRE_BUTTON_Y, FIRE_BUTTON_RADIUS * 1.42f * pulse, fireGlowPaint)
        canvas.drawCircle(FIRE_BUTTON_X, FIRE_BUTTON_Y, FIRE_BUTTON_RADIUS * pulse, fireButtonPaint)
        canvas.drawCircle(FIRE_BUTTON_X, FIRE_BUTTON_Y, FIRE_BUTTON_RADIUS * pulse,
            outlinePaint.apply { strokeWidth = 6f })

        // A cat eye, blazing.
        val eyeW = FIRE_BUTTON_RADIUS * 0.62f
        val eyeH = FIRE_BUTTON_RADIUS * 0.40f
        canvas.drawOval(
            FIRE_BUTTON_X - eyeW, FIRE_BUTTON_Y - eyeH - 8f,
            FIRE_BUTTON_X + eyeW, FIRE_BUTTON_Y + eyeH - 8f, catEyeWhitePaint
        )
        canvas.drawOval(
            FIRE_BUTTON_X - 7f, FIRE_BUTTON_Y - eyeH - 6f,
            FIRE_BUTTON_X + 7f, FIRE_BUTTON_Y + eyeH - 10f, catEyeSlitPaint
        )
        canvas.drawText("ZAP!", FIRE_BUTTON_X, FIRE_BUTTON_Y + FIRE_BUTTON_RADIUS * 0.62f, zapLabelPaint)
    }

    /** The intro reveal and the victory sequence cards. */
    fun renderBossCutscene(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT
        val t = world.bossModeTimer

        when (world.runMode) {
            RunMode.BOSS_INTRO -> {
                val beat = KittyBoss.introBeatAt(t)
                // The room dims as she rises, then holds for the title.
                val dim = KittyBoss.introRiseProgress(t)
                canvas.drawRect(0f, 0f, w, h,
                    Paint().apply { color = Color.argb((110 * dim).toInt(), 20, 10, 5) })

                if (beat != IntroBeat.TITLE) return

                val titleT = t - GameConstants.BOSS_INTRO_PAUSE_SEC - GameConstants.BOSS_INTRO_RUMBLE_SEC
                // Slam in, overshoot slightly, settle.
                val slam = (titleT / 0.28f).coerceIn(0f, 1f)
                val scale = if (slam < 1f) 2.4f - 1.4f * slam else
                    1f + sin((titleT - 0.28f) * 9f) * 0.02f

                canvas.save()
                canvas.scale(scale, scale, w / 2f, h / 2f - 60f)
                val bossTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FF5252")
                    textSize = 104f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(14f, 5f, 7f, Color.argb(160, 0, 0, 0))
                }
                val bossTitleStroke = Paint(bossTitle).apply {
                    color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 18f
                    clearShadowLayer()
                }
                canvas.drawText("KITTY HAS HAD ENOUGH", w / 2f, h / 2f - 60f, bossTitleStroke)
                canvas.drawText("KITTY HAS HAD ENOUGH", w / 2f, h / 2f - 60f, bossTitle)
                canvas.restore()

                if (titleT > 0.45f) {
                    val subFade = ((titleT - 0.45f) / 0.4f).coerceIn(0f, 1f)
                    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.argb((240 * subFade).toInt(), 255, 248, 225)
                        textSize = 38f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                        textAlign = Paint.Align.CENTER
                        setShadowLayer(8f, 2f, 3f, Color.argb(150, 0, 0, 0))
                    }
                    // The debug summon can fire well below the real threshold,
                    // and "0 toys was too many" reads as a bug rather than a joke.
                    val toys = world.score.toysCollected
                    val line = if (toys > 0) "$toys toys was too many."
                               else "That was one zoomie too many."
                    canvas.drawText(line, w / 2f, h / 2f + 10f, subPaint)
                }
            }

            RunMode.BOSS_VICTORY -> {
                if (KittyBoss.victoryBeatAt(t) != VictoryBeat.REVEAL) return
                val p = KittyBoss.victoryBeatProgress(t)
                val fade = (p / 0.18f).coerceIn(0f, 1f) * (1f - ((p - 0.82f) / 0.18f).coerceIn(0f, 1f))

                val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb((255 * fade).toInt(), 76, 175, 80)
                    textSize = 88f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                val winStroke = Paint(winPaint).apply {
                    color = Color.argb((255 * fade).toInt(), 255, 255, 255)
                    style = Paint.Style.STROKE; strokeWidth = 16f
                }
                canvas.drawText("SHE'S OVER IT!", w / 2f, 300f, winStroke)
                canvas.drawText("SHE'S OVER IT!", w / 2f, 300f, winPaint)

                val bonusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb((255 * fade).toInt(), 255, 179, 0)
                    textSize = 54f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(10f, 3f, 4f, Color.argb((150 * fade).toInt(), 0, 0, 0))
                }
                canvas.drawText("+${GameConstants.BOSS_VICTORY_BONUS} BONUS", w / 2f, 372f, bonusPaint)

                val flavour = when (world.boss.exit) {
                    KittyExit.WADDLE_AWAY -> "Kitty waddles off to be somewhere else."
                    KittyExit.BECOME_LOAF -> "Kitty becomes a loaf. The loaf is final."
                    KittyExit.GROOM_AND_IGNORE -> "Kitty washes a paw and pretends you are not there."
                }
                val flavourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb((240 * fade).toInt(), 255, 248, 225)
                    textSize = 36f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(8f, 2f, 3f, Color.argb((150 * fade).toInt(), 0, 0, 0))
                }
                canvas.drawText(flavour, w / 2f, 436f, flavourPaint)
            }

            else -> {}
        }
    }

    companion object {
        /**
         * The virtual world is 1920x1080, but it is drawn with *fill* scaling, so
         * on a ~19.5:9 phone (both the S24 Ultra and the dev emulator) the top and
         * bottom are cropped away. Only world y in roughly 97..983 is actually on
         * screen. Anything the player must see or touch has to live inside that.
         */
        const val SAFE_TOP = 100f
        const val SAFE_BOTTOM = 980f

        // The ZAP button lives bottom-left, clear of the right-hand tap-to-jump
        // zone (virtX > 40% of the world), so firing and jumping can never be
        // confused for one another. GameSurfaceView hit-tests against these.
        const val FIRE_BUTTON_X = 150f
        const val FIRE_BUTTON_Y = 872f
        const val FIRE_BUTTON_RADIUS = 86f
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
