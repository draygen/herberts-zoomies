package com.draygen.herbertzoom

import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin
import java.util.Random

enum class ParticleType {
    DUST,
    SPARKLE,
    HEART,
    SPEED_LINE,
    FLOATING_TEXT
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,      // 0..maxLife
    var maxLife: Float,
    var size: Float,
    var color: Int,
    var type: ParticleType,
    var text: String = "",
    var rotation: Float = 0f,
    var vRot: Float = 0f
)

class ParticleManager {
    private val particles = mutableListOf<Particle>()
    private val rng = Random()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    fun spawnDust(x: Float, y: Float, count: Int = 3) {
        for (i in 0 until count) {
            val angle = Math.PI + (rng.nextFloat() - 0.5) * 0.8
            val speed = 80f + rng.nextFloat() * 120f
            particles.add(
                Particle(
                    x = x + (rng.nextFloat() - 0.5f) * 20f,
                    y = y + (rng.nextFloat() - 0.5f) * 10f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat() - 30f,
                    life = 0f,
                    maxLife = 0.35f + rng.nextFloat() * 0.2f,
                    size = 12f + rng.nextFloat() * 14f,
                    color = Color.parseColor("#E0D8D0"),
                    type = ParticleType.DUST
                )
            )
        }
    }

    fun spawnSparkles(x: Float, y: Float, count: Int = 8, colorHex: String = "#FFD700") {
        val baseColor = Color.parseColor(colorHex)
        for (i in 0 until count) {
            val angle = rng.nextFloat() * 2.0 * Math.PI
            val speed = 120f + rng.nextFloat() * 220f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    life = 0f,
                    maxLife = 0.4f + rng.nextFloat() * 0.3f,
                    size = 8f + rng.nextFloat() * 12f,
                    color = baseColor,
                    type = ParticleType.SPARKLE,
                    rotation = rng.nextFloat() * 360f,
                    vRot = (rng.nextFloat() - 0.5f) * 720f
                )
            )
        }
    }

    fun spawnFloatingText(x: Float, y: Float, text: String, colorHex: String = "#FF6F00") {
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = 20f,
                vy = -140f,
                life = 0f,
                maxLife = 0.75f,
                size = 40f,
                color = Color.parseColor(colorHex),
                type = ParticleType.FLOATING_TEXT,
                text = text
            )
        )
    }

    fun spawnHearts(x: Float, y: Float, count: Int = 4) {
        for (i in 0 until count) {
            val angle = -Math.PI / 2 + (rng.nextFloat() - 0.5) * 1.2
            val speed = 100f + rng.nextFloat() * 140f
            particles.add(
                Particle(
                    x = x + (rng.nextFloat() - 0.5f) * 30f,
                    y = y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    life = 0f,
                    maxLife = 0.6f + rng.nextFloat() * 0.3f,
                    size = 18f + rng.nextFloat() * 8f,
                    color = Color.parseColor("#FF4081"),
                    type = ParticleType.HEART
                )
            )
        }
    }

    fun update(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life += dt
            if (p.life >= p.maxLife) {
                iter.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.vRot * dt

            // Gravity / friction
            when (p.type) {
                ParticleType.DUST -> {
                    p.vx *= 0.92f
                    p.vy += 60f * dt
                }
                ParticleType.SPARKLE -> {
                    p.vx *= 0.90f
                    p.vy *= 0.90f
                }
                ParticleType.FLOATING_TEXT -> {
                    p.vy *= 0.95f
                }
                ParticleType.HEART -> {
                    p.vx += sin(p.life * 10f).toFloat() * 15f
                    p.vy *= 0.96f
                }
                else -> {}
            }
        }
    }

    fun render(canvas: Canvas) {
        for (p in particles) {
            val progress = p.life / p.maxLife
            val alpha = ((1f - progress) * 255f).toInt().coerceIn(0, 255)

            when (p.type) {
                ParticleType.DUST -> {
                    paint.color = p.color
                    paint.alpha = (alpha * 0.7f).toInt()
                    val currentSize = p.size * (1f + progress * 0.8f)
                    canvas.drawCircle(p.x, p.y, currentSize, paint)
                }
                ParticleType.SPARKLE -> {
                    paint.color = p.color
                    paint.alpha = alpha
                    canvas.save()
                    canvas.translate(p.x, p.y)
                    canvas.rotate(p.rotation)
                    val s = p.size * (1f - progress * 0.5f)
                    // 4-point star sparkle
                    val path = Path().apply {
                        moveTo(0f, -s)
                        quadTo(0f, 0f, s, 0f)
                        quadTo(0f, 0f, 0f, s)
                        quadTo(0f, 0f, -s, 0f)
                        quadTo(0f, 0f, 0f, -s)
                    }
                    canvas.drawPath(path, paint)
                    canvas.restore()
                }
                ParticleType.FLOATING_TEXT -> {
                    textPaint.textSize = p.size
                    textPaint.color = p.color
                    textPaint.alpha = alpha
                    // White outline
                    val strokePaint = Paint(textPaint).apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 6f
                        this.alpha = alpha
                    }
                    canvas.drawText(p.text, p.x, p.y, strokePaint)
                    canvas.drawText(p.text, p.x, p.y, textPaint)
                }
                ParticleType.HEART -> {
                    paint.color = p.color
                    paint.alpha = alpha
                    val hs = p.size * (1f + progress * 0.2f)
                    canvas.save()
                    canvas.translate(p.x, p.y)
                    val heartPath = Path().apply {
                        moveTo(0f, -hs * 0.3f)
                        cubicTo(-hs * 0.5f, -hs * 0.8f, -hs, -hs * 0.2f, 0f, hs * 0.7f)
                        cubicTo(hs, -hs * 0.2f, hs * 0.5f, -hs * 0.8f, 0f, -hs * 0.3f)
                    }
                    canvas.drawPath(heartPath, paint)
                    canvas.restore()
                }
                else -> {}
            }
        }
    }

    fun clear() {
        particles.clear()
    }
}
