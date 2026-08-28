package com.draygen.herbertzoom

import android.graphics.*
import com.draygen.herbertzoom.core.*
import kotlin.math.cos
import kotlin.math.sin

class WorldRenderer {

    // Background Room Paints
    private val ceilingWallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF3E0"); style = Paint.Style.FILL } // Soft peach wall
    private val wallpaperStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE0B2"); style = Paint.Style.STROKE; strokeWidth = 22f }
    private val pictureFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8D6E63"); style = Paint.Style.FILL }
    private val pictureCanvasPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B2DFDB"); style = Paint.Style.FILL }
    private val baseboardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFFF"); style = Paint.Style.FILL }
    private val baseboardMoldingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7CCC8"); strokeWidth = 4f; style = Paint.Style.STROKE }

    // Parquet Floor
    private val floorPlankDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DEB887"); style = Paint.Style.FILL } // Burlywood warm wood
    private val floorPlankLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5DEB3"); style = Paint.Style.FILL } // Wheat wood
    private val floorGroovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C49A6C"); strokeWidth = 3.5f; style = Paint.Style.STROKE }

    // Background Furniture Silhouettes (Wall Layer)
    private val sofaBackdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#81C784"); style = Paint.Style.FILL } // Sage green couch in bg
    private val sofaPillowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F"); style = Paint.Style.FILL }
    private val plantPotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7043"); style = Paint.Style.FILL }
    private val plantLeafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#388E3C"); style = Paint.Style.FILL }
    private val windowGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(40, 255, 255, 200); style = Paint.Style.FILL }

    // Decorative Rugs
    private val rugBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4DB6AC"); style = Paint.Style.FILL } // Teal rug
    private val rugInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80CBC4"); style = Paint.Style.FILL }
    private val rugBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E0F2F1"); strokeWidth = 6f; style = Paint.Style.STROKE }
    private val rugTasselPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF9C4"); strokeWidth = 4f; style = Paint.Style.STROKE }

    // Obstacle Paints
    private val boxBrownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7A15C"); style = Paint.Style.FILL }
    private val boxDarkBrownPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B8860B"); style = Paint.Style.FILL }
    private val cushionTealPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#26A69A"); style = Paint.Style.FILL }
    private val cushionHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#80CBC4"); style = Paint.Style.FILL }
    private val slipperCoralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF7043"); style = Paint.Style.FILL }
    private val slipperFluffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF3E0"); style = Paint.Style.FILL }
    private val sockPinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EC407A"); style = Paint.Style.FILL }
    private val sockBluePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#42A5F5"); style = Paint.Style.FILL }
    private val sockStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 4f }
    private val postRopePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#D7CCC8"); style = Paint.Style.FILL }
    private val woodTrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D4C41"); style = Paint.Style.FILL }
    private val tunnelFabricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#AB47BC"); style = Paint.Style.FILL }
    private val tunnelHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4A148C"); style = Paint.Style.FILL }

    // Pickup Paints
    private val fishGoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFA000"); style = Paint.Style.FILL }
    private val fishHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFD54F"); style = Paint.Style.FILL }
    private val mouseGreyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#90A4AE"); style = Paint.Style.FILL }
    private val mousePinkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF80AB"); style = Paint.Style.FILL }
    private val yarnMagentaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F06292"); style = Paint.Style.FILL }
    private val yarnStrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#AD1457"); style = Paint.Style.STROKE; strokeWidth = 4.5f }

    // Outlines and Shadows
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E1C14")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val softShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 46, 28, 20); style = Paint.Style.FILL }
    private val pickupGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(120, 255, 255, 255); style = Paint.Style.FILL }

    fun renderBackground(canvas: Canvas, world: LivingRoomWorld) {
        val w = GameConstants.WORLD_WIDTH
        val h = GameConstants.WORLD_HEIGHT
        val wallHeight = 220f
        val baseboardHeight = 35f

        // 1. Cozy Pastel Wallpaper & Stripes
        canvas.drawRect(0f, 0f, w, wallHeight, ceilingWallPaint)
        for (x in 0..w.toInt() step 70) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), wallHeight, wallpaperStripePaint)
        }

        // Parallax Wall Decorations (Pictures, Bookshelf, Window)
        val wallParallax = (world.score.distanceRun * 0.25f % (w + 800f))

        // Wall Picture 1: Cat Portrait
        val picX1 = (400f - wallParallax)
        if (picX1 in -200f..w + 200f) {
            val frameRect = RectF(picX1, 40f, picX1 + 120f, 150f)
            canvas.drawRoundRect(frameRect, 8f, 8f, pictureFramePaint)
            canvas.drawRoundRect(RectF(picX1 + 10f, 50f, picX1 + 110f, 140f), 4f, 4f, pictureCanvasPaint)
            canvas.drawRoundRect(frameRect, 8f, 8f, outlinePaint.apply { strokeWidth = 3f })
            outlinePaint.strokeWidth = 6f
            // Little paw doodle on picture
            canvas.drawCircle(picX1 + 60f, 95f, 16f, plantPotPaint)
        }

        // Wall Window with soft outdoor glow
        val winX = (1300f - wallParallax)
        if (winX in -300f..w + 300f) {
            val winRect = RectF(winX, 30f, winX + 220f, 170f)
            canvas.drawRoundRect(winRect, 12f, 12f, baseboardPaint)
            canvas.drawRoundRect(RectF(winX + 12f, 42f, winX + 208f, 158f), 6f, 6f, pictureCanvasPaint)
            // Window mullions
            canvas.drawLine(winX + 110f, 42f, winX + 110f, 158f, baseboardMoldingPaint)
            canvas.drawLine(winX + 12f, 100f, winX + 208f, 100f, baseboardMoldingPaint)
            canvas.drawRoundRect(winRect, 12f, 12f, outlinePaint.apply { strokeWidth = 3.5f })
            outlinePaint.strokeWidth = 6f
        }

        // 2. White Clean Baseboard & Molding
        canvas.drawRect(0f, wallHeight, w, wallHeight + baseboardHeight, baseboardPaint)
        canvas.drawLine(0f, wallHeight, w, wallHeight, baseboardMoldingPaint)
        canvas.drawLine(0f, wallHeight + baseboardHeight, w, wallHeight + baseboardHeight, outlinePaint.apply { strokeWidth = 4f })
        outlinePaint.strokeWidth = 6f

        // 3. Warm Alternating Parquet Hardwood Floor
        val floorTop = wallHeight + baseboardHeight
        canvas.drawRect(0f, floorTop, w, h, floorPlankDarkPaint)

        // Alternating wood plank rows
        val plankHeight = 110f
        var row = 0
        for (py in floorTop.toInt()..h.toInt() step plankHeight.toInt()) {
            val isAlt = (row % 2 == 1)
            val plankPaint = if (isAlt) floorPlankLightPaint else floorPlankDarkPaint
            canvas.drawRect(0f, py.toFloat(), w, py + plankHeight, plankPaint)
            canvas.drawLine(0f, py.toFloat(), w, py.toFloat(), floorGroovePaint)

            // Animated horizontal plank joints scrolling with Herbert's run
            val rowOffset = (world.score.distanceRun * 1.0f + (row * 160f)) % 400f
            for (px in -400..(w.toInt() + 400) step 380) {
                val jx = px - rowOffset
                canvas.drawLine(jx, py.toFloat(), jx, py + plankHeight, floorGroovePaint)
            }
            row++
        }

        // 4. Parallax Living Room Area Rugs
        val rugScroll = (world.score.distanceRun * 0.85f % 3200f)
        val rx = (2000f - rugScroll)
        if (rx in -900f..w + 900f) {
            val rRect = RectF(rx, 350f, rx + 750f, 920f)
            // Soft floor shadow for rug
            canvas.drawRoundRect(RectF(rx + 8f, 358f, rx + 758f, 928f), 30f, 30f, softShadowPaint)
            // Rug outer body
            canvas.drawRoundRect(rRect, 30f, 30f, rugBodyPaint)
            // Rug inner decorative panel
            canvas.drawRoundRect(RectF(rx + 35f, 385f, rx + 715f, 885f), 20f, 20f, rugInnerPaint)
            canvas.drawRoundRect(RectF(rx + 35f, 385f, rx + 715f, 885f), 20f, 20f, rugBorderPaint)
            // Rug Fringe / Tassels
            for (fy in 370..900 step 22) {
                canvas.drawLine(rx - 14f, fy.toFloat(), rx, fy.toFloat(), rugTasselPaint)
                canvas.drawLine(rx + 750f, fy.toFloat(), rx + 764f, fy.toFloat(), rugTasselPaint)
            }
        }
    }

    fun renderPickups(canvas: Canvas, pickups: List<Pickup>, animTimer: Float) {
        for (pick in pickups) {
            if (pick.collected) continue

            // Bobbing & scaling animation
            val bob = sin(animTimer * 7f + pick.id).toFloat() * 10f
            val pulse = 1f + sin(animTimer * 10f + pick.id).toFloat() * 0.08f
            val px = pick.x
            val py = pick.y + bob

            // Ground shadow
            canvas.drawOval(px - 32f, pick.y + 35f, px + 32f, pick.y + 48f, softShadowPaint)

            // Sparkle Glow Backdrop
            canvas.drawCircle(px, py, 45f * pulse, pickupGlowPaint)

            canvas.save()
            canvas.translate(px, py)
            canvas.scale(pulse, pulse)

            when (pick.type) {
                PickupType.TREAT -> {
                    // Big delicious golden fish treat
                    val fishPath = Path().apply {
                        moveTo(-28f, 0f)
                        quadTo(0f, -24f, 28f, 0f)
                        quadTo(0f, 24f, -28f, 0f)
                        // Tail fin
                        lineTo(-42f, -16f)
                        lineTo(-42f, 16f)
                        close()
                    }
                    canvas.drawPath(fishPath, fishGoldPaint)
                    canvas.drawPath(fishPath, outlinePaint)

                    // Highlights
                    val hiPath = Path().apply {
                        moveTo(-14f, -6f)
                        quadTo(4f, -16f, 20f, -4f)
                    }
                    canvas.drawPath(hiPath, fishHighlightPaint.apply { style = Paint.Style.STROKE; strokeWidth = 5f })
                    fishHighlightPaint.style = Paint.Style.FILL

                    // Cute treat eye
                    canvas.drawCircle(15f, -4f, 4f, outlinePaint.apply { style = Paint.Style.FILL })
                    canvas.drawCircle(14f, -5f, 1.5f, Paint().apply { color = Color.WHITE; style = Paint.Style.FILL })
                    outlinePaint.style = Paint.Style.STROKE
                }

                PickupType.TOY_MOUSE -> {
                    // Cute plush mouse toy with pink ears & curly tail
                    val mouseBody = RectF(-30f, -20f, 30f, 20f)
                    canvas.drawOval(mouseBody, mouseGreyPaint)
                    canvas.drawOval(mouseBody, outlinePaint)

                    // Ears
                    canvas.drawCircle(10f, -18f, 10f, mousePinkPaint)
                    canvas.drawCircle(10f, -18f, 10f, outlinePaint)

                    // Nose
                    canvas.drawCircle(29f, 2f, 4f, mousePinkPaint)

                    // Curly spring tail
                    val tail = Path().apply {
                        moveTo(-30f, 2f)
                        quadTo(-50f, -18f, -45f, 10f)
                        quadTo(-40f, 24f, -58f, 18f)
                    }
                    canvas.drawPath(tail, outlinePaint.apply { strokeWidth = 4.5f })
                    outlinePaint.strokeWidth = 6f
                }

                PickupType.YARN_BALL -> {
                    // Vibrant yarn ball with textured weaves
                    canvas.drawCircle(0f, 0f, 30f, yarnMagentaPaint)
                    canvas.drawCircle(0f, 0f, 30f, outlinePaint)

                    // Woven strands
                    canvas.drawArc(RectF(-25f, -25f, 25f, 25f), 30f, 120f, false, yarnStrandPaint)
                    canvas.drawArc(RectF(-22f, -22f, 22f, 22f), 200f, 110f, false, yarnStrandPaint)
                    canvas.drawArc(RectF(-15f, -15f, 15f, 15f), 120f, 90f, false, yarnStrandPaint)

                    // Trailing thread
                    val thread = Path().apply {
                        moveTo(-15f, 20f)
                        quadTo(-35f, 35f, -50f, 20f)
                        quadTo(-65f, 5f, -75f, 25f)
                    }
                    canvas.drawPath(thread, yarnStrandPaint)
                }
            }

            canvas.restore()
        }
    }

    private val kittyRenderer = KittyRenderer()

    fun renderObstacles(canvas: Canvas, obstacles: List<Obstacle>) {
        for (obs in obstacles) {
            val ox = obs.x
            val oy = obs.y
            val w = obs.width
            val h = obs.height

            if (obs.isKitty) {
                kittyRenderer.render(canvas, obs)
                continue
            }

            // Drop shadow
            canvas.drawOval(obs.left - 5f, obs.bottom - 12f, obs.right + 5f, obs.bottom + 18f, softShadowPaint)


            when (obs.type) {
                ObstacleType.SLIPPER -> {
                    // Fluffy household slipper
                    val slipperRect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(slipperRect, 24f, 24f, slipperCoralPaint)
                    canvas.drawRoundRect(slipperRect, 24f, 24f, outlinePaint)
                    // Fluffy inner opening
                    val openRect = RectF(obs.left + 18f, obs.top + 10f, obs.left + 54f, obs.bottom - 10f)
                    canvas.drawOval(openRect, slipperFluffPaint)
                    canvas.drawOval(openRect, outlinePaint.apply { strokeWidth = 4f })
                    outlinePaint.strokeWidth = 6f
                }

                ObstacleType.SOCK_PILE -> {
                    // Soft cozy sock pile
                    canvas.drawCircle(ox - 22f, oy + 8f, 30f, sockPinkPaint)
                    canvas.drawCircle(ox - 22f, oy + 8f, 30f, outlinePaint)

                    canvas.drawCircle(ox + 18f, oy - 6f, 26f, sockBluePaint)
                    canvas.drawCircle(ox + 18f, oy - 6f, 26f, outlinePaint)

                    canvas.drawCircle(ox, oy + 2f, 28f, slipperCoralPaint)
                    canvas.drawCircle(ox, oy + 2f, 28f, outlinePaint)
                    // Sock stripes
                    canvas.drawLine(ox - 16f, oy, ox + 16f, oy, sockStripePaint)
                }

                ObstacleType.CARDBOARD_BOX -> {
                    // Amazon / Delivery Cardboard Box with tape & flaps
                    val boxRect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(boxRect, 8f, 8f, boxBrownPaint)
                    canvas.drawRoundRect(boxRect, 8f, 8f, outlinePaint)

                    // Open flaps at top
                    val flap1 = RectF(obs.left - 8f, obs.top - 12f, obs.left + w * 0.48f, obs.top + 16f)
                    val flap2 = RectF(obs.right - w * 0.48f, obs.top - 12f, obs.right + 8f, obs.top + 16f)
                    canvas.drawRoundRect(flap1, 4f, 4f, boxDarkBrownPaint)
                    canvas.drawRoundRect(flap1, 4f, 4f, outlinePaint)
                    canvas.drawRoundRect(flap2, 4f, 4f, boxDarkBrownPaint)
                    canvas.drawRoundRect(flap2, 4f, 4f, outlinePaint)

                    // Cute cat doodle on box
                    canvas.drawCircle(ox, oy + 12f, 14f, outlinePaint.apply { strokeWidth = 3.5f })
                    canvas.drawLine(ox - 8f, oy + 5f, ox - 14f, oy - 2f, outlinePaint)
                    canvas.drawLine(ox + 8f, oy + 5f, ox + 14f, oy - 2f, outlinePaint)
                    outlinePaint.strokeWidth = 6f
                }

                ObstacleType.COUCH_CUSHION -> {
                    // Plump bouncy sofa cushion
                    val cushionRect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(cushionRect, 22f, 22f, cushionTealPaint)
                    canvas.drawRoundRect(cushionRect, 22f, 22f, outlinePaint)

                    // Piping / Seams
                    canvas.drawRoundRect(RectF(obs.left + 8f, obs.top + 8f, obs.right - 8f, obs.bottom - 8f), 16f, 16f, cushionHighlightPaint)
                    // Button tufts
                    canvas.drawCircle(ox - 35f, oy, 6f, outlinePaint.apply { style = Paint.Style.FILL })
                    canvas.drawCircle(ox + 35f, oy, 6f, outlinePaint)
                    outlinePaint.style = Paint.Style.STROKE
                }

                ObstacleType.TABLE_LEG -> {
                    // Solid mahogany table leg with decorative turned wood bulges
                    val legRect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(legRect, 14f, 14f, woodTrimPaint)
                    canvas.drawRoundRect(legRect, 14f, 14f, outlinePaint)
                    // Turned rings
                    canvas.drawRoundRect(RectF(obs.left - 6f, oy - 30f, obs.right + 6f, oy - 15f), 6f, 6f, floorPlankDarkPaint)
                    canvas.drawRoundRect(RectF(obs.left - 6f, oy - 30f, obs.right + 6f, oy - 15f), 6f, 6f, outlinePaint)
                    canvas.drawRoundRect(RectF(obs.left - 6f, oy + 15f, obs.right + 6f, oy + 30f), 6f, 6f, floorPlankDarkPaint)
                    canvas.drawRoundRect(RectF(obs.left - 6f, oy + 15f, obs.right + 6f, oy + 30f), 6f, 6f, outlinePaint)
                }

                ObstacleType.CAT_TUNNEL -> {
                    // Colorful crinkle cat tunnel
                    val tunnelRect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(tunnelRect, 30f, 30f, tunnelFabricPaint)
                    canvas.drawRoundRect(tunnelRect, 30f, 30f, outlinePaint)
                    // Tunnel opening
                    val holeRect = RectF(obs.left + 15f, obs.top + 8f, obs.left + 55f, obs.bottom - 8f)
                    canvas.drawOval(holeRect, tunnelHolePaint)
                    canvas.drawOval(holeRect, outlinePaint)
                    // Crinkle hoops
                    canvas.drawLine(ox, obs.top, ox, obs.bottom, outlinePaint.apply { strokeWidth = 4f })
                    canvas.drawLine(ox + 40f, obs.top, ox + 40f, obs.bottom, outlinePaint)
                    outlinePaint.strokeWidth = 6f
                }

                ObstacleType.SCRATCHING_POST -> {
                    // Sisal rope scratching post on heavy base
                    val baseRect = RectF(obs.left - 10f, obs.bottom - 22f, obs.right + 10f, obs.bottom)
                    canvas.drawRoundRect(baseRect, 8f, 8f, woodTrimPaint)
                    canvas.drawRoundRect(baseRect, 8f, 8f, outlinePaint)

                    val postRect = RectF(obs.left + 8f, obs.top, obs.right - 8f, obs.bottom - 20f)
                    canvas.drawRoundRect(postRect, 10f, 10f, postRopePaint)
                    canvas.drawRoundRect(postRect, 10f, 10f, outlinePaint)
                    // Rope grooves
                    for (ry in (obs.top + 15f).toInt()..(obs.bottom - 30f).toInt() step 14) {
                        canvas.drawLine(obs.left + 8f, ry.toFloat(), obs.right - 8f, ry.toFloat(), outlinePaint.apply { strokeWidth = 3f })
                    }
                    outlinePaint.strokeWidth = 6f
                }

                ObstacleType.COUCH_SECTION -> {
                    // Large corner couch armrest
                    val sofaRect = RectF(obs.left, obs.top, obs.right, obs.bottom)
                    canvas.drawRoundRect(sofaRect, 28f, 28f, sofaBackdropPaint)
                    canvas.drawRoundRect(sofaRect, 28f, 28f, outlinePaint)
                    // Plump arm pillow
                    val pillRect = RectF(obs.left + 15f, obs.top + 15f, obs.right - 15f, obs.bottom - 15f)
                    canvas.drawRoundRect(pillRect, 18f, 18f, sofaPillowPaint)
                    canvas.drawRoundRect(pillRect, 18f, 18f, outlinePaint)
                }
                else -> {}
            }
        }
    }
}
