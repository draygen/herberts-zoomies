package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.cos
import kotlin.math.sin

class HerbertRenderer {

    private val whiteFurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFDF9"); style = Paint.Style.FILL }
    private val softCreamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5EDE2"); style = Paint.Style.FILL }
    private val darkEarMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#261B16"); style = Paint.Style.FILL }
    private val warmMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4A3525"); style = Paint.Style.FILL }

    private val paleBlueEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80D8FF"); style = Paint.Style.FILL }
    private val deepBlueRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#0091EA"); style = Paint.Style.FILL }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#120D0A"); style = Paint.Style.FILL }
    private val eyeSparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }

    private val pinkNosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5277"); style = Paint.Style.FILL }
    private val pinkBeansPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7597"); style = Paint.Style.FILL }
    private val pinkBellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFAAA6"); style = Paint.Style.FILL }
    private val blushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 255, 105, 140); style = Paint.Style.FILL }

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

        if (herbert.isInvulnerable && (herbert.animTimer * 18f).toInt() % 2 == 0) {
            return
        }

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
            else -> {
                canvas.translate(0f, -bounceY)
                val stretch = 1.0f + (bounceY / 75f)
                canvas.scale(stretch, 2.0f - stretch)
            }
        }

        // Tail
        val tailWag = sin(herbert.animTimer * 20f).toFloat() * 32f
        val tailPath = Path().apply {
            moveTo(-55f, 8f)
            quadTo(-90f, -32f + tailWag, -112f, -18f + (tailWag * 1.4f))
        }
        outlinePaint.strokeWidth = 18f
        canvas.drawPath(tailPath, outlinePaint)
        darkEarMaskPaint.style = Paint.Style.STROKE
        darkEarMaskPaint.strokeWidth = 12f
        darkEarMaskPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(tailPath, darkEarMaskPaint)

        darkEarMaskPaint.style = Paint.Style.FILL
        outlinePaint.strokeWidth = 6.5f

        // Back Paws
        if (herbert.animState != AnimationState.FLOPPED) {
            val backPawX = -38f + legCycle * 22f
            val frontPawX = 42f - legCycle * 25f
            val pawY = 36f

            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, softCreamPaint)
            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, outlinePaint)

            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, softCreamPaint)
            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, outlinePaint)
        }

        // Mostly White Body
        val bodyRect = RectF(-62f, -24f, 48f, 40f)
        canvas.drawOval(bodyRect, whiteFurPaint)
        canvas.drawOval(bodyRect, outlinePaint)

        val backSpotRect = RectF(-48f, -24f, -15f, -6f)
        canvas.drawOval(backSpotRect, warmMaskPaint)

        // Pink Belly
        val bellyRect = RectF(-38f, 2f, 25f, 36f)
        canvas.drawOval(bellyRect, pinkBellyPaint)

        // Front Paws with Pink Beans
        if (herbert.animState != AnimationState.FLOPPED) {
            val backPawX = -38f - legCycle * 18f
            val frontPawX = 42f + legCycle * 20f
            val pawY = 38f

            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, whiteFurPaint)
            canvas.drawOval(backPawX - 16f, pawY - 10f, backPawX + 16f, pawY + 12f, outlinePaint)

            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, whiteFurPaint)
            canvas.drawOval(frontPawX - 16f, pawY - 10f, frontPawX + 16f, pawY + 12f, outlinePaint)

            canvas.drawCircle(frontPawX, pawY + 4f, 4.5f, pinkBeansPaint)
            canvas.drawCircle(frontPawX - 6f, pawY - 1f, 2.5f, pinkBeansPaint)
            canvas.drawCircle(frontPawX + 6f, pawY - 1f, 2.5f, pinkBeansPaint)
        }

        // Dark Ears
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

        canvas.drawPath(earPathLeft, darkEarMaskPaint)
        canvas.drawPath(earPathLeft, outlinePaint)
        canvas.drawPath(earPathRight, darkEarMaskPaint)
        canvas.drawPath(earPathRight, outlinePaint)

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

        // Head
        val headRadius = 42f
        val headCenterX = 26f
        val headCenterY = -24f
        canvas.drawCircle(headCenterX, headCenterY, headRadius, whiteFurPaint)
        canvas.drawCircle(headCenterX, headCenterY, headRadius, outlinePaint)

        val leftMaskRect = RectF(headCenterX - 28f, headCenterY - 18f, headCenterX - 4f, headCenterY + 12f)
        val rightMaskRect = RectF(headCenterX + 4f, headCenterY - 18f, headCenterX + 28f, headCenterY + 12f)
        canvas.drawOval(leftMaskRect, warmMaskPaint)
        canvas.drawOval(rightMaskRect, warmMaskPaint)

        canvas.drawCircle(headCenterX - 24f, headCenterY + 14f, 10f, blushPaint)
        canvas.drawCircle(headCenterX + 24f, headCenterY + 14f, 10f, blushPaint)

        val blazePath = Path().apply {
            moveTo(headCenterX - 8f, headCenterY - 34f)
            lineTo(headCenterX + 8f, headCenterY - 34f)
            lineTo(headCenterX + 12f, headCenterY + 10f)
            lineTo(headCenterX - 12f, headCenterY + 10f)
            close()
        }
        canvas.drawPath(blazePath, whiteFurPaint)

        // Whiskers
        canvas.drawLine(headCenterX + 22f, headCenterY + 8f, headCenterX + 52f, headCenterY + 2f, whiskerPaint)
        canvas.drawLine(headCenterX + 22f, headCenterY + 12f, headCenterX + 56f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX + 22f, headCenterY + 16f, headCenterX + 50f, headCenterY + 26f, whiskerPaint)

        canvas.drawLine(headCenterX - 22f, headCenterY + 8f, headCenterX - 52f, headCenterY + 2f, whiskerPaint)
        canvas.drawLine(headCenterX - 22f, headCenterY + 12f, headCenterX - 56f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX - 22f, headCenterY + 16f, headCenterX - 50f, headCenterY + 26f, whiskerPaint)

        // Pale Blue Eyes
        val eyeSize = 13f * herbert.eyeWideness
        val leftEyeX = headCenterX - 14f
        val rightEyeX = headCenterX + 16f
        val eyeY = headCenterY - 4f

        canvas.drawOval(leftEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, leftEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, paleBlueEyePaint)
        canvas.drawOval(rightEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, rightEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, paleBlueEyePaint)
        canvas.drawOval(leftEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, leftEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, outlinePaint.apply { strokeWidth = 3f })
        canvas.drawOval(rightEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.15f, rightEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.15f, outlinePaint)
        outlinePaint.strokeWidth = 6.5f

        canvas.drawCircle(leftEyeX, eyeY + 1f, eyeSize * 0.75f, deepBlueRingPaint)
        canvas.drawCircle(rightEyeX, eyeY + 1f, eyeSize * 0.75f, deepBlueRingPaint)

        val pupilRadius = (7.5f * herbert.eyeWideness).coerceAtMost(14f)
        canvas.drawCircle(leftEyeX + 1f, eyeY, pupilRadius, pupilPaint)
        canvas.drawCircle(rightEyeX + 1f, eyeY, pupilRadius, pupilPaint)

        canvas.drawCircle(leftEyeX - 3f, eyeY - 4f, 4.5f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX - 3f, eyeY - 4f, 4.5f, eyeSparklePaint)
        canvas.drawCircle(leftEyeX + 3.5f, eyeY + 3.5f, 2.2f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX + 3.5f, eyeY + 3.5f, 2.2f, eyeSparklePaint)

        // Pink Button Nose & Mouth
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
