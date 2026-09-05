# CC:HQ Sound Physics Compat — validated final-release prep status

**Date:** 2026-09-06 (Africa/Cairo)

This document records the completed source/build preparation for the eventual private `0.1.0` release. It does **not** mean either runtime gate has passed.

## Branch separation

The lifecycle candidate remains preserved on:

`phase5-v7-1-lifecycle-state-finish`

The prepared final-release source lives separately on:

`phase5-v7-1-final-release-prep`

This separation is intentional. The user should still run Gate 1 against the already-audited lifecycle candidate, not substitute this prep JAR for that test.

## Gate 1 candidate remains authoritative for lifecycle testing

Audited lifecycle production-source checkpoint:

`2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`

Audited lifecycle candidate SHA-256:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Gate 1 runtime status:

**PENDING**

Do not call the lifecycle candidate runtime-approved until the user runs the full lifecycle sequence and the resulting logs/listening behavior are reviewed.

## Clean release-prep production-source checkpoint

The production source that passed final-release prep CI is:

`438762566d001ac89df47280682910af0b04e6ff`

The temporary validation workflow was removed afterward. Workflow-removal/documentation commits may advance the prep branch beyond `438762...` without changing the validated production source.

## Locked release behavior implemented on the prep branch

The prep source implements the release policy from `docs/RELEASE_POLICY_LOCK_2026-09-06.md`:

- final version `0.1.0`;
- `mod_version` is the build identity source of truth;
- NeoForge metadata is expanded from `mod_version`;
- runtime startup version is read from `ModContainer` metadata rather than a second hard-coded version constant;
- V7.1 diffraction defaults **ON**;
- final diffraction config filename is `cchq_soundphysics_compat-diffraction.toml`;
- final diffraction root is `portal_diffraction`;
- no migration/read fallback for the old experimental diffraction config;
- old experimental config may remain on disk but is intentionally ignored by this release;
- user-facing Phase/Beta/test/candidate/Issue-A/alpha20 lineage wording was cleaned from current startup/config/UI presentation;
- persisted internal advanced keys such as `BETA9_*`, `BETA10_*`, `BETA11_*` remain unchanged;
- internal historical diagnostic tokens such as `[phase5/...]`, `portalV7_1`, and `syncHf50` remain where useful for log continuity;
- stale static `Created-By: 21.0.11 (Debian)` manifest metadata was removed.

## Explicitly frozen behavior

CI asserted that these files were byte/source unchanged relative to audited lifecycle source `2a6a2f4e...`:

- `CompatAudioManager.java`
- `SoundEngineLifecycleMixin.java`
- `VerticalDiffractionRelief.java`
- `SynchronizedSpectralBalancer.java`
- `ProgressiveOcclusionModel.java`
- `EnvironmentSmoother.java`
- `SyncStartCoordinator.java`
- `PositionStabilizer.java`
- `Beta9Optimizer.java`
- `Beta10Optimizer.java`
- `Beta11RoomRayCache.java`
- `SoundPhysicsBridge.java`

Lua also remained unchanged.

Therefore the prep transformation did not rewrite the V7.1 acoustic algorithm, HF50 algorithm, progressive direct behavior, synchronized-start coordinator, lifecycle fixes, or optimizer behavior.

The deliberate eligibility expression remains exactly:

`Beta9Optimizer.isAudibleAndRecord(state.sourceId) & beta9EligibleReal(state, now)`

with bitwise `&`.

## Numeric behavior checks

CI asserted the accepted diffraction numeric values remained:

- min source above listener = 0.25
- escape clearance = 1.5
- search radius = 8.0
- portal coupling = 0.25
- portal activation raw = 2.0
- low delta scale = 4.0
- high delta scale = 1.5
- aperture spread scale = 3.0
- horizon fade start = 0.75
- candidate separation = 2.0
- selection hysteresis = 0.35
- scan interval = 1000 ms
- leg recheck distance = 0.75
- ray cache = 5000 ms

Only the diffraction **enabled default** changed to `true`, as explicitly requested.

CI also asserted HF50 remains:

- enabled = true
- dark source cutoff = 0.35
- peer clear cutoff = 0.75
- minimum peer gap = 0.40
- clarity floor ratio = 0.50
- maximum cutoff lift = 0.55

## Successful final-release prep validation

Workflow run:

`33998525833`

Job:

`101393148070`

Validated successfully:

- Java 21 / Gradle wrapper;
- ancestry to audited lifecycle source;
- frozen V7.1 refs unchanged;
- frozen acoustic/lifecycle/optimizer source files unchanged;
- no Lua changes;
- final version policy;
- clean diffraction filename/root;
- diffraction default ON;
- exact accepted diffraction numeric values;
- exact accepted HF50 defaults;
- deliberate bitwise `&` expression;
- reconstruction compile classpath;
- resource wiring;
- NeoForge metadata expansion to `0.1.0`;
- Java 21 compilation, including `ModContainer` runtime-version lookup;
- JAR build;
- final filename `cchq_soundphysics_compat-0.1.0.jar`;
- packaged NeoForge version `0.1.0`;
- 81 classes;
- stale static manifest provenance absent;
- artifact upload.

Artifact ID:

`9978794000`

Artifact ZIP digest:

`sha256:b4fd8aef5ecc2320f3789497e425901a3c15c363eee5e383f0a46c510099931d`

Prepared `0.1.0` JAR SHA-256:

`316c8405fceb2ed6227f3b06fef68c40ef3cad458ebc451bac3ed7d3e6f30f77`

Class count:

`81`

Again: this is a **validated release-prep artifact**, not a runtime-approved final release.

## Runtime status — two gates remain

### Gate 1

Still run the full lifecycle sequence using:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

If Gate 1 reveals a lifecycle regression, fix that from evidence first and port any necessary correction into the release-prep branch before continuing.

### Gate 2

After Gate 1 is clean, use the prepared/final `0.1.0` build for the short release smoke test described in `docs/RELEASE_POLICY_LOCK_2026-09-06.md`.

Gate 2 remains mandatory because the prep/final build intentionally differs from the lifecycle candidate in fresh-install behavior:

1. diffraction defaults ON; and
2. the old experimental diffraction config identity is discarded in favor of the clean filename/root with no migration.

Do not freeze/tag/archive the final `0.1.0` release until Gate 2 is clean.

## Next action when the user eventually tests

1. Use Gate-1 candidate `4b3c8c52...` and run the lifecycle sequence.
2. Analyze the full `latest.log` and `debug.log` sequence before reaching a verdict.
3. If Gate 1 passes, confirm the release-prep branch still contains the same validated production source or only intentional follow-up fixes.
4. Use/build the `0.1.0` prep source and perform the short Gate-2 smoke test.
5. If Gate 2 passes, record final source/JAR SHA and create the final stable/archive ref/tag.
6. Leave frozen V7.1 acoustic refs untouched.
