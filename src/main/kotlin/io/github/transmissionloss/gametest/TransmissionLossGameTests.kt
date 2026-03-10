package io.github.transmissionloss.gametest

import io.github.transmissionloss.TransmissionLossMod
import io.github.transmissionloss.config.TransmissionLossConfig
import io.github.transmissionloss.network.NetworkScanner
import io.github.transmissionloss.network.TransmissionBreakdown
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraftforge.gametest.GameTestHolder

@GameTestHolder(TransmissionLossMod.MOD_ID)
object TransmissionLossGameTests {
    @JvmStatic
    @GameTest(template = "empty")
    fun linearSpeedScalingApplies(helper: GameTestHelper) {
        val breakdown = TransmissionBreakdown(shaftBlocks = 1, rpm = 32f)
        val expected = TransmissionLossConfig.shaft.get() * TransmissionLossConfig.speedMultiplier(32f)
        val actual = NetworkScanner.computeLoss(breakdown)
        if (kotlin.math.abs(expected - actual) > 1e-9) {
            helper.fail("Expected $expected but got $actual")
            return
        }
        helper.succeed()
    }
}
