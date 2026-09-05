package dev.cchqphysics.compat.audio;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client-only runtime A/B commands for acoustic-mix isolation. */
public final class AcousticProbeCommands {
    private static boolean initialized;

    private AcousticProbeCommands() {}

    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        NeoForge.EVENT_BUS.addListener(AcousticProbeCommands::register);
    }

    private static void register(RegisterClientCommandsEvent event) {
        var root = Commands.literal("cchqacoustic");

        root.then(Commands.literal("status")
                .executes(context -> {
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics: " + AcousticMixProbe.status()), false);
                    return 1;
                }));

        var source = Commands.literal("source");
        var sourceId = Commands.argument("sourceId", IntegerArgumentType.integer(0));

        sourceId.then(Commands.literal("sends_off")
                .executes(context -> setMode(context, AcousticMixProbe.Mode.SENDS_OFF)));
        sourceId.then(Commands.literal("direct_hf_bypass")
                .executes(context -> setMode(context, AcousticMixProbe.Mode.DIRECT_HF_BYPASS)));
        sourceId.then(Commands.literal("auto")
                .executes(context -> setMode(context, AcousticMixProbe.Mode.AUTO)));
        sourceId.then(Commands.literal("status")
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "sourceId");
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics: " + AcousticMixProbe.sourceStatus(id)), false);
                    return 1;
                }));

        source.then(sourceId);
        root.then(source);
        event.getDispatcher().register(root);
    }

    private static int setMode(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
                               AcousticMixProbe.Mode mode) {
        int id = IntegerArgumentType.getInteger(context, "sourceId");
        AcousticMixProbe.setMode(id, mode);
        context.getSource().sendSuccess(
                () -> Component.literal("CC:HQ Physics: " + AcousticMixProbe.sourceStatus(id)
                        + " (runtime-only; allow one normal acoustic refresh before judging)"), false);
        return 1;
    }
}
