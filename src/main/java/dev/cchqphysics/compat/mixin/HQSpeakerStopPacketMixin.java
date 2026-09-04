package dev.cchqphysics.compat.mixin;

import dev.cchqphysics.compat.audio.CompatAudioManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mirrors CC:HQ stop packets into compat-owned raw OpenAL source cleanup. */
@Mixin(targets = "com.tom.hqspeaker.network.HQSpeakerStopPacket", remap = false)
public abstract class HQSpeakerStopPacketMixin {
    @Inject(
            method = "handle(Lcom/tom/hqspeaker/network/HQSpeakerStopPacket;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V",
            at = @At(value = "HEAD", remap = false),
            remap = false
    )
    private static void cchqphysics$stop(@Coerce Object payload, @Coerce Object context, CallbackInfo ci) {
        CompatAudioManager.tryHandleStopPayload(payload);
    }
}
