from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')

def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected exactly one match, found {count}: {old[:120]!r}')
    write(path, text.replace(old, new, 1))

def create_exact(path, content):
    p = ROOT / path
    if p.exists():
        if p.read_text(encoding='utf-8') == content:
            return
        raise SystemExit(f'{path}: already exists with unexpected content')
    write(path, content)

SPECTRAL_CONFIG = '''package dev.cchqphysics.compat.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Experimental synchronized-copy spectral compensation. */
public final class SpectralMixConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.DoubleValue PEER_CLEAR_CUTOFF;
    private static final ModConfigSpec.DoubleValue CLARITY_FLOOR_RATIO;
    private static final ModConfigSpec.DoubleValue MAX_CUTOFF_LIFT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("synchronized_spectral_mix");
        ENABLED = builder.comment(
                "Experimental: reduce excessive mud when synchronized copies are differently occluded.",
                "OFF preserves the already validated Phase-5 candidate behavior.",
                "Never changes source gain, source position, or reverb-send filters.")
                .define("enabled", false);
        PEER_CLEAR_CUTOFF = builder.comment(
                "At least one synchronized peer must be this clear before compensation is allowed.")
                .defineInRange("peer_clear_cutoff", 0.65D, 0.0D, 1.0D);
        CLARITY_FLOOR_RATIO = builder.comment(
                "Fraction of the clearest peer cutoff used as a conservative floor for very dark copies.")
                .defineInRange("clarity_floor_ratio", 0.18D, 0.0D, 0.75D);
        MAX_CUTOFF_LIFT = builder.comment(
                "Absolute cap on how much direct cutoff may be raised by compensation.")
                .defineInRange("max_cutoff_lift", 0.12D, 0.0D, 0.75D);
        builder.pop();
        SPEC = builder.build();
    }

    private SpectralMixConfig() {}

    private static boolean b(ModConfigSpec.BooleanValue value, boolean fallback) {
        try { return value.get(); } catch (Throwable ignored) { return fallback; }
    }

    private static double d(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            Double result = value.get();
            return result == null ? fallback : result;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean enabled() { return b(ENABLED, false); }
    public static double peerClearCutoff() { return d(PEER_CLEAR_CUTOFF, 0.65D); }
    public static double clarityFloorRatio() { return d(CLARITY_FLOOR_RATIO, 0.18D); }
    public static double maxCutoffLift() { return d(MAX_CUTOFF_LIFT, 0.12D); }
    public static void setEnabled(boolean value) { ENABLED.set(value); }
    public static void setPeerClearCutoff(double value) { PEER_CLEAR_CUTOFF.set(value); }
    public static void setClarityFloorRatio(double value) { CLARITY_FLOOR_RATIO.set(value); }
    public static void setMaxCutoffLift(double value) { MAX_CUTOFF_LIFT.set(value); }
    public static void save() { SPEC.save(); }
    public static String summary() {
        return "spectralMix=" + enabled()
                + " spectralPeerClearCutoff=" + peerClearCutoff()
                + " spectralFloorRatio=" + clarityFloorRatio()
                + " spectralMaxCutoffLift=" + maxCutoffLift();
    }
}
'''

SPECTRAL_BALANCER = '''package dev.cchqphysics.compat.audio;

import dev.cchqphysics.compat.config.ClientConfig;
import dev.cchqphysics.compat.config.SpectralMixConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Spectral-only synchronized-copy experiment. This class never changes
 * OpenAL source gain or position and never touches reverb-send filters.
 */
final class SynchronizedSpectralBalancer {
    private static final Logger LOGGER = LoggerFactory.getLogger("CC:HQ Sound Physics Compat");

    private SynchronizedSpectralBalancer() {}

    static float adjustDirectCutoff(int sourceId, float baseCutoff) {
        if (!SpectralMixConfig.enabled() || !ClientConfig.progressiveOcclusion()) return baseCutoff;
        double peerMax = clearestPeerCutoff(sourceId);
        if (!Double.isFinite(peerMax) || peerMax < SpectralMixConfig.peerClearCutoff()) return baseCutoff;
        double floor = peerMax * SpectralMixConfig.clarityFloorRatio();
        double cappedFloor = Math.min(floor, baseCutoff + SpectralMixConfig.maxCutoffLift());
        double adjusted = Math.max(baseCutoff, cappedFloor);
        return (float) Math.max(0.0D, Math.min(1.0D, adjusted));
    }

    private static double clearestPeerCutoff(int sourceId) {
        int[] peers = SyncStartCoordinator.livePeerSources(sourceId);
        double max = Double.NaN;
        for (int peer : peers) {
            double cutoff = ProgressiveOcclusionModel.currentCutoff(peer);
            if (!Double.isFinite(cutoff)) continue;
            if (!Double.isFinite(max) || cutoff > max) max = cutoff;
        }
        return max;
    }

    static void debugDump() {
        int[] sources = SyncStartCoordinator.liveGroupedSources();
        if (sources.length == 0) {
            LOGGER.info("[phase5/dump] spectral activeGroupedSources=0 {}", SpectralMixConfig.summary());
            return;
        }
        for (int sourceId : sources) {
            double base = ProgressiveOcclusionModel.currentCutoff(sourceId);
            double peerMax = clearestPeerCutoff(sourceId);
            float adjusted = Double.isFinite(base) ? adjustDirectCutoff(sourceId, (float) base) : Float.NaN;
            LOGGER.info("[phase5/dump] spectral source={} peers={} intrinsicCutoff={} clearestPeer={} adjustedCutoff={} delta={} enabled={}",
                    sourceId, SyncStartCoordinator.livePeerSources(sourceId).length,
                    round3(base), round3(peerMax), round3(adjusted),
                    Double.isFinite(base) && Float.isFinite(adjusted) ? round3(adjusted - base) : "nan",
                    SpectralMixConfig.enabled());
        }
    }

    private static String round3(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.3f", value) : "nan";
    }
}
'''

create_exact('src/main/java/dev/cchqphysics/compat/config/SpectralMixConfig.java', SPECTRAL_CONFIG)
create_exact('src/main/java/dev/cchqphysics/compat/audio/SynchronizedSpectralBalancer.java', SPECTRAL_BALANCER)

replace_once('src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java',
             'import dev.cchqphysics.compat.config.ExtendedClientConfig;\n',
             'import dev.cchqphysics.compat.config.ExtendedClientConfig;\nimport dev.cchqphysics.compat.config.SpectralMixConfig;\n')
replace_once('src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java',
             'public static final String VERSION = "0.1.0-beta11-phase5-test";',
             'public static final String VERSION = "0.1.0-beta11-phase5-mixv2-test";')
replace_once('src/main/java/dev/cchqphysics/compat/CCHQSoundPhysicsCompat.java',
             '        container.registerConfig(ModConfig.Type.CLIENT, ExtendedClientConfig.SPEC, "cchq_soundphysics_compat-advanced.toml");\n',
             '        container.registerConfig(ModConfig.Type.CLIENT, ExtendedClientConfig.SPEC, "cchq_soundphysics_compat-advanced.toml");\n        container.registerConfig(ModConfig.Type.CLIENT, SpectralMixConfig.SPEC, "cchq_sound