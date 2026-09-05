from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def patch(path, old, new):
    p = ROOT / path
    text = p.read_text(encoding="utf-8")
    if new in text:
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected one source anchor, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


patch(
    "src/main/java/dev/cchqphysics/compat/audio/ProgressiveOcclusionModel.java",
    """    public static synchronized double currentRawOcclusion(int sourceId) {
        State state = STATES.get(sourceId);
        return state != null && state.valid ? state.rawOcclusion : 0.0D;
    }
""",
    """    public static synchronized double currentRawOcclusion(int sourceId) {
        State state = STATES.get(sourceId);
        return state != null && state.valid ? state.rawOcclusion : 0.0D;
    }

    public static synchronized double currentCutoff(int sourceId) {
        State state = STATES.get(sourceId);
        return state != null && state.valid ? state.cutoff : Double.NaN;
    }
""",
)

patch(
    "src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java",
    """        float[] adjusted = ProgressiveOcclusionModel.adjust(sourceId, directCutoff, directGain);
        float targetCutoff = adjusted[0];
        float targetGain = adjusted[1];
""",
    """        float[] adjusted = ProgressiveOcclusionModel.adjust(sourceId, directCutoff, directGain);
        float targetCutoff = SynchronizedSpectralBalancer.adjustDirectCutoff(sourceId, adjusted[0]);
        float targetGain = adjusted[1];
""",
)

print("spectral core patch ready")
