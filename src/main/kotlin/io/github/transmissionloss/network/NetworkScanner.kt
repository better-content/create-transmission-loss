package io.github.transmissionloss.network

import io.github.transmissionloss.config.TransmissionLossConfig

/**
 * Version-targeted note:
 * Belts are counted as pulley-pair Manhattan span on purpose for Create 0.5.1 stability.
 */
object NetworkScanner {
    fun computeLoss(breakdown: TransmissionBreakdown): Double {
        val base = breakdown.shaftBlocks * TransmissionLossConfig.shaft.get() +
            breakdown.encasedShaftBlocks * TransmissionLossConfig.encasedShaft.get() +
            breakdown.cogwheels * TransmissionLossConfig.cogwheel.get() +
            breakdown.largeCogwheels * TransmissionLossConfig.largeCogwheel.get() +
            breakdown.gearboxes * TransmissionLossConfig.gearbox.get() +
            breakdown.beltSegments * TransmissionLossConfig.beltSegment.get() +
            breakdown.beltPulleys * TransmissionLossConfig.beltPulley.get() +
            breakdown.chainDrives * TransmissionLossConfig.chainDrive.get()
        return base * TransmissionLossConfig.speedMultiplier(breakdown.rpm)
    }

    fun computeTypeLoss(count: Int, baseCost: Double, rpm: Float): Double {
        if (count <= 0 || baseCost <= 0.0) return 0.0
        return count * baseCost * TransmissionLossConfig.speedMultiplier(rpm)
    }
}

data class TransmissionBreakdown(
    val shaftBlocks: Int = 0,
    val encasedShaftBlocks: Int = 0,
    val cogwheels: Int = 0,
    val largeCogwheels: Int = 0,
    val gearboxes: Int = 0,
    val beltSegments: Int = 0,
    val beltPulleys: Int = 0,
    val chainDrives: Int = 0,
    val rpm: Float = 0f
)
