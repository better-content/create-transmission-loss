package io.github.transmissionloss.network

import java.lang.reflect.Modifier

object NetworkRuntimeBridge {
    fun resolveNetworkId(network: Any): NetworkId? {
        val members = findMemberMapValues(network) ?: return null
        if (members.isEmpty()) return null

        val canonicalPos = members.asSequence()
            .mapNotNull { extractBlockPosLong(it) }
            .minOrNull() ?: return null

        val dimension = members.asSequence()
            .mapNotNull { extractDimensionId(it) }
            .firstOrNull() ?: "minecraft:overworld"

        return NetworkId(dimension, canonicalPos)
    }

    private fun findMemberMapValues(network: Any): Collection<Any>? {
        val fields = network.javaClass.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
        for (field in fields) {
            runCatching {
                field.isAccessible = true
                val value = field.get(network)
                if (value is Map<*, *>) {
                    val values = value.values.filterNotNull()
                    if (values.isNotEmpty()) {
                        @Suppress("UNCHECKED_CAST")
                        return values as Collection<Any>
                    }
                }
            }
        }
        return null
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
}
