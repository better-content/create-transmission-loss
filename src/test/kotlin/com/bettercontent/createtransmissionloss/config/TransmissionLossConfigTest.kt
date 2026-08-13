package com.bettercontent.createtransmissionloss.config

import net.minecraftforge.common.ForgeConfigSpec
import kotlin.test.Test
import kotlin.test.assertEquals

class TransmissionLossConfigTest {
    @Test
    fun returnsDefaultsFromAllTypedAccessors() {
        assertEquals(20, TransmissionLossConfig.recalcCooldownTicksValue())
        assertEquals(true, TransmissionLossConfig.includeEncasedShaftsValue())
        assertEquals(0.05, TransmissionLossConfig.shaftValue())
        assertEquals(0.07, TransmissionLossConfig.encasedShaftValue())
        assertEquals(0.10, TransmissionLossConfig.cogwheelValue())
        assertEquals(0.15, TransmissionLossConfig.largeCogwheelValue())
        assertEquals(0.20, TransmissionLossConfig.gearboxValue())
        assertEquals(0.04, TransmissionLossConfig.beltSegmentValue())
        assertEquals(0.10, TransmissionLossConfig.beltPulleyValue())
        assertEquals(0.12, TransmissionLossConfig.chainDriveValue())
    }

    @Test
    fun appliesMultiplierFallbackAndClampsToMaxWhenConfigured() {
        val method = TransmissionLossConfig::class.java.getDeclaredMethod(
            "valueOrDefault",
            ForgeConfigSpec.ConfigValue::class.java,
            Any::class.java
        )
        method.isAccessible = true

        assertEquals(17, method.invoke(TransmissionLossConfig, null, 17))
        assertEquals(2.0, TransmissionLossConfig.speedMultiplier(64f), 1e-9)
        assertEquals(3.0, TransmissionLossConfig.speedMultiplier(1234f), 1e-9)
    }

    @Test
    fun fallsBackToDefaultWhenConfigValueAccessThrows() {
        val method = TransmissionLossConfig::class.java.getDeclaredMethod(
            "valueOrDefault",
            Function0::class.java,
            Any::class.java
        )
        method.isAccessible = true

        val throwingReader: () -> String = { throw IllegalStateException("boom") }

        assertEquals("disabled", method.invoke(TransmissionLossConfig, throwingReader, "disabled"))
    }

}
