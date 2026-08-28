package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.cos
import kotlin.math.sin

class HerbertRenderer {

    // Palette
    private val whiteFurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAFAFA"); style = Paint.Style.FILL }
    private val blackEarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#212121"); style = Paint.Style.FILL }
    private val blueEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00B0FF"); style = Paint.Style.FILL }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A237E"); style = Paint.Style.FILL }
    private val eyeSparklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val pinkNosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF80AB"); style = Paint.Style.FILL }
    private val pinkBellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD1DC"); style = Paint.Style.FILL }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#37474F"); style = Paint.Style.STROKE; strokeWidth = 5f }
    private val whiskerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#90A4AE"); style = Paint.Style.STROKE; strokeWidth = 3f }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 0, 0, 0); style = Paint.Style.FILL }
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 0, 229, 255); style = Paint.Style.STROKE; strokeWidth = 6f }

    private val earPathLeft = Path()
    private val earPathRight = Path()
    private val innerEarPathLeft = Path()
    private val innerEarPathRight = Path()

    fun render(canvas: Canvas, herbert: Herbert) {
        val groundY = herbert.y
        val renderY = groundY - herbert.jumpHeight

        // Drop shadow on floor
        val shadowScale = (1f - (herbert.jumpHeight / (GameConstants.JUMP_MAX_HEIGHT * 1.5f))).coerceIn(0.4f, 1f)
        canvas.drawOval(
            herbert.x - 55f * shadowScale,
            groundY + 35f,
            herbert.x + 55f * shadowScale,
            groundY + 50f,
            shadowPaint
        )

        // Speed trail during Maximum Zoomies
        if (herbert.isMaxZoomies) {
            for (i in 1..4) {
                val trailX = herbert.x - i * 35f
                val wave = sin((herbert.animTimer * 20f) + i).toFloat() * 10f
                canvas.drawLine(trailX, renderY - 10f + wave, trailX - 25f, renderY - 10f + wave, trailPaint)
                canvas.drawLine(trailX, renderY + 15f - wave, trailX - 20f, renderY + 15f - wave, trailPaint)
            }
        }

        canvas.save()
        canvas.translate(herbert.x, renderY)

        // Handle animation rotation / squashes
        val legCycle = sin(herbert.animTimer * 22f).toFloat()
        val bounceY = if (herbert.animState == AnimationState.RUNNING || herbert.animState == AnimationState.MAX_ZOOM_RUNNING) {
            kotlin.math.abs(sin(herbert.animTimer * 20f)).toFloat() * 8f
        } else 0f

        when (herbert.animState) {
            AnimationState.FLOPPED -> {
                // Flopped cute state (rolled over on back showing belly)
                canvas.rotate(105f)
                canvas.translate(0f, -10f)
            }
            AnimationState.JUMPING -> {
                canvas.rotate(-15f)
            }
            AnimationState.SKIDDING -> {
                canvas.rotate(herbert.skidAngle)
            }
            else -> {
                canvas.translate(0f, -bounceY)
            }
        }

        // 1. Tail (expressive kitten tail)
        val tailWag = sin(herbert.animTimer * 18f).toFloat() * 25f
        val tailPath = Path().apply {
            moveTo(-45f, 5f)
            quadTo(-75f, -25f + tailWag, -90f, -15f + (tailWag * 1.3f))
        }
        canvas.drawPath(tailPath, outlinePaint.apply { strokeWidth = 14f; strokeCap = Paint.Cap.ROUND })
        canvas.drawPath(tailPath, whiteFurPaint.apply { strokeWidth = 10f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND })
        outlinePaint.strokeWidth = 5f
        whiteFurPaint.style = Paint.Style.FILL

        // 2. Paws / Legs
        if (herbert.animState != AnimationState.FLOPPED) {
            // Back paw
            canvas.drawCircle(-30f + legCycle * 15f, 32f, 14f, whiteFurPaint)
            canvas.drawCircle(-30f + legCycle * 15f, 32f, 14f, outlinePaint)
            // Front paw
            canvas.drawCircle(35f - legCycle * 18f, 32f, 14f, whiteFurPaint)
            canvas.drawCircle(35f - legCycle * 18f, 32f, 14f, outlinePaint)
        }

        // 3. Body (plump kitten body)
        val bodyRect = RectF(-50f, -20f, 40f, 35f)
        canvas.drawOval(bodyRect, whiteFurPaint)
        canvas.drawOval(bodyRect, outlinePaint)

        // 4. Pink Belly (visible when jumping/flopping/running)
        val bellyRect = RectF(-30f, 2f, 20f, 30f)
        canvas.drawOval(bellyRect, pinkBellyPaint)

        // 5. Ears (black/dark ear tips)
        earPathLeft.reset()
        earPathLeft.moveTo(15f, -42f)
        earPathLeft.lineTo(25f, -75f)
        earPathLeft.lineTo(40f, -40f)
        earPathLeft.close()

        earPathRight.reset()
        earPathRight.moveTo(-10f, -40f)
        earPathRight.lineTo(-5f, -72f)
        earPathRight.lineTo(10f, -42f)
        earPathRight.close()

        canvas.drawPath(earPathLeft, blackEarPaint)
        canvas.drawPath(earPathLeft, outlinePaint)
        canvas.drawPath(earPathRight, blackEarPaint)
        canvas.drawPath(earPathRight, outlinePaint)

        // Inner ears
        innerEarPathLeft.reset()
        innerEarPathLeft.moveTo(20f, -44f)
        innerEarPathLeft.lineTo(26f, -65f)
        innerEarPathLeft.lineTo(35f, -43f)
        innerEarPathLeft.close()
        canvas.drawPath(innerEarPathLeft, pinkNosePaint)

        // 6. Head (oversized cute kitten head)
        canvas.drawCircle(22f, -22f, 36f, whiteFurPaint)
        canvas.drawCircle(22f, -22f, 36f, outlinePaint)

        // Dark marking spot on head
        val markingRect = RectF(5f, -50f, 22f, -38f)
        canvas.drawOval(markingRect, blackEarPaint)

        // 7. Whiskers
        canvas.drawLine(40f, -15f, 65f, -20f, whiskerPaint)
        canvas.drawLine(40f, -12f, 68f, -10f, whiskerPaint)
        canvas.drawLine(40f, -9f, 64f, 0f, whiskerPaint)

        // 8. Eyes (Striking blue kitten eyes, extra wide in Zoomies)
        val eyeSize = 10f * herbert.eyeWideness
        val leftEyeX = 22f
        val rightEyeX = 42f
        val eyeY = -24f

        canvas.drawOval(leftEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.1f, leftEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.1f, blueEyePaint)
        canvas.drawOval(rightEyeX - eyeSize * 0.9f, eyeY - eyeSize * 1.1f, rightEyeX + eyeSize * 0.9f, eyeY + eyeSize * 1.1f, blueEyePaint)

        // Pupils
        val pupilRadius = (6f * herbert.eyeWideness).coerceAtMost(11f)
        canvas.drawCircle(leftEyeX + 1f, eyeY, pupilRadius, pupilPaint)
        canvas.drawCircle(rightEyeX + 1f, eyeY, pupilRadius, pupilPaint)

        // Sparkle / Glint
        canvas.drawCircle(leftEyeX - 2f, eyeY - 3f, 3.5f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX - 2f, eyeY - 3f, 3.5f, eyeSparklePaint)
        canvas.drawCircle(leftEyeX + 2f, eyeY + 2f, 1.8f, eyeSparklePaint)
        canvas.drawCircle(rightEyeX + 2f, eyeY + 2f, 1.8f, eyeSparklePaint)

        // 9. Pink Nose & Mouth
        val nosePath = Path().apply {
            moveTo(36f, -12f)
            lineTo(42f, -12f)
            lineTo(39f, -8f)
            close()
        }
        canvas.drawPath(nosePath, pinkNosePaint)

        // Tiny mouth
        val mouthPath = Path().apply {
            moveTo(36f, -7f)
            quadTo(39f, -4f, 42f, -7f)
        }
        canvas.drawPath(mouthPath, outlinePaint.apply { strokeWidth = 3f })
        outlinePaint.strokeWidth = 5f

        canvas.restore()
    }
}
