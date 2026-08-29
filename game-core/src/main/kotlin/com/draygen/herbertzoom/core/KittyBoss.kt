package com.draygen.herbertzoom.core

import java.util.Random

/** Which of the three comedic exits Kitty picks once she is over it. */
enum class KittyExit {
    WADDLE_AWAY,
    BECOME_LOAF,
    GROOM_AND_IGNORE
}

/** Beats of the victory sequence, for renderers and audio. */
enum class VictoryBeat {
    BLAST,
    SMOKE,
    REVEAL
}

/**
 * The Kitty boss encounter simulation. Pure logic, zero rendering, zero Android.
 *
 * Design contract, kept deliberately narrow:
 *  - Every hazard is a horizontal band or a slow projectile, telegraphed for at
 *    least [GameConstants]-level readable time before it can touch Herbert.
 *  - Every pattern leaves a corridor Herbert comfortably fits through.
 *  - Kitty is never hurt. The Grump Meter is her patience, and emptying it
 *    just means she gives up and leaves.
 */
class KittyBoss(private val rng: Random = Random()) {

    // --- Kitty's state -------------------------------------------------------

    var grump: Float = GameConstants.BOSS_GRUMP_MAX
        private set
    var phase: BossPhase = BossPhase.MILDLY_IRRITATED
        private set
    var currentAttack: BossAttack? = null
        private set

    /** 0..1 of Kitty's remaining patience, for the Grump Meter bar. */
    val grumpFraction: Float
        get() = (grump / GameConstants.BOSS_GRUMP_MAX).coerceIn(0f, 1f)

    val grumpLabel: GrumpLabel
        get() = when {
            grumpFraction > 0.70f -> GrumpLabel.UNIMPRESSED
            grumpFraction > 0.40f -> GrumpLabel.ANNOYED
            grumpFraction > 0.15f -> GrumpLabel.VERY_ANNOYED
            else -> GrumpLabel.ABSOLUTELY_DONE
        }

    // --- Herbert's Blue Eye Energy ------------------------------------------

    var eyeEnergy: Int = 0
        private set
    val beamReady: Boolean get() = eyeEnergy >= GameConstants.BOSS_ENERGY_PER_BEAM

    /** Counts down while the beams are actually on screen. */
    var beamTimer: Float = 0f
        private set
    val beamActive: Boolean get() = beamTimer > 0f

    /** 0..1 through the beam, for renderers. */
    val beamProgress: Float
        get() = if (!beamActive) 0f
                else (1f - beamTimer / GameConstants.BOSS_BEAM_DURATION).coerceIn(0f, 1f)

    /** 0..1 charge glow in Herbert's eyes as energy accumulates. */
    val eyeChargeFraction: Float
        get() = (eyeEnergy.toFloat() / GameConstants.BOSS_ENERGY_PER_BEAM).coerceIn(0f, 1f)

    // --- Entities ------------------------------------------------------------

    val yarn = mutableListOf<YarnProjectile>()
    val orbs = mutableListOf<EnergyOrb>()

    // --- Animation-facing state (read by KittyBossRenderer) ------------------

    var animTimer: Float = 0f
        private set

    /** Counts down while Kitty is visibly startled by the beams. */
    var shockTimer: Float = 0f
        private set
    val isShocked: Boolean get() = shockTimer > 0f

    val isGrooming: Boolean get() = currentAttack?.type == BossAttackType.GROOMING

    /** Ears flatten progressively as she gets more fed up. */
    val earFlatten: Float
        get() = when (phase) {
            BossPhase.MILDLY_IRRITATED -> 0.15f
            BossPhase.VERY_ANNOYED -> 0.55f
            BossPhase.HAD_ENOUGH -> 1f
        }

    /** She keeps an eye on Herbert. Renderers point her pupils here. */
    var gazeY: Float = GameConstants.WORLD_HEIGHT / 2f
        private set

    var exit: KittyExit = KittyExit.BECOME_LOAF
        private set

    // --- Internal pacing -----------------------------------------------------

    private var nextEntityId = 1L
    private var orbTimer = 0f
    private var attacksSinceGroom = 0
    private var lastAttackType: BossAttackType? = null
    private var beamsLanded = 0

    // --- Events --------------------------------------------------------------

    var onTelegraph: ((BossAttackTelegraphEvent) -> Unit)? = null
    var onStrike: ((BossAttackStrikeEvent) -> Unit)? = null
    var onEnergyCollected: ((BossEnergyCollectedEvent) -> Unit)? = null
    var onBeamFired: ((BossBeamFiredEvent) -> Unit)? = null
    var onPhaseChanged: ((BossPhaseChangedEvent) -> Unit)? = null

