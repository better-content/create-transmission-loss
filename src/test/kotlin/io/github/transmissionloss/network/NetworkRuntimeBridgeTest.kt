package io.github.transmissionloss.network

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
}

private class FakeNetwork(val members: Map<Long, FakeMember>)
private class EmptyNetwork
private class FakeMember(val worldPosition: FakePos, private val level: FakeLevel) {
    fun getLevel(): FakeLevel = level
}

private class FakePos(private val longPos: Long) {
    fun asLong(): Long = longPos
}

private class FakeLevel(private val key: FakeResourceKey) {
    fun dimension(): FakeResourceKey = key
}

private class FakeResourceKey(private val id: String) {
    fun location(): String = id
}
