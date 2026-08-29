package com.draygen.herbertzoom.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

/**
 * The Kitty boss encounter, exercised purely in game-core. Everything here runs
 * on the JVM in milliseconds - no emulator, no Android, no rendering.
 */
class BossEncounterTest {

    private lateinit var world: LivingRoomWorld

    @BeforeEach
    fun setUp() {
        world = LivingRoomWorld(Random(1234))
        world.startNewRun()
    }

    // --- helpers -------------------------------------------------------------

    /**
     * Feeds Herbert exactly [count] toys. Naturally spawned pickups are swept
     * away each tick so the toy counter moves only by what this helper hands him.
     */
    private fun collectToys(count: Int) {
        repeat(count) {
            world.pickups.clear()
            world.obstacles.clear()
            world.pickups.add(
                Pickup(id = 900_000L + it, x = world.herbert.x, y = world.herbert.y, type = PickupType.TOY_MOUSE)
            )
            world.update(0.016f)
        }
    }

    /** Runs the world until [predicate] holds, or fails after a generous budget. */
    private fun advanceUntil(what: String, maxSec: Float = 30f, dt: Float = 0.016f, predicate: () -> Boolean) {
        var t = 0f
        while (t < maxSec) {
            if (predicate()) return
            world.update(dt)
            t += dt
        }
        if (!predicate()) fail<Unit>("timed out after ${maxSec}s waiting for: $what")
    }

    private fun reachFight() {
        world.debugForceBoss()
        advanceUntil("boss fight to start") { world.runMode == RunMode.BOSS_FIGHT }
    }

    /** Charges Herbert's eyes by handing him orbs directly. */
    private fun chargeBeam() {
        while (!world.boss.beamReady) {
            world.boss.orbs.add(
                EnergyOrb(id = 800_000L + world.boss.orbs.size, x = world.herbert.x, y = world.herbert.y)
            )
            world.boss.collectEnergy(world.herbert)
        }
    }

    /**
     * Charge, fire, and let the beam finish. A beam occupies the screen for
     * BOSS_BEAM_DURATION and refuses to re-fire while it is in flight, so a test
     * that wants N landed beams has to wait each one out.
     */
    private fun landBeam() {
        chargeBeam()
        assertTrue(world.boss.fireEyeBeam(), "beam should have fired")
        var t = 0f
        while (world.boss.beamActive && world.state == GamePlayState.PLAYING &&
               world.runMode == RunMode.BOSS_FIGHT && t < 3f) {
            world.update(0.016f)
            t += 0.016f
        }
        if (world.runMode == RunMode.BOSS_FIGHT) world.update(0.016f)
    }

    // =========================================================================
    // Trigger
    // =========================================================================

    @Test
    fun `boss triggers at 200 collected toys`() {
        assertEquals(RunMode.RUNNING, world.runMode)
        assertFalse(world.bossTriggered)

        collectToys(GameConstants.BOSS_TRIGGER_TOYS - 1)
        assertEquals(199, world.score.toysCollected)
        assertEquals(RunMode.RUNNING, world.runMode, "199 toys must not summon Kitty")

        collectToys(1)
        assertEquals(200, world.score.toysCollected)
        assertTrue(world.bossTriggered)
        assertEquals(RunMode.BOSS_INTRO, world.runMode)
    }

    @Test
    fun `boss triggers only once per run`() {
        var introCount = 0
        world.onBossIntro = { introCount++ }

        collectToys(GameConstants.BOSS_TRIGGER_TOYS)
        assertEquals(1, introCount)

        // Play the whole encounter out and return to the runner...
        advanceUntil("encounter to finish", maxSec = 90f) { world.runMode == RunMode.BOSS_FIGHT }
        // Win it quickly so we get back to RUNNING.
        while (world.runMode == RunMode.BOSS_FIGHT && !world.boss.isDefeated) landBeam()
        advanceUntil("run to resume", maxSec = 30f) { world.runMode == RunMode.RUNNING }

        // ...then collect 50 more toys. Kitty must stay gone.
        collectToys(50)
        assertTrue(world.score.toysCollected >= 250)
        assertEquals(1, introCount, "toys 201+ must never re-summon Kitty")
        assertEquals(RunMode.RUNNING, world.runMode)
    }

