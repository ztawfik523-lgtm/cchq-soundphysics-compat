from pathlib import Path

VERSION = '0.1.0-beta11.1-rc1'


def replace_once(path: str, old: str, new: str, label: str) -> None:
    p = Path(path)
    s = p.read_text()
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'{path}: {label}: expected one match, found {count}')
    p.write_text(s.replace(old, new, 1))

# Unify Gradle/JAR identity.
replace_once(
    'gradle.properties',
    'mod_version=0.1.0-beta11-phase5-v7-1-performance-test',
    f'mod_version={VERSION}',
    'gradle version')

# Runtime-facing mod initialization and stable diffraction config filename.
replace_once(
    'src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java',
    'public static final String VERSION = "0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test";',
    f'public static final String VERSION = "{VERSION}";',
    'runtime version')
replace_once(
    'src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java',
    '// Fresh V6 filename prevents any V1-V5 experimental values from altering the portal-energy test.\n        container.registerConfig(ModConfig.Type.CLIENT, DiffractionConfig.SPEC, "cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml");',
    '// Promote the runtime-approved V7.1 model onto a fresh stable release config.\n        // The old experimental file is intentionally left untouched and ignored so stale test values cannot alter release defaults.\n        container.registerConfig(ModConfig.Type.CLIENT, DiffractionConfig.SPEC, "cchq_soundphysics_compat-diffraction.toml");',
    'diffraction config filename')
replace_once(
    'src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java',
    'LOGGER.info("CC:HQ Sound Physics Compat {} initialized; approved HF50 preserved, V7.1 spreading-only aperture-energy diffraction test available and OFF by default", VERSION);',
    'LOGGER.info("CC:HQ Sound Physics Compat {} initialized; approved HF50 and V7.1 aperture diffraction enabled by default", VERSION);',
    'startup text')
replace_once(
    'src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java',
    'LOGGER.info("Phase 5 advanced config: {} {} {}", ExtendedClientConfig.summary(), SpectralMixConfig.summary(), DiffractionConfig.summary());',
    'LOGGER.info("Advanced config: {} {} {}", ExtendedClientConfig.summary(), SpectralMixConfig.summary(), DiffractionConfig.summary());',
    'advanced config log')

# Promote only the already-approved V7.1 activation/default surface; no acoustic equations change.
diff = 'src/main/java/dev/cchqphysics/compat/config/DiffractionConfig.java'
replace_once(diff,
    '/** Experimental V7.1 spreading-only aperture-energy model. */',
    '/** Runtime-approved V7.1 spreading-only aperture-energy model. */',
    'diffraction javadoc')
replace_once(diff,
    'builder.push("portal_diffraction_v7_1_spreading_only_test");',
    'builder.push("portal_diffraction");',
    'stable config section')
replace_once(diff,
    '''        ENABLED = builder.comment(
                "Experimental Phase-5 V7.1 spreading-only aperture-energy model.",
                "OFF by default. The normal progressive direct path remains authoritative.",
                "A verified opening only adds a bounded secondary energy contribution; it never replaces the direct path.",
                "Never changes source position, playback timing, synchronized starts, reflection routing, or reverb sends.")
                .define("enabled", false);
''',
    '''        ENABLED = builder.comment(
                "Runtime-approved V7.1 spreading-only aperture-energy diffraction model.",
                "Enabled by default after the V7.1 listening and performance validation pass.",
                "A verified opening only adds a bounded secondary energy contribution; it never replaces the direct path.",
                "Never changes source position, playback timing, synchronized starts, reflection routing, or reverb sends.")
                .define("enabled", true);
''',
    'promote approved default')
replace_once(diff,
    '"Narrow experiment scope: source must be at least this far above the listener before the portal model is considered.",\n                "This avoids broadening the Phase-5 elevation fix into unrelated same-height room acoustics.")',
    '"Source must be at least this far above the listener before the portal model is considered.",\n                "This keeps the approved elevation correction out of unrelated same-height room acoustics.")',
    'scope comments')
replace_once(diff,
    '"Implicit open-top geometry uses zero aperture distance in this isolated test to preserve the V6 open-top result.")',
    '"Implicit open-top geometry uses zero aperture distance to preserve the approved V6 open-top result.")',
    'spread comment')
replace_once(diff,
    'public static boolean enabled() { return b(ENABLED, false); }',
    'public static boolean enabled() { return b(ENABLED, true); }',
    'enabled fallback')

# Persist the existing diffraction toggle through the normal config screen and expose only a simple on/off control.
screen = 'src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java'
replace_once(screen,
    '''    private static void saveAll() {
        ClientConfigAccess.save();
        ExtendedClientConfigAccess.save();
        SpectralMixConfig.save();
    }
''',
    '''    private static void saveAll() {
        ClientConfigAccess.save();
        ExtendedClientConfigAccess.save();
        SpectralMixConfig.save();
        DiffractionConfig.save();
    }
''',
    'save diffraction config')
