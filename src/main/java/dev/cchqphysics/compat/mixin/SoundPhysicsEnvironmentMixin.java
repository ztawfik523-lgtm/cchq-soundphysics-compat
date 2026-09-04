package dev.cchqphysics.compat.mixin;

import dev.cchqphysics.compat.audio.AcousticCapture;
import dev.cchqphysics.compat.audio.EnvironmentSmoother;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
public abstract class SoundPhysicsEnvironmentMixin {
    @Inject(method = "setEnvironment(IFFFFFFFFFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cchqphysics$captureOrSmoothEnvironment(
            int sourceId,
            float r0,
            float r1,
            float r2,
            float r3,
            float h0,
            float h1,
            float h2,
            float h3,
            float directCutoff,
            float directGain,
            CallbackInfo ci) {
        if (AcousticCapture.shouldBypassEnvironment(sourceId)) {
            return;
        }
        if (AcousticCapture.captureEnvironment(sourceId, r0, r1, r2, r3, h0, h1, h2, h3, directCutoff, directGain)) {
            ci.cancel();
            return;
        }
        if (EnvironmentSmoother.intercept(sourceId, r0, r1, r2, r3, h0, h1, h2, h3, directCutoff, directGain)) {
            ci.cancel();
        }
    }
}
