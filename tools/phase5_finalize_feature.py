from pathlib import Path


def replace(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"expected snippet not found in {path}: {old[:120]!r}")
    text = text.replace(old, new, 1)
    p.write_text(text, encoding="utf-8")


# Distinguish the final feature-test artifact from the already-tested candidate.
replace(
    "gradle.properties",
    "mod_version=0.1.0-beta11-phase5-test",
    "mod_version=0.1.0-beta11-phase5-final-test",
)
replace(
    "src/main/resources/META-INF/neoforge.mods.toml",
    'version="0.1.0-beta11-phase5-test"',
    'version="0.1.0-beta11-phase5-final-test"',
)

# Register the optional third client spec and identify the final test build.
p = "src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java"
replace(p,
    "import dev.cchqphysics.compat.config.ExtendedClientConfig;\n",
    "import dev.cchqphysics.compat.config.ExtendedClientConfig;\nimport dev.cchqphysics.compat.config.MixClientConfig;\n")
replace(p,
    'public static final String VERSION = "0.1.0-beta11-phase5-test";',
    'public static final String VERSION = "0.1.0-beta11-phase5-final-test";')
replace(p,
    '        container.registerConfig(ModConfig.Type.CLIENT, ExtendedClientConfig.SPEC, "cchq_soundphysics_compat-advanced.toml");\n',
    '        container.registerConfig(ModConfig.Type.CLIENT, ExtendedClientConfig.SPEC, "cchq_soundphysics_compat-advanced.toml");\n'
    '        container.registerConfig(ModConfig.Type.CLIENT, MixClientConfig.SPEC, "cchq_soundphysics_compat-mixing.toml");\n')
replace(p,
    '            LOGGER.info("Phase 5 advanced config: {}", ExtendedClientConfig.summary());\n',
    '            LOGGER.info("Phase 5 advanced config: {} | {}", ExtendedClientConfig.summary(), MixClientConfig.summary());\n')

# Add optional synchronized-mix attenuation. The normal path is byte-shape-distinct
# only when the feature is explicitly enabled; OFF leaves the existing gain call/order intact.
p = "src/main/java/dev/cchqphysics/compat/audio/CompatAudioManager.java"
replace(p,
    "import dev.cchqphysics.compat.mixin.SoundEngineAccessor;\n",
    "import dev.cchqphysics.compat.config.MixClientConfig;\nimport dev.cchqphysics.compat.mixin.SoundEngineAccessor;\n")
replace(p,
    "import org.lwjgl.openal.AL10;\n",
    "import org.lwjgl.openal.AL10;\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\n")
replace(p,
    "public final class CompatAudioManager {\n",
    "public final class CompatAudioManager {\n    private static final Logger LOGGER = LoggerFactory.getLogger(\"CC:HQ Sound Physics Compat\");\n")
replace(p,
    "                SoundPhysicsBridge.apply(active.sourceId, active.audio.source(), active.audio.x(), active.audio.y(), active.audio.z());\n            }\n",
    "                SoundPhysicsBridge.apply(active.sourceId, active.audio.source(), active.audio.x(), active.audio.y(), active.audio.z());\n"
    "                if (MixClientConfig.enabled()) {\n"
    "                    float mixedGain = synchronizedMixGain(active, gain);\n"
    "                    Beta10Optimizer.alSourcefStable(active.sourceId, AL10.AL_GAIN, mixedGain);\n"
    "                }\n"
    "            }\n")
replace(p,
    "    private static float effectiveGain(HQPayloadView.Audio audio) {\n        return DistanceBridge.effectiveGain(audio);\n    }\n",
    "    private static float effectiveGain(HQPayloadView.Audio audio) {\n"
    "        return DistanceBridge.effectiveGain(audio);\n"
    "    }\n\n"
    "    private static float synchronizedMixGain(ActiveSource active, float baseGain) {\n"
    "        if (!MixClientConfig.enabled() || baseGain <= 0.0F) return baseGain;\n"
    "        UUID groupId = active.audio.syncGroupId();\n"
    "        if (groupId == null) return baseGain;\n"
    "        int peers = synchronizedPeerCount(active);\n"
    "        if (peers <= 0) return baseGain;\n"
    "        double raw = ProgressiveOcclusionModel.currentRawOcclusion(active.sourceId);\n"
    "        double excess = Math.max(0.0D, raw - MixClientConfig.threshold());\n"
    "        if (excess <= 0.0D) return baseGain;\n"
    "        double factor = Math.exp(-excess * MixClientConfig.strength());\n"
    "        factor = Math.max(MixClientConfig.minimumGainFactor(), Math.min(1.0D, factor));\n"
    "        return (float) (baseGain * factor);\n"
    "    }\n\n"
    "    private static int synchronizedPeerCount(ActiveSource active) {\n"
    "        UUID groupId = active.audio.syncGroupId();\n"
    "        if (groupId == null) return 0;\n"
    "        int peers = 0;\n"
    "        for (ActiveSource other : ACTIVE.values()) {\n"
    "            if (other.sourceId == active.sourceId) continue;\n"
    "            if (!active.key.equals(other.key)) continue;\n"
    "            if (groupId.equals(other.audio.syncGroupId())) peers++;\n"
    "        }\n"
    "        return peers;\n"
    "    }\n\n"
    "    static void debugDumpMixing() {\n"
    "        onSoundThread(() -> {\n"
    "            if (ACTIVE.isEmpty()) {\n"
    "                LOGGER.info(\"[phase5/dump] mix active=0 {}\", MixClientConfig.summary());\n"
    "                return;\n"
    "            }\n"
    "            for (ActiveSource active : ACTIVE.values()) {\n"
    "                float baseGain = effectiveGain(active.audio);\n"
    "                int peers = synchronizedPeerCount(active);\n"
    "                double raw = ProgressiveOcclusionModel.currentRawOcclusion(active.sourceId);\n"
    "                float mixedGain = synchronizedMixGain(active, baseGain);\n"
    "                double factor = baseGain > 0.0F ? mixedGain / baseGain : 1.0D;\n"
    "                LOGGER.info(\"[phase5/dump] mix source={} group={} peers={} rawOcclusion={} baseGain={} factor={} finalGain={} enabled={}\",\n"
    "                        active.sourceId, active.audio.syncGroupId(), peers, raw, baseGain, factor, mixedGain, MixClientConfig.enabled());\n"
    "            }\n"
    "        });\n"
    "    }\n")

