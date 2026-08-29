package com.draygen.herbertzoom.core

/**
 * The run's mode. The normal runner is [RUNNING]; the Kitty encounter is a
 * contained detour that always hands the run back.
 *
 * RUNNING -> BOSS_INTRO -> BOSS_FIGHT -> BOSS_VICTORY -> RUNNING
 *
 * This is deliberately separate from [GamePlayState] (TITLE / PLAYING /
 * GAME_OVER), which stays the app-level state machine. The boss lives entirely
 * inside PLAYING.
 */
enum class RunMode {
    RUNNING,
    BOSS_INTRO,
    BOSS_FIGHT,
    BOSS_VICTORY
}

/** How fed up Kitty currently is. Drives pattern pool and animation intensity. */
enum class BossPhase {
    MILDLY_IRRITATED,
    VERY_ANNOYED,
    HAD_ENOUGH
}

/** Grump Meter caption. Empties as Herbert wears her down until she leaves. */
enum class GrumpLabel(val caption: String) {
    UNIMPRESSED("UNIMPRESSED"),
    ANNOYED("ANNOYED"),
    VERY_ANNOYED("VERY ANNOYED"),
    ABSOLUTELY_DONE("ABSOLUTELY DONE")
}

enum class BossAttackType {
    /** One giant paw slams a single telegraphed band. Dodge vertically. */
    GIANT_PAW_SWAT,

    /** Two paws slam above and below a guaranteed corridor. Dive for the gap. */
    DOUBLE_SWAT,

    /** The tail sweeps the floor of the arena. Jump it, or stay high. */
    TAIL_SWEEP,

    /** Several slow yarn balls batted across distinct lanes. */
    YARN_BARRAGE,

    /** She tracks Herbert, locks on, then strikes where he *was*. */
    KITTY_STARE,

    /** She stops to wash a paw. Harmless, funny, and a free charging window. */
    GROOMING
}

/** Where an attack is in its telegraph -> strike -> recover cycle. */
enum class AttackPhase {
    TELEGRAPH,
    STRIKE,
    RECOVER,
    FINISHED
}

/**
 * A horizontal danger band across the arena. Every hazard in the fight reduces
 * to one of these so "is there a safe gap?" is a question the sim can answer.
 */
data class DangerZone(
    var top: Float,
    var bottom: Float,
    /** Clearing it with a jump is a valid dodge (tail sweep, rolling yarn). */
    val jumpable: Boolean = false
) {
    val centerY: Float get() = (top + bottom) / 2f
    val height: Float get() = bottom - top

    fun contains(y: Float, pad: Float = 0f): Boolean = y >= top - pad && y <= bottom + pad
}

/**
 * One telegraphed attack. Owns its own clock; the boss just ticks it.
 *
 * Danger only exists during [AttackPhase.STRIKE], and the player has always
 * seen the zones for the whole of [telegraphSec] before that.
 */
class BossAttack(
    val type: BossAttackType,
    val zones: List<DangerZone>,
    val telegraphSec: Float,
    val strikeSec: Float,
    val recoverSec: Float
) {
    var phase: AttackPhase = AttackPhase.TELEGRAPH
        private set
    var timer: Float = 0f
        private set

    /** For KITTY_STARE: the reticle follows Herbert until it locks. */
    var trackedY: Float = GameConstants.WORLD_HEIGHT / 2f
    var locked: Boolean = false
        private set

    val isDangerous: Boolean get() = phase == AttackPhase.STRIKE
    val isFinished: Boolean get() = phase == AttackPhase.FINISHED

    /** 0..1 through the current phase, for renderers and audio cues. */
    val phaseProgress: Float
        get() = when (phase) {
            AttackPhase.TELEGRAPH -> (timer / telegraphSec).coerceIn(0f, 1f)
            AttackPhase.STRIKE -> (timer / strikeSec).coerceIn(0f, 1f)
            AttackPhase.RECOVER -> (timer / recoverSec).coerceIn(0f, 1f)
            AttackPhase.FINISHED -> 1f
        }

    fun update(dt: Float, herbertY: Float) {
        if (phase == AttackPhase.FINISHED) return
        timer += dt

        if (phase == AttackPhase.TELEGRAPH) {
            if (type == BossAttackType.KITTY_STARE) {
                // She tracks him for the first 60% of the wind-up, then commits.
                // The remaining 40% is his window to simply walk out of it.
                if (timer < telegraphSec * 0.6f) {
                    trackedY = herbertY
                    retarget(trackedY)
                } else {
                    locked = true
                }
            }
            if (timer >= telegraphSec) {
                timer = 0f
                phase = AttackPhase.STRIKE
            }
            return
        }

        if (phase == AttackPhase.STRIKE) {
            if (timer >= strikeSec) {
                timer = 0f
                phase = AttackPhase.RECOVER
            }
            return
        }

        if (timer >= recoverSec) {
            phase = AttackPhase.FINISHED
        }
    }

    /** Herbert's eye beam interrupts her mid-swing. A beam is always a reward. */
    fun interrupt() {
        if (phase == AttackPhase.TELEGRAPH || phase == AttackPhase.STRIKE) {
            timer = 0f
            phase = AttackPhase.RECOVER
        }
    }

    private fun retarget(centerY: Float) {
        val z = zones.firstOrNull() ?: return
        val half = z.height / 2f
        z.top = centerY - half
        z.bottom = centerY + half
    }
}

/** A slow yarn ball batted across the arena. Dodge it or jump it. */
data class YarnProjectile(
    val id: Long,
    var x: Float,
    var y: Float,
    val speed: Float,
    val radius: Float = GameConstants.BOSS_YARN_RADIUS,
    var spin: Float = 0f,
    var spent: Boolean = false
)

/**
 * BLUE ZOOMIE ENERGY. Three of these charge Herbert's eye beams.
 * They only ever spawn in lanes that are safe at the moment of spawning -
 * energy must never bait Herbert into a paw.
 */
data class EnergyOrb(
    val id: Long,
    var x: Float,
    var y: Float,
    val radius: Float = GameConstants.BOSS_ENERGY_ORB_RADIUS,
    var bobPhase: Float = 0f,
    var collected: Boolean = false
)

// --- Events the app layer listens to for audio / haptics / particles --------

data class BossIntroEvent(val toysCollected: Int)
data class BossAttackTelegraphEvent(val attack: BossAttack, val phase: BossPhase)
data class BossAttackStrikeEvent(val attack: BossAttack)
data class BossEnergyCollectedEvent(val orb: EnergyOrb, val energy: Int, val ready: Boolean)
data class BossBeamFiredEvent(val grumpBefore: Float, val grumpAfter: Float, val finishing: Boolean)
data class BossPhaseChangedEvent(val phase: BossPhase)
data class BossHitEvent(val herbertStumbled: Boolean, val energyLost: Int)
data class BossVictoryEvent(val bonus: Long)
data class BossEndedEvent(val victory: Boolean)
