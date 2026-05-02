package io.github.transmissionloss.network

import io.github.transmissionloss.config.TransmissionLossConfig
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.registries.ForgeRegistries
import java.lang.reflect.Modifier
import kotlin.math.abs

object NetworkRuntimeBridge {
    data class BlockLossSummary(
        val individualLoss: Double,
        val networkTypeLoss: Double,
        val networkTypeCount: Int,
        val networkTypeLabel: String
    )

    fun computeBlockLoss(state: BlockState, rpm: Float): Double {
        val baseCost = blockBaseLoss(state)
        if (baseCost <= 0.0) return 0.0
        return baseCost * TransmissionLossConfig.speedMultiplier(rpm)
    }

    fun summarizeBlockLoss(blockEntity: Any, state: BlockState, rpm: Float): BlockLossSummary? {
        val kind = blockKind(state) ?: return null
        val baseCost = blockBaseLoss(kind)
        if (baseCost <= 0.0) return null

        val individualLoss = NetworkScanner.computeTypeLoss(1, baseCost, rpm)
        val cached = refreshLossFromBlockEntity(blockEntity)
        val count = cached?.breakdown?.count(kind) ?: 1
        val networkRpm = cached?.breakdown?.rpm ?: rpm

        return BlockLossSummary(
            individualLoss = individualLoss,
            networkTypeLoss = NetworkScanner.computeTypeLoss(count, baseCost, networkRpm),
            networkTypeCount = count,
            networkTypeLabel = kind.label(count)
        )
    }

    fun resolveNetworkId(network: Any): NetworkId? {
        val members = findMemberValues(network)
        if (members.isEmpty()) return null

        val canonicalPos = members.asSequence()
            .mapNotNull { extractBlockPosLong(it) }
            .minOrNull() ?: return null

        val dimension = members.asSequence()
            .mapNotNull { extractDimensionId(it) }
            .firstOrNull() ?: "minecraft:overworld"

        return NetworkId(dimension, canonicalPos)
    }

    fun refreshLoss(network: Any, force: Boolean = false): CachedLoss? {
        val sample = sampleNetwork(network) ?: return null
        val cached = LossCache.snapshot(sample.id)
        val shouldRefresh = force ||
            cached == null ||
            cached.breakdown != sample.breakdown ||
            LossCache.shouldRecalc(sample.id, sample.gameTime, force = false)

        if (shouldRefresh) {
            LossCache.set(sample.id, sample.gameTime, sample.breakdown)
        }

        return LossCache.snapshot(sample.id)
    }

    fun refreshLossFromBlockEntity(blockEntity: Any, force: Boolean = false): CachedLoss? {
        val network = resolveNetwork(blockEntity) ?: return null
        return refreshLoss(network, force)
    }

    private fun sampleNetwork(network: Any): NetworkSample? {
        val members = findMemberValues(network)
        if (members.isEmpty()) return null

        val canonicalPos = members.asSequence()
            .mapNotNull { extractBlockPos(it)?.asLong() ?: extractBlockPosLong(it) }
            .minOrNull() ?: return null

        val dimension = members.asSequence()
            .mapNotNull { extractDimensionId(it) }
            .firstOrNull() ?: "minecraft:overworld"

        var gameTime = 0L
        var maxRpm = 0f
        val tally = BreakdownTally()

        members.forEach { member ->
            extractLevel(member)?.let { level ->
                gameTime = maxOf(gameTime, level.gameTime)
                extractBlockPos(member)?.let { pos ->
                    countTransmissionBlock(level.getBlockState(pos), tally)
                }
            }

            extractSpeed(member)?.let { speed ->
                maxRpm = maxOf(maxRpm, abs(speed))
            }
        }

        return NetworkSample(
            id = NetworkId(dimension, canonicalPos),
            gameTime = gameTime,
            breakdown = tally.toBreakdown(maxRpm)
        )
    }

