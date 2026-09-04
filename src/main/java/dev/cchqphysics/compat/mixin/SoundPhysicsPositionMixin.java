package dev.cchqphysics.compat.mixin;

import dev.cchqphysics.compat.audio.AcousticCapture;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
public abstract class SoundPhysicsPositionMixin {
    @Inject(method = "setSoundPos(ILnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cchqphysics$captureReflectedPosition(int sourceId, Vec3 position, CallbackInfo ci) {
        if (AcousticCapture.captureSoundPos(sourceId, position)) {
            ci.cancel();
        }
    }
}
