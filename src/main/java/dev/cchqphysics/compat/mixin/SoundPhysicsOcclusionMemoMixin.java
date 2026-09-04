package dev.cchqphysics.compat.mixin;

import dev.cchqphysics.compat.audio.Beta10Optimizer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
public abstract class SoundPhysicsOcclusionMemoMixin {
    @Redirect(
            method = "calculateOcclusion",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sonicether/soundphysics/SoundPhysics;runOcclusion(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)D",
                    remap = false
            ),
            remap = false,
            require = 1
    )
    private static double cchqphysics$memoizedOcclusion(Vec3 from, Vec3 to) {
        return Beta10Optimizer.runOcclusionSpr(from, to);
    }
}