    /** Total beams that have landed this encounter, for HUD flourishes. */
    val beamsFired: Int get() = beamsLanded

    // =========================================================================
    // Lifecycle
    // =========================================================================

    fun reset() {
        grump = GameConstants.BOSS_GRUMP_MAX
        phase = BossPhase.MILDLY_IRRITATED
        currentAttack = null
        eyeEnergy = 0
        beamTimer = 0f
        yarn.clear()
        orbs.clear()
        animTimer = 0f
        shockTimer = 0f
        gazeY = GameConstants.WORLD_HEIGHT / 2f
        orbTimer = 0f
        attacksSinceGroom = 0
        lastAttackType = null
        beamsLanded = 0
        nextEntityId = 1L
        exit = KittyExit.BECOME_LOAF
    }

    /** Called once when the fight proper begins (after the intro reveal). */
    fun beginFight() {
        // She opens with a wind-up the player has plenty of time to read.
        orbTimer = 0.6f
        startAttack(BossAttackType.GIANT_PAW_SWAT)
    }

    /** Purely cosmetic ticking during the intro reveal and victory sequence. */
    fun tickAnimationOnly(dt: Float, herbertY: Float) {
        animTimer += dt
        gazeY += (herbertY - gazeY) * (1f - Math.exp((-4.0 * dt)).toFloat())
        if (shockTimer > 0f) shockTimer -= dt
        if (beamTimer > 0f) beamTimer = (beamTimer - dt).coerceAtLeast(0f)
    }

    // =========================================================================
    // Fight tick
    // =========================================================================

    fun update(dt: Float, herbert: Herbert) {
        animTimer += dt
        if (shockTimer > 0f) shockTimer -= dt
        if (beamTimer > 0f) beamTimer = (beamTimer - dt).coerceAtLeast(0f)

        // Tracking stare: her gaze eases toward Herbert continuously.
        gazeY += (herbert.y - gazeY) * (1f - Math.exp((-4.0 * dt)).toFloat())

        updateAttack(dt, herbert)
        updateYarn(dt)
        updateOrbs(dt)
        spawnEnergy(dt)
    }

    private fun updateAttack(dt: Float, herbert: Herbert) {
        val attack = currentAttack
        if (attack == null) {
            startAttack(pickNextType())
            return
        }

        val phaseBefore = attack.phase
        attack.update(dt, herbert.y)

        if (phaseBefore == AttackPhase.TELEGRAPH && attack.phase == AttackPhase.STRIKE) {
            if (attack.type == BossAttackType.YARN_BARRAGE) {
                launchYarnBarrage()
            }
            onStrike?.invoke(BossAttackStrikeEvent(attack))
        }

        if (attack.isFinished) {
            currentAttack = null
            startAttack(pickNextType())
        }
    }

    /**
     * She bats the yarn in on the beat the lane markers promised. Balls are
     * staggered horizontally so they arrive as a readable rhythm rather than a
     * wall, and the live count is hard-capped for the frame budget.
     */
    private fun launchYarnBarrage() {
        val attack = currentAttack ?: return
        val speed = when (phase) {
            BossPhase.MILDLY_IRRITATED -> 380f
            BossPhase.VERY_ANNOYED -> 460f
            BossPhase.HAD_ENOUGH -> 540f
        }
        for ((i, zone) in attack.zones.withIndex()) {
            if (yarn.size >= GameConstants.BOSS_MAX_YARN) break
            yarn.add(
                YarnProjectile(
                    id = nextEntityId++,
                    x = GameConstants.WORLD_WIDTH + 160f + i * 190f,
                    y = zone.centerY,
                    speed = speed,
                    spin = rng.nextFloat() * 360f
                )
            )
        }
    }

    private fun updateYarn(dt: Float) {
        val iter = yarn.iterator()
        while (iter.hasNext()) {
            val ball = iter.next()
            ball.x -= ball.speed * dt
            ball.spin += dt * 260f
            if (ball.spent || ball.x + ball.radius < -200f) iter.remove()
        }
    }

    private fun updateOrbs(dt: Float) {
        val iter = orbs.iterator()
        while (iter.hasNext()) {
            val orb = iter.next()
            orb.x -= GameConstants.BOSS_ENERGY_ORB_SPEED * dt
            orb.bobPhase += dt * 3.4f
            if (orb.collected || orb.x + orb.radius < -200f) iter.remove()
        }
    }

