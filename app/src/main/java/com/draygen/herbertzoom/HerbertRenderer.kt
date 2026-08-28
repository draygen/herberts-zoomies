package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.cos
import kotlin.math.sin

class HerbertRenderer {

    // Palette
    private val whiteFurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFDF9"); style = Paint.Style.FILL }
    private val shadingFurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#ECE0D1"); style = Paint.Style.FILL }
    private val blackEarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1C1917"); style = Paint.Style.FILL }
    private val blueEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00B0FF"); style = Paint.Style.FILL }
    private val darkBlueIrisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#01579B"); style = Paint.Style.FILL }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0D1B2A"); style = Paint.Style.FILL }
    private val eyeSparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val pinkNosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF6584"); style = Paint.Style.FILL }
    private val pinkBellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFAAA6"); style = Paint.Style.FILL }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(110, 255, 128, 171); style = Paint.Style.FILL }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E1C14")
        style = Paint.Style.STROKE
        strokeWidth = 6.5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val whiskerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5D4037")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 46, 28, 20); style = Paint.Style.FILL }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 0, 229, 255)
        style = Paint.Style.STROKE
        strokeWidth = 9f
        strokeCap = Paint.Cap.ROUND
    }
    private val speedGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 0, 229, 255)
        style = Paint.Style.FILL
    }

    private val earPathLeft = Path()
    private val earPathRight = Path()
    private val innerEarPathLeft = Path()
    private val innerEarPathRight = Path()

    fun render(canvas: Canvas, herbert: Herbert) {
        val groundY = herbert.y
        val renderY = groundY - herbert.jumpHeight

        // 1. Dynamic Drop Shadow on Floor
        val jumpRatio = (herbert.jumpHeight / GameConstants.JUMP_MAX_HEIGHT).coerceIn(0f, 1f)
        val shadowScale = 1f - jumpRatio * 0.45f
        val shadowAlpha = (70f * (1f - jumpRatio * 0.5f)).toInt()
        shadowPaint.alpha = shadowAlpha
        val shadowWidth = 65f * shadowScale
        val shadowHeight = 22f * shadowScale
        canvas.drawOval(
            herbert.x - shadowWidth,
            groundY + 38f - (shadowHeight / 2f),
            herbert.x + shadowWidth,
            groundY + 38f + (shadowHeight / 2f),
            shadowPaint
        )

        // 2. Speed Aura and Trails during Maximum Zoomies
        if (herbert.isMaxZoomies) {
            // Pulsing aura behind Herbert
            val auraPulse = 1f + sin(herbert.animTimer * 20f).toFloat() * 0.15f
            canvas.drawCircle(herbert.x, renderY, 80f * auraPulse, speedGlowPaint)

            // Multiple lightning-fast speed streaks
            for (i in 1..5) {
                val trailX = herbert.x - i * 40f
                val waveTop = sin((herbert.animTimer * 24f) + i * 1.5f).toFloat() * 12f
                val waveBot = cos((herbert.animTimer * 24f) + i * 1.5f).toFloat() * 12f
                canvas.drawLine(trailX, renderY - 20f + waveTop, trailX - 35f, renderY - 20f + waveTop, trailPaint)
                canvas.drawLine(trailX, renderY + 20f + waveBot, trailX - 30f, renderY + 20f + waveBot, trailPaint)
            }
        }

        canvas.save()
        canvas.translate(herbert.x, renderY)

        // Squash and stretch animations
        val legCycle = sin(herbert.animTimer * 24f).toFloat()
        val bounceY = if (herbert.animState == AnimationState.RUNNING || herbert.animState == AnimationState.MAX_ZOOM_RUNNING) {
            kotlin.math.abs(sin(herbert.animTimer * 22f)).toFloat() * 10f
        } else 0f

        when (herbert.animState) {
            AnimationState.FLOPPED -> {
                // Completely flopped over onto back showing huge cute pink belly
                canvas.rotate(115f)
                canvas.translate(5f, -15f)
            }
            AnimationState.JUMPING -> {
                // Pouncing pose: stretched forward and tilted upward
                canvas.rotate(-18f)
                canvas.scale(1.15f, 0.9f)
            }
            AnimationState.SKIDDING -> {
                canvas.rotate(herbert.skidAngle * 1.3f)
                canvas.scale(0.95f, 1.05f)
            }
            else -> {
                // Running gallop bounce
                canvas.translate(0f, -bounceY)
                val stretch = 1.0f + (bounceY / 80f)
                canvas.scale(stretch, 2.0f - stretch)
            }
        }

        // --- LAYER 1: Expressive Tail ---
        val tailWag = sin(herbert.animTimer * 20f).toFloat() * 32f
        val tailPath = Path().apply {
            moveTo(-55f, 8f)
            quadTo(-90f, -30f + tailWag, -110f, -15f + (tailWag * 1.4f))
        }
        // Dark outline for tail
        outlinePaint.strokeWidth = 18f
        canvas.drawPath(tailPath, outlinePaint)
        // White inner fur
        whiteFurPaint.style = Paint.Style.STROKE
        whiteFurPaint.strokeWidth = 12f
        whiteFurPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(tailPath, whiteFurPaint)
        // Black tip on tail!
        blackEarPaint.style = Paint.Style.STROKE
        blackEarPaint.strokeWidth = 12f
        blackEarPaint.strokeCap = Paint.Cap.ROUND
        val tipPath = Path().apply {
            moveTo(-95f, -22f + (tailWag * 1.2f))
            lineTo(-110f, -15f + (tailWag * 1.4f))
        }
        canvas.drawPath(tipPath, blackEarPaint)

        // Reset paints
        whiteFurPaint.style = Paint.Style.FILL
        blackEarPaint.style = Paint.Style.FILL
        outlinePaint.strokeWidth = 6.5f

        // --- LAYER 2: Back Paws (when running) ---
        if (herbert.animState != AnimationState.FLOPPED) {
            val backPawX = -38f + legCycle * 22f
            val frontPawX = 42f - legCycle * 25f
            val pawY = 36f

            // Shaded rear paws
            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, shadingFurPaint)
            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, outlinePaint)

            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, shadingFurPaint)
            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, outlinePaint)
        }

        // --- LAYER 3: Plump Body ---
        val bodyRect = RectF(-62f, -24f, 48f, 40f)
        canvas.drawOval(bodyRect, whiteFurPaint)
        canvas.drawOval(bodyRect, outlinePaint)

        // Dark back patch / spots based on Herbert's coat
        val backSpotRect = RectF(-45f, -24f, -10f, -5f)
        canvas.drawOval(backSpotRect, blackEarPaint)

        // --- LAYER 4: Big Cute Pink Belly ---
        val bellyRect = RectF(-38f, 2f, 25f, 36f)
        canvas.drawOval(bellyRect, pinkBellyPaint)

        // --- LAYER 5: Front Paws ---
        if (herbert.animState != AnimationState.FLOPPED) {
            val backPawX = -38f - legCycle * 18f
            val frontPawX = 42f + legCycle * 20f
            val pawY = 38f

            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, whiteFurPaint)
            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, outlinePaint)

            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, whiteFurPaint)
            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, outlinePaint)
            // Tiny cute toe beans
            canvas.drawCircle(frontPawX, pawY + 4f, 4f, pinkNosePaint)
        }

        // --- LAYER 6: Ears (Distinct black/dark ear markings) ---
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

        canvas.drawPath(earPathLeft, blackEarPaint)
        canvas.drawPath(earPathLeft, outlinePaint)
        canvas.drawPath(earPathRight, blackEarPaint)
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

        // --- LAYER 7: Large Kitten Head ---
        val headRadius = 42f
        val headCenterX = 26f
        val headCenterY = -24f
        canvas.drawCircle(headCenterX, headCenterY, headRadius, whiteFurPaint)
        canvas.drawCircle(headCenterX, headCenterY, headRadius, outlinePaint)

        // Head forehead dark patch
        val foreHeadSpot = RectF(6f, -58f, 28f, -42f)
        canvas.drawOval(foreHeadSpot, blackEarPaint)

        // Cheerful rosy cheeks / blush
        canvas.drawCircle(headCenterX - 24f, headCenterY + 14f, 11f, blushPaint)
        canvas.drawCircle(headCenterX + 24f, headCenterY + 14f, 11f, blushPaint)

        // --- LAYER 8: Whiskers ---
        canvas.drawLine(headCenterX + 22f, headCenterY + 8f, headCenterX + 52f, headCenterY + 2f, whiskerPaint)
        canvas.drawLine(headCenterX + 22f, headCenterY + 12f, headCenterX + 56f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX + 22f, headCenterY + 16f, headCenterX + 50f, headCenterY + 26f, whiskerPaint)

        canvas.drawLine(headCenterX - 22f, headCenterY + 8f, headCenterX - 52f, headCenterY + 2f, whiskerPaint)
        canvas.drawLine(headCenterX - 22f, headCenterY + 12f, headCenterX - 56f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX - 22f, headCenterY + 16f, headCenterX - 50f, headCenterY + 26f, whiskerPaint)

        // --- LAYER 9: Striking Big Blue Eyes ---
        val eyeSize = 13f * herbert.eyeWideness
        val leftEyeX = headCenterX - 14f
        val rightEyeX = headCenterX + 16f
        val eyeY = headCenterY - 4f

        // Outer blue iris
        canvas.drawOval(leftEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, leftEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, blueEyePaint)
        canvas.drawOval(rightEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, rightEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, blueEyePaint)
        canvas.drawOval(leftEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, leftEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, outlinePaint.apply { strokeWidth = 3f })
        canvas.drawOval(rightEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, rightEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, outlinePaint)
        outlinePaint.strokeWidth = 6.5f

        // Deep blue inner shadow
        canvas.drawCircle(leftEyeX, eyeY + 2f, eyeSize * 0.7f, darkBlueIrisPaint)
        canvas.drawCircle(rightEyeX, eyeY + 2f, eyeSize * 0.7f, darkBlueIrisPaint)

        // Black pupils (huge dilated eyes during Zoomies)
        val pupilRadius = (7.5f * herbert.eyeWideness).coerceAtMost(14f)
        canvas.drawCircle(leftEyeX + 1f, eyeY, pupilRadius, pupilPaint)
        canvas.drawCircle(rightEyeX + 1f, eyeY, pupilRadius, pupilPaint)

        // Bright anime gleams / sparkles
        canvas.drawCircle(leftEyeX - 3f, eyeY - 4f, 4.5f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX - 3f, eyeY - 4f, 4.5f, eyeSparklePaint)
        canvas.drawCircle(leftEyeX + 3.5f, eyeY + 3.5f, 2.2f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX + 3.5f, eyeY + 3.5f, 2.2f, eyeSparklePaint)

        // --- LAYER 10: Pink Button Nose & Cat Mouth (:3) ---
        val nosePath = Path().apply {
            moveTo(headCenterX - 6f, headCenterY + 12f)
            lineTo(headCenterX + 6f, headCenterY + 12f)
            lineTo(headCenterX, headCenterY + 18f)
            close()
        }
        canvas.drawPath(nosePath, pinkNosePaint)
        canvas.drawPath(nosePath, outlinePaint.apply { strokeWidth = 2.5f })
        outlinePaint.strokeWidth = 6.5f

        // W-shaped :3 smile
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
