from pathlib import Path

ROOT = Path('.')


def read(path):
    return (ROOT / path).read_text()


def write(path, text):
    (ROOT / path).write_text(text)


def replace_once(text, old, new, label):
    assert old in text, f"missing marker for {label}: {old[:120]!r}"
    return text.replace(old, new, 1)


# -----------------------------------------------------------------------------
# Build / metadata identity
# -----------------------------------------------------------------------------
path = 'gradle.properties'
text = read(path)
text = text.replace('the frozen parity workflows already build with the\n# configuration cache disabled; keep the Phase-5 test branch locally reproducible.',
                    'the validation workflows build with the configuration cache disabled; keep\n# local release builds reproducible.')
text = text.replace('# Phase 2 build baseline', '# Build baseline')
text = text.replace('# Phase 5 V6 replaces the V3-V5 alternate-path replacement formulas with a\n# bounded secondary aperture-energy contribution driven by path difference.\n',
                    '# Mod identity\n')
text = replace_once(text,
                    'mod_version=0.1.0-beta11-phase5-v7-1-performance-test',
                    'mod_version=0.1.0-beta11-rc1',
                    'gradle release version')
write(path, text)

path = 'src/main/resources/META-INF/neoforge.mods.toml'
text = read(path)
text = replace_once(text,
                    'version="0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test"',
                    'version="0.1.0-beta11-rc1"',
                    'mods toml version')
start = text.index("description='''")
end = text.index("'''", start + len("description='''")) + 3
text = text[:start] + "description='''Client-side compatibility layer that routes CC:HQ whole-file speaker audio through Sound Physics Remastered. It adds positional wall-thickness occlusion, stable reflected direction, synchronized-copy clarity balancing, opening-aware vertical sound, cached acoustic work, and targeted diagnostics while preserving the original playback timing and source lifecycle.'''" + text[end:]
write(path, text)

