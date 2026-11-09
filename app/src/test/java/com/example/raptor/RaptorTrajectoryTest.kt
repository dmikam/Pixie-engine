package com.example.raptor

import com.example.raptor.entities.GunshipEnemyShip
import com.example.raptor.entities.InterceptorEnemyShip
import com.example.raptor.entities.ScoutEnemyShip
import com.pixie.engine.core.math.Vec2
import com.pixie.engine.core.weapon.ProjectilePool
import com.pixie.engine.core.weapon.Trajectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RaptorTrajectoryTest {

    @Test
    fun `ScoutEnemyShip uses Trajectory SineWave with expected frequency and amplitude`() {
        val pool = ProjectilePool(10)
        val scout = ScoutEnemyShip("test_scout", Vec2(50f, 10f), pool)

        assertTrue(scout.trajectory is Trajectory.SineWave)
        val sineWave = scout.trajectory as Trajectory.SineWave
        assertEquals(1.5707963f, sineWave.baseAngleRad, 0.001f)
        assertEquals(1.8f, sineWave.frequency, 0.001f)
        assertEquals(18f, sineWave.amplitude, 0.001f)

        val vel = scout.calculateFlightVelocity(52f)
        // Downward forward velocity should equal speed (52f)
        assertEquals(52f, vel.y, 0.05f)
        // Horizontal oscillation should not exceed amplitude (18f)
        assertTrue(abs(vel.x) <= 18.01f)
    }

    @Test
    fun `InterceptorEnemyShip uses Trajectory SineWave with expected frequency and amplitude`() {
        val pool = ProjectilePool(10)
        val interceptor = InterceptorEnemyShip("test_interceptor", Vec2(60f, 20f), pool)

        assertTrue(interceptor.trajectory is Trajectory.SineWave)
        val sineWave = interceptor.trajectory as Trajectory.SineWave
        assertEquals(1.5707963f, sineWave.baseAngleRad, 0.001f)
        assertEquals(2.2f, sineWave.frequency, 0.001f)
        assertEquals(42f, sineWave.amplitude, 0.001f)

        val vel = interceptor.calculateFlightVelocity(44f)
        assertEquals(44f, vel.y, 0.05f)
        assertTrue(abs(vel.x) <= 42.01f)
    }

    @Test
    fun `GunshipEnemyShip uses Trajectory SineWave with hover deceleration past Y 85`() {
        val pool = ProjectilePool(10)
        val gunship = GunshipEnemyShip("test_gunship", Vec2(100f, 40f), pool)

        assertTrue(gunship.trajectory is Trajectory.SineWave)
        val sineWave = gunship.trajectory as Trajectory.SineWave
        assertEquals(1.5707963f, sineWave.baseAngleRad, 0.001f)
        assertEquals(1.2f, sineWave.frequency, 0.001f)
        assertEquals(14f, sineWave.amplitude, 0.001f)

        // Before Y = 85, vertical speed is full speed
        gunship.position = Vec2(100f, 40f)
        val velBefore = gunship.calculateFlightVelocity(28f)
        assertEquals(28f, velBefore.y, 0.05f)
        assertTrue(abs(velBefore.x) <= 14.01f)

        // Past Y = 85, vertical speed decelerates to 70%
        gunship.position = Vec2(100f, 90f)
        val velAfter = gunship.calculateFlightVelocity(28f)
        assertEquals(28f * 0.7f, velAfter.y, 0.05f)
        assertTrue(abs(velAfter.x) <= 14.01f)
    }

    @Test
    fun `StealthBomberEnemyShip uses Trajectory Linear with downward angle`() {
        val pool = ProjectilePool(10)
        val bomber = com.example.raptor.entities.StealthBomberEnemyShip("test_bomber", Vec2(80f, 15f), pool)

        assertTrue(bomber.trajectory is Trajectory.Linear)
        val linear = bomber.trajectory as Trajectory.Linear
        assertEquals(1.5707963f, linear.angleRad, 0.001f)

        val vel = bomber.calculateFlightVelocity(22f)
        assertEquals(22f, vel.y, 0.05f)
        assertEquals(0f, vel.x, 0.05f)
    }
}
