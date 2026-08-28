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
    fun `test obstacle collision results in wholesome flop`() {
        world.startNewRun()
        var flopFired = false
        world.onFlop = { flopFired = true }

        // Place solid obstacle right at Herbert
        val obstacle = Obstacle(
            id = 1L,
            x = world.herbert.x,
            y = world.herbert.y,
            type = ObstacleType.TABLE_LEG,
            width = 60f,
            height = 180f,
            canJumpOver = false,
            isSoft = false
        )
        world.obstacles.add(obstacle)

        world.update(0.016f)

        assertEquals(GamePlayState.GAME_OVER, world.state)
        assertEquals(AnimationState.FLOPPED, world.herbert.animState)
        assertTrue(flopFired)
    }

    @Test
    fun `test jumping over jumpable obstacle prevents game over`() {
        world.startNewRun()

        val obstacle = Obstacle(
            id = 1L,
            x = world.herbert.x,
            y = world.herbert.y,
            type = ObstacleType.SLIPPER,
            width = 90f,
            height = 60f,
            canJumpOver = true,
            isSoft = true
        )
        world.obstacles.add(obstacle)

        // Initiate jump and update to mid-air peak
        world.herbert.jump()
        world.herbert.update(GameConstants.JUMP_DURATION_SEC / 2f, false)

        world.update(0.016f)

        // Should still be playing
        assertEquals(GamePlayState.PLAYING, world.state)
        assertNotEquals(AnimationState.FLOPPED, world.herbert.animState)
    }
}