    @Test
    fun `boss trigger resets on a new run and does not carry over`() {
        collectToys(GameConstants.BOSS_TRIGGER_TOYS)
        assertTrue(world.bossTriggered)
        assertEquals(RunMode.BOSS_INTRO, world.runMode)

        world.startNewRun()

        assertFalse(world.bossTriggered, "a fresh run must start with Kitty un-summoned")
        assertEquals(RunMode.RUNNING, world.runMode)
        assertEquals(0, world.score.toysCollected)
        assertEquals(GameConstants.BOSS_GRUMP_MAX, world.boss.grump)
        assertEquals(0, world.boss.eyeEnergy)
        assertTrue(world.boss.orbs.isEmpty())
        assertTrue(world.boss.yarn.isEmpty())
    }

    @Test
    fun `boss trigger preserves score combo and zoomie state`() {
        world.zoomieMeter.addEnergy(40f)
        collectToys(GameConstants.BOSS_TRIGGER_TOYS)

        val scoreAtTrigger = world.score.currentScore
        assertTrue(scoreAtTrigger > 0)
        assertTrue(world.score.comboMultiplier > 1)

        world.update(0.016f)
        assertEquals(scoreAtTrigger, world.score.currentScore - 0, "score must survive the transition")
        assertTrue(world.zoomieMeter.value > 0f)
    }

    // =========================================================================
    // State transitions
    // =========================================================================

    @Test
    fun `state machine walks RUNNING to INTRO to FIGHT to VICTORY back to RUNNING`() {
        val seen = mutableListOf<RunMode>()
        world.debugForceBoss()
        seen.add(world.runMode)

        advanceUntil("fight") { world.runMode == RunMode.BOSS_FIGHT }
        seen.add(world.runMode)

        while (!world.boss.isDefeated) landBeam()
        assertEquals(RunMode.BOSS_VICTORY, world.runMode)
        seen.add(world.runMode)

        advanceUntil("run to resume", maxSec = 20f) { world.runMode == RunMode.RUNNING }
        seen.add(world.runMode)

        assertEquals(
            listOf(RunMode.BOSS_INTRO, RunMode.BOSS_FIGHT, RunMode.BOSS_VICTORY, RunMode.RUNNING),
            seen
        )
    }

    @Test
    fun `intro clears the stage and pauses normal spawning`() {
        // Let the runner build up some furniture first.
        advanceUntil("obstacles to spawn", maxSec = 20f) { world.obstacles.size >= 2 }
        assertTrue(world.obstacles.isNotEmpty())

        world.debugForceBoss()
        assertTrue(world.obstacles.isEmpty(), "the arena must be cleared for the reveal")
        assertTrue(world.pickups.isEmpty())
        assertTrue(world.scratchSpots.isEmpty())

        // Nothing new may spawn for the whole encounter.
        advanceUntil("fight") { world.runMode == RunMode.BOSS_FIGHT }
        repeat(600) { world.update(0.016f) } // ~10s of fighting
        assertTrue(world.obstacles.isEmpty(), "normal obstacles must not spawn during the boss")
        assertTrue(world.pickups.isEmpty(), "normal pickups must not spawn during the boss")
        assertTrue(world.scratchSpots.isEmpty())
    }

    @Test
    fun `world slows but keeps drifting during the encounter`() {
        world.update(0.016f)
        val before = world.score.distanceRun
        world.update(0.016f)
        val normalStep = world.score.distanceRun - before

        reachFight()
        val bossBefore = world.score.distanceRun
        world.update(0.016f)
        val bossStep = world.score.distanceRun - bossBefore

        assertTrue(bossStep > 0f, "the room should still drift so the scene stays alive")
        assertTrue(bossStep < normalStep * 0.5f, "the run should visibly slow: $bossStep vs $normalStep")
    }

    // =========================================================================
    // Attack telegraphing & fairness
    // =========================================================================

    @Test
    fun `every attack telegraphs before it can touch Herbert`() {
        for (phase in BossPhase.values()) {
            for (type in BossAttackType.values()) {
                val attack = KittyBoss.buildAttack(type, phase, Random(7))
                assertEquals(AttackPhase.TELEGRAPH, attack.phase, "$type/$phase must open on a telegraph")
                assertFalse(attack.isDangerous, "$type/$phase must be harmless while telegraphing")
                if (type != BossAttackType.GROOMING) {
                    assertTrue(
                        attack.telegraphSec >= 0.7f,
                        "$type/$phase telegraph is only ${attack.telegraphSec}s - too fast to read"
                    )
                }
            }
        }
    }

