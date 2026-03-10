package io.github.transmissionloss.mixin

import io.github.transmissionloss.network.LossCache
import io.github.transmissionloss.network.NetworkId
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/**
 * Hooks network topology changes and marks cache entries dirty for debounced recompute.
 */
@Pseudo
@Mixin(targets = ["com.simibubi.create.content.kinetics.KineticNetwork"], remap = false)
abstract class NetworkDirtyMixin {
    @Inject(method = ["add"], at = [At("TAIL")], require = 0)
    private fun transmissionloss$onNodeAdded(ci: CallbackInfo) {
        markDirty()
    }

    @Inject(method = ["remove"], at = [At("TAIL")], require = 0)
    private fun transmissionloss$onNodeRemoved(ci: CallbackInfo) {
        markDirty()
    }

    private fun markDirty() {
        val id = transmissionloss$readNetworkId() ?: return
        LossCache.markDirty(id)
    }

    private fun transmissionloss$readNetworkId(): NetworkId? {
        return null
    }
}
