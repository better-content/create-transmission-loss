package com.bettercontent.createtransmissionloss.network

import com.bettercontent.createtransmissionloss.config.TransmissionLossConfig
import com.simibubi.create.content.kinetics.KineticNetwork
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.registries.ForgeRegistries
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

    fun summarizeBlockLoss(blockEntity: KineticBlockEntity, state: BlockState, rpm: Float): BlockLossSummary? {
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

    fun resolveNetworkId(network: KineticNetwork): NetworkId? {
        val members = networkMembers(network)
        if (members.isEmpty()) return null
        return selectNetworkId(members.map {
            NetworkMemberIdentity(it.level?.dimension()?.location()?.toString(), it.blockPos.asLong())
        })
    }

    fun refreshLoss(network: KineticNetwork, force: Boolean = false): CachedLoss? {
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

    fun refreshLossFromBlockEntity(blockEntity: KineticBlockEntity, force: Boolean = false): CachedLoss? =
        refreshLoss(blockEntity.getOrCreateNetwork(), force)

    private fun sampleNetwork(network: KineticNetwork): NetworkSample? {
        val members = networkMembers(network)
        if (members.isEmpty()) return null

        val canonicalPos = members.asSequence()
            .map { it.blockPos.asLong() }
            .minOrNull() ?: return null

        val dimension = members.asSequence()
            .mapNotNull { it.level?.dimension()?.location()?.toString() }
            .firstOrNull() ?: "minecraft:overworld"

        var gameTime = 0L
        var maxRpm = 0f
        val tally = BreakdownTally()

        members.forEach { member ->
            member.level?.let { level ->
                gameTime = maxOf(gameTime, level.gameTime)
                countTransmissionBlock(level.getBlockState(member.blockPos), tally)
            }
            maxRpm = maxOf(maxRpm, abs(member.speed))
        }

        return NetworkSample(
            id = NetworkId(dimension, canonicalPos),
            gameTime = gameTime,
            breakdown = tally.toBreakdown(maxRpm)
        )
    }

    private fun networkMembers(network: KineticNetwork): Set<KineticBlockEntity> =
        (network.members.keys + network.sources.keys).toSet()

    internal fun selectNetworkId(members: Collection<NetworkMemberIdentity>): NetworkId? {
        val canonicalPos = members.minOfOrNull(NetworkMemberIdentity::blockPos) ?: return null
        val dimension = members.firstNotNullOfOrNull(NetworkMemberIdentity::dimension)
            ?: "minecraft:overworld"
        return NetworkId(dimension, canonicalPos)
    }

    internal data class NetworkMemberIdentity(val dimension: String?, val blockPos: Long)

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