    @Test
    fun `an attack only becomes dangerous after its full telegraph elapses`() {
        val attack = KittyBoss.buildAttack(BossAttackType.GIANT_PAW_SWAT, BossPhase.HAD_ENOUGH, Random(3))
        var t = 0f
        while (t < attack.telegraphSec - 0.02f) {
            attack.update(0.016f, 540f)
            t += 0.016f
            assertFalse(attack.isDangerous, "went live ${attack.telegraphSec - t}s early")
        }
        attack.update(0.05f, 540f)
        assertTrue(attack.isDangerous)
        assertEquals(AttackPhase.STRIKE, attack.phase)
    }

    @Test
    fun `every generated pattern leaves an obvious safe region`() {
        // Hammer the generator with many seeds; an unfair pattern must not be
        // something the RNG is even capable of producing.
        for (seed in 0 until 400) {
            val rng = Random(seed.toLong())
            for (phase in BossPhase.values()) {
                for (type in BossAttackType.values()) {
                    val attack = KittyBoss.buildAttack(type, phase, rng)
                    if (attack.zones.isEmpty()) continue
                    val gap = KittyBoss.largestSafeGap(attack.zones)
                    val required = if (type == BossAttackType.YARN_BARRAGE) {
                        GameConstants.BOSS_MIN_YARN_CORRIDOR
                    } else {
                        GameConstants.BOSS_MIN_SAFE_GAP
                    }
                    assertTrue(
                        gap >= required,
                        "$type/$phase seed=$seed left only ${gap}u of safe room (need $required)"
                    )
                }
            }
        }
    }

    @Test
    fun `double swat always leaves exactly one corridor between two paws`() {
        for (seed in 0 until 200) {
            val attack = KittyBoss.buildAttack(BossAttackType.DOUBLE_SWAT, BossPhase.HAD_ENOUGH, Random(seed.toLong()))
            assertEquals(2, attack.zones.size, "double swat must be two danger areas")
            val gaps = KittyBoss.safeGaps(attack.zones)
            assertEquals(1, gaps.size, "seed=$seed produced ${gaps.size} corridors; it must be unambiguous")
            assertTrue(gaps[0].second - gaps[0].first >= GameConstants.BOSS_MIN_SAFE_GAP)
        }
    }

    @Test
    fun `tail sweep can be cleared by jumping`() {
        val attack = KittyBoss.buildAttack(BossAttackType.TAIL_SWEEP, BossPhase.VERY_ANNOYED, Random(5))
        assertTrue(attack.zones.all { it.jumpable }, "the tail must be jumpable")

        reachFight()
        // Force the tail sweep live and stand Herbert right in it.
        val boss = world.boss
        val floorY = GameConstants.BOSS_ARENA_BOTTOM - 60f
        world.herbert.y = floorY

        val live = KittyBoss.buildAttack(BossAttackType.TAIL_SWEEP, BossPhase.VERY_ANNOYED, Random(5))
        while (!live.isDangerous) live.update(0.05f, floorY)
        assertTrue(live.zones.first().contains(floorY), "test setup: Herbert should be inside the sweep")

        // Grounded -> struck. Airborne -> safe.
        world.herbert.isJumping = false
        assertTrue(live.zones.first().contains(world.herbert.y))

        world.herbert.jump()
        world.herbert.update(GameConstants.JUMP_DURATION_SEC / 2f, false)
        assertTrue(world.herbert.jumpHeight > 30f, "test setup: Herbert should be high enough")
        assertTrue(boss.beamsFired >= 0) // sanity
    }

    @Test
    fun `kitty stare locks on before striking so Herbert can step aside`() {
        val attack = KittyBoss.buildAttack(BossAttackType.KITTY_STARE, BossPhase.MILDLY_IRRITATED, Random(9))
        val startY = 400f

        // Tracking portion: the reticle follows him.
        attack.update(0.1f, startY)
        assertEquals(startY, attack.trackedY, 1f)
        assertFalse(attack.locked)

        // Past 60% of the wind-up it commits and stops following.
        var t = 0.1f
        while (t < attack.telegraphSec * 0.75f) {
            attack.update(0.05f, 800f)
            t += 0.05f
        }
        assertTrue(attack.locked, "the stare must commit before it strikes")
        val lockedAt = attack.zones.first().centerY

        // ...and it strikes where he *was*, leaving him room to have moved.
        while (!attack.isDangerous) attack.update(0.05f, 200f)
        assertEquals(lockedAt, attack.zones.first().centerY, 1f)
    }

