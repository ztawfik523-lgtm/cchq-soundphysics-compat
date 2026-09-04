package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;

final class AttenuationBridge {
    private static final float CCHQ_BASE_DISTANCE = 16.0F;
    private static volatile long lastConfigReadNs;
    private static volatile float cachedAttenuationFactor = 1.0F;
    private static volatile float cachedSoundDistanceAllowance = 4.0F;
    private static volatile double cachedMaxProcessingDistance = 512.0D;
    private static volatile int cachedLevelCloneRange = 4;

    private AttenuationBridge() {}

    static float directDistance(HQPayloadView.Audio audio) {
        float volume = clamp(audio.volume(), 0.0F, 3.0F);
        float base = CCHQ_BASE_DISTANCE * Math.max(1.0F, volume);
        return base / attenuationFactor();
    }

    static float referenceDistance(HQPayloadView.Audio audio) {
        return directDistance(audio) * 0.5F;
    }

    static float physicsMaxDistance() {
        refreshConfig();
        double cloneSafe = Math.max(24.0D, cachedLevelCloneRange * 16.0D - 4.0D);
        double processingSafe = Math.min(cloneSafe, cachedMaxProcessingDistance);
        return (float) Math.min(2048.0D, Math.max(16.0D, processingSafe));
    }

    static float audibleMaxDistance(HQPayloadView.Audio audio) {
        refreshConfig();
        double base = CCHQ_BASE_DISTANCE / attenuationFactor();
        double allowance = base * cachedSoundDistanceAllowance;
        double endpoint = Math.min(allowance, physicsMaxDistance());
        endpoint = Math.max(base * 0.5D + 8.0D, endpoint);
        double rangeScale = 1.0D;
        if (ClientConfig.rangeScaling()) {
            rangeScale = Math.max(1.0D, clamp(audio.volume(), 0.0F, 3.0F));
        }
        double audible = endpoint * rangeScale * ClientConfig.audibleRangeMultiplier();
        audible = Math.max(referenceDistance(audio) + 8.0D, audible);
        return (float) Math.min(2048.0D, audible);
    }

    static float maxDistance(HQPayloadView.Audio audio) {
        return audibleMaxDistance(audio);
    }

    static double maxDistanceSquared(HQPayloadView.Audio audio) {
        double max = audibleMaxDistance(audio);
        return max * max;
    }

    static float soundDistanceAllowance() { refreshConfig(); return cachedSoundDistanceAllowance; }
    static double maxProcessingDistance() { refreshConfig(); return cachedMaxProcessingDistance; }
    static int levelCloneRange() { refreshConfig(); return cachedLevelCloneRange; }
    private static float attenuationFactor() { refreshConfig(); return cachedAttenuationFactor; }

    private static void refreshConfig() {
        long now = System.nanoTime();
        if (now - lastConfigReadNs < 1_000_000_000L) return;
        synchronized (AttenuationBridge.class) {
            now = System.nanoTime();
            if (now - lastConfigReadNs < 1_000_000_000L) return;
            float attenuationFactor = 1.0F;
            float soundDistanceAllowance = 4.0F;
            double maxProcessingDistance = 512.0D;
            int levelCloneRange = 4;
            try {
                Class<?> mod = Class.forName("com.sonicether.soundphysics.SoundPhysicsMod");
                Object config = mod.getField("CONFIG").get(null);
                attenuationFactor = readFloat(config, "attenuationFactor", attenuationFactor);
                soundDistanceAllowance = readFloat(config, "soundDistanceAllowance", soundDistanceAllowance);
                maxProcessingDistance = readDouble(config, "maxSoundProcessingDistance", maxProcessingDistance);
                levelCloneRange = readInt(config, "levelCloneRange", levelCloneRange);
            } catch (Throwable ignored) {}
            cachedAttenuationFactor = clamp(attenuationFactor, 0.1F, 1.0F);
            cachedSoundDistanceAllowance = clamp(soundDistanceAllowance, 1.0F, 6.0F);
            cachedMaxProcessingDistance = Math.max(16.0D, maxProcessingDistance);
            cachedLevelCloneRange = Math.max(2, Math.min(16, levelCloneRange));
            lastConfigReadNs = now;
        }
    }

    private static float readFloat(Object config, String name, float fallback) throws Exception {
        Object entry = config.getClass().getField(name).get(config);
        Object value = entry.getClass().getMethod("get").invoke(entry);
        return value instanceof Number number ? number.floatValue() : fallback;
    }

    private static double readDouble(Object config, String name, double fallback) throws Exception {
        Object entry = config.getClass().getField(name).get(config);
        Object value = entry.getClass().getMethod("get").invoke(entry);
        return value instanceof Number number ? number.doubleValue() : fallback;
    }

    private static int readInt(Object config, String name, int fallback) throws Exception {
        Object entry = config.getClass().getField(name).get(config);
        Object value = entry.getClass().getMethod("get").invoke(entry);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