    private fun findMemberValues(network: Any): Collection<Any> {
        val fields = network.javaClass.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .sortedBy {
                when (it.name) {
                    "members" -> 0
                    "sources" -> 1
                    else -> 2
                }
            }
        for (field in fields) {
            runCatching {
                field.isAccessible = true
                val value = field.get(network)
                if (value is Map<*, *>) {
                    val values = value.values.filterNotNull()
                    if (values.isNotEmpty()) {
                        return values
                    }
                }
            }
        }
        return emptyList()
    }

    private fun extractBlockPosLong(member: Any): Long? {
        return runCatching {
            val worldPositionField = member.javaClass.getDeclaredField("worldPosition")
            worldPositionField.isAccessible = true
            val pos = worldPositionField.get(member) ?: return null
            val asLong = pos.javaClass.methods.firstOrNull { it.name == "asLong" && it.parameterCount == 0 } ?: return null
            asLong.invoke(pos) as? Long
        }.getOrNull()
    }

    private fun extractDimensionId(member: Any): String? {
        return runCatching {
            val levelMethod = member.javaClass.methods.firstOrNull { it.name == "getLevel" && it.parameterCount == 0 } ?: return null
            val level = levelMethod.invoke(member) ?: return null
            val dimensionMethod = level.javaClass.methods.firstOrNull { it.name == "dimension" && it.parameterCount == 0 } ?: return null
            val resourceKey = dimensionMethod.invoke(level) ?: return null
            val locationMethod = resourceKey.javaClass.methods.firstOrNull { it.name == "location" && it.parameterCount == 0 } ?: return null
            val location = locationMethod.invoke(resourceKey) ?: return null
            location.toString()
        }.getOrNull()
    }

    private fun extractLevel(member: Any): Level? {
        return runCatching {
            val levelMethod = member.javaClass.methods.firstOrNull { it.name == "getLevel" && it.parameterCount == 0 } ?: return null
            levelMethod.invoke(member) as? Level
        }.getOrNull()
    }

    private fun extractBlockPos(member: Any): BlockPos? {
        return runCatching {
            val worldPositionField = member.javaClass.getDeclaredField("worldPosition")
            worldPositionField.isAccessible = true
            worldPositionField.get(member) as? BlockPos
        }.getOrNull()
    }

    private fun extractSpeed(member: Any): Float? {
        return runCatching {
            val speedMethod = member.javaClass.methods.firstOrNull { it.name == "getSpeed" && it.parameterCount == 0 } ?: return null
            when (val speed = speedMethod.invoke(member)) {
                is Float -> speed
                is Double -> speed.toFloat()
                is Number -> speed.toFloat()
                else -> null
            }
        }.getOrNull()
    }

    private fun resolveNetwork(blockEntity: Any): Any? {
        return runCatching {
            val method = blockEntity.javaClass.methods.firstOrNull { it.name == "getOrCreateNetwork" && it.parameterCount == 0 } ?: return null
            method.invoke(blockEntity)
        }.getOrNull()
    }

    private fun countTransmissionBlock(state: BlockState, tally: BreakdownTally) {
        when (blockKind(state)) {
            TransmissionBlockKind.GEARBOX -> tally.gearboxes += 1
            TransmissionBlockKind.LARGE_COGWHEEL -> tally.largeCogwheels += 1
            TransmissionBlockKind.COGWHEEL -> tally.cogwheels += 1
            TransmissionBlockKind.BELT -> tally.beltSegments += 1
            TransmissionBlockKind.ENCASED_SHAFT -> tally.encasedShaftBlocks += 1
            TransmissionBlockKind.CHAIN_DRIVE -> tally.chainDrives += 1
            TransmissionBlockKind.SHAFT -> tally.shaftBlocks += 1
            null -> Unit
        }
    }

    private fun blockBaseLoss(state: BlockState): Double {
        return blockKind(state)?.let(::blockBaseLoss) ?: 0.0
    }