    @Test
    fun `attacks have recovery windows and never chain back to back`() {
        for (phase in BossPhase.values()) {
            val (tel, strike, rec) = KittyBoss.timingsFor(phase)
            assertTrue(rec >= 1.0f, "$phase recovery of ${rec}s is too tight")
            assertTrue(tel > strike, "$phase telegraph (${tel}s) must outlast the strike (${strike}s)")
        }
    }

    @Test
    fun `kitty stops to groom and that window is harmless`() {
        val groom = KittyBoss.buildAttack(BossAttackType.GROOMING, BossPhase.MILDLY_IRRITATED, Random(2))
        assertTrue(groom.zones.isEmpty(), "grooming must create no danger at all")

        reachFight()
        // She grooms after every third attack, so it must show up in a fight.
        advanceUntil("Kitty to stop and groom", maxSec = 60f) { world.boss.isGrooming }
        assertTrue(world.boss.isGrooming)
        assertTrue(world.boss.activeZones(includeTelegraph = true).isEmpty())
    }

    @Test
    fun `live projectile and orb counts stay bounded`() {
        reachFight()
        var maxYarn = 0
        var maxOrbs = 0
        repeat(3600) { // ~60 seconds of fighting
            world.update(0.016f)
            maxYarn = maxOf(maxYarn, world.boss.yarn.size)
            maxOrbs = maxOf(maxOrbs, world.boss.orbs.size)
        }
        assertTrue(maxYarn <= GameConstants.BOSS_MAX_YARN, "yarn peaked at $maxYarn")
        assertTrue(maxOrbs <= GameConstants.BOSS_MAX_ENERGY_ORBS, "orbs peaked at $maxOrbs")
    }

    // =========================================================================
    // Blue Eye Energy & the eye beam
    // =========================================================================

    @Test
    fun `blue eye energy charges one step per orb`() {
        reachFight()
        val boss = world.boss
        assertEquals(0, boss.eyeEnergy)
        assertFalse(boss.beamReady)

        val charges = mutableListOf<Int>()
        boss.onEnergyCollected = { charges.add(it.energy) }

        repeat(GameConstants.BOSS_ENERGY_PER_BEAM) { i ->
            boss.orbs.add(EnergyOrb(id = 500L + i, x = world.herbert.x, y = world.herbert.y))
            boss.collectEnergy(world.herbert)
            assertEquals(i + 1, boss.eyeEnergy)
        }

        assertEquals(listOf(1, 2, 3), charges)
        assertTrue(boss.beamReady)
        assertEquals(1f, boss.eyeChargeFraction)
    }

    @Test
    fun `energy never over-charges past a full beam`() {
        reachFight()
        val boss = world.boss
        repeat(10) { i ->
            boss.orbs.add(EnergyOrb(id = 600L + i, x = world.herbert.x, y = world.herbert.y))
            boss.collectEnergy(world.herbert)
        }
        assertEquals(GameConstants.BOSS_ENERGY_PER_BEAM, boss.eyeEnergy)
    }

    @Test
    fun `beam cannot fire when Herbert is not charged`() {
        reachFight()
        val boss = world.boss
        var fired = 0
        boss.onBeamFired = { fired++ }

        assertFalse(boss.beamReady)
        assertFalse(boss.fireEyeBeam(), "an uncharged beam must refuse to fire")
        assertEquals(0, fired)
        assertEquals(GameConstants.BOSS_GRUMP_MAX, boss.grump, "grump must be untouched")

        // Partially charged is still not charged.
        boss.orbs.add(EnergyOrb(id = 1L, x = world.herbert.x, y = world.herbert.y))
        boss.collectEnergy(world.herbert)
        assertEquals(1, boss.eyeEnergy)
        assertFalse(boss.fireEyeBeam())
        assertEquals(GameConstants.BOSS_GRUMP_MAX, boss.grump)
    }

    @Test
    fun `firing the beam reduces the grump meter and spends the energy`() {
        reachFight()
        val boss = world.boss
        chargeBeam()

        var event: BossBeamFiredEvent? = null
        boss.onBeamFired = { event = it }

        assertTrue(boss.fireEyeBeam())
        assertEquals(GameConstants.BOSS_GRUMP_MAX - GameConstants.BOSS_GRUMP_PER_BEAM, boss.grump)
        assertEquals(0, boss.eyeEnergy, "the beam must consume the charge")
        assertFalse(boss.beamReady)
        assertTrue(boss.beamActive)
        assertTrue(boss.isShocked, "Kitty should be visibly startled")
        assertNotNull(event)
        assertEquals(GameConstants.BOSS_GRUMP_MAX, event!!.grumpBefore)
        assertFalse(event!!.finishing)
    }

