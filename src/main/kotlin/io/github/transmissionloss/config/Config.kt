package io.github.transmissionloss.config

object TransmissionLossConfig {
    val enabled = BoolValue(true)
    val recalcCooldownTicks = IntValue(20)
    val includeEncasedShafts = BoolValue(true)
    val countDecorative = BoolValue(false)

    val speedMode = EnumValue(SpeedMode.LINEAR)
    val baseRpm = DoubleValue(32.0)
    val k = DoubleValue(0.5)
    val maxMult = DoubleValue(3.0)

    val shaft = DoubleValue(0.05)
    val encasedShaft = DoubleValue(0.07)
    val cogwheel = DoubleValue(0.10)
    val largeCogwheel = DoubleValue(0.15)
    val gearbox = DoubleValue(0.20)
    val beltSegment = DoubleValue(0.04)
    val beltPulley = DoubleValue(0.10)
    val chainDrive = DoubleValue(0.12)

    val ignoreNetworksWithTag = ListValue(listOf("create:hand_crank_networks"))

    fun register() = Unit

    fun speedMultiplier(rpm: Float): Double {
        val normalized = kotlin.math.abs(rpm.toDouble()) / baseRpm.get()
        val raw = when (speedMode.get()) {
            SpeedMode.NONE -> 1.0
            SpeedMode.LINEAR -> 1.0 + k.get() * normalized
            SpeedMode.QUADRATIC -> 1.0 + k.get() * normalized * normalized
        }
        return raw.coerceAtMost(maxMult.get())
    }

    enum class SpeedMode { NONE, LINEAR, QUADRATIC }
}

class BoolValue(private var value: Boolean) {
    fun get(): Boolean = value
}

class IntValue(private var value: Int) {
    fun get(): Int = value
}

class DoubleValue(private var value: Double) {
    fun get(): Double = value
}

class EnumValue<T : Enum<T>>(private var value: T) {
    fun get(): T = value
}

class ListValue<T>(private var value: List<T>) {
    fun get(): List<T> = value
}
