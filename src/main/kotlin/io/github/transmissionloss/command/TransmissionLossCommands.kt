package io.github.transmissionloss.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object TransmissionLossCommands {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            Commands.literal("transloss")
                .then(Commands.literal("debug").then(Commands.literal("here").executes { 1 }))
                .then(Commands.literal("recalc").then(Commands.literal("here").executes { 1 }))
                .then(Commands.literal("profile").executes { 1 })
        )
    }
}
