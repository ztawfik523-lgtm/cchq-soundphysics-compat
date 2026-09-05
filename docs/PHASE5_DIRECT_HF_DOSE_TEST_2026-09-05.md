# Phase 5 — Direct-HF Dose Test

Date: 2026-09-05

## Why this test exists

The no-ID acoustic-mix A/B probe produced a repeatable subjective split while automatically targeting the darkest tracked synchronized source:

- `sends_off`: sounded the same as AUTO.
- `direct_hf_bypass`: substantially reduced the painful skew / one-sided / doubled-like coloration, but spatial quality became somewhat worse.
- AUTO restored/switched the spatial skew again.

Uploaded runtime logs confirm that, during the useful repeated comparisons, the automatic selector repeatedly chose the same strong acoustic outlier at approximately `cutoff=0.091`, `gain=0.710` for both `sends_off` and `direct_hf_bypass`.

Interpretation at this checkpoint:

- Auxiliary reverb sends are strongly weakened as the cause of the reproduced coloration.
- Persistent OpenAL playback cursor micro-desync had already been strongly weakened by the prior synchronized cursor telemetry.
- Reflection redirection had already been strongly weakened for this reproduction.
- The direct high-frequency low-pass contrast is now the strongest causal lead.
- Full direct-HF bypass is not acceptable as a final behavior because it improves the skew/coloration while degrading some useful spatial quality.

## Relationship to old V2

The rejected/unpromoted V2 spectral experiment used defaults:

- `clarity_floor_ratio=0.18`
- `max_cutoff_lift=0.12`

For a dark copy around `0.091` with a clear peer around `1.0`, that only lifts the direct cutoff to about `0.18`. The new full bypass A/B shows that stronger direct-HF relief can audibly reduce the reproduced problem, so the next diagnostic is a dose-response test rather than restoring V2 unchanged.

## New diagnostic build

Branch:

- `phase5-direct-hf-dose-test`

Exact runtime source commit:

- `620f6428b48187e40bcc96c8576e2070fc79a512`

Verified artifact:

- `cchq_soundphysics_compat-0.1.0-beta11-phase5-hfdose-test.jar`
- SHA-256: `967e6ae961f620097f07d324cee1f21d27341b92fccf82ee1ff7e85b7911621b`
- class count: 73

CI:

- workflow run `33941616120`
- artifact `9962024538`
- compile/build/inspection/upload all passed.

Frozen/reference lineage retained:

- Phase 4 frozen: `79eed29767343ee34022e8f6268b386f75e84c9f`
- known-good Phase 5 candidate: `44612192d875e43ecef66ca51798cab7adb17020`
- reviewed Issue-A source: `973f1df7dad886fb0f5fffd4264015fecac2e786`
- sync diagnostic runtime source: `95bd4b06b78786d4f7b1ad33b665f4685e45a54b`
- no-ID acoustic probe source: `1e3aafc566f9e57588d39ed75fa53bde1035375a`

## What the dose modes do

The darkest active tracked source is selected automatically. No source ID is required.

HF modes preserve:

- source gain,
- source position,
- auxiliary reverb sends,
- playback timing,
- underlying computed occlusion/cutoff state.

They change only the OpenAL direct-filter HF factor for the selected source by blending from the current computed cutoff toward `1.0`:

- `hf_lift_25`: `base + (1-base)*0.25`
- `hf_lift_50`: `base + (1-base)*0.50`
- `hf_lift_75`: `base + (1-base)*0.75`
- `direct_hf_bypass`: `1.0`

For a representative `base=0.091`, the approximate applied HF values are:

- AUTO: `0.091`
- 25%: `0.318`
- 50%: `0.546`
- 75%: `0.773`
- bypass: `1.000`

## Test objective

Find the smallest HF lift that removes or materially reduces the painful synchronized skew/coloration while retaining as much useful spatial localization as possible.

Suggested comparison order:

1. AUTO
2. `hf_lift_25`
3. AUTO
4. `hf_lift_50`
5. AUTO
6. `hf_lift_75`
7. AUTO

Full bypass remains available as a known upper-bound reference.

Do not promote any value to final behavior from one subjective pass alone. Prefer a repeatable dose-response pattern and, if needed, a second geometry.
