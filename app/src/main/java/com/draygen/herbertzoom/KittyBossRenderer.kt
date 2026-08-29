package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Draws the Kitty boss encounter.
 *
 * Kitty keeps the exact palette and markings of her hazard-sized self in
 * [KittyRenderer] - she is the same 9-year-old tabby, just enormous and
 * genuinely fed up. Everything here is presentation only; every timing,
 * position and danger band is read straight off the simulation in `game-core`.
 *
 * The one rule the visuals owe the player: **an attack must be readable before
 * it can hurt**. Telegraph bands are drawn hazard-yellow and grow toward the
 * strike; the moment they turn red they are live. Nothing is ever drawn on top
 * of a danger band that could hide it.
 */
class KittyBossRenderer {

    // Kitty's fur, matched to KittyRenderer so she reads as the same cat.
    private val tabbyBase = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D7B68") }
    private val tabbyShade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6A5844") }
    private val tabbyStripe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#423223"); style = Paint.Style.STROKE
        strokeWidth = 16f; strokeCap = Paint.Cap.ROUND
    }
    private val bellyCream = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7CCC8") }
    private val muzzleCream = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFEBE9") }
    private val earOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5D4037") }
    private val earInner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D1A7A0") }
    private val eyeGold = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C0CA33") }
    private val eyeGoldHot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F") }
    private val pupil = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B1B1B") }
    private val nosePink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BCAAA4") }
    private val clawWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val tonguePink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F48FB1") }

    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D2013"); style = Paint.Style.STROKE
        strokeWidth = 9f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    private val whisker = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3E2723"); style = Paint.Style.STROKE
        strokeWidth = 6f; strokeCap = Paint.Cap.ROUND
    }
    private val bossShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 42, 24, 16) }

    // Telegraph / strike bands.
    private val telegraphFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 193, 7) }
    private val telegraphStripe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 255, 235, 59); style = Paint.Style.STROKE; strokeWidth = 26f
    }
    private val telegraphEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFC107"); style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val strikeFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(96, 244, 67, 54) }
    private val strikeEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252"); style = Paint.Style.STROKE; strokeWidth = 8f
    }
    // The coaching line tells the player to dodge into the green gap, so the
    // gap has to actually read as green against a warm tan floor.
    private val safeGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(74, 76, 175, 80) }
    private val safeEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 76, 175, 80); style = Paint.Style.STROKE; strokeWidth = 4f
    }

    // Yarn, orbs and beams.
    private val yarnBody = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F06292") }
    private val yarnStrand = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AD1457"); style = Paint.Style.STROKE; strokeWidth = 5f
    }
    private val orbCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E5FF") }
    private val orbInner = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B3F5FF") }
    private val orbHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 0, 229, 255) }
    private val beamCore = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val beamMid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#40E9FF") }
    private val beamOuter = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(130, 0, 176, 255) }

    private val fluffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE6DC") }
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF59D") }

    // Scratch paints, reused every frame. The boss scene draws far more shapes
    // than the runner, and allocating a Paint per shape per frame showed up as
    // GC hitches: p90 frame time went from 18ms to 33ms. Nothing in the draw
    // path allocates now.
    private val tintPaint = Paint()
    private val arenaEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 46, 28, 20); style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val moodTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4E342E")
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val shockMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7043"); style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val pawShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 42, 24, 16)
    }
    private val beanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C48B8B")
    }
    private val reticlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 6f
    }
    private val stareChargePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 213, 79); style = Paint.Style.STROKE; strokeWidth = 5f
    }
    private val stareBeamPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stareBeamCorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // A static dash: the pulsing alpha carries the urgency, and a marching phase
    // would mean a fresh (immutable) DashPathEffect every frame.
    private val yarnLanePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F06292")
        style = Paint.Style.STROKE
        strokeWidth = 7f
        pathEffect = DashPathEffect(floatArrayOf(46f, 34f), 0f)
    }

    private val path = Path()
    private val rect = RectF()

    /** Where Kitty stands, and how big she is relative to her hazard self. */
    private val bossX = BOSS_X
    private val bossY = BOSS_Y
    private val bossScale = 3.6f

    // =========================================================================
    // Public entry points, in the order GameSurfaceView draws them
    // =========================================================================

    /** A soft arena vignette so the fight reads as its own space. */
    fun renderArena(canvas: Canvas, world: LivingRoomWorld) {
        val boss = world.boss
        val intensity = when (world.runMode) {
            RunMode.BOSS_INTRO -> KittyBoss.introRiseProgress(world.bossModeTimer)
            RunMode.BOSS_VICTORY -> 1f - KittyBoss.victoryBeatProgress(world.bossModeTimer) * 0.6f
            else -> 1f
        }
        tintPaint.color = when (boss.phase) {
            BossPhase.MILDLY_IRRITATED -> Color.argb((26 * intensity).toInt(), 120, 80, 160)
            BossPhase.VERY_ANNOYED -> Color.argb((36 * intensity).toInt(), 150, 70, 140)
            BossPhase.HAD_ENOUGH -> Color.argb((48 * intensity).toInt(), 180, 60, 90)
        }
        canvas.drawRect(0f, 0f, GameConstants.WORLD_WIDTH, GameConstants.WORLD_HEIGHT, tintPaint)

        // Arena bounds, drawn faintly so the dodgeable band is never ambiguous.
        canvas.drawLine(0f, GameConstants.BOSS_ARENA_TOP, GameConstants.WORLD_WIDTH,
            GameConstants.BOSS_ARENA_TOP, arenaEdgePaint)
        canvas.drawLine(0f, GameConstants.BOSS_ARENA_BOTTOM, GameConstants.WORLD_WIDTH,
            GameConstants.BOSS_ARENA_BOTTOM, arenaEdgePaint)
    }

    /**
     * Kitty herself. Handles the intro rise, the fight, and all three victory
     * beats, because she is a single continuous performance across them.
     */
    fun renderKitty(canvas: Canvas, world: LivingRoomWorld) {
        val boss = world.boss
        val t = world.bossModeTimer

        // Vertical offset: she rises out of frame during the intro, and slumps /
        // exits during the victory sequence.
        var offsetY = 0f
        var alpha = 1f
        when (world.runMode) {
            RunMode.BOSS_INTRO -> {
                val rise = KittyBoss.introRiseProgress(t)
                // Ease-out so the last of the rise settles rather than snapping.
                val eased = 1f - (1f - rise) * (1f - rise)
                offsetY = (1f - eased) * 780f
            }
            RunMode.BOSS_VICTORY -> {
                val beat = KittyBoss.victoryBeatAt(t)
                val p = KittyBoss.victoryBeatProgress(t)
                if (beat == VictoryBeat.REVEAL) {
                    // She leaves in whichever undignified way she picked.
                    when (boss.exit) {
                        KittyExit.WADDLE_AWAY -> offsetY = p * p * 620f
                        KittyExit.BECOME_LOAF -> offsetY = p * 40f
                        KittyExit.GROOM_AND_IGNORE -> offsetY = 0f
                    }
                    if (boss.exit == KittyExit.WADDLE_AWAY) alpha = (1f - p * 0.9f).coerceIn(0f, 1f)
                }
            }
            else -> {}
        }
        if (alpha <= 0.02f) return

        canvas.save()
        canvas.translate(bossX, bossY + offsetY)

        // Ground shadow, shrinking as she rises so she reads as grounded.
        val shadowSpread = 1f - (offsetY / 780f).coerceIn(0f, 1f)
        bossShadow.alpha = (60 * shadowSpread * alpha).toInt()
        canvas.drawOval(-300f * shadowSpread, 300f, 300f * shadowSpread, 372f, bossShadow)

        canvas.scale(bossScale, bossScale)

        // A slow, heavy breath, plus a jolt when a beam has just landed.
        val breath = sin(boss.animTimer * 2.2f) * 4f
        if (boss.isShocked) {
            val jolt = sin(boss.animTimer * 40f) * boss.shockTimer * 5f
            canvas.translate(jolt, 0f)
        }

        drawBossBody(canvas, world, breath, alpha)
        canvas.restore()

        // The fluff cloud sits above her, so it is drawn after the body.
        if (world.runMode == RunMode.BOSS_VICTORY) drawVictoryFluff(canvas, world)
    }

    /** Telegraph bands, live strikes, and the limbs that deliver them. */
    fun renderAttack(canvas: Canvas, world: LivingRoomWorld) {
        if (world.runMode != RunMode.BOSS_FIGHT) return
        val attack = world.boss.currentAttack ?: return
        if (attack.type == BossAttackType.GROOMING) return

        val telegraphing = attack.phase == AttackPhase.TELEGRAPH
        val striking = attack.phase == AttackPhase.STRIKE
        if (!telegraphing && !striking) return

        // The corridor is highlighted during the wind-up: the player should be
        // reading "go there", not "avoid that".
        if (telegraphing) drawSafeCorridors(canvas, attack.zones)

        for (zone in attack.zones) {
            drawDangerBand(canvas, zone, attack, telegraphing, striking)
        }

        when (attack.type) {
            BossAttackType.GIANT_PAW_SWAT, BossAttackType.DOUBLE_SWAT ->
                for (zone in attack.zones) drawGiantPaw(canvas, world, zone, attack)
            BossAttackType.TAIL_SWEEP ->
                attack.zones.firstOrNull()?.let { drawTailSweep(canvas, world, it, attack) }
            BossAttackType.KITTY_STARE ->
                attack.zones.firstOrNull()?.let { drawStare(canvas, world, it, attack) }
            BossAttackType.YARN_BARRAGE ->
                if (telegraphing) for (zone in attack.zones) drawYarnLaneMarker(canvas, world, zone, attack)
            BossAttackType.GROOMING -> {}
        }
    }

    /** Yarn balls and Blue Eye Energy orbs. */
    fun renderProjectiles(canvas: Canvas, world: LivingRoomWorld) {
        val boss = world.boss

        for (ball in boss.yarn) {
            if (ball.spent) continue
            canvas.save()
            canvas.translate(ball.x, ball.y)
            bossShadow.alpha = 50
            canvas.drawOval(-ball.radius, ball.radius * 0.6f, ball.radius, ball.radius * 1.15f, bossShadow)
            canvas.rotate(ball.spin)
            canvas.drawCircle(0f, 0f, ball.radius, yarnBody)
            canvas.drawCircle(0f, 0f, ball.radius, outline.also { it.strokeWidth = 5f })
            // Wound strands, so the spin is visible at speed.
            canvas.drawArc(-ball.radius * 0.8f, -ball.radius * 0.8f, ball.radius * 0.8f,
                ball.radius * 0.8f, 20f, 180f, false, yarnStrand)
            canvas.drawArc(-ball.radius * 0.5f, -ball.radius * 0.9f, ball.radius * 0.5f,
                ball.radius * 0.9f, 200f, 170f, false, yarnStrand)
            canvas.restore()
        }
        outline.strokeWidth = 9f

        for (orb in boss.orbs) {
            if (orb.collected) continue
            val bob = sin(orb.bobPhase) * 12f
            val pulse = 1f + sin(orb.bobPhase * 2.2f) * 0.09f
            val cy = orb.y + bob
            canvas.drawCircle(orb.x, cy, orb.radius * 1.45f * pulse, orbHalo)
            canvas.drawCircle(orb.x, cy, orb.radius * pulse, orbCore)
            canvas.drawCircle(orb.x - orb.radius * 0.22f, cy - orb.radius * 0.24f,
                orb.radius * 0.42f * pulse, orbInner)
            // A tiny cat-eye slit, so the orbs read as "Herbert's power", not generic pickups.
            canvas.drawOval(orb.x - 4f, cy - orb.radius * 0.5f, orb.x + 4f, cy + orb.radius * 0.5f, pupil)
        }
    }

    /**
     * Herbert's two ridiculous eye beams. Drawn after Herbert so they read as
     * coming out of his face, and they always reach all the way to Kitty.
     */
    fun renderEyeBeams(canvas: Canvas, world: LivingRoomWorld) {
        val boss = world.boss
        if (!boss.beamActive) return

        val h = world.herbert
        val originX = h.x + 44f
        val originY = h.y - h.jumpHeight - 28f
        val p = boss.beamProgress

        // Snap on, hold, then fade - a beam should never look like it is easing in.
        val strength = when {
            p < 0.12f -> p / 0.12f
            p > 0.72f -> ((1f - p) / 0.28f).coerceIn(0f, 1f)
            else -> 1f
        }
        if (strength <= 0.01f) return

        val flicker = 1f + sin(boss.animTimer * 60f) * 0.06f
        val reach = bossX + 120f - originX

        for (side in intArrayOf(-1, 1)) {
            val eyeY = originY + side * 11f
            val targetY = boss.gazeY - (boss.gazeY - eyeY) * 0.35f
            val halfOuter = GameConstants.BOSS_BEAM_HALF_HEIGHT * strength * flicker
            drawBeamWedge(canvas, originX, eyeY, reach, targetY, halfOuter, beamOuter)
            drawBeamWedge(canvas, originX, eyeY, reach, targetY, halfOuter * 0.6f, beamMid)
            drawBeamWedge(canvas, originX, eyeY, reach, targetY, halfOuter * 0.26f, beamCore)
        }

        // Muzzle flare at his eyes and a burst where it lands on Kitty.
        val flare = 46f * strength * flicker
        canvas.drawCircle(originX, originY, flare, beamOuter)
        canvas.drawCircle(originX, originY, flare * 0.5f, beamCore)

        val impactX = bossX - 150f
        val impactY = boss.gazeY
        canvas.drawCircle(impactX, impactY, 90f * strength * flicker, beamOuter)
        canvas.drawCircle(impactX, impactY, 52f * strength, beamMid)
        for (i in 0 until 9) {
            val a = boss.animTimer * 5f + i * 0.7f
            val r = 90f + sin(a * 3f) * 40f
            canvas.drawCircle(impactX + cos(a) * r, impactY + sin(a) * r, 11f * strength, sparkPaint)
        }
    }

    // =========================================================================
    // Kitty's body
    // =========================================================================

    private fun drawBossBody(canvas: Canvas, world: LivingRoomWorld, breath: Float, alpha: Float) {
        val boss = world.boss
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        setAlpha(a)

        val victoryBeat = if (world.runMode == RunMode.BOSS_VICTORY)
            KittyBoss.victoryBeatAt(world.bossModeTimer) else null
        val blasted = victoryBeat == VictoryBeat.BLAST
        val defeated = victoryBeat != null
        val loafing = defeated && boss.exit == KittyExit.BECOME_LOAF &&
            victoryBeat == VictoryBeat.REVEAL

        // A blast shoves her back and squashes her; a loaf flattens her.
        if (blasted) {
            val p = KittyBoss.victoryBeatProgress(world.bossModeTimer)
            canvas.translate(sin(p * 30f) * 9f, 0f)
            canvas.scale(1f + p * 0.06f, 1f - p * 0.08f)
        }
        if (loafing) canvas.scale(1.12f, 0.72f)

        // --- Tail ---
        val tailWag = sin(boss.animTimer * (if (boss.phase == BossPhase.HAD_ENOUGH) 7f else 3.4f)) *
            (if (boss.phase == BossPhase.HAD_ENOUGH) 26f else 14f)
        path.reset()
        path.moveTo(46f, 16f)
        path.quadTo(84f, 6f + tailWag, 100f, 24f + tailWag)
        outline.strokeWidth = 20f
        canvas.drawPath(path, outline)
        tabbyBase.style = Paint.Style.STROKE
        tabbyBase.strokeWidth = 14f
        tabbyBase.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, tabbyBase)
        tabbyBase.style = Paint.Style.FILL
        outline.strokeWidth = 9f

        // --- Body ---
        rect.set(-58f, -24f + breath * 0.4f, 58f, 44f + breath)
        canvas.drawRoundRect(rect, 40f, 40f, tabbyBase)
        canvas.drawRoundRect(rect, 40f, 40f, outline)

        tabbyStripe.strokeWidth = 7f
        canvas.drawLine(-26f, -13f, -26f, 15f, tabbyStripe)
        canvas.drawLine(-6f, -15f, -6f, 17f, tabbyStripe)
        canvas.drawLine(16f, -13f, 16f, 15f, tabbyStripe)
        canvas.drawLine(36f, -9f, 36f, 13f, tabbyStripe)

        rect.set(-50f, 10f, 10f, 40f)
        canvas.drawRoundRect(rect, 20f, 20f, bellyCream)

        // --- Front paws (one is busy if she is grooming) ---
        if (!loafing) {
            canvas.drawOval(-44f, 32f, -16f, 52f, tabbyShade)
            canvas.drawOval(-44f, 32f, -16f, 52f, outline)
            if (boss.isGrooming) {
                // Paw raised to her mouth, mid-wash.
                val lick = sin(boss.animTimer * 7f) * 5f
                canvas.drawOval(-40f, -34f + lick, -14f, -12f + lick, tabbyShade)
                canvas.drawOval(-40f, -34f + lick, -14f, -12f + lick, outline)
            } else {
                canvas.drawOval(14f, 32f, 42f, 52f, tabbyShade)
                canvas.drawOval(14f, 32f, 42f, 52f, outline)
            }
        }

        // --- Head ---
        val headX = -30f
        val headY = -18f
        val headR = 40f

        drawEars(canvas, headX, headY, boss.earFlatten, defeated)

        canvas.drawCircle(headX, headY, headR, tabbyBase)
        canvas.drawCircle(headX, headY, headR, outline)

        // Forehead "M"
        path.reset()
        path.moveTo(headX - 15f, headY - 27f)
        path.lineTo(headX - 7f, headY - 35f)
        path.lineTo(headX, headY - 27f)
        path.lineTo(headX + 7f, headY - 35f)
        path.lineTo(headX + 15f, headY - 27f)
        tabbyStripe.strokeWidth = 4.5f
        canvas.drawPath(path, tabbyStripe)

        rect.set(headX - 19f, headY + 8f, headX + 19f, headY + 29f)
        canvas.drawRoundRect(rect, 15f, 15f, muzzleCream)

        path.reset()
        path.moveTo(headX - 5f, headY + 12f)
        path.lineTo(headX + 5f, headY + 12f)
        path.lineTo(headX, headY + 19f)
        path.close()
        canvas.drawPath(path, nosePink)

        drawMouth(canvas, headX, headY, boss, blasted, loafing)
        drawEyes(canvas, headX, headY, world, defeated, blasted)

        whisker.strokeWidth = 3.4f
        for (dir in intArrayOf(-1, 1)) {
            canvas.drawLine(headX + dir * 15f, headY + 16f, headX + dir * 48f, headY + 13f, whisker)
            canvas.drawLine(headX + dir * 15f, headY + 21f, headX + dir * 50f, headY + 25f, whisker)
        }

        drawMood(canvas, headX, headY, world, defeated)
        setAlpha(255)
    }

    private fun drawEars(canvas: Canvas, hx: Float, hy: Float, flatten: Float, defeated: Boolean) {
        // Ears rotate outward and down as she gets more fed up. Fully flat by
        // HAD_ENOUGH, and flatter still once she has given up entirely.
        val f = if (defeated) 1f else flatten
        for (side in intArrayOf(-1, 1)) {
            canvas.save()
            canvas.translate(hx + side * 14f, hy - 28f)
            canvas.rotate(side * f * 62f)
            path.reset()
            path.moveTo(side * -8f, 0f)
            path.lineTo(side * 2f, -34f)
            path.lineTo(side * 18f, -2f)
            path.close()
            canvas.drawPath(path, earOuter)
            canvas.drawPath(path, outline)

            path.reset()
            path.moveTo(side * -3f, -3f)
            path.lineTo(side * 3f, -25f)
            path.lineTo(side * 12f, -4f)
            path.close()
            canvas.drawPath(path, earInner)
            canvas.restore()
        }
    }

    private fun drawEyes(
        canvas: Canvas, hx: Float, hy: Float,
        world: LivingRoomWorld, defeated: Boolean, blasted: Boolean
    ) {
        val boss = world.boss
        val eyeY = hy - 3f
        val spread = 15f

        if (blasted) {
            // Eyes screwed shut against the beam.
            for (side in intArrayOf(-1, 1)) {
                path.reset()
                path.moveTo(hx + side * spread - 11f, eyeY - 4f)
                path.quadTo(hx + side * spread, eyeY + 7f, hx + side * spread + 11f, eyeY - 4f)
                outline.strokeWidth = 5f
                canvas.drawPath(path, outline)
            }
            outline.strokeWidth = 9f
            return
        }

        // Her pupils track Herbert. gazeY is world-space, so convert it into the
        // local, scaled space we are drawing in.
        val gazeLocal = ((boss.gazeY - bossY) / bossScale).coerceIn(-60f, 60f)
        val look = (gazeLocal / 60f) * 4.5f

        // Blown pupils while startled; heavy unamused lids the rest of the time.
        val shockAmount = if (boss.isShocked) (boss.shockTimer / 1.1f).coerceIn(0f, 1f) else 0f
        val eyeR = 13f + shockAmount * 2.5f
        val pupilW = 3.2f + shockAmount * 5.5f
        val hotEye = boss.phase == BossPhase.HAD_ENOUGH && !defeated

        for (side in intArrayOf(-1, 1)) {
            val ex = hx + side * spread
            canvas.drawCircle(ex, eyeY, eyeR, if (hotEye) eyeGoldHot else eyeGold)
            outline.strokeWidth = 4f
            canvas.drawCircle(ex, eyeY, eyeR, outline)
            canvas.drawOval(ex - pupilW, eyeY - 10f + look, ex + pupilW, eyeY + 10f + look, pupil)

            // The lid: lower when she is unimpressed, up when startled.
            val lidDrop = if (defeated) 9f else (10f - shockAmount * 9f)
            if (lidDrop > 0.5f) {
                rect.set(ex - eyeR - 1.5f, eyeY - eyeR - 4f, ex + eyeR + 1.5f, eyeY - eyeR + lidDrop)
                canvas.drawRect(rect, tabbyBase)
                canvas.drawLine(ex - eyeR, eyeY - eyeR + lidDrop, ex + eyeR,
                    eyeY - eyeR + lidDrop, outline)
            }
        }
        outline.strokeWidth = 9f
    }

    private fun drawMouth(
        canvas: Canvas, hx: Float, hy: Float,
        boss: KittyBoss, blasted: Boolean, loafing: Boolean
    ) {
        whisker.strokeWidth = 3.6f
        when {
            boss.isGrooming -> {
                // Tongue out, mid-lick. The whole point of the bit.
                canvas.drawOval(hx - 6f, hy + 21f, hx + 6f, hy + 33f, tonguePink)
                canvas.drawOval(hx - 6f, hy + 21f, hx + 6f, hy + 33f,
                    outline.also { it.strokeWidth = 3f })
                outline.strokeWidth = 9f
            }
            blasted -> {
                // A flat, resigned line. She is not hurt, just extremely over it.
                canvas.drawLine(hx - 10f, hy + 26f, hx + 10f, hy + 26f, whisker)
            }
            loafing -> {
                path.reset()
                path.moveTo(hx - 9f, hy + 24f)
                path.quadTo(hx, hy + 29f, hx + 9f, hy + 24f)
                canvas.drawPath(path, whisker)
            }
            else -> {
                // Default: a small downturned frown.
                path.reset()
                path.moveTo(hx - 9f, hy + 27f)
                path.quadTo(hx, hy + 21f, hx + 9f, hy + 27f)
                canvas.drawPath(path, whisker)
            }
        }
    }

    /** Floating "Zzz", sweat drops, sparkles - whatever suits the moment. */
    private fun drawMood(
        canvas: Canvas, hx: Float, hy: Float,
        world: LivingRoomWorld, defeated: Boolean
    ) {
        val boss = world.boss
        val textPaint = moodTextPaint
        val bob = sin(boss.animTimer * 3.2f) * 5f

        if (defeated) {
            if (boss.exit == KittyExit.GROOM_AND_IGNORE &&
                KittyBoss.victoryBeatAt(world.bossModeTimer) == VictoryBeat.REVEAL
            ) {
                canvas.drawText("hmph", hx + 4f, hy - 58f + bob, textPaint)
            }
            return
        }

        when {
            boss.isShocked -> {
                // Cartoon shock marks.
                for (i in 0 until 3) {
                    val a = -0.9f + i * 0.55f
                    canvas.drawLine(
                        hx + cos(a) * 48f, hy + sin(a) * 48f - 14f,
                        hx + cos(a) * 66f, hy + sin(a) * 66f - 14f, shockMarkPaint
                    )
                }
            }
            boss.isGrooming -> canvas.drawText("(busy)", hx + 2f, hy - 56f + bob, textPaint)
            boss.phase == BossPhase.HAD_ENOUGH ->
                canvas.drawText("!!", hx + 2f, hy - 56f + bob, textPaint)
        }
    }

    private fun drawVictoryFluff(canvas: Canvas, world: LivingRoomWorld) {
        val beat = KittyBoss.victoryBeatAt(world.bossModeTimer)
        val p = KittyBoss.victoryBeatProgress(world.bossModeTimer)

        // A comedic puff of loose fur hides her, then clears to reveal her fine.
        val cover = when (beat) {
            VictoryBeat.BLAST -> p
            VictoryBeat.SMOKE -> 1f
            VictoryBeat.REVEAL -> (1f - p * 2.4f).coerceIn(0f, 1f)
        }
        if (cover <= 0.02f) return

        fluffPaint.alpha = (215 * cover).toInt()
        val t = world.boss.animTimer
        for (i in 0 until 16) {
            val a = i * 0.3925f + t * 0.5f
            val r = (150f + sin(a * 2.3f + t) * 60f) * cover
            val puffR = (60f + sin(a * 3.1f) * 26f) * cover
            canvas.drawCircle(bossX + cos(a) * r, bossY - 30f + sin(a) * r * 0.8f, puffR, fluffPaint)
        }
        fluffPaint.alpha = 255
    }

    // =========================================================================
    // Attacks
    // =========================================================================

    private fun drawSafeCorridors(canvas: Canvas, zones: List<DangerZone>) {
        for ((top, bottom) in KittyBoss.safeGaps(zones)) {
            if (bottom - top < 120f) continue
            canvas.drawRect(0f, top, BAND_RIGHT, bottom, safeGlow)
            canvas.drawLine(0f, top + 2f, BAND_RIGHT, top + 2f, safeEdge)
            canvas.drawLine(0f, bottom - 2f, BAND_RIGHT, bottom - 2f, safeEdge)
        }
    }

    private fun drawDangerBand(
        canvas: Canvas, zone: DangerZone, attack: BossAttack,
        telegraphing: Boolean, striking: Boolean
    ) {
        val top = max(zone.top, -60f)
        val bottom = zone.bottom
        // The band stops short of Kitty. It marks the row Herbert has to clear,
        // and running it under her only tints her with her own telegraph.
        val w = BAND_RIGHT

        if (telegraphing) {
            // Grows in confidence as the strike approaches, so the wind-up is
            // legible as a countdown rather than a static warning.
            val p = attack.phaseProgress
            val pulse = 0.45f + 0.55f * abs(sin(p * 11f))
            telegraphFill.alpha = (54 * pulse).toInt() + 16
            canvas.drawRect(0f, top, w, bottom, telegraphFill)

            // Hazard stripes, marching toward the player.
            canvas.save()
            canvas.clipRect(0f, top, w, bottom)
            telegraphStripe.alpha = (58 * pulse).toInt()
            val shift = (p * 220f) % 90f
            var x = -140f - shift
            while (x < w + 140f) {
                canvas.drawLine(x, bottom + 40f, x + 120f, top - 40f, telegraphStripe)
                x += 90f
            }
            canvas.restore()

            telegraphEdge.alpha = (150 + 105 * pulse).toInt().coerceAtMost(255)
            canvas.drawLine(0f, top, w, top, telegraphEdge)
            canvas.drawLine(0f, bottom, w, bottom, telegraphEdge)
        } else if (striking) {
            val p = attack.phaseProgress
            val flash = (1f - p).coerceIn(0f, 1f)
            strikeFill.alpha = (105 + 90 * flash).toInt().coerceAtMost(255)
            canvas.drawRect(0f, top, w, bottom, strikeFill)
            strikeEdge.alpha = 255
            canvas.drawLine(0f, top, w, top, strikeEdge)
            canvas.drawLine(0f, bottom, w, bottom, strikeEdge)
        }
    }

    /**
     * The paw that delivers a swat.
     *
     * It hangs raised during the wind-up, casting a growing shadow on the band,
     * then SLAMS down onto the band and stays there for the whole strike. It
     * deliberately does not sweep across: the simulation makes the entire row
     * dangerous for the full strike, so a paw travelling left to right would
     * tell the player the left of the screen is still safe when it is not.
     */
    private fun drawGiantPaw(
        canvas: Canvas, world: LivingRoomWorld, zone: DangerZone, attack: BossAttack
    ) {
        val p = attack.phaseProgress
        val striking = attack.phase == AttackPhase.STRIKE

        // DOUBLE_SWAT's outer zones run past the arena on purpose, so clamp the
        // paw itself back into view - the band is the truth, this is decoration.
        val cy = zone.centerY.coerceIn(
            GameConstants.BOSS_ARENA_TOP + 60f,
            GameConstants.BOSS_ARENA_BOTTOM - 60f
        )
        val half = (zone.height / 2f).coerceIn(110f, 185f)
        val pawX = GameConstants.WORLD_WIDTH * 0.46f

        if (!striking) {
            // Wind-up: the paw is up and back, and its shadow closes on the band.
            val liftedY = cy - half * 2.4f - 40f
            pawShadowPaint.color = Color.argb((40 + 70 * p).toInt().coerceIn(0, 255), 42, 24, 16)
            val sw = half * (0.55f + 0.5f * p)
            canvas.drawOval(pawX - sw * 1.15f, cy - sw * 0.42f, pawX + sw * 0.85f, cy + sw * 0.42f, pawShadowPaint)
            drawPawLimb(canvas, pawX, liftedY, half * 0.85f, showClaws = false, squash = 1f)
            return
        }

        // Impact: overshoot, then settle.
        val land = (p / 0.18f).coerceIn(0f, 1f)
        val squash = 1f + (1f - land) * 0.26f
        drawPawLimb(canvas, pawX, cy, half, showClaws = true, squash = squash)
    }

    /** One paw pad plus the limb reaching back to Kitty, so it reads as hers. */
    private fun drawPawLimb(
        canvas: Canvas, cx: Float, cy: Float, half: Float,
        showClaws: Boolean, squash: Float
    ) {
        // The limb: a thick, round-capped stroke back to her shoulder.
        val shoulderX = bossX - 30f
        val shoulderY = bossY - 40f
        outline.strokeWidth = half * 1.02f + 16f
        outline.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx, cy, shoulderX, shoulderY, outline)
        tabbyShade.style = Paint.Style.STROKE
        tabbyShade.strokeWidth = half * 1.02f
        tabbyShade.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx, cy, shoulderX, shoulderY, tabbyShade)
        tabbyShade.style = Paint.Style.FILL
        outline.strokeWidth = 9f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(1f / squash, squash)

        // Claws sit under the pad so only their tips show.
        if (showClaws) {
            for (i in 0 until 3) {
                val by = -half * 0.52f + i * half * 0.52f
                path.reset()
                path.moveTo(-half * 0.9f, by - half * 0.15f)
                path.lineTo(-half * 1.52f, by)
                path.lineTo(-half * 0.9f, by + half * 0.15f)
                path.close()
                canvas.drawPath(path, clawWhite)
                outline.strokeWidth = 5f
                canvas.drawPath(path, outline)
            }
            outline.strokeWidth = 9f
        }

        // The pad itself.
        canvas.drawOval(-half * 1.12f, -half, half * 0.88f, half, tabbyBase)
        outline.strokeWidth = 10f
        canvas.drawOval(-half * 1.12f, -half, half * 0.88f, half, outline)
        outline.strokeWidth = 9f

        // Toe beans, because she is still a cat.
        canvas.drawOval(-half * 0.58f, -half * 0.30f, -half * 0.06f, half * 0.36f, beanPaint)
        for (i in 0 until 3) {
            val by = -half * 0.52f + i * half * 0.52f
            canvas.drawOval(-half * 0.94f, by - half * 0.17f, -half * 0.60f, by + half * 0.17f, beanPaint)
        }

        canvas.restore()
    }

    private fun drawTailSweep(
        canvas: Canvas, world: LivingRoomWorld, zone: DangerZone, attack: BossAttack
    ) {
        val p = attack.phaseProgress
        val cy = zone.centerY
        val sweepX = if (attack.phase == AttackPhase.TELEGRAPH) {
            GameConstants.WORLD_WIDTH + 200f - p * 160f
        } else {
            GameConstants.WORLD_WIDTH + 40f - (p * 1.5f).coerceIn(0f, 1f) * (GameConstants.WORLD_WIDTH + 300f)
        }

        // One long, heavy tail arcing across the floor of the arena.
        path.reset()
        path.moveTo(GameConstants.WORLD_WIDTH + 380f, cy - 40f)
        path.quadTo((sweepX + GameConstants.WORLD_WIDTH) / 2f, cy - 110f, sweepX, cy + 10f)
        outline.strokeWidth = 78f
        canvas.drawPath(path, outline)
        tabbyBase.style = Paint.Style.STROKE
        tabbyBase.strokeWidth = 62f
        tabbyBase.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, tabbyBase)
        tabbyBase.style = Paint.Style.FILL
        outline.strokeWidth = 9f

        // Tail rings.
        tabbyStripe.strokeWidth = 13f
        for (i in 1..4) {
            val rx = sweepX + i * 150f
            if (rx > GameConstants.WORLD_WIDTH + 300f) break
            canvas.drawLine(rx, cy - 34f, rx + 16f, cy + 22f, tabbyStripe)
        }
        tabbyStripe.strokeWidth = 16f
    }

    private fun drawStare(
        canvas: Canvas, world: LivingRoomWorld, zone: DangerZone, attack: BossAttack
    ) {
        val cy = zone.centerY
        val p = attack.phaseProgress

        if (attack.phase == AttackPhase.TELEGRAPH) {
            // A reticle that follows Herbert, then snaps shut when she commits.
            val locked = attack.locked
            val ring = if (locked) 70f else 120f - p * 40f
            val rp = reticlePaint
            rp.color = if (locked) Color.parseColor("#FF5252") else Color.parseColor("#FFC107")
            rp.strokeWidth = if (locked) 9f else 6f
            val rx = world.herbert.x + 120f
            canvas.drawCircle(rx, cy, ring, rp)
            canvas.drawCircle(rx, cy, ring * 0.55f, rp)
            canvas.drawLine(rx - ring * 1.5f, cy, rx - ring, cy, rp)
            canvas.drawLine(rx + ring, cy, rx + ring * 1.5f, cy, rp)
            canvas.drawLine(rx, cy - ring * 1.5f, rx, cy - ring, rp)
            canvas.drawLine(rx, cy + ring, rx, cy + ring * 1.5f, rp)

            // Her stare, drawn as two thin gold lines charging up.
            if (locked) {
                canvas.drawLine(bossX - 150f, cy - 12f, rx, cy - 6f, stareChargePaint)
                canvas.drawLine(bossX - 150f, cy + 12f, rx, cy + 6f, stareChargePaint)
            }
        } else {
            // The strike itself: two hot gold beams down the band.
            val flash = (1f - p).coerceIn(0f, 1f)
            stareBeamPaint.color = Color.argb((200 * flash).toInt().coerceIn(0, 255), 255, 213, 79)
            stareBeamCorePaint.color = Color.argb((230 * flash).toInt().coerceIn(0, 255), 255, 253, 231)
            canvas.drawRect(0f, cy - 46f, BAND_RIGHT, cy + 46f, stareBeamPaint)
            canvas.drawRect(0f, cy - 17f, BAND_RIGHT, cy + 17f, stareBeamCorePaint)
        }
    }

    private fun drawYarnLaneMarker(
        canvas: Canvas, world: LivingRoomWorld, zone: DangerZone, attack: BossAttack
    ) {
        // Dashed lane markers promise exactly which rows the yarn will roll down.
        val p = attack.phaseProgress
        yarnLanePaint.alpha = (110 + 90 * abs(sin(p * 12f))).toInt().coerceIn(0, 255)
        canvas.drawLine(0f, zone.centerY, BAND_RIGHT, zone.centerY, yarnLanePaint)
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** A beam that starts tight at the eye and opens out toward Kitty. */
    private fun drawBeamWedge(
        canvas: Canvas, x0: Float, y0: Float, reach: Float, yEnd: Float,
        halfEnd: Float, paint: Paint
    ) {
        val halfStart = (halfEnd * 0.3f).coerceAtLeast(3f)
        path.reset()
        path.moveTo(x0, y0 - halfStart)
        path.lineTo(x0 + reach, yEnd - halfEnd)
        path.lineTo(x0 + reach, yEnd + halfEnd)
        path.lineTo(x0, y0 + halfStart)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun setAlpha(a: Int) {
        tabbyBase.alpha = a; tabbyShade.alpha = a; tabbyStripe.alpha = a
        bellyCream.alpha = a; muzzleCream.alpha = a; earOuter.alpha = a
        earInner.alpha = a; eyeGold.alpha = a; eyeGoldHot.alpha = a
        pupil.alpha = a; nosePink.alpha = a; outline.alpha = a
        whisker.alpha = a; tonguePink.alpha = a; clawWhite.alpha = a
    }

    companion object {
        /** Kitty's anchor in world space. Particles and juice aim at this. */
        const val BOSS_X = 1600f

        /** Danger and safe bands span the play area, stopping before Kitty. */
        const val BAND_RIGHT = BOSS_X - 170f
        val BOSS_Y = (GameConstants.BOSS_ARENA_TOP + GameConstants.BOSS_ARENA_BOTTOM) / 2f + 60f
    }
}
