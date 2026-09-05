package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reflection bridge used only by the Cloth Config screen for advanced runtime controls. */
public final class ExtendedClientConfigAccess {
    private static final Map<String, ModConfigSpec.ConfigValue<?>> CACHE = new ConcurrentHashMap<>();

    private ExtendedClientConfigAccess() {}

    private static ModConfigSpec.ConfigValue<?> value(String name) {
        return CACHE.computeIfAbsent(name, key -> {
            try {
                Field field = ExtendedClientConfig.class.getDeclaredField(key);
                field.setAccessible(true);
                return (ModConfigSpec.ConfigValue<?>) field.get(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to access advanced config field " + key, e);
            }
        });
    }

    public static boolean bool(String name, boolean fallback) {
        try {
            Object raw = value(name).getRaw();
            return raw instanceof Boolean b ? b : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static double dbl(String name, double fallback) {
        try {
            Object raw = value(name).getRaw();
            return raw instanceof Number n ? n.doubleValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static int integer(String name, int fallback) {
        try {
            Object raw = value(name).getRaw();
            return raw instanceof Number n ? n.intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void set(String name, Object newValue) {
        ((ModConfigSpec.ConfigValue) value(name)).set(newValue);
    }

    public static void save() {
        ExtendedClientConfig.SPEC.save();
    }
}