# -----------------------------------------------------------------------------
# Main config comments: explain effects instead of historical test names.
# Values and keys intentionally stay unchanged here.
# -----------------------------------------------------------------------------
path = 'src/main/java/dev/cchqphysics/compat/config/ClientConfig.java'
text = read(path)
repls = {
    'Multiplier applied to the compat audible endpoint after SPR-derived range calculation. 1.0 reproduces alpha20.':
        'Multiplies the final audible range. Higher = speakers can be heard farther away; lower = shorter audible range. 1.0 is the tuned default.',
    'Inner probe-ring offset in blocks.':
        'Distance of the inner obstruction probes from the center path, in blocks. Higher samples a wider area around the direct path.',
    'Outer probe-ring offset in blocks.':
        'Distance of the outer obstruction probes from the center path, in blocks. Higher samples farther around the direct path.',
    'Weight of the exact source-to-listener ray.':
        'Importance of the exact speaker-to-listener path. Higher makes the center path dominate the muffling result more strongly.',
    'Weight of the eight inner-ring rays.':
        'Importance of the eight inner surrounding paths. Higher makes nearby geometry affect muffling more strongly.',
    'Weight of the eight outer-ring rays.':
        'Importance of the eight outer surrounding paths. Higher makes wider nearby geometry affect muffling more strongly.',
    'How much surrounding probes can influence a source whose exact center path is clear.':
        'How much nearby blocked geometry can matter while the exact center path is clear. Higher adds more surrounding influence; 0 trusts the clear center path completely.',
    'Strength of progressive high-frequency muffling.':
        'How strongly obstruction removes high frequencies. Higher = darker/more muffled behind walls; lower = clearer.',
    'Strength of progressive direct-volume attenuation.':
        'How strongly obstruction lowers direct volume. Higher = quieter behind walls; lower = less volume loss.',
    'Minimum progressive raw occlusion before reflected positioning is allowed.':
        'How blocked the direct path must be before reflected direction can move the apparent source. Higher keeps the sound anchored to the real speaker longer.',
    "Fraction of SPR's reflected displacement to retain.":
        "How much of Sound Physics' reflected direction shift is used. Higher = stronger apparent bending toward reflections; 0 keeps the source at its real position.",
    'Smoothing factor while following a reflected position.':
        'How quickly the apparent source follows a new reflected position. Higher reacts faster; lower moves more smoothly.',
    'Smoothing factor while returning toward the real speaker position.':
        'How quickly the apparent source returns to the real speaker after reflection redirection clears. Higher returns faster.',
    'Smoothing factor used when a reflected route changes to the opposite side.':
        'How quickly opposite-side reflection changes are recentred. Higher reacts faster; lower makes the transition smoother.',
    'Direct-filter smoothing while obstruction increases.':
        'How quickly new obstruction becomes audible. Higher = muffling reacts faster; lower = smoother/slower.',
    'Log-space cutoff smoothing while obstruction clears.':
        'How quickly high frequencies return after obstruction clears. Higher = faster clearing; lower = smoother/slower.',
    'Log-space gain smoothing while obstruction clears.':
        'How quickly direct volume returns after obstruction clears. Higher = faster recovery; lower = smoother/slower.',
    'Smoothing applied to SPR wet/reverb send targets.':
        'How quickly reverb/wet sends react to a changed environment. Higher = faster response; lower = smoother/slower.',
    'Minimum interval between full SPR processSound evaluations for each compat speaker. Alpha20 = 100 ms.':
        'Minimum time between full Sound Physics environment updates for one compat speaker. Lower = more responsive but more CPU; higher = cheaper but slower.',
    'Minimum interval between progressive obstruction calculations while moving. Alpha20 = 100 ms.':
        'Minimum time between wall-obstruction updates while moving. Lower = more responsive but more CPU; higher = cheaper but slower.',
    'Progressive obstruction refresh interval while the listener is effectively stationary. Alpha20 = 200 ms.':
        'Wall-obstruction refresh interval while the listener is stationary. Lower = fresher results but more CPU; higher = cheaper.',
    'Listener movement in blocks that counts as movement for progressive obstruction refreshes. Alpha20 = 0.15.':
        'Listener movement that switches to the faster moving update rate. Lower = more sensitive to small movement; higher = stays on the stationary rate longer.',
    'Reuse one exact 8-probe ring briefly while refreshing the other. Keeps the same 17-point model but reduces raycasts; full refreshes are forced around meaningful changes.':
        'Reuses one 8-probe ring briefly while refreshing the center and other ring. ON reduces raycasts; meaningful movement or center-path changes still force all 17 probes fresh.',
    'Cumulative listener movement in blocks since the last full 17-probe refresh that forces both rings fresh again.':
        'Movement since the last full 17-probe refresh that forces both rings fresh again. Lower = more full refreshes and more CPU.',
    'Change in center-path occlusion since the last full refresh that forces both probe rings fresh immediately.':
        'Center-path obstruction change that forces both probe rings fresh immediately. Lower = more sensitive and uses more raycasts around changes.',
}
for old, new in repls.items():
    assert old in text, f'missing ClientConfig comment: {old}'
    text = text.replace(old, new, 1)
write(path, text)

# -----------------------------------------------------------------------------
# Advanced config: release-facing TOML sections/keys and plain descriptions.
# Internal Java field/method names stay unchanged so runtime behavior is untouched.
# -----------------------------------------------------------------------------
path = 'src/main/java/dev/cchqphysics/compat/config/ExtendedClientConfig.java'
text = read(path)
old_doc = '''/**
 * Phase-5 test/extended controls.
 *
 * <p>Every default is intentionally the Phase-4 / Hotfix3-equivalent value so
 * simply installing the extended build does not change the verified acoustic
 * behavior. These controls exist to make real-game validation and diagnosis
 * easier without destroying the frozen parity branch.</p>
 */'''
