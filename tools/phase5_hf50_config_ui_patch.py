from pathlib import Path

path = Path("src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java")
text = path.read_text(encoding="utf-8")

old = '''    private static void spectralMix(ConfigBuilder builder, ConfigEntryBuilder entries) {
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
'''

new = '''    private static void spectralMix(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Synchronized HF Balance"));
        category.addEntry(entries.startTextDescription(
                        t("Reduces painful spectral skew when synchronized copies of the same audio have very different direct low-pass cutoffs."))
                .setColor(DESCRIPTION)
                .build());
        category.addEntry(entries.startTextDescription(
                        t("Validated defaults reproduce the approved HF50 Issue-A candidate. Gain, position, reverb sends and playback timing are untouched."))
                .setColor(8374527)
                .build());
        category.addEntry(entries.startBooleanToggle(t("Enable synchronized HF balance"), SpectralMixConfig.enabled())
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Only synchronized sources that pass all gates below receive a direct-HF cutoff lift.",
                        "Recommended default: ON for the validated HF50 candidate."))
                .setSaveConsumer(SpectralMixConfig::setEnabled)
                .build());
        category.addEntry(entries.startDoubleField(t("Dark-source cutoff gate"), SpectralMixConfig.darkSourceCutoff())
                .setDefaultValue(0.35D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "Only sources at or below this intrinsic cutoff are eligible for correction.",
                        "Lower = correction is restricted to more severely muffled copies."))
                .setSaveConsumer(SpectralMixConfig::setDarkSourceCutoff)
                .build());
        category.addEntry(entries.startDoubleField(t("Clear-peer cutoff gate"), SpectralMixConfig.peerClearCutoff())
                .setDefaultValue(0.75D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "At least one synchronized peer must be this clear before correction can activate.",
                        "Higher = requires a more obviously clear comparison speaker."))
                .setSaveConsumer(SpectralMixConfig::setPeerClearCutoff)
                .build());
        category.addEntry(entries.startDoubleField(t("Minimum peer cutoff gap"), SpectralMixConfig.minPeerGap())
                .setDefaultValue(0.40D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "Minimum cutoff difference between the dark source and clearest synchronized peer.",
                        "Higher = preserves more ordinary acoustic differences."))
                .setSaveConsumer(SpectralMixConfig::setMinPeerGap)
                .build());
        category.addEntry(entries.startIntSlider(
                        t("HF lift strength"), pct(SpectralMixConfig.clarityFloorRatio()), 0, 100)
                .setDefaultValue(50)
                .setTextGetter(value -> t(value + "%"))
                .setTooltip(tip(
                        "How far an eligible dark copy moves toward its clearest synchronized peer.",
                        "50% is the user-selected best balance from the Issue-A A/B test."))
                .setSaveConsumer(value -> SpectralMixConfig.setClarityFloorRatio(value / 100.0D))
                .build());
        category.addEntry(entries.startDoubleField(t("Maximum cutoff lift"), SpectralMixConfig.maxCutoffLift())
                .setDefaultValue(0.55D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "Absolute safety cap on the cutoff increase applied to one synchronized copy.",
                        "The 50% blend can never raise a source by more than this amount."))
                .setSaveConsumer(SpectralMixConfig::setMaxCutoffLift)
                .build());
    }
'''

count = text.count(old)
if count == 0:
    if new in text:
        print("HF50 Cloth Config UI already patched")
        raise SystemExit(0)
    raise SystemExit("Expected old synchronized-mix UI block was not found")
if count != 1:
    raise SystemExit(f"Expected exactly one old synchronized-mix UI block, found {count}")

path.write_text(text.replace(old, new), encoding="utf-8")
print("Patched HF50 Cloth Config UI")