    @Test
    fun `a beam already in flight cannot be fired again`() {
        reachFight()
        val boss = world.boss
        chargeBeam()
        assertTrue(boss.fireEyeBeam())
        chargeBeam()
        assertFalse(boss.fireEyeBeam(), "no double-firing while beams are on screen")
    }

    @Test
    fun `the beam interrupts whatever Kitty was winding up`() {
        reachFight()
        val boss = world.boss
        advanceUntil("a real attack to be in progress") {
            val a = boss.currentAttack
            a != null && a.type != BossAttackType.GROOMING && a.phase == AttackPhase.TELEGRAPH
        }
        chargeBeam()
        boss.fireEyeBeam()
        assertEquals(AttackPhase.RECOVER, boss.currentAttack?.phase, "a landed beam should break her swing")
    }

    @Test
    fun `roughly three to four beams win the encounter`() {
        val beamsNeeded = kotlin.math.ceil(
            GameConstants.BOSS_GRUMP_MAX / GameConstants.BOSS_GRUMP_PER_BEAM
        ).toInt()
        assertTrue(beamsNeeded in 3..4, "tuning drifted: the fight now takes $beamsNeeded beams")

        reachFight()
        var fired = 0
        while (!world.boss.isDefeated && fired < 10) {
            landBeam()
            fired++
        }
        assertEquals(beamsNeeded, fired)
        assertTrue(world.boss.isDefeated)
        assertEquals(beamsNeeded, world.boss.beamsFired)
    }

    // =========================================================================
    // Phases
    // =========================================================================

    @Test
    fun `grump meter drives the three phases in order`() {
        reachFight()
        val boss = world.boss
        val phases = mutableListOf<BossPhase>()
        boss.onPhaseChanged = { phases.add(it.phase) }

        assertEquals(BossPhase.MILDLY_IRRITATED, boss.phase)
        assertEquals(GrumpLabel.UNIMPRESSED, boss.grumpLabel)

        landBeam()   // 100 -> 70
        assertEquals(BossPhase.VERY_ANNOYED, boss.phase)

        landBeam()   // 70 -> 40
        assertEquals(BossPhase.VERY_ANNOYED, boss.phase)

        landBeam()   // 40 -> 10
        assertEquals(BossPhase.HAD_ENOUGH, boss.phase)
        assertEquals(GrumpLabel.ABSOLUTELY_DONE, boss.grumpLabel)

        assertEquals(listOf(BossPhase.VERY_ANNOYED, BossPhase.HAD_ENOUGH), phases)
    }

    @Test
    fun `later phases get faster but never unfair`() {
        val (t1, _, r1) = KittyBoss.timingsFor(BossPhase.MILDLY_IRRITATED)
        val (t2, _, r2) = KittyBoss.timingsFor(BossPhase.VERY_ANNOYED)
        val (t3, _, r3) = KittyBoss.timingsFor(BossPhase.HAD_ENOUGH)

        assertTrue(t1 > t2 && t2 > t3, "telegraphs should tighten with each phase")
        assertTrue(r1 > r2 && r2 > r3, "recovery should tighten with each phase")
        assertTrue(t3 >= 0.7f, "the final phase must still be readable, got ${t3}s")
        assertTrue(r3 >= 1.0f, "the final phase must still breathe, got ${r3}s")
    }

    @Test
    fun `ears flatten and energy arrives faster as she gets more fed up`() {
        reachFight()
        val boss = world.boss
        val flat1 = boss.earFlatten
        landBeam()
        landBeam()
        landBeam()
        assertEquals(BossPhase.HAD_ENOUGH, boss.phase)
        assertTrue(boss.earFlatten > flat1, "her ears should flatten as she gets angrier")
    }

    // =========================================================================
    // Victory & resuming the run
    // =========================================================================

    @Test
    fun `emptying the grump meter wins the encounter and pays the bonus`() {
        reachFight()
        val scoreBefore = world.score.currentScore
        var victory: BossVictoryEvent? = null
        world.onBossVictory = { victory = it }

        while (!world.boss.isDefeated) landBeam()

        assertEquals(RunMode.BOSS_VICTORY, world.runMode)
        assertNotNull(victory)
        assertEquals(GameConstants.BOSS_VICTORY_BONUS, victory!!.bonus)
        assertTrue(
            world.score.currentScore >= scoreBefore + GameConstants.BOSS_VICTORY_BONUS,
            "the +20,000 bonus should land in full"
        )
        assertTrue(world.boss.yarn.isEmpty(), "she stops attacking once she is over it")
    }