new_doc = '''/**
 * Advanced runtime, performance and diagnostic controls.
 *
 * <p>Defaults are the release-tuned values. These settings are intentionally
 * separated from ordinary acoustic controls because most users should leave
 * them unchanged unless diagnosing performance or compatibility problems.</p>
 */'''
text = replace_once(text, old_doc, new_doc, 'advanced config class doc')
for old, new in {
    'builder.push("scheduler")': 'builder.push("room_updates")',
    'builder.push("sentinel")': 'builder.push("clearing_detection")',
    'builder.push("sync")': 'builder.push("synchronized_start")',
    'builder.push("features")': 'builder.push("optimizations")',
    '.define("private_efx", true)': '.define("private_source_filters", true)',
    '.define("beta9_direct_reuse", true)': '.define("reuse_direct_acoustics", true)',
    '.define("beta9_room_backoff", true)': '.define("stable_room_slowdown", true)',
    '.define("beta9_adaptive_controller", true)': '.define("load_aware_room_scheduling", true)',
    '.defineInRange("beta9_recent_movement_ms", 400, 0, 5000)': '.defineInRange("movement_hold_ms", 400, 0, 5000)',
    '.defineInRange("beta9_listener_move_distance", 0.05D, 0.0D, 4.0D)': '.defineInRange("stability_reset_distance", 0.05D, 0.0D, 4.0D)',
    '.defineInRange("beta9_max_room_factor", 2.0D, 1.0D, 6.0D)': '.defineInRange("max_room_slowdown", 2.0D, 1.0D, 6.0D)',
    '.defineInRange("beta9_max_room_interval_ms", 1500, 50, 10000)': '.defineInRange("max_room_interval_ms", 1500, 50, 10000)',
    '.define("beta10_ray_cache", true)': '.define("reuse_occlusion_rays", true)',
    '.define("beta11_room_ray_memo", true)': '.define("reuse_room_rays", true)',
}.items():
    assert old in text, f'missing advanced key marker: {old}'
    text = text.replace(old, new, 1)

