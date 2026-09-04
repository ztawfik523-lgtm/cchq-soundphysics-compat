package dev.cchqphysics.compat.audio;

import java.lang.reflect.Field;
import java.util.UUID;

final class HQPayloadView {
    private static final String HQ_PREFIX = "com.tom.hqspeaker.";
    private static final String AUDIO_PACKET = "HQSpeakerAudioPacket";
    private static final String STOP_PACKET = "HQSpeakerStopPacket";

    private HQPayloadView() {
    }

    static boolean isAudioPayload(Object payload) {
        return hasName(payload, AUDIO_PACKET);
    }

    static boolean isStopPayload(Object payload) {
        return hasName(payload, STOP_PACKET);
    }

    private static boolean hasName(Object payload, String simpleName) {
        if (payload == null) {
            return false;
        }
        Class<?> type = payload.getClass();
        return simpleName.equals(type.getSimpleName()) && type.getName().startsWith(HQ_PREFIX);
    }

    static Audio audio(Object payload) throws ReflectiveOperationException {
        Class<?> type = payload.getClass();
        UUID source = (UUID) field(type, "source").get(payload);
        Object format = field(type, "format").get(payload);
        float volume = ((Number) field(type, "volume").get(payload)).floatValue();
        float x = ((Number) field(type, "x").get(payload)).floatValue();
        float y = ((Number) field(type, "y").get(payload)).floatValue();
        float z = ((Number) field(type, "z").get(payload)).floatValue();
        byte[] data = (byte[]) field(type, "data").get(payload);
        long startTick = ((Number) field(type, "startTick").get(payload)).longValue();
        Object group = field(type, "syncGroupId").get(payload);
        UUID syncGroupId = group instanceof UUID uuid ? uuid : null;
        int syncGroupSize = ((Number) field(type, "syncGroupSize").get(payload)).intValue();

        if (source == null || format == null) {
            throw new ReflectiveOperationException("CC:HQ audio packet missing source or format");
        }

        return new Audio(source, String.valueOf(format), volume, x, y, z, data, startTick, syncGroupId, syncGroupSize);
    }

    static UUID stopSource(Object payload) throws ReflectiveOperationException {
        Object source = field(payload.getClass(), "source").get(payload);
        if (source instanceof UUID uuid) {
            return uuid;
        }
        throw new ReflectiveOperationException("CC:HQ stop packet source is not a UUID");
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        return type.getField(name);
    }

    record Audio(UUID source, String format, float volume, float x, float y, float z, byte[] data,
                 long startTick, UUID syncGroupId, int syncGroupSize) {
    }
}
