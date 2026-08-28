package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.cos
import kotlin.math.sin

class HerbertRenderer {

    // Authentic snowshoe Siamese / pointed kitten palette based on real Herbert photos:
    private val whiteFurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFDF9"); style = Paint.Style.FILL }
    private val creamBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F3E9DF"); style = Paint.Style.FILL } // Soft warm cream on back
    private val darkEarSealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2E1F1A"); style = Paint.Style.FILL } // Dark seal brown ears
    private val eyeMaskBrownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#684E3F"); style = Paint.Style.FILL } // Warm seal mask around eyes
    private val softTanBridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#A98A78"); style = Paint.Style.FILL }

    // Eyes: Distinct pale icy blue with thin darker blue outer ring
    private val paleIceBluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#98E4FF"); style = Paint.Style.FILL }
    private val irisRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00A2E8"); style = Paint.Style.FILL }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#16110E"); style = Paint.Style.FILL }
    private val eyeSparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }

    // Nose & Paw Beans
    private val pinkNosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6384"); style = Paint.Style.FILL }
    private val pinkBeansPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7997"); style = Paint.Style.FILL }
    private val pinkBellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFAAA6"); style = Paint.Style.FILL }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 255, 99, 132); style = Paint.Style.FILL }

    // Outlines & Whiskers
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2A1810")
        style = Paint.Style.STROKE
        strokeWidth = 6.5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val whiskerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4E342E")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(65, 42, 24, 16); style = Paint.Style.FILL }
    private val speedTrailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 0, 229, 255)
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }
    private val scratchArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 120, 88, 56)
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val zoomieAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(85, 0, 229, 255)
        style = Paint.Style.FILL
    }

    private val earPathLeft = Path()
    private val earPathRight = Path()
    private val innerEarPathLeft = Path()
    private val innerEarPathRight = Path()

    fun render(canvas: Canvas, herbert: Herbert) {
        val groundY = herbert.y
        val renderY = groundY - herbert.jumpHeight

        if (herbert.isStumbleInvulnerable && (herbert.animTimer * 18f).toInt() % 2 == 0) {
            return
        }

        // Dynamic Drop Shadow
        val jumpRatio = (herbert.jumpHeight / GameConstants.JUMP_MAX_HEIGHT).coerceIn(0f, 1f)
        val shadowScale = 1f - jumpRatio * 0.45f
        val shadowAlpha = (65f * (1f - jumpRatio * 0.5f)).toInt()
        shadowPaint.alpha = shadowAlpha
        val shadowWidth = 68f * shadowScale
        val shadowHeight = 24f * shadowScale
        canvas.drawOval(
            herbert.x - shadowWidth,
            groundY + 38f - (shadowHeight / 2f),
            herbert.x + shadowWidth,
            groundY + 38f + (shadowHeight / 2f),
            shadowPaint
        )

        // Speed Aura & Trails
        if (herbert.isMaxZoomies) {
            val auraPulse = 1f + sin(herbert.animTimer * 22f).toFloat() * 0.12f
            canvas.drawCircle(herbert.x, renderY, 82f * auraPulse, zoomieAuraPaint)

            for (i in 1..5) {
                val trailX = herbert.x - i * 38f
                val waveTop = sin((herbert.animTimer * 26f) + i * 1.5f).toFloat() * 12f
                val waveBot = cos((herbert.animTimer * 26f) + i * 1.5f).toFloat() * 12f
                canvas.drawLine(trailX, renderY - 22f + waveTop, trailX - 35f, renderY - 22f + waveTop, speedTrailPaint)
                canvas.drawLine(trailX, renderY + 22f + waveBot, trailX - 30f, renderY + 22f + waveBot, speedTrailPaint)
            }
        }

        canvas.save()
        canvas.translate(herbert.x, renderY)

        val legCycle = sin(herbert.animTimer * 24f).toFloat()
        val isScratching = herbert.animState == AnimationState.SCRATCHING
        // Both front paws windmill in tight circles, half a turn out of phase
        val scratchPhase = herbert.animTimer * 27f
        val bounceY = if (herbert.animState == AnimationState.RUNNING || herbert.animState == AnimationState.MAX_ZOOM_RUNNING) {
            kotlin.math.abs(sin(herbert.animTimer * 22f)).toFloat() * 10f
        } else 0f

        when (herbert.animState) {
            AnimationState.FLOPPED -> {
                canvas.rotate(112f)
                canvas.translate(5f, -15f)
            }
            AnimationState.STUMBLING -> {
                val shake = sin(herbert.animTimer * 30f).toFloat() * 8f
                canvas.rotate(shake)
                canvas.scale(0.95f, 1.05f)
            }
            AnimationState.JUMPING -> {
                canvas.rotate(-18f)
                canvas.scale(1.15f, 0.9f)
            }
            AnimationState.SKIDDING -> {
                canvas.rotate(herbert.skidAngle * 1.2f)
                canvas.scale(0.95f, 1.05f)
            }
            AnimationState.SCRATCHING -> {
                // Front end down, bum up, leaning into the lap around the patch
                val lean = sin(herbert.scratchAngle).toFloat() * -11f
                val judder = sin(herbert.animTimer * 27f).toFloat() * 2.5f
                canvas.rotate(9f + lean + judder)
                canvas.translate(0f, 8f)
                canvas.scale(1.03f, 0.95f)
            }
            else -> {
                canvas.translate(0f, -bounceY)
                val stretch = 1.0f + (bounceY / 75f)
                canvas.scale(stretch, 2.0f - stretch)
            }
        }

        // --- LAYER 1: Dark Seal Point Tail ---
        val tailWag = sin(herbert.animTimer * (if (isScratching) 30f else 20f)).toFloat() *
            (if (isScratching) 52f else 32f)
        val tailPath = Path().apply {
            moveTo(-55f, 8f)
            quadTo(-90f, -32f + tailWag, -112f, -18f + (tailWag * 1.4f))
        }
        outlinePaint.strokeWidth = 18f
        canvas.drawPath(tailPath, outlinePaint)
        darkEarSealPaint.style = Paint.Style.STROKE
        darkEarSealPaint.strokeWidth = 12f
        darkEarSealPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(tailPath, darkEarSealPaint)
        darkEarSealPaint.style = Paint.Style.FILL
        outlinePaint.strokeWidth = 6.5f

        // --- LAYER 2: Back Paws (Pure White "Mittens") ---
        if (herbert.animState != AnimationState.FLOPPED) {
            // Back paws stay planted while he scratches; they only brace him.
            val backPawX = if (isScratching) -40f else -38f + legCycle * 22f
            val frontPawX = if (isScratching) 30f + cos(scratchPhase + 1.4f).toFloat() * 10f
                            else 42f - legCycle * 25f
            val pawY = if (isScratching) 38f + sin(scratchPhase + 1.4f).toFloat() * 7f else 36f

            canvas.drawOval(backPawX - 16f, 36f - 10f, backPawX + 16f, 36f + 12f, whiteFurPaint)
            canvas.drawOval(backPawX - 16f, 36f - 10f, backPawX + 16f, 36f + 12f, outlinePaint)

            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, whiteFurPaint)
            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, outlinePaint)
        }

        // --- LAYER 3: Body (Cream back with pure white chest and tummy) ---
        val bodyRect = RectF(-62f, -24f, 48f, 40f)
        canvas.drawOval(bodyRect, creamBodyPaint)
        canvas.drawOval(bodyRect, outlinePaint)

        // White chest and front shoulder bib
        val chestBib = RectF(-10f, -22f, 44f, 32f)
        canvas.drawOval(chestBib, whiteFurPaint)

        // --- LAYER 4: Pink Belly ---
        val bellyRect = RectF(-38f, 4f, 25f, 36f)
        canvas.drawOval(bellyRect, pinkBellyPaint)

        // --- LAYER 5: Front Paws with Pink Beans ---
        if (herbert.animState != AnimationState.FLOPPED) {
            val backPawX = if (isScratching) -34f else -38f - legCycle * 18f
            val backPawY = if (isScratching) 40f else 38f

            // The whole point of the bit: the leading front paw rakes in a circle
            val frontPawX = if (isScratching) 50f + cos(scratchPhase).toFloat() * 19f
                            else 42f + legCycle * 20f
            val frontPawY = if (isScratching) 30f + sin(scratchPhase).toFloat() * 15f else 38f

            canvas.drawOval(backPawX - 16f, backPawY - 10f, backPawX + 16f, backPawY + 12f, whiteFurPaint)
            canvas.drawOval(backPawX - 16f, backPawY - 10f, backPawX + 16f, backPawY + 12f, outlinePaint)

            // Faint circular motion trail so the raking reads at speed
            if (isScratching) {
                canvas.drawArc(RectF(50f - 19f, 30f - 15f, 50f + 19f, 30f + 15f),
                    Math.toDegrees(scratchPhase.toDouble()).toFloat() + 40f, 190f, false, scratchArcPaint)
            }

            canvas.drawOval(frontPawX - 16f, frontPawY - 10f, frontPawX + 16f, frontPawY + 12f, whiteFurPaint)
            canvas.drawOval(frontPawX - 16f, frontPawY - 10f, frontPawX + 16f, frontPawY + 12f, outlinePaint)

            // Pink Paw Beans
            canvas.drawCircle(frontPawX, frontPawY + 4f, 4.5f, pinkBeansPaint)
            canvas.drawCircle(frontPawX - 6f, frontPawY - 1f, 2.5f, pinkBeansPaint)
            canvas.drawCircle(frontPawX + 6f, frontPawY - 1f, 2.5f, pinkBeansPaint)
        }

        // --- LAYER 6: Tall Seal Point Ears ---
        earPathLeft.reset()
        earPathLeft.moveTo(18f, -48f)
        earPathLeft.lineTo(30f, -88f)
        earPathLeft.lineTo(48f, -44f)
        earPathLeft.close()

        earPathRight.reset()
        earPathRight.moveTo(-12f, -45f)
        earPathRight.lineTo(-8f, -84f)
        earPathRight.lineTo(12f, -48f)
        earPathRight.close()

        canvas.drawPath(earPathLeft, darkEarSealPaint)
        canvas.drawPath(earPathLeft, outlinePaint)
        canvas.drawPath(earPathRight, darkEarSealPaint)
        canvas.drawPath(earPathRight, outlinePaint)

        // Pink inner ears
        innerEarPathLeft.reset()
        innerEarPathLeft.moveTo(24f, -50f)
        innerEarPathLeft.lineTo(32f, -76f)
        innerEarPathLeft.lineTo(42f, -48f)
        innerEarPathLeft.close()
        canvas.drawPath(innerEarPathLeft, pinkNosePaint)

        innerEarPathRight.reset()
        innerEarPathRight.moveTo(-4f, -48f)
        innerEarPathRight.lineTo(-3f, -72f)
        innerEarPathRight.lineTo(7f, -50f)
        innerEarPathRight.close()
        canvas.drawPath(innerEarPathRight, pinkNosePaint)

        // --- LAYER 7: Kitten Head & Real Snowshoe Siamese Mask ---
        val headRadius = 42f
        val headCenterX = 26f
        val headCenterY = -24f

        // Head Base (Cream-white)
        canvas.drawCircle(headCenterX, headCenterY, headRadius, whiteFurPaint)
        canvas.drawCircle(headCenterX, headCenterY, headRadius, outlinePaint)

        // The Distinct Brown Eye-Goggle Mask:
        // Surrounds both eyes from the ears down to the cheeks, leaving the central nose bridge and muzzle white
        val leftMaskRect = RectF(headCenterX - 30f, headCenterY - 20f, headCenterX - 4f, headCenterY + 12f)
        val rightMaskRect = RectF(headCenterX + 4f, headCenterY - 20f, headCenterX + 30f, headCenterY + 12f)
        canvas.drawOval(leftMaskRect, eyeMaskBrownPaint)
        canvas.drawOval(rightMaskRect, eyeMaskBrownPaint)

        // Soft forehead bridge shading
        val bridgeRect = RectF(headCenterX - 18f, headCenterY - 32f, headCenterX + 18f, headCenterY - 14f)
        canvas.drawOval(bridgeRect, softTanBridgePaint)

        // Distinct inverted "V" white blaze on face from forehead to nose
        val blazePath = Path().apply {
            moveTo(headCenterX, headCenterY - 30f)
            lineTo(headCenterX + 10f, headCenterY + 14f)
            lineTo(headCenterX - 10f, headCenterY + 14f)
            close()
        }
        canvas.drawPath(blazePath, whiteFurPaint)

        // Rosy blush
        canvas.drawCircle(headCenterX - 24f, headCenterY + 14f, 10f, blushPaint)
        canvas.drawCircle(headCenterX + 24f, headCenterY + 14f, 10f, blushPaint)

        // Whiskers
        canvas.drawLine(headCenterX + 22f, headCenterY + 8f, headCenterX + 52f, headCenterY + 2f, whiskerPaint)
        canvas.drawLine(headCenterX + 22f, headCenterY + 12f, headCenterX + 56f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX + 22f, headCenterY + 16f, headCenterX + 50f, headCenterY + 26f, whiskerPaint)

        canvas.drawLine(headCenterX - 22f, headCenterY + 8f, headCenterX - 52f, headCenterY + 2f, whiskerPaint)
        canvas.drawLine(headCenterX - 22f, headCenterY + 12f, headCenterX - 56f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX - 22f, headCenterY + 16f, headCenterX - 50f, headCenterY + 26f, whiskerPaint)

        // --- LAYER 8: Herbert's Pale Ice-Blue Eyes ---
        val eyeSize = 13.5f * herbert.eyeWideness
        val leftEyeX = headCenterX - 14f
        val rightEyeX = headCenterX + 16f
        val eyeY = headCenterY - 4f

        canvas.drawOval(leftEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, leftEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, paleIceBluePaint)
        canvas.drawOval(rightEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, rightEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, paleIceBluePaint)
        canvas.drawOval(leftEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, leftEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, outlinePaint.apply { strokeWidth = 3f })
        canvas.drawOval(rightEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, rightEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, outlinePaint)
        outlinePaint.strokeWidth = 6.5f

        canvas.drawCircle(leftEyeX, eyeY + 1f, eyeSize * 0.72f, irisRingPaint)
        canvas.drawCircle(rightEyeX, eyeY + 1f, eyeSize * 0.72f, irisRingPaint)

        val pupilRadius = (7.5f * herbert.eyeWideness).coerceAtMost(14f)
        canvas.drawCircle(leftEyeX + 1f, eyeY, pupilRadius, pupilPaint)
        canvas.drawCircle(rightEyeX + 1f, eyeY, pupilRadius, pupilPaint)

        canvas.drawCircle(leftEyeX - 3f, eyeY - 4f, 4.5f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX - 3f, eyeY - 4f, 4.5f, eyeSparklePaint)
        canvas.drawCircle(leftEyeX + 3.5f, eyeY + 3.5f, 2.2f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX + 3.5f, eyeY + 3.5f, 2.2f, eyeSparklePaint)

        // --- LAYER 9: Pink Button Nose & Cute Smile ---
        val nosePath = Path().apply {
            moveTo(headCenterX - 6f, headCenterY + 12f)
            lineTo(headCenterX + 6f, headCenterY + 12f)
            lineTo(headCenterX, headCenterY + 18f)
            close()
        }
        canvas.drawPath(nosePath, pinkNosePaint)
        canvas.drawPath(nosePath, outlinePaint.apply { strokeWidth = 2.5f })
        outlinePaint.strokeWidth = 6.5f

        val mouthPath = Path().apply {
            moveTo(headCenterX - 8f, headCenterY + 19f)
            quadTo(headCenterX - 4f, headCenterY + 24f, headCenterX, headCenterY + 19f)
            quadTo(headCenterX + 4f, headCenterY + 24f, headCenterX + 8f, headCenterY + 19f)
        }
        whiskerPaint.strokeWidth = 3.5f
        canvas.drawPath(mouthPath, whiskerPaint)

        canvas.restore()
    }
}
