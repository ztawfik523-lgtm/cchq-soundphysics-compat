package dev.cchqphysics.compat.audio;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

final class DistanceBridge {
    private DistanceBridge() {
    }

    static float effectiveGain(HQPayloadView.Audio audio) {
        Minecraft minecraft = Minecraft.getInstance();
        float blocksVolume = minecraft.options == null ? 1.0F : minecraft.options.getSoundSourceVolume(SoundSource.BLOCKS);
        float baseGain = clamp(audio.volume(), 0.0F, 1.0F) * blocksVolume;
        if (baseGain <= 0.0F || minecraft.player == null) {
            return baseGain;
        }

        double dx = minecraft.player.getX() - audio.x();
        double dy = minecraft.player.getY() - audio.y();
        double dz = minecraft.player.getZ() - audio.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double referenceDistance = Math.max(1.0D, AttenuationBridge.referenceDistance(audio));
        double directDistance = Math.max(referenceDistance + 1.0D, AttenuationBridge.directDistance(audio));
        double audibleMaxDistance = Math.max(directDistance + 1.0D, AttenuationBridge.audibleMaxDistance(audio));
        if (distance >= audibleMaxDistance) {
            return 0.0F;
        }

        double inverse = distance <= referenceDistance ? 1.0D : referenceDistance / distance;
        double tail = 1.0D;
        if (distance > directDistance) {
            double t = clamp01((distance - directDistance) / (audibleMaxDistance - directDistance));
            double smoothstep = t * t * (3.0D - 2.0D * t);
            tail = 1.0D - smoothstep;
        }

        double result = baseGain * inverse * tail;
        if (result < 0.001D) {
            return 0.0F;
        }
        return (float) Math.min(1.0D, result);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
