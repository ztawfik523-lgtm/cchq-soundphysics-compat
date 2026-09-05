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
                            () -> Component.literal("CC:HQ Physics: " + AcousticMixProbe.status()
                                    + " " + EnvironmentSmoother.debugDarkestSourceSummary()), false);
                    return 1;
                }));

        root.then(Commands.literal("sends_off")
                .executes(context -> setDarkestMode(context, AcousticMixProbe.Mode.SENDS_OFF)));
        root.then(Commands.literal("hf_lift_25")
                .executes(context -> setDarkestMode(context, AcousticMixProbe.Mode.HF_LIFT_25)));
        root.then(Commands.literal("hf_lift_50")
                .executes(context -> setDarkestMode(context, AcousticMixProbe.Mode.HF_LIFT_50)));
        root.then(Commands.literal("hf_lift_75")
                .executes(context -> setDarkestMode(context, AcousticMixProbe.Mode.HF_LIFT_75)));
        root.then(Commands.literal("direct_hf_bypass")
                .executes(context -> setDarkestMode(context, AcousticMixProbe.Mode.DIRECT_HF_BYPASS)));
        root.then(Commands.literal("auto")
                .executes(context -> {
                    AcousticMixProbe.clearAll();
                    context.getSource().sendSuccess(
                            () -> Component.literal("CC:HQ Physics: acoustic probe restored to AUTO for all sources"), false);
                    return 1;
                }));

        // Explicit source targeting is retained only for deep diagnostics; normal testing uses the no-ID commands above.
        var source = Commands.literal("source");
        var sourceId = Commands.argument("sourceId", IntegerArgumentType.integer(0));

        sourceId.then(Commands.literal("sends_off")
                .executes(context -> setMode(context, AcousticMixProbe.Mode.SENDS_OFF)));
        sourceId.then(Commands.literal("hf_lift_25")
                .executes(context -> setMode(context, AcousticMixProbe.Mode.HF_LIFT_25)));
        sourceId.then(Commands.literal("hf_lift_50")
                .executes(context -> setMode(context, AcousticMixProbe.Mode.HF_LIFT_50)));
        sourceId.then(Commands.literal("hf_lift_75")
                .executes(context -> setMode(context, AcousticMixProbe.Mode.HF_LIFT_75)));
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

    private static int setDarkestMode(
            com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
            AcousticMixProbe.Mode mode) {
        int id = EnvironmentSmoother.debugDarkestSourceId();
        if (id < 0) {
            context.getSource().sendFailure(Component.literal(
                    "CC:HQ Physics: no initialized private-EFX source is available yet; start the synchronized playback first"));
            return 0;
        }
        AcousticMixProbe.clearAll();
        AcousticMixProbe.setMode(id, mode);
        String summary = EnvironmentSmoother.debugDarkestSourceSummary();
        SoundPhysicsBridge.beta9Log("[phase5/acoustic-probe-auto] selected " + summary);
        context.getSource().sendSuccess(
                () -> Component.literal("CC:HQ Physics: applied " + mode.wireName()
                        + " to the darkest tracked speaker automatically; allow one normal acoustic refresh before judging"), false);
        return 1;
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
