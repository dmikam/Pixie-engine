package com.pixie.engine.ports

import com.pixie.engine.core.ports.HapticFeedbackType
import com.pixie.engine.core.ports.HapticPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticPortTest {

    @Test
    fun testPlatformerHapticPatternsPresence() {
        val expected = listOf(
            HapticFeedbackType.LIGHT,
            HapticFeedbackType.MEDIUM,
            HapticFeedbackType.HEAVY,
            HapticFeedbackType.DOUBLE_PULSE,
            HapticFeedbackType.TICK,
            HapticFeedbackType.DOUBLE_TAP,
            HapticFeedbackType.PULSE,
            HapticFeedbackType.JOLT
        )

        for (type in expected) {
            assertTrue("HapticFeedbackType should contain $type", HapticFeedbackType.entries.contains(type))
        }
        assertEquals(8, HapticFeedbackType.entries.size)
    }

    @Test
    fun testHapticPortTriggerConvenienceSignatures() {
        val triggered = mutableListOf<HapticFeedbackType>()
        val mockPort = object : HapticPort {
            override var isEnabled: Boolean = true
            override fun trigger(type: HapticFeedbackType) {
                if (isEnabled) {
                    triggered.add(type)
                }
            }
        }

        mockPort.triggerJump()
        mockPort.triggerCoinCollect()
        mockPort.triggerStomp()
        mockPort.triggerCrateThrow()
        mockPort.triggerBrickShatter()
        mockPort.triggerPlayerDamage()

        assertEquals(6, triggered.size)
        assertEquals(HapticFeedbackType.TICK, triggered[0])
        assertEquals(HapticFeedbackType.DOUBLE_TAP, triggered[1])
        assertEquals(HapticFeedbackType.PULSE, triggered[2])
        assertEquals(HapticFeedbackType.PULSE, triggered[3])
        assertEquals(HapticFeedbackType.JOLT, triggered[4])
        assertEquals(HapticFeedbackType.JOLT, triggered[5])

        mockPort.isEnabled = false
        mockPort.triggerJump()
        assertEquals(6, triggered.size)
    }
}