comments = {
    'Minimum global room-scheduler slot. Hotfix3 = 50 ms.': 'Base spacing between room/reverb scheduling opportunities. Lower = more responsive but more CPU. Default: 50 ms.',
    'Minimum room-target hard-stale threshold. Hotfix3 = 500 ms.': 'Earliest age at which an old room result may be forced fresh. Lower = fresher room updates but more work. Default: 500 ms.',
    'Maximum room-target hard-stale threshold. Hotfix3 = 2000 ms.': 'Longest a room result may remain stale before a forced refresh. Lower = fresher but more CPU. Default: 2000 ms.',
    'How recently a source must have been seen to remain scheduler-eligible. Hotfix3 = 1000 ms.': 'How long a recently active speaker stays eligible for room updates. Higher keeps inactive/recent speakers tracked longer. Default: 1000 ms.',
    'Listener movement in blocks treated as a teleport, forcing room refreshes. Hotfix3 = 4.0 blocks.': 'Listener movement treated as a teleport and forcing room refreshes. Lower = smaller jumps trigger a full refresh. Default: 4 blocks.',
    'Speaker movement in blocks that marks its room state urgent. Hotfix3 = 0.1 blocks.': 'Speaker movement that makes its room state urgent. Lower = more sensitive to small source movement. Default: 0.1 blocks.',
    'Minimum listener movement in blocks before the clearing sentinel samples. Hotfix3 = 0.05.': 'Minimum listener movement before fast blocked-to-clear detection checks again. Lower = more sensitive. Default: 0.05 blocks.',
    'Raw progressive-occlusion level that can arm clearing detection. Hotfix3 = 0.075.': 'Obstruction level required before fast clearing detection can arm. Higher = requires a more blocked starting state. Default: 0.075.',
    'Center-ray occlusion used to re-arm clearing detection. Hotfix3 = 0.12.': 'Center-path obstruction needed to re-arm fast clearing detection after it has fired. Higher = requires stronger blockage. Default: 0.12.',
    'Center-ray value treated as effectively open. Hotfix3 = 0.035.': 'Center-path obstruction treated as effectively open. Higher = more paths count as clear. Default: 0.035.',
    'Required center-ray drop for a clearing candidate. Hotfix3 = 0.15.': 'Required improvement in the center path before a possible blocked-to-clear transition is considered. Lower = more sensitive. Default: 0.15.',
    'Required raw-occlusion drop to confirm a clearing transition. Hotfix3 = 0.035.': 'Required overall obstruction improvement to confirm a blocked-to-clear transition. Lower = easier to confirm. Default: 0.035.',
    'Required direct-cutoff rise to confirm a clearing transition. Hotfix3 = 0.055.': 'Required clarity/cutoff improvement to confirm a blocked-to-clear transition. Lower = easier to confirm. Default: 0.055.',
    'Cooldown between confirmed clearing triggers. Hotfix3 = 300 ms.': 'Minimum time between confirmed fast-clearing triggers. Higher prevents repeated triggers for longer. Default: 300 ms.',
    'Grace before an incomplete synchronized-start group is flushed. Hotfix3 = 100 ms.': 'How long an incomplete synchronized group waits for missing members before starting anyway. Higher = more waiting; lower = faster partial starts. Default: 100 ms.',
    'Age after which abandoned sync groups are discarded. Hotfix3 = 5000 ms.': 'How long an abandoned pending synchronized group is kept before cleanup. Higher keeps pending state longer. Default: 5000 ms.',
    'Use compat-owned per-source EFX filters. Disable only for diagnosis; native SPR fallback is then used. Hotfix3 = true.': 'Use isolated per-source OpenAL filters owned by the compat. Recommended: ON. Turn OFF only to diagnose filter-routing problems.',
    'Enable exact whole-direct-result reuse when source/environment inputs are unchanged. Hotfix3 = true.': 'Reuse an unchanged direct acoustic result instead of recalculating it. ON saves CPU without changing the result.',
    'Enable stable/relevance room-interval backoff. Hotfix3 = true.': 'Update stable or less-relevant room/reverb state less often. ON saves CPU; movement and stale-state rules still force refreshes.',
    'Enable load-pressure contribution to room backoff. Hotfix3 = true.': 'Allow current acoustic load to slow non-urgent room updates within the configured limits. ON reduces spikes under load.',
    'Window after listener movement during which stability backoff is suppressed. Hotfix3 = 400 ms.': 'Time after listener movement during which room updates stay responsive instead of slowing down. Higher keeps the fast mode longer. Default: 400 ms.',
    'Listener movement that resets Beta9 stability state. Hotfix3 = 0.05 blocks.': 'Listener movement that resets stable-room tracking. Lower = more sensitive to movement. Default: 0.05 blocks.',
    'Maximum combined room-interval backoff multiplier. Hotfix3 = 2.0.': 'Maximum amount stable/load-aware scheduling may slow room updates. 2.0 means at most twice the base interval.',
    'Absolute ceiling for a backed-off room interval. Hotfix3 = 1500 ms.': 'Absolute longest room-update interval allowed after slowdown. Lower = fresher rooms; higher = cheaper. Default: 1500 ms.',
    'Enable exact direct/SPR occlusion-ray reuse when the room stamp is reusable. Hotfix3 = true.': 'Reuse identical obstruction ray results when their inputs have not changed. ON saves CPU without changing the reused result.',
    'Enable same-clone memoization for SPR environment/bounce raycasts. Hotfix3 = true.': 'Reuse identical room/bounce ray results within the same Sound Physics world snapshot. ON saves repeated raycasts.',
    'Period for compat performance reports when diagnostics are enabled. Hotfix3 = 10000 ms.': 'How often performance diagnostics are printed while enabled. Lower = more frequent logs. Default: 10000 ms.',
}
for old, new in comments.items():
    assert old in text, f'missing advanced comment: {old}'
    text = text.replace(old, new, 1)
