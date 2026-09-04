package dev.cchqphysics.compat.mixin;

import dev.cchqphysics.compat.audio.RoomSchedulerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftRoomSchedulerMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void cchqphysics$roomSchedulerTick(CallbackInfo ci) {
        RoomSchedulerClient.clientTick();
    }
}