    // =========================================================================
    // Attack construction
    // =========================================================================

    private fun pickNextType(): BossAttackType {
        // She grooms herself after every third attack. Deliberately silly, and
        // it hands the player a guaranteed calm window to bank energy in.
        if (attacksSinceGroom >= 3) return BossAttackType.GROOMING

        val pool = when (phase) {
            BossPhase.MILDLY_IRRITATED -> listOf(
                BossAttackType.GIANT_PAW_SWAT,
                BossAttackType.YARN_BARRAGE,
                BossAttackType.GIANT_PAW_SWAT,
                BossAttackType.KITTY_STARE
            )
            BossPhase.VERY_ANNOYED -> listOf(
                BossAttackType.DOUBLE_SWAT,
                BossAttackType.TAIL_SWEEP,
                BossAttackType.YARN_BARRAGE,
                BossAttackType.KITTY_STARE,
                BossAttackType.GIANT_PAW_SWAT
            )
            BossPhase.HAD_ENOUGH -> listOf(
                BossAttackType.DOUBLE_SWAT,
                BossAttackType.TAIL_SWEEP,
                BossAttackType.KITTY_STARE,
                BossAttackType.YARN_BARRAGE,
                BossAttackType.DOUBLE_SWAT
            )
        }

        // Never the same pattern twice running; repetition reads as unfair.
        var pick = pool[rng.nextInt(pool.size)]
        var guard = 0
        while (pick == lastAttackType && guard++ < 6) {
            pick = pool[rng.nextInt(pool.size)]
        }
        return pick
    }

    private fun startAttack(type: BossAttackType) {
        val attack = buildAttack(type, phase, rng)
        currentAttack = attack
        lastAttackType = type
        attacksSinceGroom = if (type == BossAttackType.GROOMING) 0 else attacksSinceGroom + 1
        onTelegraph?.invoke(BossAttackTelegraphEvent(attack, phase))
    }

    // =========================================================================
    // Blue Eye Energy
    // =========================================================================

    private fun spawnEnergy(dt: Float) {
        if (orbs.size >= GameConstants.BOSS_MAX_ENERGY_ORBS) return
        orbTimer -= dt
        if (orbTimer > 0f) return

        // Grooming is her weak spot: energy arrives twice as fast.
        val base = when (phase) {
            BossPhase.MILDLY_IRRITATED -> 2.1f
            BossPhase.VERY_ANNOYED -> 1.9f
            BossPhase.HAD_ENOUGH -> 1.5f
        }
        orbTimer = if (isGrooming) base * 0.5f else base

        orbs.add(
            EnergyOrb(
                id = nextEntityId++,
                x = GameConstants.WORLD_WIDTH + 120f,
                y = pickSafeSpawnY(),
                bobPhase = rng.nextFloat() * 6.28f
            )
        )
    }

    /**
     * Energy must never bait Herbert into a paw, so orbs spawn inside whichever
     * corridor the current pattern leaves open.
     */
    private fun pickSafeSpawnY(): Float {
        val gaps = safeGaps(activeZones(includeTelegraph = true))
        val usable = gaps.filter { it.second - it.first >= 160f }
        if (usable.isEmpty()) {
            return GameConstants.BOSS_ARENA_TOP + rng.nextFloat() * GameConstants.BOSS_ARENA_HEIGHT
        }
        val gap = usable[rng.nextInt(usable.size)]
        val inset = 70f
        val lo = gap.first + inset
        val hi = gap.second - inset
        return if (hi <= lo) (gap.first + gap.second) / 2f else lo + rng.nextFloat() * (hi - lo)
    }

    /**
     * Player-triggered. Two ridiculous cyan beams, straight out of Herbert's
     * eyes. Returns false (and does nothing) if he is not charged.
     */
    fun fireEyeBeam(): Boolean {
        if (!beamReady || beamActive) return false

        val before = grump
        eyeEnergy = 0
        beamTimer = GameConstants.BOSS_BEAM_DURATION
        beamsLanded++
        grump = (grump - GameConstants.BOSS_GRUMP_PER_BEAM).coerceAtLeast(0f)
        shockTimer = 1.1f

        // A landed beam always interrupts whatever she was winding up.
        currentAttack?.interrupt()

        val finishing = grump <= 0f
        onBeamFired?.invoke(BossBeamFiredEvent(before, grump, finishing))
        if (!finishing) updatePhase()
        return true
    }