write(path, text)

path = 'src/main/java/dev/cchqphysics/compat/config/ExtendedClientConfigAccess.java'
text = read(path)
text = text.replace('/** Reflection bridge used only by the Cloth Config screen for the Phase-5 advanced spec. */',
                    '/** Reflection bridge used only by the Cloth Config screen for advanced runtime controls. */')
write(path, text)

# -----------------------------------------------------------------------------
# Cloth Config: add openings page, save it, simplify labels and historical text.
# -----------------------------------------------------------------------------
path = 'src/main/java/dev/cchqphysics/compat/config/ClothConfigScreen.java'
text = read(path)
text = replace_once(text,
                    '        SpectralMixConfig.save();\n    }',
                    '        SpectralMixConfig.save();\n        DiffractionConfig.save();\n    }',
                    'save diffraction')
text = replace_once(text,
                    '        performance(builder, entries);\n        advancedRuntime(builder, entries);',
                    '        performance(builder, entries);\n        openings(builder, entries);\n        advancedRuntime(builder, entries);',
                    'openings category call')
for old, new in {
    'Reference preset: beta1 / alpha20 acoustics': 'Defaults use the release-tuned acoustic preset',
    '1.0 reproduces the approved beta1 distance behavior.': 'Higher = farther audible reach. Lower = shorter reach. 1.0 is the tuned default.',
    'Exact 17-probe geometry and weighting. Defaults are the approved beta1 model.': 'Exact 17-probe geometry and weighting. Most users should leave these at their defaults.',
    'beta3 reduces progressive ray cost without changing the approved 17 probe positions or weighting.': 'Adaptive probing reduces ray cost without changing the 17 probe positions or weighting.',
    'Disable for the full beta1b 17-fresh-probes-every-update reference behavior.': 'Disable to recalculate all 17 probes on every update.',
    'Advanced Runtime': 'Advanced / Troubleshooting',
    'Phase 5 test controls. Every default below is the verified Hotfix3/Phase 4 value.': 'Low-level runtime and optimization controls. Defaults are tuned for normal use.',
    'Change one setting at a time while diagnosing. The frozen parity branch is not modified by these options.': 'Most users should leave these alone. Change one setting at a time when diagnosing performance or compatibility problems.',
    'Private per-source EFX': 'Private per-source filters',
    'Beta9 whole-direct reuse': 'Reuse unchanged direct acoustics',
    'Beta9 room backoff': 'Slow room updates when stable',
    'Beta9 adaptive load controller': 'Use load-aware room scheduling',
    'Beta10 exact ray cache': 'Reuse unchanged occlusion rays',
    'Beta11 room-ray memo': 'Reuse room rays within one calculation',
    'Beta9 room backoff': 'Stable-room slowdown',
    "High-level bounds around Hotfix3's adaptive room scheduling. Defaults reproduce Hotfix3.": 'Limits for how much stable/load-aware scheduling may slow room updates.',
    'Hotfix3 group-start grace and abandoned-group cleanup timings.': 'Timing controls for synchronized group starts and abandoned-group cleanup.',
    'Targeted INFO-level diagnostics for your real-game Phase 5 test. All are OFF by default.': 'Targeted INFO-level diagnostics for troubleshooting. All are OFF by default.',
}.items():
    if old in text:
        text = text.replace(old, new)

