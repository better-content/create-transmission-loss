package io.github.transmissionloss.network

import io.github.transmissionloss.config.TransmissionLossConfig

/**
 * Belts are counted as pulley-pair Manhattan span on purpose for stability across Create internals.
 */
object NetworkScanner {
    fun computeLoss(breakdown: TransmissionBreakdown): Double {
        val base = breakdown.shaftBlocks * TransmissionLossConfig.shaftValue() +
            breakdown.encasedShaftBlocks * TransmissionLossConfig.encasedShaftValue() +
            breakdown.cogwheels * TransmissionLossConfig.cogwheelValue() +
            breakdown.largeCogwheels * TransmissionLossConfig.largeCogwheelValue() +
            breakdown.gearboxes * TransmissionLossConfig.gearboxValue() +
            breakdown.beltSegments * TransmissionLossConfig.beltSegmentValue() +
            breakdown.beltPulleys * TransmissionLossConfig.beltPulleyValue() +
            breakdown.chainDrives * TransmissionLossConfig.chainDriveValue()
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