replace_once(screen,
    '''        category.addEntry(entries.startBooleanToggle(
                        t("Progressive occlusion"), rawBool("PROGRESSIVE_OCCLUSION", true))
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Adds speaker-only progressive obstruction on top of SPR.",
                        "Recommended: ON."))
                .setSaveConsumer(value -> ClientConfigAccess.set("PROGRESSIVE_OCCLUSION", value))
                .build());

        category.addEntry(entries.startDoubleField(
''',
    '''        category.addEntry(entries.startBooleanToggle(
                        t("Progressive occlusion"), rawBool("PROGRESSIVE_OCCLUSION", true))
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Adds speaker-only progressive obstruction on top of SPR.",
                        "Recommended: ON."))
                .setSaveConsumer(value -> ClientConfigAccess.set("PROGRESSIVE_OCCLUSION", value))
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Vertical opening diffraction"), DiffractionConfig.enabled())
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Adds the approved bounded V7.1 secondary aperture-energy path for vertical/opening cases.",
                        "The normal progressive direct path stays authoritative.",
                        "Recommended: ON."))
                .setSaveConsumer(DiffractionConfig::setEnabled)
                .build());

        category.addEntry(entries.startDoubleField(
''',
    'diffraction UI toggle')
replace_once(screen,
    't("Phase 5 test controls. Every default below is the verified Hotfix3/Phase 4 value.")',
    't("Advanced runtime controls. Defaults preserve the verified Hotfix3-derived behavior unless explicitly noted.")',
    'advanced runtime wording')
replace_once(screen,
    't("Change one setting at a time while diagnosing. The frozen parity branch is not modified by these options.")',
    't("Change advanced settings deliberately; the normal defaults are the validated compatibility profile.")',
    'advanced runtime guidance')
replace_once(screen,
    't("Targeted INFO-level diagnostics for your real-game Phase 5 test. All are OFF by default.")',
    't("Targeted INFO-level diagnostics for troubleshooting and validation. All are OFF by default.")',
    'debug wording')

# Debug commands remain diagnostics, not part of normal behavior.
debug = 'src/main/java/dev/cchqphysics/compat/audio/DebugCommands.java'
replace_once(debug,
    '/** Client-only Phase-5 validation commands. No command mutates server state. */',
    '/** Client-only diagnostics and validation commands. No command mutates server state. */',
    'debug javadoc')
replace_once(debug, 'LOGGER.info("[phase5/dump] config {} {} {}",', 'LOGGER.info("[cchq/dump] config {} {} {}",', 'dump config tag')
replace_once(debug, 'LOGGER.info("[phase5/dump] {}",', 'LOGGER.info("[cchq/dump] {}",', 'dump status tag')
replace_once(debug, 'Component.literal("CC:HQ diffraction test: ON (runtime only)")', 'Component.literal("CC:HQ diffraction: ON (runtime only)")', 'diffraction on text')
replace_once(debug, 'Component.literal("CC:HQ diffraction test: OFF (runtime only)")', 'Component.literal("CC:HQ diffraction: OFF (runtime only)")', 'diffraction off text')
replace_once(debug, 'Component.literal("CC:HQ diffraction test: " + summary)', 'Component.literal("CC:HQ diffraction: " + summary)', 'diffraction status text')
replace_once(debug, 'LOGGER.info("[phase5/config] {}", summary);', 'LOGGER.info("[cchq/config] {}", summary);', 'config tag')

# User-facing NeoForge metadata.
mods = 'src/main/resources/META-INF/neoforge.mods.toml'
replace_once(mods,
    'version="0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test"',
    f'version="{VERSION}"',
    'mods.toml version')
old_description = '''description=''' + "'''" + '''Phase 5 isolated aperture-energy diffraction experiment V6 built on the runtime-approved configurable HF50 baseline. V7.1 preserves V6 portal spectral/transmission math and adds only explicit-aperture distance spreading; V6 retires the V3-V5 behavior where an alternate opening route could replace the direct occlusion result. The normal progressive direct path remains authoritative; a verified roof/open-top aperture contributes only a bounded secondary low/high energy path. Aperture strength uses the exact V6 path-length/leg-transmission model, multiplied by a softened inverse-distance spreading term for explicit roof openings and the existing smooth finite-search fade. High-band aperture energy decays faster than low-band energy. At most two candidates are acoustically verified, listener-side topology is shared, and verification legs are cached. Source position, source playback gain, PCM/OpenAL clocks, synchronized starts, reflection routing, and reverb sends are not mutated. The experiment is OFF by default.''''' + "'''"
new_description = '''description=''' + "'''" + '''Client-side compatibility layer for CC:HQ Speakers and Sound Physics Remastered on Minecraft 1.21.1 / NeoForge. Preserves the validated progressive direct-occlusion path, private per-source EFX, synchronized-start handling, approved HF50 synchronized spectral balancing, and the approved V7.1 bounded aperture-energy diffraction path. V7.1 remains a secondary energy contribution only: it never replaces the direct path and does not change source position, PCM/OpenAL playback clocks, synchronized-start timing, reflection routing, or reverb sends. This release candidate also includes performance, lifecycle, decode-backpressure, and OpenAL resource-cleanup hardening.''''' + "'''"
replace_once(mods, old_description, new_description, 'mods.toml description')