openings_method = r'''    private static void openings(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Openings & Vertical Sound"));
        category.addEntry(entries.startTextDescription(
                        t("Lets blocked sound respond to real nearby openings, especially in tunnels, shafts and lower floors."))
                .setColor(DESCRIPTION)
                .build());
        category.addEntry(entries.startTextDescription(
                        t("Defaults are the approved release behavior. Playback timing, source position and reverb routing are not changed."))
                .setColor(8374527)
                .build());

        category.addEntry(entries.startBooleanToggle(
                        t("Enable opening-aware sound"), DiffractionConfig.enabled())
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Allows a real nearby opening to add some clarity/loudness when the direct path is blocked.",
                        "OFF uses only the normal direct wall-obstruction result.",
                        "Recommended: ON."))
                .setSaveConsumer(DiffractionConfig::setEnabled)
                .build());

        category.addEntry(entries.startIntSlider(
                        t("Opening effect strength"), pct(DiffractionConfig.portalCoupling()), 0, 100)
                .setDefaultValue(25)
                .setTextGetter(value -> t(value + "%"))
                .setTooltip(tip(
                        "How strongly a usable opening makes blocked sound clearer/louder.",
                        "Higher = stronger opening effect. Lower = subtler. 0 = no added opening contribution."))
                .setSaveConsumer(value -> DiffractionConfig.setPortalCoupling(value / 100.0D))
                .build());

        category.addEntry(entries.startDoubleField(
                        t("Opening influence distance"), DiffractionConfig.apertureSpreadScale())
                .setDefaultValue(3.0D).setMin(0.25D).setMax(16.0D)
                .setTooltip(tip(
                        "How quickly the opening effect weakens as you move away from it inside the enclosed area.",
                        "Higher = noticeable farther away. Lower = fades sooner."))
                .setSaveConsumer(DiffractionConfig::setApertureSpreadScale)
                .build());

        category.addEntry(entries.startDoubleField(
                        t("Opening search radius"), DiffractionConfig.openingSearchRadius())
                .setDefaultValue(8.0D).setMin(1.0D).setMax(8.0D)
                .setTooltip(tip(
                        "How far around you the mod looks for a usable ceiling opening, in blocks.",
                        "Higher can find farther openings but costs more block checks. Current maximum: 8 blocks."))
                .setSaveConsumer(DiffractionConfig::setOpeningSearchRadius)
                .build());

        SubCategoryBuilder soundShape = entries.startSubCategory(t("Advanced sound shape"))
                .setExpanded(false)
                .setTooltip(tip("Fine tuning for how an indirect opening changes bass and clarity. Defaults are recommended."));
        soundShape.add(entries.startDoubleField(t("Minimum vertical separation"), DiffractionConfig.minSourceAboveListener())
                .setDefaultValue(0.25D).setMin(0.0D).setMax(8.0D)
                .setTooltip(tip(
                        "Minimum above/below height difference before opening-aware sound is considered.",
                        "Higher = feature is limited to stronger vertical separation. Lower = activates in shallower differences."))
                .setSaveConsumer(DiffractionConfig::setMinSourceAboveListener).build());
        soundShape.add(entries.startDoubleField(t("Opening clearance"), DiffractionConfig.escapeClearance())
                .setDefaultValue(1.5D).setMin(0.25D).setMax(8.0D)
                .setTooltip(tip(
                        "How far above the ceiling/opening the source-side route point is placed, in blocks.",
                        "Higher clears thicker roof edges more aggressively; lower stays closer to the opening."))
                .setSaveConsumer(DiffractionConfig::setEscapeClearance).build());
        soundShape.add(entries.startDoubleField(t("Full-effect obstruction"), DiffractionConfig.portalActivationRaw())
                .setDefaultValue(2.0D).setMin(0.25D).setMax(8.0D)
                .setTooltip(tip(
                        "How blocked the normal direct sound must be before the opening effect reaches full strength.",
                        "Higher requires stronger blockage. Lower reaches full effect sooner."))
                .setSaveConsumer(DiffractionConfig::setPortalActivationRaw).build());
        soundShape.add(entries.startDoubleField(t("Bass carry around openings"), DiffractionConfig.lowDeltaScale())
                .setDefaultValue(4.0D).setMin(0.10D).setMax(32.0D)
                .setTooltip(tip(
                        "How well low frequencies survive a longer indirect opening route.",
                        "Higher = more bass carries around the opening. Lower = bass fades more strongly."))
                .setSaveConsumer(DiffractionConfig::setLowDeltaScale).build());
        soundShape.add(entries.startDoubleField(t("Clarity carry around openings"), DiffractionConfig.highDeltaScale())
                .setDefaultValue(1.5D).setMin(0.05D).setMax(32.0D)
                .setTooltip(tip(
                        "How well high frequencies survive a longer indirect opening route.",
                        "Higher = brighter/clearer indirect sound. Lower = darker indirect sound."))
                .setSaveConsumer(DiffractionConfig::setHighDeltaScale).build());
        soundShape.add(entries.startIntSlider(t("Search-edge fade start"), pct(DiffractionConfig.horizonFadeStartRatio()), 0, 99)
                .setDefaultValue(75)
                .setTextGetter(value -> t(value + "% of radius"))
                .setTooltip(tip(
                        "Where the effect begins fading near the outer search limit.",
                        "Higher = stays stronger closer to the edge. Lower = begins fading earlier."))
                .setSaveConsumer(value -> DiffractionConfig.setHorizonFadeStartRatio(value / 100.0D)).build());
        soundShape.add(entries.startDoubleField(t("Opening separation"), Math.sqrt(DiffractionConfig.candidateSeparationSq()))
                .setDefaultValue(2.0D).setMin(0.0D).setMax(8.0D)
                .setTooltip(tip(
                        "Minimum spacing between the two openings that may be fully checked.",
                        "Higher avoids spending both checks on adjacent cells of the same hole."))
                .setSaveConsumer(DiffractionConfig::setCandidateSeparation).build());
        soundShape.add(entries.startDoubleField(t("Opening switching stability"), DiffractionConfig.selectionHysteresis())
                .setDefaultValue(0.35D).setMin(0.0D).setMax(4.0D)
                .setTooltip(tip(
                        "How much better another opening must become before selection switches to it.",
                        "Higher = steadier. Lower = reacts to small advantages sooner."))
                .setSaveConsumer(DiffractionConfig::setSelectionHysteresis).build());
        category.addEntry(soundShape.build());

        SubCategoryBuilder perf = entries.startSubCategory(t("Opening performance"))
                .setExpanded(false)
                .setTooltip(tip("Caching and rescan controls. Higher reuse generally lowers CPU cost."));
        perf.add(entries.startDoubleField(t("Opening scan interval"), DiffractionConfig.openingScanIntervalNs() / 1_000_000.0D)
                .setDefaultValue(1000.0D).setMin(100.0D).setMax(5000.0D)
                .setTooltip(tip(
                        "Minimum milliseconds between nearby-opening scans while you stay in the same block.",
                        "Lower reacts to changed blocks sooner but scans more often."))
                .setSaveConsumer(DiffractionConfig::setOpeningScanIntervalMs).build());
        perf.add(entries.startDoubleField(t("Movement recheck distance"), Math.sqrt(DiffractionConfig.openingLegRecheckDistanceSq()))
                .setDefaultValue(0.75D).setMin(0.10D).setMax(4.0D)
                .setTooltip(tip(
                        "How far you move before a cached listener-to-opening path is checked again.",
                        "Lower = more checks. Higher = more reuse while moving."))
                .setSaveConsumer(DiffractionConfig::setOpeningLegRecheckDistance).build());
        perf.add(entries.startDoubleField(t("Opening path cache time"), DiffractionConfig.openingRayCacheNs() / 1_000_000.0D)
                .setDefaultValue(5000.0D).setMin(250.0D).setMax(30000.0D)
                .setTooltip(tip(
                        "Maximum milliseconds a verified opening path may be reused while endpoints stay stable.",
                        "Higher = fewer Sound Physics rechecks in stable scenes."))
                .setSaveConsumer(DiffractionConfig::setOpeningRayCacheMs).build());
        category.addEntry(perf.build());
    }

'''
marker = '    private static void advancedRuntime(ConfigBuilder builder, ConfigEntryBuilder entries) {'
assert marker in text, 'advancedRuntime marker missing'
assert 'private static void openings(' not in text, 'openings method already exists'
text = text.replace(marker, openings_method + marker, 1)

