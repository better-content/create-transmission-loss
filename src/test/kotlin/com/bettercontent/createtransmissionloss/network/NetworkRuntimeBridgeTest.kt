package com.bettercontent.createtransmissionloss.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkRuntimeBridgeTest {
    @Test
    fun resolvesNetworkIdFromMemberMap() {
        val network = FakeNetwork(
            mapOf(
                1L to FakeMember(FakePos(20L), FakeLevel(FakeResourceKey("minecraft:the_nether"))),
                2L to FakeMember(FakePos(7L), FakeLevel(FakeResourceKey("minecraft:the_nether")))
            )
        )

        val id = NetworkRuntimeBridge.resolveNetworkId(network)

        assertEquals(NetworkId("minecraft:the_nether", 7L), id)
    }

    @Test
    fun returnsNullWhenNoMembersCanBeResolved() {
        assertNull(NetworkRuntimeBridge.resolveNetworkId(EmptyNetwork()))
    }

    @Test
    fun resolvesNetworkIdFromSourcesWhenMembersAreEmpty() {
        val network = FakeNetworkWithFallbackSources(
            mapOf(
                3L to FakeMember(FakePos(12L), FakeLevel(FakeResourceKey("minecraft:the_end"))),
                4L to FakeMember(FakePos(2L), FakeLevel(FakeResourceKey("minecraft:the_end")))
            )
        )

        val id = NetworkRuntimeBridge.resolveNetworkId(network)

        assertEquals(NetworkId("minecraft:the_end", 2L), id)
    }

    @Test
    fun returnsDefaultDimensionWhenDimensionCannotBeExtracted() {
        val network = FakeNetwork(
            mapOf(5L to FakeMemberWithoutLevel(FakePos(9L)))
        )

        val id = NetworkRuntimeBridge.resolveNetworkId(network)

        assertEquals(NetworkId("minecraft:overworld", 9L), id)
    }
}

private class FakeNetwork(val members: Map<Long, Any>)
private class FakeNetworkWithFallbackSources(private val sources: Map<Long, FakeMember>)
private class EmptyNetwork
private class FakeMember(val worldPosition: FakePos, private val level: FakeLevel) {
    fun getLevel(): FakeLevel = level
}
private class FakeMemberWithoutLevel(val worldPosition: FakePos) {
    fun getLevel(): FakeLevelWithoutDimension = FakeLevelWithoutDimension()
}

private class FakePos(private val longPos: Long) {
    fun asLong(): Long = longPos
}

private class FakeLevel(private val key: FakeResourceKey) {
    fun dimension(): FakeResourceKey = key
}
private class FakeLevelWithoutDimension {
    fun dimension(): Nothing? = null
}

private class FakeResourceKey(private val id: String) {
    fun location(): String = id
}