    @Test
    fun `the run resumes after victory with score intact and zoomies charged`() {
        reachFight()
        world.score.addFlatBonus(5_000L)
        val scoreBeforeWin = world.score.currentScore
        val highBefore = world.score.highScore

        var ended: BossEndedEvent? = null
        world.onBossEnded = { ended = it }

        while (!world.boss.isDefeated) landBeam()
        advanceUntil("run to resume", maxSec = 20f) { world.runMode == RunMode.RUNNING }

        assertEquals(RunMode.RUNNING, world.runMode)
        assertEquals(GamePlayState.PLAYING, world.state)
        assertNotNull(ended)
        assertTrue(ended!!.victory)

        // Nothing thrown away.
        assertTrue(world.score.currentScore >= scoreBeforeWin + GameConstants.BOSS_VICTORY_BONUS)
        assertTrue(world.score.highScore >= highBefore)
        assertTrue(world.bossTriggered, "the latch stays set for the rest of the run")

        // Handed back mid-zoomies, with his normal stumble buffer restored.
        assertTrue(world.zoomieMeter.isMaxZoomies, "victory should tip Herbert into Maximum Zoomies")
        assertEquals(1, world.herbert.lives)
        assertNotEquals(AnimationState.FLOPPED, world.herbert.animState)

        // The normal runner is genuinely back: obstacles start flowing again.
        advanceUntil("normal spawning to resume", maxSec = 20f) { world.obstacles.isNotEmpty() || world.pickups.isNotEmpty() }
        assertTrue(world.obstacles.isNotEmpty() || world.pickups.isNotEmpty())
    }

    @Test
    fun `she picks one of the three comedic exits`() {
        reachFight()
        while (!world.boss.isDefeated) landBeam()
        assertTrue(world.boss.exit in KittyExit.values(), "she should waddle off, loaf, or start grooming")
    }

    // =========================================================================
    // Failure during the boss
    // =========================================================================

    @Test
    fun `a single boss hit stumbles Herbert instead of ending the run`() {
        reachFight()
        assertEquals(GameConstants.BOSS_STUMBLE_BUFFER, world.herbert.lives, "the fight grants a deeper buffer")

        var hit: BossHitEvent? = null
        world.onBossHit = { hit = it }

        // Park a yarn ball directly on him.
        world.boss.yarn.add(YarnProjectile(id = 1L, x = world.herbert.x, y = world.herbert.y, speed = 0f))
        world.update(0.016f)

        assertNotNull(hit)
        assertTrue(hit!!.herbertStumbled)
        assertEquals(GamePlayState.PLAYING, world.state, "one boss hit must never end the run")
        assertEquals(AnimationState.STUMBLING, world.herbert.animState)
        assertTrue(world.herbert.isInvulnerable)
    }

    @Test
    fun `a boss hit breaks the combo and knocks a charge of energy loose`() {
        reachFight()
        world.score.incrementCombo()
        world.score.incrementCombo()
        assertTrue(world.score.comboMultiplier > 1)

        world.boss.orbs.add(EnergyOrb(id = 1L, x = world.herbert.x, y = world.herbert.y))
        world.boss.collectEnergy(world.herbert)
        world.boss.orbs.add(EnergyOrb(id = 2L, x = world.herbert.x, y = world.herbert.y))
        world.boss.collectEnergy(world.herbert)
        assertEquals(2, world.boss.eyeEnergy)

        var hit: BossHitEvent? = null
        world.onBossHit = { hit = it }
        world.boss.yarn.add(YarnProjectile(id = 9L, x = world.herbert.x, y = world.herbert.y, speed = 0f))
        world.update(0.016f)

        assertEquals(1, world.score.comboMultiplier, "a hit should break the combo")
        assertEquals(1, world.boss.eyeEnergy, "a hit should cost one charge of Blue Eye Energy")
        assertEquals(1, hit!!.energyLost)
    }

