package com.example.raptor

import com.example.raptor.ui.RaptorControls
import com.pixie.android.input.ArcadeActionType
import com.pixie.android.input.ArcadeButtonShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RaptorControlsTest {

    @Test
    fun `createAutoFireButton returns expected config with pill shape and primary action`() {
        var toggled = false
        val config = RaptorControls.createAutoFireButton(
            isAutoFire = true,
            weaponTier = 3,
            onToggle = { toggled = true }
        )

        assertEquals("auto_fire_toggle", config.id)
        assertEquals(ArcadeButtonShape.PILL, config.shape)
        assertEquals(ArcadeActionType.ACTION_PRIMARY, config.actionType)
        assertEquals("AUTO (T3)", config.label)

        config.onPressDown()
        assertTrue(toggled)
    }

    @Test
    fun `createMissileButton returns expected config with secondary action and ammo badge`() {
        var fired = false
        val config = RaptorControls.createMissileButton(
            missilesLeft = 4,
            onFire = { fired = true }
        )

        assertEquals("missile_fire_button", config.id)
        assertEquals(ArcadeButtonShape.PILL, config.shape)
        assertEquals(ArcadeActionType.ACTION_SECONDARY, config.actionType)
        assertEquals("MISSILE (4)", config.label)

        config.onPressDown()
        assertTrue(fired)
    }

    @Test
    fun `createMegaBombButton returns expected config with octagon shape and special action`() {
        var triggered = false
        val config = RaptorControls.createMegaBombButton(
            bombsLeft = 2,
            onTrigger = { triggered = true }
        )

        assertEquals("mega_bomb_button", config.id)
        assertEquals(ArcadeButtonShape.OCTAGON, config.shape)
        assertEquals(ArcadeActionType.ACTION_SPECIAL, config.actionType)
        assertEquals("BOMB (2)", config.label)

        config.onPressDown()
        assertTrue(triggered)
    }

    @Test
    fun `button configs specify proportional weights and minimum width for console deck`() {
        val autoBtn = RaptorControls.createAutoFireButton(true, 1) {}
        val missileBtn = RaptorControls.createMissileButton(5) {}
        val bombBtn = RaptorControls.createMegaBombButton(2) {}

        assertEquals(1.0f, autoBtn.weight)
        assertEquals(1.0f, missileBtn.weight)
        assertEquals(1.0f, bombBtn.weight)

        assertTrue((autoBtn.minWidthDp?.value ?: 0f) > 0f)
        assertTrue((missileBtn.minWidthDp?.value ?: 0f) > 0f)
        assertTrue((bombBtn.minWidthDp?.value ?: 0f) > 0f)
    }

    @Test
    fun `createPauseButton returns expected config with circle shape and pause action`() {
        var paused = false
        val config = RaptorControls.createPauseButton(onPause = { paused = true })

        assertEquals("raptor_pause_button", config.id)
        assertEquals(ArcadeButtonShape.CIRCLE, config.shape)
        assertEquals(ArcadeActionType.PAUSE, config.actionType)

        config.onPressDown()
        assertTrue(paused)
    }

    @Test
    fun `createFloatingButtons returns positioned buttons with custom floating offsets and ids`() {
        val autoBtn = RaptorControls.createAutoFireButton(true, 1) {}
        val missileBtn = RaptorControls.createMissileButton(5) {}
        val bombBtn = RaptorControls.createMegaBombButton(2) {}
        val pauseBtn = RaptorControls.createPauseButton {}

        val floatingList = RaptorControls.createFloatingButtons(autoBtn, missileBtn, bombBtn, pauseBtn)

        assertEquals(4, floatingList.size)
        assertEquals("fl_auto_fire", floatingList[0].id)
        assertEquals("fl_missile", floatingList[1].id)
        assertEquals("fl_bomb", floatingList[2].id)
        assertEquals("fl_pause", floatingList[3].id)

        floatingList.forEach { btn ->
            assertTrue(btn.floatingOffset.x > 0f && btn.floatingOffset.y > 0f)
        }
    }

    @Test
    fun `createWeaponSwitchButton returns expected config with pill shape and custom action`() {
        var cycled = false
        val config = RaptorControls.createWeaponSwitchButton(
            activeWeaponId = "twin_plasma",
            unlockedCount = 3,
            onCycle = { cycled = true }
        )

        assertEquals("weapon_switch_button", config.id)
        assertEquals(ArcadeButtonShape.PILL, config.shape)
        assertEquals(ArcadeActionType.CUSTOM, config.actionType)
        assertEquals("WPN: PLASMA", config.label)

        config.onPressDown()
        assertTrue(cycled)
    }

    @Test
    fun `createFloatingButtons includes weapon button when supplied`() {
        val autoBtn = RaptorControls.createAutoFireButton(true, 1) {}
        val weaponBtn = RaptorControls.createWeaponSwitchButton("odin_laser", 2) {}
        val missileBtn = RaptorControls.createMissileButton(5) {}
        val bombBtn = RaptorControls.createMegaBombButton(2) {}
        val pauseBtn = RaptorControls.createPauseButton {}

        val floatingList = RaptorControls.createFloatingButtons(
            autoFireBtn = autoBtn,
            missileBtn = missileBtn,
            bombBtn = bombBtn,
            pauseBtn = pauseBtn,
            weaponBtn = weaponBtn
        )

        assertEquals(5, floatingList.size)
        assertEquals("fl_auto_fire", floatingList[0].id)
        assertEquals("fl_weapon", floatingList[1].id)
        assertEquals("fl_missile", floatingList[2].id)
        assertEquals("fl_bomb", floatingList[3].id)
        assertEquals("fl_pause", floatingList[4].id)
    }
}
