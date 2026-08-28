package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.sin

class WorldRenderer {

    // Background & Floors
    private val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFEBE9"); style = Paint.Style.FILL } // warm parquet
    private val floorPlankLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7CCC8"); strokeWidth = 3f; style = Paint.Style.STROKE }
    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CFD8DC"); style = Paint.Style.FILL }
    private val baseboardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val baseboardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B0BEC5"); strokeWidth = 4f; style = Paint.Style.STROKE }

    // Rugs
    private val rugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80CBC4"); style = Paint.Style.FILL }
    private val rugPatternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4DB6AC"); style = Paint.Style.STROKE; strokeWidth = 4f }

    // Obstacle Paints
    private val slipperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7043"); style = Paint.Style.FILL }
    private val sockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#AB47BC"); style = Paint.Style.FILL }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7A15C"); style = Paint.Style.FILL }
    private val boxDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B8860B"); style = Paint.Style.FILL }
    private val cushionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#42A5F5"); style = Paint.Style.FILL }
    private val legWoodPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5D4037"); style = Paint.Style.FILL }

    // Pickups
    private val treatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFB300"); style = Paint.Style.FILL }
    private val mousePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#78909C"); style = Paint.Style.FILL }
    private val yarnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EC407A"); style = Paint.Style.FILL }

