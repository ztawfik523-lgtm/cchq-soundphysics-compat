# Phase 5 Elevation Diffraction V2 — 2026-09-05

## Runtime evidence that invalidated V1 activation gates

With diffraction enabled in the user's real open-pit test, V1 did not apply because the camera/ear-to-source geometry was smaller than the visible hole depth:

- user-described geometry: about 6 blocks away, 3 blocks deep
- measured raw occlusion: 6.844
- measured center occlusion: 7.000
- measured ear-to-source vertical separation: 1.880
- measured horizontal separation: 7.251
- measured slope: 0.259
- V1 result: `reason=vertical-gate applied=false`

A prior dump in the same run similarly measured vertical=1.880, horizontal=6.144, slope=0.306 and was rejected by the same V1 gate.

Interpretation: using camera/ear position is correct for acoustics, but a visually 2-3 block-deep pit does not produce a 2-3 block ear-to-source Y delta. Therefore V1 defaults `minY=3.0` and `minSlope=0.5` were too strict and prevented the diffraction safety rays from running at all.

## V2 scope

V2 keeps the V1 two-segment route algorithm byte-identical and changes only activation defaults:

- `min_vertical_separation`: 3.0 -> 0.75
- `min_vertical_horizontal_ratio`: 0.50 -> 0.10

Unchanged safety/behavior:

- diffraction remains OFF by default
- raw occlusion gate remains 3.0
- horizontal separation bounds remain 1.5..12.0
- vertical escape leg must remain <= 0.25
- diffraction penalty remains 1.0
- minimum raw improvement remains 1.0
- at most two extra rays per qualified evaluation
- sealed vertical leg rejection unchanged
- near-vertical floor/ceiling exclusion unchanged
- no source AL gain mutation
- no source position mutation
- no reflection mutation
- no reverb-send mutation
- no playback timing or synchronized-start mutation
- approved configurable HF50 baseline unchanged

## Verified build

Branch: `phase5-elevation-diffraction-test-v2`

Runtime/build source commit: `896fffb6fe0ee6840079e9b1c3a64644181f5d42`

V1 diffraction runtime source: `01761f3bc385dcc0c88c1cb3f21795518db01f3c`

Approved configurable HF50 source: `62d3a7a0a176c901402b913946d98f3cb455a8f4`

Workflow run: `33946206205` — SUCCESS

Artifact ID: `9963420851`

JAR: `cchq_soundphysics_compat-0.1.0-beta11-phase5-diffraction-v2-test.jar`

JAR SHA-256: `fd0dbf248bd5db89d647315fd0c392e8596ec67295cb8df54f755c872b5d9e9f`

Class count: 71

## Next runtime test

1. Replace V1 test JAR with V2 test JAR.
2. Reproduce the same 3-block-deep/open-top pit position.
3. Run `/cchqphysics diffraction on`.
4. Listen without moving initially.
5. Run `/cchqphysics dump`.
6. Repeat at the 2-block-deep position.
7. Negative control: a genuinely sealed floor/ceiling should not suddenly become clear.

The key V2 evidence is whether the dump advances past `vertical-gate`/`slope-gate` and reports either `applied` or a meaningful safety rejection such as `sealed-vertical-leg` or `insufficient-improvement`.
