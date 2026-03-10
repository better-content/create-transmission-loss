package io.github.transmissionloss.mixin

import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Pseudo

/**
 * Placeholder client hook for adding "Transmission Loss: X SU" into goggles/overlay text.
 */
@Pseudo
@Mixin(targets = ["com.simibubi.create.foundation.gui.RemovedGuiOverlay"], remap = false)
abstract class OverlayMixin