    private fun updatePhase() {
        val next = when {
            grump > GameConstants.BOSS_PHASE2_GRUMP -> BossPhase.MILDLY_IRRITATED
            grump > GameConstants.BOSS_PHASE3_GRUMP -> BossPhase.VERY_ANNOYED
            else -> BossPhase.HAD_ENOUGH
        }
        if (next != phase) {
            phase = next
            onPhaseChanged?.invoke(BossPhaseChangedEvent(next))
        }
    }

    val isDefeated: Boolean get() = grump <= 0f

    fun chooseExit() {
        exit = when (rng.nextInt(3)) {
            0 -> KittyExit.WADDLE_AWAY
            1 -> KittyExit.BECOME_LOAF
            else -> KittyExit.GROOM_AND_IGNORE
        }
    }

    // =========================================================================
    // Collision queries (the world applies the consequences)
    // =========================================================================

    /** Orbs Herbert touched this frame. Removes them and charges his eyes. */
    fun collectEnergy(herbert: Herbert) {
        val reach = GameConstants.HERBERT_HITBOX_RADIUS
        for (orb in orbs) {
            if (orb.collected) continue
            val dx = herbert.x - orb.x
            val dy = herbert.y - orb.y
            val touch = reach + orb.radius
            if (dx * dx + dy * dy <= touch * touch) {
                orb.collected = true
                if (eyeEnergy < GameConstants.BOSS_ENERGY_PER_BEAM) eyeEnergy++
                onEnergyCollected?.invoke(
                    BossEnergyCollectedEvent(orb, eyeEnergy, beamReady)
                )
            }
        }
    }

    /**
     * True if a hazard is touching Herbert right now. Jumping clears anything
     * marked jumpable; being outside every band clears everything.
     */
    fun isHerbertStruck(herbert: Herbert): Boolean {
        val clearingJump = herbert.isJumping && herbert.jumpHeight > 30f

        val attack = currentAttack
        if (attack != null && attack.isDangerous) {
            for (zone in attack.zones) {
                if (zone.jumpable && clearingJump) continue
                if (zone.contains(herbert.y)) return true
            }
        }

        for (ball in yarn) {
            if (ball.spent) continue
            if (clearingJump) continue // yarn rolls; a pounce clears it
            val dx = herbert.x - ball.x
            val dy = herbert.y - ball.y
            // Slightly shrunk contact radius, in line with the game's forgiving feel
            val touch = GameConstants.HERBERT_HITBOX_RADIUS * 0.8f + ball.radius * 0.85f
            if (dx * dx + dy * dy <= touch * touch) {
                ball.spent = true
                return true
            }
        }
        return false
    }

    /** A boss hit knocks one charge of Blue Eye Energy loose. */
    fun loseEnergyOnHit(): Int {
        if (eyeEnergy <= 0) return 0
        eyeEnergy--
        return 1
    }

    // =========================================================================
    // Safe-corridor analysis (also used by tests)
    // =========================================================================

    fun activeZones(includeTelegraph: Boolean = false): List<DangerZone> {
        val attack = currentAttack ?: return emptyList()
        val relevant = attack.isDangerous ||
            (includeTelegraph && attack.phase == AttackPhase.TELEGRAPH)
        return if (relevant) attack.zones else emptyList()
    }

