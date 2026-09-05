# CC:HQ Sound Physics Compat — final release execution plan

**Date:** 2026-09-06 (Africa/Cairo)

This is the concrete file-by-file plan to apply **only after** the current audited lifecycle candidate passes the user's runtime lifecycle test.

Authoritative policy decisions are in:

`docs/RELEASE_POLICY_LOCK_2026-09-06.md`

Current lifecycle runtime candidate remains:

- source checkpoint: `2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`
- JAR SHA-256: `4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Do not apply the production changes below until that candidate passes Gate 1.

---

## 1. Version/metadata centralization

### `gradle.properties`

Set:

`mod_version=0.1.0`

Clean stale Phase-5 test comments where they describe the current release rather than historical build provenance.

### `build.gradle`

Keep `version = mod_version` as the artifact source of truth.

Add resource expansion/processing so the NeoForge metadata version is derived from `mod_version` rather than another literal. The implementation should preserve existing resource-wiring verification and avoid introducing configuration-cache assumptions that conflict with the reconstruction build setup.

### `src/main/resources/META-INF/neoforge.mods.toml`

Replace the literal experimental version with a Gradle-expanded version placeholder sourced from `mod_version`.

Replace the long experiment-history description with a concise release description of the actual mod:

- CC:HQ whole-file positional audio compatibility with Sound Physics Remastered;
- positional distance/occlusion/reflection processing;
- synchronized-speaker handling;
- approved diffraction/opening behavior;
- client-side only.

Do not include V3/V4/V5/V6 experiment history in shipping metadata.

### `CCHQSoundPhysicsCompat.java`

Do not maintain a second independent hard-coded release version.

Use one of the smallest safe mechanisms supported by the existing NeoForge runtime/build structure so runtime reporting resolves to the same `0.1.0` identity. Prefer build/resource metadata as the authority rather than creating another manual literal.

---

## 2. Final diffraction config identity and default

### `CCHQSoundPhysicsCompat.java`

Change registered diffraction config filename from:

`cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml`

to:

`cchq_soundphysics_compat-diffraction.toml`

No migration or fallback read of the old filename should be added.

### `DiffractionConfig.java`

Change schema/root key from:

`portal_diffraction_v7_1_spreading_only_test`

to:

`portal_diffraction`

Change the `enabled` default/fallback from `false` to `true` consistently:

- `.define("enabled", true)`;
- `enabled()` fallback -> `true`.

Do not change the accepted numeric values:

- min source above listener = 0.25
- escape clearance = 1.5
- search radius = 8
- portal coupling = 0.25
- activation raw = 2.0
- low delta scale = 4.0
- high delta scale = 1.5
- aperture spread scale = 3.0
- horizon fade start ratio = 0.75
- candidate separation = 2.0
- selection hysteresis = 0.35
- scan interval = 1000 ms
- leg recheck distance = 0.75
- ray cache = 5000 ms

Rewrite comments as stable behavior descriptions; remove `Experimental`, `Phase-5`, `isolated test`, and obsolete V6/V7.1 experiment wording where it is presentation-only.

Keep the actual V7.1 algorithm implementation in `VerticalDiffractionRelief` unchanged.

---

## 3. User-facing terminology cleanup

### `CCHQSoundPhysicsCompat.java`

Replace startup text describing an experiment/test with neutral release wording.

Remove `Phase 5 advanced config` wording; use something like `advanced config` or `effective config`.

### `DebugCommands.java`

Keep command paths stable unless there is a strong reason to change them.

Clean output text:

- `diffraction test: ON/OFF` -> `diffraction: ON/OFF`;
- `diffraction test:` status -> `diffraction:`;
- class/comment wording -> client diagnostics rather than Phase-5 validation.

Keep `/cchqphysics diffraction` itself for continuity.

### `ClothConfigScreen.java`

Change visible labels/descriptions while preserving underlying config keys.

Planned display cleanup:

- `Reference preset: beta1 / alpha20 acoustics` -> `Reference acoustic preset` or equivalent neutral wording;
- `beta3 reduces...` -> descriptive adaptive-probe wording without beta numbering;
- `Beta9 room backoff` -> `Adaptive room backoff`;
- `Beta9 adaptive load controller` -> `Adaptive load controller`;
- `Beta10 exact ray cache` -> `Exact occlusion ray cache`;
- `Beta11 room-ray memo` -> `Room ray memoization`;
- beta-number references in tooltips -> descriptive behavior where practical.

Do **not** rename the persisted `BETA9_*`, `BETA10_*`, or `BETA11_*` keys as part of this release cleanup.

### `ExtendedClientConfig.java`

Rewrite the class header from `Phase-5 test/extended controls` to neutral advanced/runtime configuration wording.

Persisted keys/defaults remain unchanged.

### `SpectralMixConfig.java`

Remove `Phase-5 HF50 A/B`, `candidate`, and `Issue-A candidate` wording from comments.

Keep the approved HF50 schema/defaults exactly:

- enabled = true
- dark source cutoff = 0.35
- peer clear cutoff = 0.75
- minimum peer gap = 0.40
- clarity floor ratio = 0.50
- maximum cutoff lift = 0.55

No HF50 algorithm change is part of release cleanup.

---

## 4. Diagnostics/log continuity

### `DebugDiagnostics.java`

Do not rename `[phase5/... ]` log tags unless there is a concrete user-facing benefit. Historical logs, current analysis instructions and support notes already use those tags.

This release prioritizes stable diagnostics over cosmetic internal log naming.

### Config summaries

Internal summary tokens such as `portalV7_1=true`, `syncHf50=...`, and beta-number technical fields may remain if they are useful for log correlation.

Only user-facing UI text needs full terminology cleanup.

---

## 5. Manifest cleanup

### `src/main/resources/META-INF/MANIFEST.MF`

Remove the stale static line:

`Created-By: 21.0.11 (Debian)`

Keep:

`Manifest-Version: 1.0`

Do not add a new static machine/JDK claim. CI/build records provide better provenance.

---

## 6. Files that must remain behaviorally frozen

Release cleanup must not alter the accepted acoustic math in these areas unless a new concrete bug is independently discovered:

- `VerticalDiffractionRelief.java` — V7.1 equations/selection behavior;
- `SynchronizedSpectralBalancer.java` — approved HF50 behavior;
- `ProgressiveOcclusionModel.java` — direct progressive geometry/weighting;
- `EnvironmentSmoother.java` — accepted environment/EFX behavior;
- `SyncStartCoordinator.java` — synchronized start/partial grace/pending-INITIAL semantics;
- `PositionStabilizer.java` — approved direction/reflection behavior;
- `Beta9Optimizer.java` / `Beta10Optimizer.java` / `Beta11RoomRayCache.java` — established performance semantics.

Specifically preserve:

`Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

