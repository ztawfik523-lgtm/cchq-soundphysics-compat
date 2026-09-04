package dev.cchqphysics.compat.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public final class ClothConfigScreen {
    private static final int DESCRIPTION = 11053224;

    private ClothConfigScreen() {}

    private static Component t(String text) {
        return Component.literal(text);
    }

    private static Component[] tip(String... lines) {
        Component[] result = new Component[lines.length];
        for (int i = 0; i < lines.length; i++) {
            result[i] = t(lines[i]);
        }
        return result;
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(t("CC:HQ × Sound Physics"))
                .setTransparentBackground(true)
                .setShouldTabsSmoothScroll(true)
                .setShouldListSmoothScroll(true)
                .setAlwaysShowTabs(true)
                .setDoesConfirmSave(false)
                .setSavingRunnable(ClientConfigAccess::save);

        ConfigEntryBuilder entries = builder.entryBuilder();
        general(builder, entries);
        distance(builder, entries);
        occlusion(builder, entries);
        direction(builder, entries);
        smoothing(builder, entries);
        performance(builder, entries);
        return builder.build();
    }

    private static void general(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("General"));
        category.addEntry(entries.startTextDescription(
                        t("CC:HQ whole-file audio processed as real positional Sound Physics sources."))
                .setColor(DESCRIPTION)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Enable compatibility"), rawBool("ENABLED", true))
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Intercept CC:HQ whole-file MP3/OGG/WAV playback.",
                        "Disable to let CC:HQ handle newly-started sounds normally.",
                        "Already-playing compat sources are not forcibly restarted."))
                .setSaveConsumer(value -> ClientConfigAccess.set("ENABLED", value))
                .build());

        category.addEntry(entries.startTextDescription(
                        t("Reference preset: beta1 / alpha20 acoustics"))
                .setColor(8374527)
                .build());
    }

    private static void distance(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Distance & Range"));
        category.addEntry(entries.startTextDescription(
                        t("Controls audible reach only. SPR geometry processing still stays inside its safe range."))
                .setColor(DESCRIPTION)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Scale range above volume 1"), rawBool("RANGE_SCALING", true))
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Volume values above 1 extend audible range instead of boosting near-field gain above 100%.",
                        "Recommended: ON."))
                .setSaveConsumer(value -> ClientConfigAccess.set("RANGE_SCALING", value))
                .build());

        category.addEntry(entries.startDoubleField(
                        t("Audible range multiplier"), rawDouble("AUDIBLE_RANGE_MULTIPLIER", 1.0D))
                .setDefaultValue(1.0D)
                .setMin(0.25D)
                .setMax(4.0D)
                .setTooltip(tip(
                        "Multiplier applied after the SPR-derived audible endpoint calculation.",
                        "1.0 reproduces the approved beta1 distance behavior."))
                .setSaveConsumer(value -> ClientConfigAccess.set("AUDIBLE_RANGE_MULTIPLIER", value))
                .build());
    }

    private static void occlusion(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Occlusion & Muffling"));
        category.addEntry(entries.startTextDescription(
                        t("Progressive wall-thickness muffling using the approved 17-probe model."))
                .setColor(DESCRIPTION)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Progressive occlusion"), rawBool("PROGRESSIVE_OCCLUSION", true))
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Adds speaker-only progressive obstruction on top of SPR.",
                        "Recommended: ON."))
                .setSaveConsumer(value -> ClientConfigAccess.set("PROGRESSIVE_OCCLUSION", value))
                .build());

        category.addEntry(entries.startDoubleField(
                        t("Muffling strength"), rawDouble("CUTOFF_OCCLUSION_SCALE", 0.35D))
                .setDefaultValue(0.35D)
                .setMin(0.0D)
                .setMax(2.0D)
                .setTooltip(tip(
                        "Controls progressive high-frequency attenuation.",
                        "Higher = darker/more muffled behind walls."))
                .setSaveConsumer(value -> ClientConfigAccess.set("CUTOFF_OCCLUSION_SCALE", value))
                .build());

        category.addEntry(entries.startDoubleField(
                        t("Occluded volume loss"), rawDouble("GAIN_OCCLUSION_SCALE", 0.5D))
                .setDefaultValue(0.5D)
                .setMin(0.0D)
                .setMax(2.0D)
                .setTooltip(tip(
                        "Controls progressive direct-volume loss through obstruction.",
                        "Higher = quieter behind walls."))
                .setSaveConsumer(value -> ClientConfigAccess.set("GAIN_OCCLUSION_SCALE", value))
                .build());

        SubCategoryBuilder advanced = entries.startSubCategory(t("Advanced probe model"))
                .setExpanded(false)
                .setTooltip(tip(
                        "Exact 17-probe geometry and weighting. Defaults are the approved beta1 model."));

        advanced.add(entries.startDoubleField(
                        t("Inner probe offset"), rawDouble("INNER_VARIATION", 0.2D))
                .setDefaultValue(0.2D)
                .setMin(0.05D)
                .setMax(0.75D)
                .setTooltip(tip("Inner ring offset in blocks."))
                .setSaveConsumer(value -> ClientConfigAccess.set("INNER_VARIATION", value))
                .build());

        advanced.add(entries.startDoubleField(
                        t("Outer probe offset"), rawDouble("OUTER_VARIATION", 0.49D))
                .setDefaultValue(0.49D)
                .setMin(0.1D)
                .setMax(1.25D)
                .setTooltip(tip("Outer ring offset in blocks."))
                .setSaveConsumer(value -> ClientConfigAccess.set("OUTER_VARIATION", value))
                .build());

        advanced.add(entries.startDoubleField(
                        t("Center-path weight"), rawDouble("CENTER_WEIGHT", 4.0D))
                .setDefaultValue(4.0D)
                .setMin(0.1D)
                .setMax(32.0D)
                .setTooltip(tip("Weight of the exact speaker-to-listener path."))
                .setSaveConsumer(value -> ClientConfigAccess.set("CENTER_WEIGHT", value))
                .build());

        advanced.add(entries.startDoubleField(
                        t("Inner-ring weight"), rawDouble("INNER_WEIGHT", 1.0D))
                .setDefaultValue(1.0D)
                .setMin(0.0D)
                .setMax(8.0D)
                .setTooltip(tip("Weight of each inner-ring path."))
                .setSaveConsumer(value -> ClientConfigAccess.set("INNER_WEIGHT", value))
                .build());

        advanced.add(entries.startDoubleField(
                        t("Outer-ring weight"), rawDouble("OUTER_WEIGHT", 0.5D))
                .setDefaultValue(0.5D)
                .setMin(0.0D)
                .setMax(8.0D)
                .setTooltip(tip("Weight of each outer-ring path."))
                .setSaveConsumer(value -> ClientConfigAccess.set("OUTER_WEIGHT", value))
                .build());

        advanced.add(entries.startIntSlider(
                        t("Open-path ring influence"),
                        pct(rawDouble("OPEN_CENTER_RING_SCALE", 0.2D)), 0, 100)
                .setDefaultValue(20)
                .setTextGetter(value -> t(value + "%"))
                .setTooltip(tip(
                        "How much nearby geometry may influence a speaker whose exact center path is fully clear.",
                        "The ring contribution then increases smoothly as the center path becomes obstructed."))
                .setSaveConsumer(value -> ClientConfigAccess.set(
                        "OPEN_CENTER_RING_SCALE", value / 100.0D))
                .build());

        category.addEntry(advanced.build());
    }

    private static void direction(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Direction & Reflections"));
        category.addEntry(entries.startTextDescription(
                        t("Keeps long-running speakers spatially stable while still allowing SPR reflection bending."))
                .setColor(DESCRIPTION)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Stabilize reflected direction"), rawBool("STABILIZE_REFLECTIONS", true))
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Prevents reflected virtual sources from rapidly jumping left/right.",
                        "Recommended: ON."))
                .setSaveConsumer(value -> ClientConfigAccess.set("STABILIZE_REFLECTIONS", value))
                .build());

        category.addEntry(entries.startDoubleField(
                        t("Reflection occlusion threshold"), rawDouble("REFLECTION_THRESHOLD", 0.45D))
                .setDefaultValue(0.45D)
                .setMin(0.0D)
                .setMax(16.0D)
                .setTooltip(tip(
                        "Minimum progressive raw occlusion before reflected positioning is allowed.",
                        "Higher keeps more speakers anchored to their real block."))
                .setSaveConsumer(value -> ClientConfigAccess.set("REFLECTION_THRESHOLD", value))
                .build());

        category.addEntry(entries.startIntSlider(
                        t("Reflection displacement strength"),
                        pct(rawDouble("REFLECTION_BLEND", 0.35D)), 0, 100)
                .setDefaultValue(35)
                .setTextGetter(value -> t(value + "%"))
                .setTooltip(tip(
                        "Fraction of SPR's reflected displacement that the compat retains."))
                .setSaveConsumer(value -> ClientConfigAccess.set("REFLECTION_BLEND", value / 100.0D))
                .build());

        category.addEntry(entries.startDoubleField(
                        t("Maximum reflection offset"), rawDouble("MAX_REFLECTION_OFFSET", 2.5D))
                .setDefaultValue(2.5D)
                .setMin(0.0D)
                .setMax(16.0D)
                .setTooltip(tip(
                        "Maximum virtual-source displacement from the physical speaker, in blocks."))
                .setSaveConsumer(value -> ClientConfigAccess.set("MAX_REFLECTION_OFFSET", value))
                .build());

        SubCategoryBuilder advanced = entries.startSubCategory(t("Advanced direction smoothing"))
                .setExpanded(false)
                .setTooltip(tip(
                        "How quickly the stabilized virtual position follows changing reflection targets."));

        advanced.add(percentEntry(entries,
                "Reflection follow smoothing", "REDIRECT_ALPHA", 0.22D, 22,
                "Higher follows reflected-position changes faster."));
        advanced.add(percentEntry(entries,
                "Return-to-speaker smoothing", "CLEAR_POSITION_ALPHA", 0.28D, 28,
                "Higher returns toward the real speaker faster when redirection clears."));
        advanced.add(percentEntry(entries,
                "Opposite-side transition smoothing", "FLIP_TO_CENTER_ALPHA", 0.35D, 35,
                "Used when the preferred reflected route changes to the opposite side."));

        category.addEntry(advanced.build());
    }

    private static void smoothing(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Smoothing"));
        category.addEntry(entries.startTextDescription(
                        t("Controls how quickly filters and wet/reverb sends move toward newly calculated targets."))
                .setColor(DESCRIPTION)
                .build());

        category.addEntry(percentEntry(entries,
                "Muffling response", "MUFFLE_ALPHA", 0.30D, 30,
                "Higher = obstruction becomes audible faster."));
        category.addEntry(percentEntry(entries,
                "Unmuffling cutoff response", "CLEAR_CUTOFF_ALPHA", 0.18D, 18,
                "Higher = high frequencies return faster when obstruction clears."));
        category.addEntry(percentEntry(entries,
                "Unmuffling gain response", "CLEAR_GAIN_ALPHA", 0.16D, 16,
                "Higher = direct volume returns faster when obstruction clears."));
        category.addEntry(percentEntry(entries,
                "Reverb response", "REVERB_ALPHA", 0.22D, 22,
                "Higher = wet/reverb sends react faster to a new environment."));
    }

    private static void performance(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Performance"));
        category.addEntry(entries.startTextDescription(
                        t("beta3 reduces progressive ray cost without changing the approved 17 probe positions or weighting."))
                .setColor(DESCRIPTION)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Adaptive probe cache"), rawBool("ADAPTIVE_PROBE_CACHE", true))
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Keeps the center path fresh and refreshes one exact 8-probe ring at a time.",
                        "The other exact ring is reused briefly; meaningful movement or center-path changes force all 17 probes fresh.",
                        "Disable for the full beta1b 17-fresh-probes-every-update reference behavior."))
                .setSaveConsumer(value -> ClientConfigAccess.set("ADAPTIVE_PROBE_CACHE", value))
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Performance diagnostics"), rawBool("DIAGNOSTICS", false))
                .setDefaultValue(false)
                .setTooltip(tip(
                        "Writes one compact CC:HQ compat performance line roughly every 10 seconds while speakers are active.",
                        "Does not enable global DEBUG logging or SPR debug rays."))
                .setSaveConsumer(value -> ClientConfigAccess.set("DIAGNOSTICS", value))
                .build());

        category.addEntry(intervalEntry(entries,
                "Full SPR update interval", "FULL_SPR_UPDATE_MS", 100, 50, 2000,
                "Minimum interval between full Sound Physics processSound evaluations per compat speaker."));
        category.addEntry(intervalEntry(entries,
                "Moving occlusion interval", "OCCLUSION_MIN_UPDATE_MS", 100, 50, 2000,
                "Minimum interval between progressive obstruction calculations while the listener is moving."));
        category.addEntry(intervalEntry(entries,
                "Stationary occlusion interval", "OCCLUSION_STATIONARY_UPDATE_MS", 200, 50, 5000,
                "Progressive obstruction refresh interval while the listener is effectively stationary."));

        category.addEntry(entries.startDoubleField(
                        t("Movement threshold"), rawDouble("OCCLUSION_MOVE_THRESHOLD", 0.15D))
                .setDefaultValue(0.15D)
                .setMin(0.01D)
                .setMax(2.0D)
                .setTooltip(tip(
                        "Listener movement in blocks that counts as movement for progressive obstruction refreshes."))
                .setSaveConsumer(value -> ClientConfigAccess.set("OCCLUSION_MOVE_THRESHOLD", value))
                .build());

        SubCategoryBuilder advanced = entries.startSubCategory(t("Adaptive probe safety"))
                .setExpanded(false)
                .setTooltip(tip(
                        "Conservative thresholds that force both cached rings to be recalculated immediately."));

        advanced.add(entries.startDoubleField(
                        t("Full refresh movement"), rawDouble("PROBE_FULL_REFRESH_DISTANCE", 0.5D))
                .setDefaultValue(0.5D)
                .setMin(0.1D)
                .setMax(4.0D)
                .setTooltip(tip(
                        "Cumulative listener movement since the last full 17-probe refresh.",
                        "At this distance both rings are forced fresh even if the center path stayed similar."))
                .setSaveConsumer(value -> ClientConfigAccess.set("PROBE_FULL_REFRESH_DISTANCE", value))
                .build());

        advanced.add(entries.startDoubleField(
                        t("Center-change full refresh"), rawDouble("PROBE_CENTER_DELTA", 0.2D))
                .setDefaultValue(0.2D)
                .setMin(0.01D)
                .setMax(4.0D)
                .setTooltip(tip(
                        "Center-path occlusion change since the last full refresh that immediately refreshes both rings.",
                        "Lower values are more conservative and spend more raycasts around transitions."))
                .setSaveConsumer(value -> ClientConfigAccess.set("PROBE_CENTER_DELTA", value))
                .build());

        category.addEntry(advanced.build());
    }

    private static AbstractConfigListEntry percentEntry(
            ConfigEntryBuilder entries,
            String label,
            String key,
            double defaultValue,
            int defaultPercent,
            String detail) {
        return entries.startIntSlider(t(label), pct(rawDouble(key, defaultValue)), 1, 100)
                .setDefaultValue(defaultPercent)
                .setTextGetter(value -> t(value + "%"))
                .setTooltip(tip(
                        detail,
                        "100% reacts fastest; lower values smooth more heavily."))
                .setSaveConsumer(value -> ClientConfigAccess.set(key, value / 100.0D))
                .build();
    }

    private static AbstractConfigListEntry intervalEntry(
            ConfigEntryBuilder entries,
            String label,
            String key,
            int defaultValue,
            int min,
            int max,
            String detail) {
        return entries.startIntSlider(t(label), rawInt(key, defaultValue), min, max)
                .setDefaultValue(defaultValue)
                .setTextGetter(value -> t(value + " ms  •  " + hz(value) + " Hz"))
                .setTooltip(tip(
                        detail,
                        "Lower interval = more frequent updates and higher CPU cost."))
                .setSaveConsumer(value -> ClientConfigAccess.set(key, value))
                .build();
    }

    private static String hz(int ms) {
        double hz = 1000.0D / Math.max(1, ms);
        return hz >= 10.0D
                ? String.format(Locale.ROOT, "%.0f", hz)
                : String.format(Locale.ROOT, "%.1f", hz);
    }

    private static int pct(double value) {
        return Math.max(0, Math.min(100, (int) Math.round(value * 100.0D)));
    }

    private static boolean rawBool(String name, boolean fallback) {
        return ClientConfigAccess.bool(name, fallback);
    }

    private static double rawDouble(String name, double fallback) {
        return ClientConfigAccess.dbl(name, fallback);
    }

    private static int rawInt(String name, int fallback) {
        return ClientConfigAccess.integer(name, fallback);
    }
}