    companion object {

        /** Per-phase attack clock. Telegraphs never drop below 0.7s. */
        fun timingsFor(phase: BossPhase): Triple<Float, Float, Float> = when (phase) {
            BossPhase.MILDLY_IRRITATED -> Triple(1.05f, 0.45f, 1.90f)
            BossPhase.VERY_ANNOYED -> Triple(0.88f, 0.42f, 1.40f)
            BossPhase.HAD_ENOUGH -> Triple(0.74f, 0.40f, 1.10f)
        }

        /**
         * Builds one attack. Patterns are constructed *around* a guaranteed safe
         * corridor rather than placed randomly and checked afterwards, so an
         * unfair pattern is not something the RNG can produce.
         */
        fun buildAttack(type: BossAttackType, phase: BossPhase, rng: Random): BossAttack {
            val (tel, strike, rec) = timingsFor(phase)
            val top = GameConstants.BOSS_ARENA_TOP
            val bottom = GameConstants.BOSS_ARENA_BOTTOM
            val gap = GameConstants.BOSS_MIN_SAFE_GAP

            return when (type) {
                BossAttackType.GIANT_PAW_SWAT -> {
                    // One big slam. Sized so that even dead-centre in the arena
                    // it leaves a walkable corridor on BOTH sides - a 300-tall
                    // paw does not, it leaves only 228 either way.
                    val half = 140f
                    val lo = top + half
                    val hi = bottom - half
                    val center = if (hi <= lo) (top + bottom) / 2f else lo + rng.nextFloat() * (hi - lo)
                    BossAttack(type, listOf(DangerZone(center - half, center + half)), tel, strike, rec)
                }

                BossAttackType.DOUBLE_SWAT -> {
                    // Two paws that between them cover everything *except* one
                    // obvious corridor. Pick the corridor first, then the paws.
                    val corridorHalf = gap / 2f + 8f // slack, so rounding can't shave the guarantee
                    val lo = top + corridorHalf + 120f
                    val hi = bottom - corridorHalf - 120f
                    val center = if (hi <= lo) (top + bottom) / 2f else lo + rng.nextFloat() * (hi - lo)
                    BossAttack(
                        type,
                        listOf(
                            DangerZone(top - 200f, center - corridorHalf),
                            DangerZone(center + corridorHalf, bottom + 200f)
                        ),
                        tel, strike, rec
                    )
                }

                BossAttackType.TAIL_SWEEP -> {
                    // A low sweep across the floor: jump it, or simply stay high.
                    val sweepTop = bottom - 210f
                    BossAttack(
                        type,
                        listOf(DangerZone(sweepTop, bottom + 260f, jumpable = true)),
                        tel + 0.15f, strike + 0.30f, rec
                    )
                }

                BossAttackType.KITTY_STARE -> {
                    // Tracks Herbert, locks, then strikes where he was standing.
                    val half = 130f
                    BossAttack(
                        type,
                        listOf(DangerZone(GameConstants.WORLD_HEIGHT / 2f - half, GameConstants.WORLD_HEIGHT / 2f + half)),
                        tel + 0.25f, strike, rec
                    )
                }

                BossAttackType.YARN_BARRAGE -> {
                    // Lane markers telegraph which rows the yarn will roll down.
                    val lanes = yarnLanes(phase, rng)
                    val half = GameConstants.BOSS_YARN_RADIUS
                    BossAttack(
                        type,
                        lanes.map { DangerZone(it - half, it + half, jumpable = true) },
                        tel, strike, rec
                    )
                }

                BossAttackType.GROOMING -> {
                    // No zones at all. She is busy washing a paw.
                    val groomSec = when (phase) {
                        BossPhase.MILDLY_IRRITATED -> 2.6f
                        BossPhase.VERY_ANNOYED -> 2.2f
                        BossPhase.HAD_ENOUGH -> 1.8f
                    }
                    BossAttack(type, emptyList(), 0.4f, 0.01f, groomSec)
                }
            }
        }

        /**
         * Yarn lanes, always on one parity of a 6-lane grid so no two balls in a
         * wave are ever adjacent. Corridors stay wide enough to walk through.
         */
        fun yarnLanes(phase: BossPhase, rng: Random): List<Float> {
            val laneCount = 6
            val laneH = GameConstants.BOSS_ARENA_HEIGHT / laneCount
            val parity = rng.nextInt(2)
            val candidates = (0 until laneCount).filter { it % 2 == parity }
                .map { GameConstants.BOSS_ARENA_TOP + (it + 0.5f) * laneH }
            val count = when (phase) {
                BossPhase.MILDLY_IRRITATED -> 2
                else -> 3
            }
            return candidates.shuffled(rng).take(count).sorted()
        }

        /**
         * Uncovered vertical stretches of the arena, as (top, bottom) pairs.
         * The fight guarantees at least one of these is comfortably walkable.
         */
        fun safeGaps(zones: List<DangerZone>): List<Pair<Float, Float>> {
            val top = GameConstants.BOSS_ARENA_TOP
            val bottom = GameConstants.BOSS_ARENA_BOTTOM
            if (zones.isEmpty()) return listOf(top to bottom)

            val sorted = zones.sortedBy { it.top }
            val gaps = mutableListOf<Pair<Float, Float>>()
            var cursor = top
            for (z in sorted) {
                if (z.top > cursor) gaps.add(cursor to minOf(z.top, bottom))
                cursor = maxOf(cursor, z.bottom)
                if (cursor >= bottom) break
            }
            if (cursor < bottom) gaps.add(cursor to bottom)
            return gaps.filter { it.second > it.first }
        }

        /** Tallest corridor a pattern leaves open. */
        fun largestSafeGap(zones: List<DangerZone>): Float =
            safeGaps(zones).maxOfOrNull { it.second - it.first } ?: 0f
    }
}
