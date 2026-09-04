package dev.cchqphysics.compat.mixin;

import com.sonicether.soundphysics.utils.RaycastUtils;
import dev.cchqphysics.compat.audio.Beta11RoomRayCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
public abstract class SoundPhysicsRoomRayMemoMixin {
    @Redirect(
            method = "evaluateEnvironment",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sonicether/soundphysics/utils/RaycastUtils;rayCast(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;",
                    remap = false
            ),
            remap = false,
            require = 2
    )
    private static BlockHitResult cchqphysics$memoizedRoomRay(BlockGetter level, Vec3 from, Vec3 to, BlockPos ignore) {
        return Beta11RoomRayCache.rayCast(level, from, to, ignore);
    }
}
