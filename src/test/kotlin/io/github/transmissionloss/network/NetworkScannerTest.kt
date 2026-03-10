package io.github.transmissionloss.network

import io.github.transmissionloss.config.TransmissionLossConfig
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkScannerTest {
    @Test
    fun computesExpectedLossWithLinearScaling() {
        val breakdown = TransmissionBreakdown(
            shaftBlocks = 10,
            encasedShaftBlocks = 2,
            cogwheels = 3,
            largeCogwheels = 1,
            gearboxes = 1,
            beltSegments = 8,
            beltPulleys = 2,
            chainDrives = 1,
            rpm = 64f
        )

        val expectedBase =
            10 * TransmissionLossConfig.shaft.get() +
                2 * TransmissionLossConfig.encasedShaft.get() +
                3 * TransmissionLossConfig.cogwheel.get() +
                1 * TransmissionLossConfig.largeCogwheel.get() +
                1 * TransmissionLossConfig.gearbox.get() +
                8 * TransmissionLossConfig.beltSegment.get() +
                2 * TransmissionLossConfig.beltPulley.get() +
                1 * TransmissionLossConfig.chainDrive.get()

        val normalized = abs(breakdown.rpm.toDouble()) / TransmissionLossConfig.baseRpm.get()
        val multiplier = (1.0 + TransmissionLossConfig.k.get() * normalized)
            .coerceAtMost(TransmissionLossConfig.maxMult.get())

        assertEquals(expectedBase * multiplier, NetworkScanner.computeLoss(breakdown), 1e-9)
    }

    @Test
    fun cacheRecalcRespectsCooldown() {
        val id = NetworkId("minecraft:overworld", 1L)
        LossCache.bootstrap()
        LossCache.set(id, 100L, TransmissionBreakdown(shaftBlocks = 1, rpm = 16f))
        LossCache.markDirty(id)

        assertEquals(false, LossCache.shouldRecalc(id, 110L, force = false))
        assertEquals(true, LossCache.shouldRecalc(id, 120L, force = false))
        assertEquals(true, LossCache.shouldRecalc(id, 110L, force = true))
    }
}