start = text.index('    private static void spectralMix(ConfigBuilder builder, ConfigEntryBuilder entries) {')
end = text.index('    private static void debugValidation(ConfigBuilder builder, ConfigEntryBuilder entries) {', start)
new_spectral = r'''    private static void spectralMix(ConfigBuilder builder, ConfigEntryBuilder entries) {
        ConfigCategory category = builder.getOrCreateCategory(t("Synchronized Sound Balance"));
        category.addEntry(entries.startTextDescription(
                        t("Reduces extreme clarity differences when synchronized copies of the same sound reach you through very different paths."))
                .setColor(DESCRIPTION)
                .build());
        category.addEntry(entries.startTextDescription(
                        t("Only direct clarity is corrected. Volume, position, reverb sends and playback timing are untouched."))
                .setColor(8374527)
                .build());
        category.addEntry(entries.startBooleanToggle(t("Enable synchronized clarity balance"), SpectralMixConfig.enabled())
                .setDefaultValue(true)
                .setTooltip(tip(
                        "Gently brightens only severely muffled synchronized copies when another copy is much clearer.",
                        "Recommended: ON."))
                .setSaveConsumer(SpectralMixConfig::setEnabled)
                .build());
        category.addEntry(entries.startDoubleField(t("How muffled a copy must be"), SpectralMixConfig.darkSourceCutoff())
                .setDefaultValue(0.35D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "Cutoff scale: 0 = extremely muffled, 1 = clear.",
                        "Only copies at or below this value can be corrected.",
                        "Higher = more copies qualify. Lower = only more heavily muffled copies qualify."))
                .setSaveConsumer(SpectralMixConfig::setDarkSourceCutoff)
                .build());
        category.addEntry(entries.startDoubleField(t("How clear another copy must be"), SpectralMixConfig.peerClearCutoff())
                .setDefaultValue(0.75D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "At least one synchronized copy must be this clear before correction can activate.",
                        "Higher = requires a clearer reference copy. Lower = easier to activate."))
                .setSaveConsumer(SpectralMixConfig::setPeerClearCutoff)
                .build());
        category.addEntry(entries.startDoubleField(t("Minimum clarity difference"), SpectralMixConfig.minPeerGap())
                .setDefaultValue(0.40D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "Minimum cutoff difference between the muffled copy and the clearest synchronized copy.",
                        "Higher = only large mismatches are corrected. Lower = smaller differences can be corrected."))
                .setSaveConsumer(SpectralMixConfig::setMinPeerGap)
                .build());
        category.addEntry(entries.startIntSlider(
                        t("Clarity correction strength"), pct(SpectralMixConfig.clarityFloorRatio()), 0, 100)
                .setDefaultValue(50)
                .setTextGetter(value -> t(value + "%"))
                .setTooltip(tip(
                        "How far an eligible muffled copy moves toward the clearest synchronized copy.",
                        "0% = no correction. 50% = halfway. 100% = fully match before the maximum-increase cap."))
                .setSaveConsumer(value -> SpectralMixConfig.setClarityFloorRatio(value / 100.0D))
                .build());
        category.addEntry(entries.startDoubleField(t("Maximum clarity increase"), SpectralMixConfig.maxCutoffLift())
                .setDefaultValue(0.55D).setMin(0.0D).setMax(1.0D)
                .setTooltip(tip(
                        "Safety cap on how much one copy's cutoff may be raised.",
                        "Higher allows a larger correction. Lower limits the change more strongly."))
                .setSaveConsumer(SpectralMixConfig::setMaxCutoffLift)
                .build());
    }

'''
text = text[:start] + new_spectral + text[end:]
write(path, text)

print('Config cleanup patch applied.')
