package com.bettercontent.createtransmissionloss.mixin

import com.bettercontent.createtransmissionloss.network.NetworkRuntimeBridge
import com.simibubi.create.content.kinetics.KineticNetwork
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Pseudo
@Mixin(targets = ["com.simibubi.create.content.kinetics.KineticNetwork"], remap = false)
abstract class StressCalculatorMixin {
    @Inject(method = ["calculateStress"], at = [At("RETURN")], cancellable = true, require = 0, remap = false)
    private fun appendNetworkLoss(cir: CallbackInfoReturnable<Float>) {
        val cached = NetworkRuntimeBridge.refreshLoss(this as KineticNetwork) ?: return
        cir.returnValue = cir.returnValue + cached.lossSu.toFloat()
    }
}
