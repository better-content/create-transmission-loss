package io.github.transmissionloss.mixin

import io.github.transmissionloss.network.LossCache
import io.github.transmissionloss.network.NetworkId
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Intentional soft target:
 * In Create 0.5.x this should be retargeted to the kinetic network stress aggregation method.
 */
@Pseudo
@Mixin(targets = ["com.simibubi.create.content.kinetics.KineticNetwork"], remap = false)
abstract class StressCalculatorMixin {
    @Inject(method = ["calculateStress"], at = [At("RETURN")], cancellable = true, require = 0)
    private fun transmissionloss$appendNetworkLoss(cir: CallbackInfoReturnable<Float>) {
        val networkId = transmissionloss$readNetworkId() ?: return
        val loss = LossCache.getLoss(networkId).toFloat()
        cir.returnValue = cir.returnValue + loss
    }

    private fun transmissionloss$readNetworkId(): NetworkId? {
        return null
    }
}
