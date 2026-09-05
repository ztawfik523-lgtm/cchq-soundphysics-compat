package dev.cchqphysics.compat.audio;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.cchqphysics.compat.config.ExtendedClientConfig;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only Phase-5 validation commands. No command mutates server state. */
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
        var root = Commands.literal("cchqphysics");

        root.then(Commands.literal("status")
                .executes(context -> {
                    String status = DebugControl.compactStatus() + " " + ReflectionDiagnostics.status();
                    context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + status), false);
                    return 1;
                }));

        root.then(Commands.literal("dump")
                .executes(context -> {
                    LOGGER.info("[phase5/dump] config {}", ExtendedClientConfig.summary());
                    LOGGER.info("[phase5/dump] {} {}", DebugControl.compactStatus(), ReflectionDiagnostics.status());
                    IssueADiagnostics.dump();
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics Issue-A snapshot written to latest.log"), false);
                    return 1;
                }));

        var reflection = Commands.literal("reflection_redirect");
        reflection.then(Commands.literal("on")
                .executes(context -> {
                    ReflectionDiagnostics.setGlobalRedirectEnabled(true);
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics reflection position redirection: ON globally (known-good default)"), false);
                    return 1;
                }));
        reflection.then(Commands.literal("off")
                .executes(context -> {
                    ReflectionDiagnostics.setGlobalRedirectEnabled(false);
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics reflection position redirection: OFF globally (runtime diagnostic only)"), false);
                    return 1;
                }));
        reflection.then(Commands.literal("status")
                .executes(context -> {
                    String status = ReflectionDiagnostics.status();
                    context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + status), false);
                    return 1;
                }));

        var source = Commands.literal("source");
        var sourceId = Commands.argument("sourceId", IntegerArgumentType.integer(0));
        sourceId.then(Commands.literal("on")
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "sourceId");
                    ReflectionDiagnostics.setSourceOverride(id, true);
                    String status = ReflectionDiagnostics.sourceStatus(id);
                    context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + status), false);
                    return 1;
                }));
        sourceId.then(Commands.literal("off")
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "sourceId");
                    ReflectionDiagnostics.setSourceOverride(id, false);
                    String status = ReflectionDiagnostics.sourceStatus(id);
                    context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + status), false);
                    return 1;
                }));
        sourceId.then(Commands.literal("auto")
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "sourceId");
                    ReflectionDiagnostics.setSourceOverride(id, null);
                    String status = ReflectionDiagnostics.sourceStatus(id);
                    context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + status), false);
                    return 1;
                }));
        sourceId.then(Commands.literal("status")
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "sourceId");
                    String status = ReflectionDiagnostics.sourceStatus(id);
                    context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + status), false);
                    return 1;
                }));
        source.then(sourceId);
        reflection.then(source);
        root.then(reflection);

        root.then(Commands.literal("refresh_rooms")
                .executes(context -> {
                    DebugControl.requestRoomRefresh();
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics room refresh queued on the sound thread"), false);
                    return 1;
                }));

        root.then(Commands.literal("reset_caches")
                .executes(context -> {
                    DebugControl.requestCacheReset();
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics safe cache reset queued on the sound thread"), false);
                    return 1;
                }));

        root.then(Commands.literal("reset_efx")
                .executes(context -> {
                    DebugControl.requestEfxReset();
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics private EFX reset queued on the sound thread"), false);
                    return 1;
                }));

        root.then(Commands.literal("config")
                .executes(context -> {
                    String summary = ExtendedClientConfig.summary() + " " + ReflectionDiagnostics.status();
                    LOGGER.info("[phase5/config] {}", summary);
                    context.getSource().sendSuccess(() -> Component.literal("CC:HQ Physics: " + summary), false);
                    return 1;
                }));

        event.getDispatcher().register(root);
    }
}
