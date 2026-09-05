# Phase 5 — elevation / open-top diffraction isolation

## Baseline

This experiment starts from the runtime-approved configurable HF50 source candidate (`62d3a7a0a176c901402b913946d98f3cb455a8f4`). HF50 itself is not changed.

The separate reported problem is steep elevation geometry: a speaker can be only a few blocks away horizontally while the listener is several Y levels lower in an open-topped depression. The normal center + 8 inner + 8 outer probes all remain straight source-to-listener rays, so a diagonal through terrain can accumulate roughly 7–15 raw occlusion even when an acoustic path exists over the rim.

## Experiment

Normal progressive occlusion remains primary. Diffraction is OFF by default.

Only when all conservative gates pass:

- raw progressive occlusion >= 3.0
- |dy| >= 3.0 blocks
- horizontal separation >= 1.5 blocks
- horizontal separation <= 12 blocks
- |dy| / horizontal separation >= 0.5

then the test chooses the lower endpoint (source or listener) and creates one waypoint directly above it at `max(sourceY, listenerY) + 1.5`.

The probe is deliberately two-stage:

1. lower endpoint -> waypoint (vertical escape)
2. higher endpoint -> waypoint (cross-rim leg)

The vertical escape leg must have SPR occlusion <= 0.25. If not, the candidate is rejected immediately. This is the sealed-floor / sealed-ceiling safety gate.

If the vertical leg is open, the candidate raw value is:

`verticalLeg + crossLeg + 1.0 diffraction penalty`

It must beat the normal raw value by at least 1.0. If it does, the existing progressive direct cutoff/gain are remapped to the candidate raw value without changing source position, reverb sends, reflection routing, PCM timing, synchronized starts, or Lua behavior.

At most two extra SPR occlusion rays are used for a qualifying severe/steep case; a sealed vertical leg costs only one extra ray before rejection.

## Runtime A/B

No source IDs are required.

- `/cchqphysics diffraction off`
- reproduce the steep open-top case
- `/cchqphysics dump`
- `/cchqphysics diffraction on`
- hold the same listener/speaker geometry for a few seconds
- `/cchqphysics dump`
- compare sound and send `latest.log` / `debug.log`

Also test one negative control if convenient: a truly sealed floor/ceiling or ordinary wall. With the feature ON, that case should remain strongly occluded and the dump should show a rejected diffraction candidate.

## Interpretation

- Open-top case improves while sealed control stays blocked -> strong support for this model; tune penalty/gates only if needed.
- Open-top case unchanged and dump says `sealed-vertical-leg` -> waypoint/open gate is too strict or geometry is not actually vertically open.
- Open-top case unchanged and dump says another gate -> adjust only that gate in a follow-up diagnostic.
- Sealed control becomes much clearer -> reject the algorithm; do not tune around the failure casually.

This experiment is not a Phase-5 final feature until runtime evidence supports it.
