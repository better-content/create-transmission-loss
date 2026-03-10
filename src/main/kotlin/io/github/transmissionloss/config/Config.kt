package io.github.transmissionloss.config

import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig

object TransmissionLossConfig {
    private val builder = ForgeConfigSpec.Builder()

    val enabled: ForgeConfigSpec.BooleanValue
    val recalcCooldownTicks: ForgeConfigSpec.IntValue
    val includeEncasedShafts: ForgeConfigSpec.BooleanValue
    val countDecorative: ForgeConfigSpec.BooleanValue

    val speedMode: ForgeConfigSpec.EnumValue<SpeedMode>
    val baseRpm: ForgeConfigSpec.DoubleValue
    val k: ForgeConfigSpec.DoubleValue
    val maxMult: ForgeConfigSpec.DoubleValue

    val shaft: ForgeConfigSpec.DoubleValue
    val encasedShaft: ForgeConfigSpec.DoubleValue
    val cogwheel: ForgeConfigSpec.DoubleValue
    val largeCogwheel: ForgeConfigSpec.DoubleValue
    val gearbox: ForgeConfigSpec.DoubleValue
    val beltSegment: ForgeConfigSpec.DoubleValue
    val beltPulley: ForgeConfigSpec.DoubleValue
    val chainDrive: ForgeConfigSpec.DoubleValue

    val ignoreNetworksWithTag: ForgeConfigSpec.ConfigValue<List<out String>>

    val spec: ForgeConfigSpec

    init {
        builder.push("loss")
        enabled = builder.define("enabled", true)
        recalcCooldownTicks = builder.defineInRange("recalcCooldownTicks", 20, 1, 20 * 60)
        includeEncasedShafts = builder.define("includeEncasedShafts", true)
        countDecorative = builder.define("countDecorative", false)
        builder.pop()

        builder.push("speedScaling")
        speedMode = builder.defineEnum("mode", SpeedMode.LINEAR)
        baseRpm = builder.defineInRange("baseRpm", 32.0, 1.0, 1024.0)
        k = builder.defineInRange("k", 0.5, 0.0, 100.0)
        maxMult = builder.defineInRange("maxMult", 3.0, 1.0, 100.0)
        builder.pop()

        builder.push("coeff")
        shaft = builder.defineInRange("shaft", 0.05, 0.0, 1000.0)
        encasedShaft = builder.defineInRange("encasedShaft", 0.07, 0.0, 1000.0)
        cogwheel = builder.defineInRange("cogwheel", 0.10, 0.0, 1000.0)
        largeCogwheel = builder.defineInRange("largeCogwheel", 0.15, 0.0, 1000.0)
        gearbox = builder.defineInRange("gearbox", 0.20, 0.0, 1000.0)
        beltSegment = builder.defineInRange("beltSegment", 0.04, 0.0, 1000.0)
        beltPulley = builder.defineInRange("beltPulley", 0.10, 0.0, 1000.0)
        chainDrive = builder.defineInRange("chainDrive", 0.12, 0.0, 1000.0)
        builder.pop()

        builder.push("compat")
        ignoreNetworksWithTag = builder.defineListAllowEmpty(
            "ignoreNetworksWithTag",
            listOf("create:hand_crank_networks")
        ) { it is String }
        builder.pop()

        spec = builder.build()
    }

    fun register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec)
    }

    fun speedMultiplier(rpm: Float): Double {
        val normalized = kotlin.math.abs(rpm.toDouble()) / baseRpm.get()
        val raw = when (speedMode.get()) {
            SpeedMode.NONE -> 1.0
            SpeedMode.LINEAR -> 1.0 + k.get() * normalized
            SpeedMode.QUADRATIC -> 1.0 + k.get() * normalized * normalized
        }
        return raw.coerceAtMost(maxMult.get())
    }

    enum class SpeedMode {
        NONE,
        LINEAR,
        QUADRATIC
    }
}
