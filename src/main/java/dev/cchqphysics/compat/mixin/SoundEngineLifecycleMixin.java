package dev.cchqphysics.compat.mixin;

import dev.cchqphysics.compat.audio.CompatAudioManager;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes compat-owned raw OpenAL sources follow the vanilla sound-engine lifecycle. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineLifecycleMixin {
    @Inject(method = "pause", at = @At("HEAD"))
    private void cchqphysics$pause(CallbackInfo ci) {
        CompatAudioManager.pauseCompatSources();
    }

    @Inject(method = "resume", at = @At("HEAD"))
    private void cchqphysics$resume(CallbackInfo ci) {
        CompatAudioManager.resumeCompatSources();
    }

    @Inject(method = "stopAll", at = @At("HEAD"))
    private void cchqphysics$stopAll(CallbackInfo ci) {
        CompatAudioManager.stopAllCompatSources();
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void cchqphysics$destroy(CallbackInfo ci) {
        CompatAudioManager.resetForSoundEngine();
    }

    @Inject(method = "emergencyShutdown", at = @At("HEAD"))
    private void cchqphysics$emergencyShutdown(CallbackInfo ci) {
        CompatAudioManager.resetForSoundEngine();
    }

    @Inject(method = "reload", at = @At("HEAD"))
    private void cchqphysics$reload(CallbackInfo ci) {
        CompatAudioManager.resetForSoundEngine();
    }
}