    // General outlines / shadows
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(50, 0, 0, 0); style = Paint.Style.FILL }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#37474F"); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 255, 255); style = Paint.Style.FILL }

    fun renderBackground(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT

        // Wall top header
        canvas.drawRect(0f, 0f, w, 110f, wallPaint)
        // Baseboard
        canvas.drawRect(0f, 110f, w, 130f, baseboardPaint)
        canvas.drawLine(0f, 130f, w, 130f, baseboardShadowPaint)

        // Wood floor
        canvas.drawRect(0f, 130f, w, h, floorPaint)

        // Horizontal plank seams
        for (y in 130..h.toInt() step 90) {
            canvas.drawLine(0f, y.toFloat(), w, y.toFloat(), floorPlankLinePaint)
        }

        // Animated vertical wood grain shift based on distance
        val offset = (world.score.distanceRun % 300f)
        for (x in -300..(w.toInt() + 300) step 240) {
            val drawX = x - offset
            canvas.drawLine(drawX, 130f, drawX, h, floorPlankLinePaint)
        }

        // Cozy Living Room Area Rug in midground
        val rugX = (1200f - (world.score.distanceRun * 0.8f % 2600f))
        val rugRect = RectF(rugX, 280f, rugX + 600f, 800f)
        canvas.drawRoundRect(rugRect, 25f, 25f, rugPaint)
        canvas.drawRoundRect(rugRect, 25f, 25f, rugPatternPaint)
        // Rug fringe
        for (fy in 300..780 step 20) {
            canvas.drawLine(rugX - 10f, fy.toFloat(), rugX, fy.toFloat(), baseboardPaint)
            canvas.drawLine(rugX + 600f, fy.toFloat(), rugX + 610f, fy.toFloat(), baseboardPaint)
        }
    }

    fun renderPickups(canvas: Canvas, pickups: List<Pickup>, herbertAnimTimer: Float) {
        for (pick in pickups) {
            if (pick.collected) continue

            // Float/bounce animation
            val floatOffset = sin(herbertAnimTimer * 8f + pick.id).toFloat() * 6f
            val px = pick.x
            val py = pick.y + floatOffset

            // Drop shadow
            canvas.drawOval(px - 22f, pick.y + 25f, px + 22f, pick.y + 35f, shadowPaint)

            when (pick.type) {
                PickupType.TREAT -> {
                    // Golden fish-shaped kitten treat
                    val treatPath = Path().apply {
                        moveTo(px - 18f, py)
                        quadTo(px, py - 16f, px + 18f, py)
                        quadTo(px, py + 16f, px - 18f, py)
                        // Fish tail
                        lineTo(px - 26f, py - 10f)
                        lineTo(px - 26f, py + 10f)
                        close()
                    }
                    canvas.drawPath(treatPath, treatPaint)
                    canvas.drawPath(treatPath, outlinePaint)
                    // Eye dot
                    canvas.drawCircle(px + 10f, py - 3f, 2.5f, outlinePaint)
                }
                PickupType.TOY_MOUSE -> {
                    // Cute plush mouse
                    val mouseRect = RectF(px - 20f, py - 14f, px + 20f, py + 14f)
                    canvas.drawOval(mouseRect, mousePaint)
                    canvas.drawOval(mouseRect, outlinePaint)
                    // Mouse ears
                    canvas.drawCircle(px + 6f, py - 12f, 7f, treatPaint)
                    // Tail
                    canvas.drawLine(px - 20f, py, px - 35f, py + 8f, outlinePaint)
                }
                PickupType.YARN_BALL -> {
                    // Bright yarn ball with string loops
                    canvas.drawCircle(px, py, 20f, yarnPaint)
                    canvas.drawCircle(px, py, 20f, outlinePaint)
                    // Highlight curve
                    canvas.drawArc(RectF(px - 14f, py - 14f, px + 14f, py + 14f), 200f, 100f, false, highlightPaint)
                    // Unspooled string trail
                    canvas.drawLine(px - 12f, py + 12f, px - 30f, py + 22f, yarnPaint.apply { style = Paint.Style.STROKE; strokeWidth = 4f })
                    yarnPaint.style = Paint.Style.FILL
                }
            }
        }
    }

    fun renderObstacles(canvas: Canvas, obstacles: List<Obstacle>) {
        for (obs in obstacles) {
            val ox = obs.x
            val oy = obs.y
            val w = obs.width
            val h = obs.height

            // Drop shadow
            canvas.drawOval(obs.left, obs.bottom - 10f, obs.right, obs.bottom + 15f, shadowPaint)

            when (obs.type) {
                ObstacleType.SLIPPER -> {
                    val rect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(rect, 20f, 20f, slipperPaint)
                    canvas.drawRoundRect(rect, 20f, 20f, outlinePaint)
                    // Slipper opening
                    canvas.drawOval(obs.left + 15f, obs.top + 8f, obs.left + 45f, obs.bottom - 8f, outlinePaint)
                }
                ObstacleType.SOCK_PILE -> {
                    // Soft pile of colorful socks
                    canvas.drawCircle(ox - 15f, oy + 5f, 24f, sockPaint)
                    canvas.drawCircle(ox + 12f, oy - 2f, 20f, slipperPaint)
                    canvas.drawCircle(ox, oy, 22f, yarnPaint)
                    canvas.drawCircle(ox, oy, 22f, outlinePaint)
                }
                ObstacleType.CARDBOARD_BOX -> {
                    val rect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRect(rect, boxPaint)
                    canvas.drawRect(rect, outlinePaint)
                    // Flaps
                    canvas.drawRect(obs.left, obs.top, obs.left + w * 0.45f, obs.top + 20f, boxDarkPaint)
                    canvas.drawRect(obs.right - w * 0.45f, obs.top, obs.right, obs.top + 20f, boxDarkPaint)
                    // "Cat" doodle on box
                    canvas.drawCircle(ox, oy + 10f, 12f, outlinePaint)
                }
                ObstacleType.COUCH_CUSHION -> {
                    val rect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(rect, 18f, 18f, cushionPaint)
                    canvas.drawRoundRect(rect, 18f, 18f, outlinePaint)
                    // Button tufts
                    canvas.drawCircle(ox - 30f, oy, 4f, outlinePaint)
                    canvas.drawCircle(ox + 30f, oy, 4f, outlinePaint)
                }
                ObstacleType.TABLE_LEG -> {
                    // Heavy polished wood leg from above/side
                    val rect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(rect, 10f, 10f, legWoodPaint)
                    canvas.drawRoundRect(rect, 10f, 10f, outlinePaint)
                }
            }
        }
    }
}
