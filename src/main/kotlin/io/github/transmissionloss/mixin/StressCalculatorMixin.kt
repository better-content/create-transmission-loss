package io.github.transmissionloss.mixin

import io.github.transmissionloss.network.LossCache
import io.github.transmissionloss.network.NetworkId
import io.github.transmissionloss.network.NetworkRuntimeBridge
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@Pseudo
@Mixin(targets = ["com.simibubi.create.content.kinetics.KineticNetwork"], remap = false)
abstract class StressCalculatorMixin {
    @Inject(method = ["calculateStress"], at = [At("RETURN")], cancellable = true, require = 0)
    private fun transmissionloss$appendNetworkLoss(cir: CallbackInfoReturnable<Float>) {
        val networkId = transmissionloss$readNetworkId() ?: return
        val loss = LossCache.getLoss(networkId).toFloat()
        cir.returnValue = cir.returnValue + loss
    }

    private fun transmissionloss$readNetworkId(): NetworkId? = NetworkRuntimeBridge.resolveNetworkId(this)
}
