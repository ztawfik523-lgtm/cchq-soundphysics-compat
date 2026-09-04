from pathlib import Path


def replace_exact(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/Beta9Optimizer.java",
    "        boolean audible;\n        float gain;",
    "        boolean audible = true;\n        float gain = 1.0F;",
)

replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/ProgressiveOcclusionModel.java",
    "        double cachedInnerVariation;\n        double cachedOuterVariation;\n        boolean ringsValid;\n        double innerSum;\n        double outerSum;\n        boolean refreshInnerNext;",
    "        double cachedInnerVariation = Double.NaN;\n        double cachedOuterVariation = Double.NaN;\n        boolean ringsValid;\n        double innerSum;\n        double outerSum;\n        boolean refreshInnerNext = true;",
)

replace_exact(
    "src/main/java/dev/cchqphysics/compat/audio/EnvironmentSmoother.java",
    "        float r0, r1, r2, r3, h0, h1, h2, h3, cutoff, gain;\n        long lastLogNs;\n        float lastLoggedTargetCutoff;",
    "        float r0, r1, r2, r3, h0, h1, h2, h3;\n        float cutoff = 1.0F;\n        float gain = 1.0F;\n        long lastLogNs;\n        float lastLoggedTargetCutoff = Float.NaN;",
)

print("Phase 4 exact default-state corrections applied")