# Persist all three client specs and expose the optional feature in Cloth Config.
p = "src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java"
replace(p,
    ".setSavingRunnable(ClientConfigAccess::save);",
    ".setSavingRunnable(() -> {\n                    ClientConfigAccess.save();\n                    ExtendedClientConfigAccess.save();\n                    MixClientConfig.save();\n                });")
needle = '''        category.addEntry(extendedBoolEntry(entries, "Beta11 room-ray memo", "BETA11_ROOM_RAY_MEMO", true,
                "OFF disables same-clone environment/bounce ray memoization."));

'''
insert = '''        category.addEntry(extendedBoolEntry(entries, "Beta11 room-ray memo", "BETA11_ROOM_RAY_MEMO", true,
                "OFF disables same-clone environment/bounce ray memoization."));

        SubCategoryBuilder synchronizedMix = entries.startSubCategory(t("Synchronized multi-speaker mixing"))
                .setExpanded(false)
                .setTooltip(tip(
                        "Optional post-parity feature for several synchronized copies of the same payload.",
                        "OFF preserves the already-validated Hotfix3/Phase-5 behavior."));
        synchronizedMix.add(entries.startBooleanToggle(
                        t("Reduce occluded synchronized copies"), MixClientConfig.enabled())
                .setDefaultValue(false)
                .setTooltip(tip(
                        "When several compat sources share the same sync group and payload, add extra gain attenuation only to occluded copies.",
                        "Clear copies are untouched. OFF preserves Hotfix3 behavior."))
                .setSaveConsumer(MixClientConfig::setEnabled)
                .build());
        synchronizedMix.add(entries.startDoubleField(
                        t("Suppression strength"), MixClientConfig.strength())
                .setDefaultValue(0.55D).setMin(0.0D).setMax(3.0D)
                .setTooltip(tip("Higher values make clear synchronized speakers dominate the combined mix more strongly."))
                .setSaveConsumer(MixClientConfig::setStrength)
                .build());
        synchronizedMix.add(entries.startDoubleField(
                        t("Occlusion threshold"), MixClientConfig.threshold())
                .setDefaultValue(0.075D).setMin(0.0D).setMax(4.0D)
                .setTooltip(tip("Raw progressive occlusion below this threshold receives no extra synchronized-mix attenuation."))
                .setSaveConsumer(MixClientConfig::setThreshold)
                .build());
        synchronizedMix.add(entries.startIntSlider(
                        t("Minimum blocked-source gain"), pct(MixClientConfig.minimumGainFactor()), 0, 100)
                .setDefaultValue(30)
                .setTextGetter(value -> t(value + "%"))
                .setTooltip(tip("Floor for the extra attenuation so blocked copies still contribute some room/reverb character."))
                .setSaveConsumer(value -> MixClientConfig.setMinimumGainFactor(value / 100.0D))
                .build());
        category.addEntry(synchronizedMix.build());

'''
replace(p, needle, insert)

# Make /dump and /config include the new mixing state.
p = "src/main/java/dev/cchqphysics/compat/audio/DebugCommands.java"
replace(p,
    "import dev.cchqphysics.compat.config.ExtendedClientConfig;\n",
    "import dev.cchqphysics.compat.config.ExtendedClientConfig;\nimport dev.cchqphysics.compat.config.MixClientConfig;\n")
replace(p,
    '                            LOGGER.info("[phase5/dump] config {}", ExtendedClientConfig.summary());\n',
    '                            LOGGER.info("[phase5/dump] config {} | {}", ExtendedClientConfig.summary(), MixClientConfig.summary());\n')
replace(p,
    "                            EnvironmentSmoother.debugDumpEfx();\n",
    "                            EnvironmentSmoother.debugDumpEfx();\n                            CompatAudioManager.debugDumpMixing();\n")
replace(p,
    '                            String summary = ExtendedClientConfig.summary();\n',
    '                            String summary = ExtendedClientConfig.summary() + " | " + MixClientConfig.summary();\n')

print("Phase 5 final feature patch applied successfully")
