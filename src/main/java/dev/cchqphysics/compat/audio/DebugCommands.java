package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.DiffractionConfig;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
import dev.cchqphysics.compat.config.SpectralMixConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only diagnostics and safe runtime controls. No command mutates server state. */
public final class DebugCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");
    private static boolean initialized;

    private DebugCommands() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        NeoForge.EVENT_BUS.addListener(DebugCommands::register);
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cchqphysics")
                .then(Commands.literal("status")
                        .executes(context -> {
                            String status = DebugControl.compactStatus();
                            context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + status), false);
                            return 1;
                        }))
                .then(Commands.literal("dump")
                        .executes(context -> {
                            LOGGER.info("[phase5/dump] config {} {} {}", ExtendedClientConfig.summary(), SpectralMixConfig.summary(), DiffractionConfig.summary());
                            LOGGER.info("[phase5/dump] {}", DebugControl.compactStatus());
                            SoundPhysicsBridge.debugDumpSources();
                            EnvironmentSmoother.debugDumpEfx();
                            SynchronizedSpectralBalancer.debugDump();
                            VerticalDiffractionRelief.debugDump();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("CC:HQ Physics snapshot written to latest.log"), false);
                            return 1;
                        }))
                .then(Commands.literal("diffraction")
                        .then(Commands.literal("on")
                                .executes(context -> {
                                    DiffractionConfig.setEnabled(true);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("CC:HQ diffraction: ON (runtime only)"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("off")
                                .executes(context -> {
                                    DiffractionConfig.setEnabled(false);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("CC:HQ diffraction: OFF (runtime only)"), false);
                                    return 1;
                                }))
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    String summary = DiffractionConfig.summary();
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("CC:HQ diffraction: " + summary), false);
                                    return 1;
                                })))
                .then(Commands.literal("refresh_rooms")
                        .executes(context -> {
                            DebugControl.requestRoomRefresh();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("CC:HQ Physics room refresh queued on the sound thread"), false);
                            return 1;
                        }))
                .then(Commands.literal("reset_caches")
                        .executes(context -> {
                            DebugControl.requestCacheReset();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("CC:HQ Physics safe cache reset queued on the sound thread"), false);
                            return 1;
                        }))
                .then(Commands.literal("reset_efx")
                        .executes(context -> {
                            DebugControl.requestEfxReset();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("CC:HQ Physics private EFX reset queued on the sound thread"), false);
                            return 1;
                        }))
                .then(Commands.literal("config")
                        .executes(context -> {
                            String summary = ExtendedClientConfig.summary() + " " + SpectralMixConfig.summary() + " " + DiffractionConfig.summary();
                            LOGGER.info("[phase5/config] {}", summary);
                            context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + summary), false);
                            return 1;
                        })));
    }
}