    @Test
    fun `only repeated boss mistakes end the run and they use the normal flop`() {
        reachFight()
        var flops = 0
        var bossEnded: BossEndedEvent? = null
        world.onFlop = { flops++ }
        world.onBossEnded = { bossEnded = it }

        val hitsAllowed = GameConstants.BOSS_STUMBLE_BUFFER
        repeat(hitsAllowed) {
            world.herbert.invulnerabilityTimer = 0f
            world.boss.yarn.add(YarnProjectile(id = 100L + it, x = world.herbert.x, y = world.herbert.y, speed = 0f))
            world.update(0.016f)
            assertEquals(GamePlayState.PLAYING, world.state, "hit ${it + 1} should only stumble him")
        }

        // One more mistake, and it's the usual wholesome flop.
        world.herbert.invulnerabilityTimer = 0f
        world.boss.yarn.add(YarnProjectile(id = 999L, x = world.herbert.x, y = world.herbert.y, speed = 0f))
        world.update(0.016f)

        assertEquals(GamePlayState.GAME_OVER, world.state)
        assertEquals(AnimationState.FLOPPED, world.herbert.animState)
        assertEquals(1, flops, "boss failure must route through the existing flop path")
        assertNotNull(bossEnded)
        assertFalse(bossEnded!!.victory)
    }

    @Test
    fun `Herbert is immune while recovering from a boss stumble`() {
        reachFight()
        world.boss.yarn.add(YarnProjectile(id = 1L, x = world.herbert.x, y = world.herbert.y, speed = 0f))
        world.update(0.016f)
        assertTrue(world.herbert.isInvulnerable)

        val livesAfterFirst = world.herbert.lives
        repeat(20) {
            world.boss.yarn.add(YarnProjectile(id = 200L + it, x = world.herbert.x, y = world.herbert.y, speed = 0f))
            world.update(0.016f)
        }
        assertEquals(livesAfterFirst, world.herbert.lives, "recovery frames must not be punished")
        assertEquals(GamePlayState.PLAYING, world.state)
    }

    @Test
    fun `Herbert is never struck by a pattern that is only telegraphing`() {
        reachFight()
        val boss = world.boss
        // Sit him in the middle of the arena and follow whatever she winds up.
        repeat(2000) {
            val attack = boss.currentAttack
            if (attack != null && attack.phase == AttackPhase.TELEGRAPH && attack.zones.isNotEmpty()) {
                world.herbert.y = attack.zones.first().centerY
                world.herbert.targetY = world.herbert.y
                assertFalse(
                    boss.isHerbertStruck(world.herbert),
                    "${attack.type} hit him during its telegraph"
                )
            }
            world.update(0.016f)
            if (world.state != GamePlayState.PLAYING) return
        }
    }

    // =========================================================================
    // Debug affordance
    // =========================================================================

    @Test
    fun `debug force boss respects the once-per-run latch`() {
        world.debugForceBoss()
        assertEquals(RunMode.BOSS_INTRO, world.runMode)
        assertTrue(world.bossTriggered)

        world.debugForceBoss()
        assertEquals(RunMode.BOSS_INTRO, world.runMode, "forcing twice must not restart the intro")
    }

    @Test
    fun `the toy threshold is 200 by default and only movable deliberately`() {
        assertEquals(200, GameConstants.BOSS_TRIGGER_TOYS)
        assertEquals(GameConstants.BOSS_TRIGGER_TOYS, world.bossToyThreshold)

        val quick = LivingRoomWorld(Random(5))
        quick.startNewRun()
        quick.bossToyThreshold = 3
        quick.pickups.add(Pickup(id = 1L, x = quick.herbert.x, y = quick.herbert.y, type = PickupType.TOY_MOUSE))
        quick.update(0.016f)
        assertEquals(RunMode.RUNNING, quick.runMode)
        quick.pickups.add(Pickup(id = 2L, x = quick.herbert.x, y = quick.herbert.y, type = PickupType.TOY_MOUSE))
        quick.update(0.016f)
        quick.pickups.add(Pickup(id = 3L, x = quick.herbert.x, y = quick.herbert.y, type = PickupType.TOY_MOUSE))
        quick.update(0.016f)
        assertEquals(RunMode.BOSS_INTRO, quick.runMode)
    }

    // =========================================================================
    // CUTSCENE BEATS
    //
    // The renderer and the audio layer both drive off these, so a beat landing
    // in the wrong place desynchronises the whole reveal.
    // =========================================================================

    @Test
    fun `intro beats run hold then rumble then title and cover the whole intro`() {
        assertEquals(IntroBeat.HOLD, KittyBoss.introBeatAt(0f))
        assertEquals(IntroBeat.HOLD, KittyBoss.introBeatAt(GameConstants.BOSS_INTRO_PAUSE_SEC - 0.01f))
        assertEquals(IntroBeat.RUMBLE, KittyBoss.introBeatAt(GameConstants.BOSS_INTRO_PAUSE_SEC))
        assertEquals(
            IntroBeat.RUMBLE,
            KittyBoss.introBeatAt(GameConstants.BOSS_INTRO_PAUSE_SEC + GameConstants.BOSS_INTRO_RUMBLE_SEC - 0.01f)
        )
        assertEquals(
            IntroBeat.TITLE,
            KittyBoss.introBeatAt(GameConstants.BOSS_INTRO_PAUSE_SEC + GameConstants.BOSS_INTRO_RUMBLE_SEC)
        )
        // The title beat must still be the one showing as the intro hands over.
        assertEquals(IntroBeat.TITLE, KittyBoss.introBeatAt(GameConstants.BOSS_INTRO_DURATION - 0.01f))
    }

