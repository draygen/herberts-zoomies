package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.cos
import kotlin.math.sin

class KittyRenderer {

    private val tabbyBasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D7B68"); style = Paint.Style.FILL }
    private val tabbyShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6A5844"); style = Paint.Style.FILL }
    private val tabbyStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#423223")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
    }
    private val bellyCreamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7CCC8"); style = Paint.Style.FILL }
    private val earOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5D4037"); style = Paint.Style.FILL }
    private val earInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D1A7A0"); style = Paint.Style.FILL }

    private val eyeGoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C0CA33"); style = Paint.Style.FILL }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B1B1B"); style = Paint.Style.FILL }
    private val eyelidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D7B68"); style = Paint.Style.FILL }

    private val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#BCAAA4"); style = Paint.Style.FILL }
    private val muzzleCreamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFEBE9"); style = Paint.Style.FILL }

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D2013")
        style = Paint.Style.STROKE
        strokeWidth = 6.5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val whiskerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3E2723")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 42, 24, 16); style = Paint.Style.FILL }
    private val swatClawPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val swatWhooshPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 235, 59)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val earPathLeft = Path()
    private val earPathRight = Path()
    private val innerEarPathLeft = Path()
    private val innerEarPathRight = Path()

    fun render(canvas: Canvas, obstacle: Obstacle) {
        val ox = obstacle.x
        val oy = obstacle.y
        val isSleeping = obstacle.type == ObstacleType.KITTY_SLEEPING
        val isWaddling = obstacle.type == ObstacleType.KITTY_WADDLE

        val shadowW = 75f
        val shadowH = 28f
        canvas.drawOval(ox - shadowW, oy + 42f - (shadowH / 2f), ox + shadowW, oy + 42f + (shadowH / 2f), shadowPaint)

        canvas.save()
        canvas.translate(ox, oy)

        val breath = sin(obstacle.animTimer * 4f).toFloat() * 3f
        val waddleBounce = if (isWaddling) sin(obstacle.animTimer * 12f).toFloat() * 5f else 0f
        val waddleTilt = if (isWaddling) sin(obstacle.animTimer * 12f).toFloat() * 6f else 0f

        canvas.rotate(waddleTilt)
        canvas.translate(0f, -waddleBounce)

        // Tail
        val tailWag = sin(obstacle.animTimer * 5f).toFloat() * 12f
        val tailPath = Path().apply {
            moveTo(45f, 15f)
            quadTo(75f, 5f + tailWag, 90f, 22f + tailWag)
        }
        outlinePaint.strokeWidth = 16f
        canvas.drawPath(tailPath, outlinePaint)
        tabbyBasePaint.style = Paint.Style.STROKE
        tabbyBasePaint.strokeWidth = 11f
        tabbyBasePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(tailPath, tabbyBasePaint)

        canvas.drawLine(60f, 12f + tailWag * 0.5f, 63f, 20f + tailWag * 0.5f, tabbyStripePaint)
        canvas.drawLine(74f, 12f + tailWag * 0.8f, 77f, 22f + tailWag * 0.8f, tabbyStripePaint)

        tabbyBasePaint.style = Paint.Style.FILL
        outlinePaint.strokeWidth = 6.5f

        // Body
        val bodyRect = RectF(-55f, -22f + breath * 0.5f, 55f, 42f + breath)
        canvas.drawRoundRect(bodyRect, 38f, 38f, tabbyBasePaint)
        canvas.drawRoundRect(bodyRect, 38f, 38f, outlinePaint)

        canvas.drawLine(-25f, -12f, -25f, 14f, tabbyStripePaint)
        canvas.drawLine(-5f, -14f, -5f, 16f, tabbyStripePaint)
        canvas.drawLine(15f, -12f, 15f, 14f, tabbyStripePaint)
        canvas.drawLine(35f, -8f, 35f, 12f, tabbyStripePaint)

        val chestRect = RectF(-48f, 10f, 10f, 38f)
        canvas.drawRoundRect(chestRect, 20f, 20f, bellyCreamPaint)

        // Paws
        if (!isSleeping) {
            val pawY = 38f
            canvas.drawOval(-42f, pawY - 8f, -15f, pawY + 10f, tabbyShadePaint)
            canvas.drawOval(-42f, pawY - 8f, -15f, pawY + 10f, outlinePaint)

            canvas.drawOval(15f, pawY - 8f, 42f, pawY + 10f, tabbyShadePaint)
            canvas.drawOval(15f, pawY - 8f, 42f, pawY + 10f, outlinePaint)
        }

        // Head
        val headRadius = 38f
        val headCenterX = -28f
        val headCenterY = -14f

        earPathLeft.reset()
        earPathLeft.moveTo(headCenterX - 22f, headCenterY - 24f)
        earPathLeft.lineTo(headCenterX - 28f, headCenterY - 56f)
        earPathLeft.lineTo(headCenterX - 6f, headCenterY - 32f)
        earPathLeft.close()

        earPathRight.reset()
        earPathRight.moveTo(headCenterX + 6f, headCenterY - 32f)
        earPathRight.lineTo(headCenterX + 24f, headCenterY - 56f)
        earPathRight.lineTo(headCenterX + 22f, headCenterY - 24f)
        earPathRight.close()

        canvas.drawPath(earPathLeft, earOuterPaint)
        canvas.drawPath(earPathLeft, outlinePaint)
        canvas.drawPath(earPathRight, earOuterPaint)
        canvas.drawPath(earPathRight, outlinePaint)

        innerEarPathLeft.reset()
        innerEarPathLeft.moveTo(headCenterX - 20f, headCenterY - 27f)
        innerEarPathLeft.lineTo(headCenterX - 25f, headCenterY - 48f)
        innerEarPathLeft.lineTo(headCenterX - 9f, headCenterY - 32f)
        innerEarPathLeft.close()
        canvas.drawPath(innerEarPathLeft, earInnerPaint)

        innerEarPathRight.reset()
        innerEarPathRight.moveTo(headCenterX + 8f, headCenterY - 32f)
        innerEarPathRight.lineTo(headCenterX + 20f, headCenterY - 48f)
        innerEarPathRight.lineTo(headCenterX + 18f, headCenterY - 27f)
        innerEarPathRight.close()
        canvas.drawPath(innerEarPathRight, earInnerPaint)

        canvas.drawCircle(headCenterX, headCenterY, headRadius, tabbyBasePaint)
        canvas.drawCircle(headCenterX, headCenterY, headRadius, outlinePaint)

        val mPath = Path().apply {
            moveTo(headCenterX - 14f, headCenterY - 26f)
            lineTo(headCenterX - 7f, headCenterY - 34f)
            lineTo(headCenterX, headCenterY - 26f)
            lineTo(headCenterX + 7f, headCenterY - 34f)
            lineTo(headCenterX + 14f, headCenterY - 26f)
        }
        tabbyStripePaint.strokeWidth = 4f
        canvas.drawPath(mPath, tabbyStripePaint)
        tabbyStripePaint.strokeWidth = 6f

        val muzzleRect = RectF(headCenterX - 18f, headCenterY + 8f, headCenterX + 18f, headCenterY + 28f)
        canvas.drawRoundRect(muzzleRect, 14f, 14f, muzzleCreamPaint)

        val nosePath = Path().apply {
            moveTo(headCenterX - 5f, headCenterY + 12f)
            lineTo(headCenterX + 5f, headCenterY + 12f)
            lineTo(headCenterX, headCenterY + 18f)
            close()
        }
        canvas.drawPath(nosePath, nosePaint)

        val mouthPath = Path().apply {
            moveTo(headCenterX - 8f, headCenterY + 24f)
            quadTo(headCenterX, headCenterY + 20f, headCenterX + 8f, headCenterY + 24f)
        }
        whiskerPaint.strokeWidth = 3f
        canvas.drawPath(mouthPath, whiskerPaint)

        canvas.drawLine(headCenterX + 14f, headCenterY + 16f, headCenterX + 45f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX + 14f, headCenterY + 20f, headCenterX + 48f, headCenterY + 24f, whiskerPaint)
        canvas.drawLine(headCenterX - 14f, headCenterY + 16f, headCenterX - 45f, headCenterY + 14f, whiskerPaint)
        canvas.drawLine(headCenterX - 14f, headCenterY + 20f, headCenterX - 48f, headCenterY + 24f, whiskerPaint)

        if (isSleeping) {
            val sleepLeft = Path().apply {
                moveTo(headCenterX - 20f, headCenterY - 2f)
                quadTo(headCenterX - 12f, headCenterY + 4f, headCenterX - 4f, headCenterY - 2f)
            }
            val sleepRight = Path().apply {
                moveTo(headCenterX + 4f, headCenterY - 2f)
                quadTo(headCenterX + 12f, headCenterY + 4f, headCenterX + 20f, headCenterY - 2f)
            }
            canvas.drawPath(sleepLeft, outlinePaint.apply { strokeWidth = 4f })
            canvas.drawPath(sleepRight, outlinePaint)
            outlinePaint.strokeWidth = 6.5f

            val zPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#5C6BC0")
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val zOff = sin(obstacle.animTimer * 4f).toFloat() * 6f
            canvas.drawText("Z", headCenterX - 35f, headCenterY - 45f + zOff, zPaint)
            canvas.drawText("z", headCenterX - 48f, headCenterY - 60f + zOff, Paint(zPaint).apply { textSize = 20f })
        } else {
            val eyeX1 = headCenterX - 14f
            val eyeX2 = headCenterX + 14f
            val eyeY = headCenterY - 4f

            canvas.drawCircle(eyeX1, eyeY, 11f, eyeGoldPaint)
            canvas.drawCircle(eyeX2, eyeY, 11f, eyeGoldPaint)
            canvas.drawCircle(eyeX1, eyeY, 11f, outlinePaint.apply { strokeWidth = 3f })
            canvas.drawCircle(eyeX2, eyeY, 11f, outlinePaint)
            outlinePaint.strokeWidth = 6.5f

            canvas.drawOval(eyeX1 - 2.5f, eyeY - 8f, eyeX1 + 2.5f, eyeY + 8f, pupilPaint)
            canvas.drawOval(eyeX2 - 2.5f, eyeY - 8f, eyeX2 + 2.5f, eyeY + 8f, pupilPaint)

            canvas.drawArc(RectF(eyeX1 - 12f, eyeY - 14f, eyeX1 + 12f, eyeY + 8f), 180f, 180f, true, eyelidPaint)
            canvas.drawArc(RectF(eyeX1 - 12f, eyeY - 14f, eyeX1 + 12f, eyeY + 8f), 180f, 180f, false, outlinePaint.apply { strokeWidth = 3f })

            canvas.drawArc(RectF(eyeX2 - 12f, eyeY - 14f, eyeX2 + 12f, eyeY + 8f), 180f, 180f, true, eyelidPaint)
            canvas.drawArc(RectF(eyeX2 - 12f, eyeY - 14f, eyeX2 + 12f, eyeY + 8f), 180f, 180f, false, outlinePaint)
            outlinePaint.strokeWidth = 6.5f
        }

        if (obstacle.isKittySwatting) {
            val swatProgress = (obstacle.kittySwatTimer / 0.6f).coerceIn(0f, 1f)
            val swatArmX = -45f - (1f - swatProgress) * 35f
            val swatArmY = 10f - sin(swatProgress * Math.PI.toFloat()) * 25f

            canvas.drawOval(swatArmX - 18f, swatArmY - 12f, swatArmX + 18f, swatArmY + 12f, tabbyBasePaint)
            canvas.drawOval(swatArmX - 18f, swatArmY - 12f, swatArmX + 18f, swatArmY + 12f, outlinePaint)

            canvas.drawCircle(swatArmX - 14f, swatArmY - 4f, 3.5f, swatClawPaint)
            canvas.drawCircle(swatArmX - 16f, swatArmY + 2f, 3.5f, swatClawPaint)
            canvas.drawCircle(swatArmX - 14f, swatArmY + 8f, 3.5f, swatClawPaint)

            canvas.drawArc(RectF(swatArmX - 45f, swatArmY - 30f, swatArmX + 15f, swatArmY + 30f), 120f, 110f, false, swatWhooshPaint)
        }

        canvas.restore()
    }
}
