package dev.cchqphysics.compat.mixin;

import dev.cchqphysics.compat.audio.CompatAudioManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Intercepts complete CC:HQ whole-file payloads after CC:HQ chunk reassembly. */
@Mixin(targets = "com.tom.hqspeaker.client.HQSpeakerClientHandler", remap = false)
public abstract class HQSpeakerClientHandlerMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cchqphysics$receive(@Coerce Object payload, CallbackInfo ci) {
        if (CompatAudioManager.tryHandleAudioPayload(payload)) {
            ci.cancel();
        }
    }
}