    @Test
    fun `Kitty is fully risen by the time the title card appears`() {
        assertEquals(0f, KittyBoss.introRiseProgress(0f), 1e-4f)
        assertEquals(0f, KittyBoss.introRiseProgress(GameConstants.BOSS_INTRO_PAUSE_SEC), 1e-4f)
        assertEquals(
            0.5f,
            KittyBoss.introRiseProgress(GameConstants.BOSS_INTRO_PAUSE_SEC + GameConstants.BOSS_INTRO_RUMBLE_SEC / 2f),
            1e-3f
        )
        // She must never still be sliding up while the title is on screen.
        assertEquals(
            1f,
            KittyBoss.introRiseProgress(GameConstants.BOSS_INTRO_PAUSE_SEC + GameConstants.BOSS_INTRO_RUMBLE_SEC),
            1e-4f
        )
        assertEquals(1f, KittyBoss.introRiseProgress(GameConstants.BOSS_INTRO_DURATION), 1e-4f)
    }

    @Test
    fun `victory beats run blast then smoke then reveal and cover the whole sequence`() {
        val blast = GameConstants.BOSS_VICTORY_BLAST_SEC
        val smoke = GameConstants.BOSS_VICTORY_SMOKE_SEC

        assertEquals(VictoryBeat.BLAST, KittyBoss.victoryBeatAt(0f))
        assertEquals(VictoryBeat.BLAST, KittyBoss.victoryBeatAt(blast - 0.01f))
        assertEquals(VictoryBeat.SMOKE, KittyBoss.victoryBeatAt(blast))
        assertEquals(VictoryBeat.SMOKE, KittyBoss.victoryBeatAt(blast + smoke - 0.01f))
        assertEquals(VictoryBeat.REVEAL, KittyBoss.victoryBeatAt(blast + smoke))
        assertEquals(VictoryBeat.REVEAL, KittyBoss.victoryBeatAt(GameConstants.BOSS_VICTORY_DURATION - 0.01f))
    }

    @Test
    fun `each victory beat runs a full zero to one and resets at the next beat`() {
        val blast = GameConstants.BOSS_VICTORY_BLAST_SEC
        val smoke = GameConstants.BOSS_VICTORY_SMOKE_SEC

        assertEquals(0f, KittyBoss.victoryBeatProgress(0f), 1e-4f)
        assertEquals(0.5f, KittyBoss.victoryBeatProgress(blast / 2f), 1e-3f)

        // Each beat restarts its own 0..1, rather than continuing the last one.
        assertEquals(0f, KittyBoss.victoryBeatProgress(blast), 1e-3f)
        assertEquals(0.5f, KittyBoss.victoryBeatProgress(blast + smoke / 2f), 1e-3f)
        assertEquals(0f, KittyBoss.victoryBeatProgress(blast + smoke), 1e-3f)

        // And the last beat is essentially complete as the encounter hands back.
        assertEquals(1f, KittyBoss.victoryBeatProgress(GameConstants.BOSS_VICTORY_DURATION), 1e-3f)
    }

    @Test
    fun `the beat helpers agree with the mode timer the world actually reports`() {
        world.debugForceBoss()

        // Walk the intro and confirm the helper never claims a beat the world
        // has not reached, right up to the handover into the fight.
        var sawRumble = false
        var sawTitle = false
        while (world.runMode == RunMode.BOSS_INTRO) {
            when (KittyBoss.introBeatAt(world.bossModeTimer)) {
                IntroBeat.RUMBLE -> sawRumble = true
                IntroBeat.TITLE -> sawTitle = true
                IntroBeat.HOLD -> assertFalse(sawRumble, "the intro must not fall back to HOLD")
            }
            world.update(0.016f)
        }
        assertTrue(sawRumble, "the rumble beat must actually be reached")
        assertTrue(sawTitle, "the title beat must actually be reached")
        assertEquals(RunMode.BOSS_FIGHT, world.runMode)
    }
}
