package io.github.transmissionloss.command

import com.mojang.brigadier.CommandDispatcher
import io.github.transmissionloss.network.LossCache
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component

object TransmissionLossCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("transloss")
                .then(Commands.literal("debug").then(Commands.literal("here").executes {
                    it.source.sendSuccess({ Component.literal("Transmission Loss cache entries: ${LossCache.size()}") }, false)
                    1
                }))
                .then(Commands.literal("recalc").then(Commands.literal("here").executes {
                    it.source.sendSuccess({ Component.literal("Marked network for recalc (manual lookup TODO).") }, false)
                    1
                }))
                .then(Commands.literal("profile").executes {
                    it.source.sendSuccess({ Component.literal("Profiling hook not yet implemented; cache size=${LossCache.size()}") }, false)
                    1
                })
        )
    }
}
