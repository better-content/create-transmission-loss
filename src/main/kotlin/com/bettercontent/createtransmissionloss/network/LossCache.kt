package com.bettercontent.createtransmissionloss.network

import com.bettercontent.createtransmissionloss.config.TransmissionLossConfig
import java.util.concurrent.ConcurrentHashMap

object LossCache {
    private val cache = ConcurrentHashMap<NetworkId, CachedLoss>()

    fun bootstrap() {
        cache.clear()
    }

    fun markDirty(networkId: NetworkId) {
        cache.compute(networkId) { _, current ->
            (current ?: CachedLoss()).copy(dirty = true)
        }
    }

    fun set(networkId: NetworkId, gameTime: Long, breakdown: TransmissionBreakdown) {
        cache[networkId] = CachedLoss(
            lossSu = NetworkScanner.computeLoss(breakdown),
            breakdown = breakdown,
            lastRecalcGameTime = gameTime,
            dirty = false
        )
    }

    fun getLoss(networkId: NetworkId): Double = cache[networkId]?.lossSu ?: 0.0

    fun snapshot(networkId: NetworkId): CachedLoss? = cache[networkId]

    fun shouldRecalc(networkId: NetworkId, gameTime: Long, force: Boolean): Boolean {
        if (force) return true
        val entry = cache[networkId] ?: return true
        if (!entry.dirty) return false
        return gameTime - entry.lastRecalcGameTime >= TransmissionLossConfig.recalcCooldownTicksValue()
    }

    fun size(): Int = cache.size
}

data class CachedLoss(
    val lossSu: Double = 0.0,
    val breakdown: TransmissionBreakdown = TransmissionBreakdown(),
    val lastRecalcGameTime: Long = 0L,
    val dirty: Boolean = true
)

data class NetworkId(
    val dimension: String,
    val canonicalPosLong: Long
)
