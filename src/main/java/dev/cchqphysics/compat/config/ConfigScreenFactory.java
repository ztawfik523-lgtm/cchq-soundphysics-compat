package dev.cchqphysics.compat.config;

import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

public final class ConfigScreenFactory {
    private ConfigScreenFactory() {
    }

    public static Screen create(ModContainer container, Screen parent) {
        try {
            Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder", false, ConfigScreenFactory.class.getClassLoader());
            Class<?> clazz = Class.forName("dev.cchqphysics.compat.config.ClothConfigScreen");
            return (Screen) clazz.getMethod("create", Screen.class).invoke(null, parent);
        } catch (Throwable ignored) {
            return new ConfigurationScreen(container, parent);
        }
    }
}