    private fun blockBaseLoss(kind: TransmissionBlockKind): Double {
        return when (kind) {
            TransmissionBlockKind.GEARBOX -> TransmissionLossConfig.gearboxValue()
            TransmissionBlockKind.LARGE_COGWHEEL -> TransmissionLossConfig.largeCogwheelValue()
            TransmissionBlockKind.COGWHEEL -> TransmissionLossConfig.cogwheelValue()
            TransmissionBlockKind.BELT -> TransmissionLossConfig.beltSegmentValue()
            TransmissionBlockKind.ENCASED_SHAFT -> TransmissionLossConfig.encasedShaftValue()
            TransmissionBlockKind.CHAIN_DRIVE -> TransmissionLossConfig.chainDriveValue()
            TransmissionBlockKind.SHAFT -> TransmissionLossConfig.shaftValue()
        }
    }

    private fun blockKind(state: BlockState): TransmissionBlockKind? {
        val key = ForgeRegistries.BLOCKS.getKey(state.block) ?: return null
        if (key.namespace != "create") return null

        return when {
            key.path == "gearbox" -> TransmissionBlockKind.GEARBOX
            key.path == "large_cogwheel" || key.path.endsWith("_large_cogwheel") -> TransmissionBlockKind.LARGE_COGWHEEL
            key.path == "cogwheel" || key.path.endsWith("_cogwheel") -> TransmissionBlockKind.COGWHEEL
            key.path == "belt" -> TransmissionBlockKind.BELT
            key.path.contains("encased_shaft") && TransmissionLossConfig.includeEncasedShaftsValue() -> TransmissionBlockKind.ENCASED_SHAFT
            key.path.contains("chain_drive") || key.path.contains("chain_gearshift") -> TransmissionBlockKind.CHAIN_DRIVE
            key.path == "shaft" || key.path.endsWith("_shaft") -> TransmissionBlockKind.SHAFT
            else -> null
        }
    }

    private data class NetworkSample(
        val id: NetworkId,
        val gameTime: Long,
        val breakdown: TransmissionBreakdown
    )

    private data class BreakdownTally(
        var shaftBlocks: Int = 0,
        var encasedShaftBlocks: Int = 0,
        var cogwheels: Int = 0,
        var largeCogwheels: Int = 0,
        var gearboxes: Int = 0,
        var beltSegments: Int = 0,
        var beltPulleys: Int = 0,
        var chainDrives: Int = 0
    ) {
        fun toBreakdown(rpm: Float) = TransmissionBreakdown(
            shaftBlocks = shaftBlocks,
            encasedShaftBlocks = encasedShaftBlocks,
            cogwheels = cogwheels,
            largeCogwheels = largeCogwheels,
            gearboxes = gearboxes,
            beltSegments = beltSegments,
            beltPulleys = beltPulleys,
            chainDrives = chainDrives,
            rpm = rpm
        )
    }

    private fun TransmissionBreakdown.count(kind: TransmissionBlockKind): Int {
        return when (kind) {
            TransmissionBlockKind.SHAFT -> shaftBlocks
            TransmissionBlockKind.ENCASED_SHAFT -> encasedShaftBlocks
            TransmissionBlockKind.COGWHEEL -> cogwheels
            TransmissionBlockKind.LARGE_COGWHEEL -> largeCogwheels
            TransmissionBlockKind.GEARBOX -> gearboxes
            TransmissionBlockKind.BELT -> beltSegments
            TransmissionBlockKind.CHAIN_DRIVE -> chainDrives
        }
    }

    private enum class TransmissionBlockKind(private val singularLabel: String, private val pluralLabel: String) {
        SHAFT("shaft", "shafts"),
        ENCASED_SHAFT("encased shaft", "encased shafts"),
        COGWHEEL("cogwheel", "cogwheels"),
        LARGE_COGWHEEL("large cogwheel", "large cogwheels"),
        GEARBOX("gearbox", "gearboxes"),
        BELT("belt segment", "belt segments"),
        CHAIN_DRIVE("chain drive", "chain drives");

        fun label(count: Int): String = if (count == 1) singularLabel else pluralLabel
    }
}
