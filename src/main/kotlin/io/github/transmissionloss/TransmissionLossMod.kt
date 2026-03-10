package io.github.transmissionloss

import com.mojang.brigadier.CommandDispatcher
import io.github.transmissionloss.command.TransmissionLossCommands
import io.github.transmissionloss.config.TransmissionLossConfig
import io.github.transmissionloss.network.LossCache
import net.minecraft.commands.CommandSourceStack
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod(TransmissionLossMod.MOD_ID)
class TransmissionLossMod {
    init {
        TransmissionLossConfig.register()
        LossCache.bootstrap()
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        register(event.dispatcher)
    }

    private fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        TransmissionLossCommands.register(dispatcher)
    }

    companion object {
        const val MOD_ID = "transmissionloss"
    }
}
