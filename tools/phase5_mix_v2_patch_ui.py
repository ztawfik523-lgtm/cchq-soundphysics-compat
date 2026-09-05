from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java"


def patch(old, new):
    text = PATH.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"ClothConfigScreen.java: expected one UI anchor, found {count}")
    PATH.write_text(text.replace(old, new, 1), encoding="utf-8")


patch(
    ".setSavingRunnable(ClientConfigAccess::save);",
    ".setSavingRunnable(ClothConfigScreen::saveAll);",
)

patch(
    """        performance(builder, entries);
        advancedRuntime(builder, entries);
        debugValidation(builder, entries);
""",
    """        performance(builder, entries);
        advancedRuntime(builder, entries);
        spectralMix(builder, entries);
        debugValidation(builder, entries);
""",
)

patch(
    "    public static Screen create(Screen parent) {\n",
    """    private static void saveAll() {
        ClientConfigAccess.save();
        ExtendedClientConfigAccess.save();
        SpectralMixConfig.save();
    }

    public static Screen create(Screen parent) {
""",
)

patch(
    "    private static void debugValidation(ConfigBuilder builder, ConfigEntryBuilder entries) {\n",
    """    private static void spectralMix(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Synchronized Mix (Experimental)"));
        category.addEntry(entries.startTextDescription(
                        t("V2 targets spectral stacking only. It never changes OpenAL source volume, position, or reverb-send filters."))
                .setColor(DESCRIPTION)
                .build());
        category.addEntry(entries.startTextDescription(
                        t("OFF is the known-good Phase 5 behavior. Enable only for the synchronized multi-speaker test."))
                .setColor(8374527)
                .build());
        category.addEntry(entries.startBooleanToggle(t("Compensate synchronized spectral mud"), SpectralMixConfig.enabled())
                .setDefaultValue(false)
                .setTooltip(tip(
                        "When a genuinely clear synchronized peer exists, only heavily low-passed copies receive a small direct-cutoff lift.",
                        "No source gain or positional panning is changed."))
                .setSaveConsumer(SpectralMixConfig::setEnabled)
                .build());
        category.addEntry(entries.startDoubleField(t("Clear-peer cutoff threshold"), SpectralMixConfig.peerClearCutoff())
                .setDefaultValue(0.65D).setMin(0.0D).setMax(1.0D)
                .setSaveConsumer(SpectralMixConfig::setPeerClearCutoff)
                .build());
        category.addEntry(entries.startDoubleField(t("Group clarity floor ratio"), SpectralMixConfig.clarityFloorRatio())
                .setDefaultValue(0.18D).setMin(0.0D).setMax(0.75D)
                .setSaveConsumer(SpectralMixConfig::setClarityFloorRatio)
                .build());
        category.addEntry(entries.startDoubleField(t("Maximum cutoff lift"), SpectralMixConfig.maxCutoffLift())
                .setDefaultValue(0.12D).setMin(0.0D).setMax(0.75D)
                .setSaveConsumer(SpectralMixConfig::setMaxCutoffLift)
                .build());
    }

    private static void debugValidation(ConfigBuilder builder, ConfigEntryBuilder entries) {
""",
)

print("spectral UI patch ready")
