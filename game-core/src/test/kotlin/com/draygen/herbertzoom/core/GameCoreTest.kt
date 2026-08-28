package com.draygen.herbertzoom.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class GameCoreTest {

    private lateinit var world: LivingRoomWorld

    @BeforeEach
    fun setUp() {
        world = LivingRoomWorld(Random(42))
    }

    @Test
    fun `test initial state and start new run`() {
        assertEquals(GamePlayState.TITLE, world.state)
        world.startNewRun()
        assertEquals(GamePlayState.PLAYING, world.state)
        assertEquals(300f, world.herbert.x)
        assertEquals(GameConstants.WORLD_HEIGHT / 2f, world.herbert.y)
        assertEquals(0L, world.score.currentScore)
    }

    @Test
    fun `test jump mechanics and height arc`() {
        world.startNewRun()
        assertFalse(world.herbert.isJumping)
        assertEquals(0f, world.herbert.jumpHeight)

        world.herbert.jump()
        assertTrue(world.herbert.isJumping)

        // Advance halfway through jump
        world.herbert.update(GameConstants.JUMP_DURATION_SEC / 2f, false)
        assertTrue(world.herbert.jumpHeight > 100f)

        // Complete jump
        world.herbert.update(GameConstants.JUMP_DURATION_SEC / 2f + 0.1f, false)
        assertFalse(world.herbert.isJumping)
        assertEquals(0f, world.herbert.jumpHeight)
    }

    @Test
    fun `test zoomie meter and max zoomies activation`() {
        world.startNewRun()
        var eventTriggered = false
        world.onMaxZoomiesStart = { eventTriggered = true }

        // Add partial energy
        val activated1 = world.zoomieMeter.addEnergy(50f)
        assertFalse(activated1)
        assertEquals(50f, world.zoomieMeter.value)
        assertFalse(world.zoomieMeter.isMaxZoomies)

        // Add remaining energy to max out
        val activated2 = world.zoomieMeter.addEnergy(50f)
        assertTrue(activated2)
        assertTrue(world.zoomieMeter.isMaxZoomies)
        assertEquals(GameConstants.ZOOMIE_METER_MAX, world.zoomieMeter.value)

        // Update time during max zoomies
        world.zoomieMeter.update(GameConstants.MAX_ZOOMIE_DURATION_SEC / 2f)
        assertTrue(world.zoomieMeter.isMaxZoomies)

        // Expire max zoomies
        world.zoomieMeter.update(GameConstants.MAX_ZOOMIE_DURATION_SEC / 2f + 0.5f)
        assertFalse(world.zoomieMeter.isMaxZoomies)
        assertEquals(0f, world.zoomieMeter.value)
    }

    @Test
    fun `test pickup collection and scoring`() {
        world.startNewRun()
        var collected = false
        world.onPickupCollected = { collected = true }

        // Place treat directly on Herbert
        val treat = Pickup(id = 1L, x = world.herbert.x, y = world.herbert.y, type = PickupType.TREAT)
        world.pickups.add(treat)

        world.update(0.016f)

        assertTrue(collected)
        assertEquals(1, world.score.treatsCollected)
        assertTrue(world.score.currentScore > 0)
        assertEquals(GameConstants.ZOOMIE_PER_TREAT, world.zoomieMeter.value)
    }

    @Test
    fun `test first collision triggers forgiving stumble and second triggers flop`() {
        world.startNewRun()
        var stumbleFired = false
        var flopFired = false
        world.onStumble = { stumbleFired = true }
        world.onFlop = { flopFired = true }

        // First hit should stumble instead of instant flop
        val obstacle1 = Obstacle(
            id = 1L,
            x = world.herbert.x,
            y = world.herbert.y,
            type = ObstacleType.TABLE_LEG,
            width = 60f,
            height = 180f,
            canJumpOver = false,
            isSoft = false
        )
        world.obstacles.add(obstacle1)

        world.update(0.016f)

        assertEquals(GamePlayState.PLAYING, world.state)
        assertTrue(stumbleFired)
        assertTrue(world.herbert.isInvulnerable)
        assertEquals(0, world.herbert.lives)

        // Expire invulnerability
        world.herbert.invulnerabilityTimer = 0f

        // Second hit triggers game over flop
        val obstacle2 = Obstacle(
            id = 2L,
            x = world.herbert.x,
            y = world.herbert.y,
            type = ObstacleType.TABLE_LEG,
            width = 60f,
            height = 180f,
            canJumpOver = false,
            isSoft = false
        )
        world.obstacles.add(obstacle2)

        world.update(0.016f)

        assertEquals(GamePlayState.GAME_OVER, world.state)
        assertEquals(AnimationState.FLOPPED, world.herbert.animState)
        assertTrue(flopFired)
    }

    @Test
    fun `test jumping over Kitty prevents collision`() {
        world.startNewRun()

        val kitty = Obstacle(
            id = 1L,
            x = world.herbert.x,
            y = world.herbert.y,
            type = ObstacleType.KITTY_LOAF,
            width = GameConstants.KITTY_WIDTH,
            height = GameConstants.KITTY_HEIGHT,
            canJumpOver = true,
            isSoft = true
        )
        world.obstacles.add(kitty)

        // Mid-air jump
        world.herbert.jump()
        world.herbert.update(GameConstants.JUMP_DURATION_SEC / 2f, false)

        world.update(0.016f)

        assertEquals(GamePlayState.PLAYING, world.state)
        assertNotEquals(AnimationState.FLOPPED, world.herbert.animState)
    }

    @Test
    fun `test scratch spot triggers circling scratch and awards bonus`() {
        world.startNewRun()
        var fired: ScratchSpotEvent? = null
        world.onScratchSpot = { fired = it }

        val spot = ScratchSpot(id = 1L, x = world.herbert.x, y = world.herbert.y)
        world.scratchSpots.add(spot)

        world.update(0.016f)

        assertNotNull(fired)
        assertTrue(spot.used)
        assertTrue(world.herbert.isScratching)
        assertEquals(AnimationState.SCRATCHING, world.herbert.animState)
        assertTrue(world.score.currentScore > 0)
        assertEquals(GameConstants.ZOOMIE_PER_SCRATCH_SPOT, world.zoomieMeter.value)
    }

    @Test
    fun `test scratching orbits the patch then returns Herbert to his lane`() {
        world.startNewRun()
        val centerY = world.herbert.y
        world.scratchSpots.add(ScratchSpot(id = 1L, x = world.herbert.x, y = centerY))

        world.update(0.016f)
        assertTrue(world.herbert.isScratching)

        // Sample the orbit: Herbert must move away from his anchored lane
        var maxOffset = 0f
        var steps = 0
        while (world.herbert.isScratching && steps < 400) {
            world.update(0.016f)
            val dx = kotlin.math.abs(world.herbert.x - GameConstants.HERBERT_HOME_X)
            val dy = kotlin.math.abs(world.herbert.y - centerY)
            maxOffset = maxOf(maxOffset, maxOf(dx, dy))
            steps++
        }

        assertTrue(maxOffset > 40f, "Herbert should visibly circle the patch, saw $maxOffset")
        assertFalse(world.herbert.isScratching)
        assertEquals(GameConstants.HERBERT_HOME_X, world.herbert.x)
        assertEquals(AnimationState.RUNNING, world.herbert.animState)
    }

    @Test
    fun `test scratch spot only fires once and is safe from collisions`() {
        world.startNewRun()
        var fireCount = 0
        world.onScratchSpot = { fireCount++ }

        world.scratchSpots.add(ScratchSpot(id = 1L, x = world.herbert.x, y = world.herbert.y))
        repeat(10) { world.update(0.016f) }

        assertEquals(1, fireCount)
        assertTrue(world.herbert.isInvulnerable)

        // An obstacle landing on him mid-scratch must not end the run
        world.obstacles.add(
            Obstacle(
                id = 2L, x = world.herbert.x, y = world.herbert.y,
                type = ObstacleType.TABLE_LEG, width = 60f, height = 180f,
                canJumpOver = false, isSoft = false
            )
        )
        world.update(0.016f)
        assertEquals(GamePlayState.PLAYING, world.state)
        assertEquals(1, world.herbert.lives)
    }

    @Test
    fun `test world nearly stops while Herbert scratches`() {
        world.startNewRun()
        world.update(0.016f)
        val normalStep = world.score.distanceRun

        world.startNewRun()
        world.scratchSpots.add(ScratchSpot(id = 1L, x = world.herbert.x, y = world.herbert.y))
        world.update(0.016f) // triggers the scratch
        val before = world.score.distanceRun
        world.update(0.016f) // this frame runs at scratch speed
        val scratchStep = world.score.distanceRun - before

        assertTrue(scratchStep < normalStep * 0.5f,
            "world should slow during a scratch: $scratchStep vs $normalStep")
    }

    @Test
    fun `test Herbert cannot jump or steer away mid-scratch`() {
        world.startNewRun()
        world.scratchSpots.add(ScratchSpot(id = 1L, x = world.herbert.x, y = world.herbert.y))
        world.update(0.016f)
        assertTrue(world.herbert.isScratching)

        world.herbert.jump()
        assertFalse(world.herbert.isJumping)

        val targetBefore = world.herbert.targetY
        world.herbert.steerTo(0.85f)
        assertEquals(targetBefore, world.herbert.targetY)
    }

    @Test
    fun `test flopping cancels an in-progress scratch`() {
        world.startNewRun()
        world.scratchSpots.add(ScratchSpot(id = 1L, x = world.herbert.x, y = world.herbert.y))
        world.update(0.016f)
        assertTrue(world.herbert.isScratching)

        world.herbert.flop()
        assertFalse(world.herbert.isScratching)
        assertEquals(AnimationState.FLOPPED, world.herbert.animState)
    }
}