with bitwise `&`.

Lifecycle source from `2a6a2f4e...` must also remain intact except for strictly presentation/version/config-registration edits described in this plan.

---

## 7. Final CI validation after transformation

Build with Java 21 from a clean checkout.

Required checks:

1. Gradle wrapper validation.
2. Frozen V7.1 refs still point to `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`.
3. Performance/lifecycle ancestry is intact.
4. Named frozen acoustic algorithm files have no unintended diff from the accepted baselines.
5. HF50 numeric defaults remain exactly approved.
6. V7.1 numeric parameters/equations remain exactly approved.
7. Diffraction `enabled` is intentionally `true` in the new final schema.
8. New diffraction filename/root are exactly:
   - `cchq_soundphysics_compat-diffraction.toml`
   - `portal_diffraction`
9. Old test filename/root are absent from active production registration/schema.
10. No migration code silently loads the old beta diffraction config.
11. Exact bitwise `&` eligibility expression remains.
12. Lifecycle fixes from `2a6a2f4e...` remain present, including actual-state vector pause/resume.
13. Lua remains unchanged.
14. Resource wiring passes.
15. Java 21 compile passes.
16. JAR builds successfully.
17. Expected class count remains 81 unless an explicitly justified release-only class is added. Prefer no new runtime classes.
18. NeoForge metadata reports version `0.1.0`.
19. JAR filename uses `0.1.0`.
20. Manifest contains no stale static Debian/JDK `Created-By` claim.
21. Record source commit, CI run, artifact ID and final JAR SHA-256.

---

## 8. Mandatory final smoke test after CI

This is Gate 2 and is required even after the lifecycle candidate passes.

Reason: final release changes default/config-loading behavior.

Use the exact short smoke sequence in `docs/RELEASE_POLICY_LOCK_2026-09-06.md`:

- verify clean new diffraction config generation with old beta file ignored;
- verify diffraction defaults ON;
- basic play -> stop -> play;
- one known V7.1 opening sanity check;
- synchronized playback plus pause/resume;
- inspect logs for version/config/OpenAL/EFX/lifecycle errors.

Do not redo the old full acoustic A/B campaign unless the smoke test reveals a real regression.

---

## 9. Freeze procedure after smoke approval

After Gate 2 passes:

1. record the user's runtime smoke verdict separately from CI/source evidence;
2. record final source commit and final JAR SHA-256;
3. create a final stable/archive ref and/or release tag for the finished build;
4. leave `phase5-diffraction-v7-1-runtime-approved` and `archive-phase5-diffraction-v7-1-runtime-approved` untouched at frozen V7.1;
5. update README/status/handoff to distinguish:
   - frozen acoustic reference;
   - audited lifecycle candidate history;
   - final release source/JAR;
6. decide separately whether to merge/repoint `main`.

No additional reconstruction phase or acoustic redesign is implied by release completion.
