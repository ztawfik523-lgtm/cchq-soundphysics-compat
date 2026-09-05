# CC:HQ Sound Physics Compat — locked final-release policy

**Date:** 2026-09-06 (Africa/Cairo)

This document records the release decisions the user made after the release-cleanup audit. These decisions are locked for the eventual final-release transformation unless the user explicitly changes them later.

## Current runtime gate remains unchanged

The current audited lifecycle candidate is still the build that must be runtime-tested first.

- audited production-source checkpoint: `2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`
- lifecycle candidate JAR SHA-256: `4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`
- successful source-audit CI run: `33996243988`
- lifecycle runtime acceptance: **pending**

Do not call that candidate runtime-approved until the user actually runs the lifecycle sequence and provides the result/logs.

## Locked release decisions

### 1. Final version identity

This is a private mod, so there is no need to preserve a long experimental public version lineage.

Use final version:

`0.1.0`

Implementation policy:

- make `mod_version` the single release source of truth;
- derive/expand NeoForge metadata from it rather than leaving independent literal versions that can drift;
- Java-visible version reporting should use the same value rather than retaining a separate stale literal.

The goal is one consistent final identity in the JAR filename, NeoForge metadata and runtime version reporting.

### 2. V7.1 diffraction ships ON by default

The user chose to leave accepted V7.1 diffraction enabled by default because the measured diffraction-specific cost was small and portal-ray cost itself was negligible in the benchmark.

Final-release default:

`DiffractionConfig.enabled = true`

Important distinction:

- this is a deliberate **default-behavior change** relative to the current lifecycle candidate, whose diffraction default is still OFF;
- do not alter the frozen V7.1 equations/parameters while making this default change;
- HF50 remains exactly at its already-approved defaults.

### 3. Clean break for the old experimental diffraction config

The user chose option **3C**: clean final config naming with **no migration** from the old beta/test diffraction config.

Final intended config identity:

- filename: `cchq_soundphysics_compat-diffraction.toml`
- schema/root key: `portal_diffraction`

The old experimental config:

- `cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml`
- root `portal_diffraction_v7_1_spreading_only_test`

is intentionally not migrated. If it remains in a user's config directory, the final build should ignore it and create/use the new clean config instead.

This is acceptable because the mod is private and the user explicitly preferred a clean break over carrying migration complexity.

### 4. Clean user-facing terminology

The user chose option **4A**.

Clean user-facing UI/startup/config descriptions so the final mod no longer looks like a Phase-5 experiment.

Examples:

- `Beta9 room backoff` -> `Adaptive room backoff`
- `Beta10 exact ray cache` -> `Exact occlusion ray cache`
- `Beta11 room-ray memo` -> `Room ray memoization`
- `diffraction test` -> `Diffraction`
- remove `Phase 5`, `candidate`, `Issue-A`, `experimental`, `spreading-only-test`, and similar development wording from production-facing text where it is not technically necessary.

Compatibility rule:

- preserve persisted internal config keys such as `beta9_*`, `beta10_*`, `beta11_*` unless a separate migration is explicitly intended;
- preserve useful internal/log tokens where changing them would only break diagnostic continuity;
- historical docs/workflows/branch names should keep their original terminology because they are provenance, not user-facing release presentation.

## Other release-cleanup decisions that do not need more user input

Handle these as implementation details:

- remove the stale static `Created-By: 21.0.11 (Debian)` manifest line rather than claiming a JDK/build host that may not match the actual final builder;
- rewrite `neoforge.mods.toml` description as a concise description of the shipping compatibility mod;
- clean production source comments/startup messages without changing acoustic numeric values;
- keep the approved HF50 values unchanged;
- keep the frozen V7.1 acoustic equations/parameters unchanged;
- keep the deliberate Beta9 eligibility expression with bitwise `&` unchanged;
- keep lifecycle hardening from source checkpoint `2a6a2f4e...` unchanged;
- do not mass-delete or rewrite historical audit documents/workflows merely for presentation.

## CRITICAL: two-stage runtime validation rule

There are **two different runtime gates**, and the second one must not be forgotten.

### Gate 1 — audited lifecycle candidate

First test the current audited lifecycle candidate:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Use the full lifecycle sequence already documented in the main handoff/source-audit docs.

If Gate 1 fails, diagnose/fix it before applying final release transformation.

### Gate 2 — final cleaned release smoke test

Even if Gate 1 passes, the eventual final cleaned build must receive a **short final smoke test** before being frozen as the release.

Reason: the final build will intentionally differ from the lifecycle candidate in fresh-install behavior because it will:

1. turn V7.1 diffraction **ON by default**; and
2. discard the old experimental diffraction config identity in favor of a new clean filename/root with **no migration**.

Therefore a passing lifecycle-candidate test does **not** by itself validate the final release's new default/config-loading behavior.

Do **not** redo the entire historical acoustic campaign. The second test is a targeted release smoke test only.

## Required final-release smoke test

After Gate 1 passes and the release transformation is applied/built:

1. Install only the final compat JAR.
2. Leave the old experimental diffraction TOML in place if it exists, but ensure the new final `cchq_soundphysics_compat-diffraction.toml` does not exist before first launch.
3. Launch Minecraft and confirm the final mod/version loads normally.
4. Confirm the new clean diffraction config is generated/used and reports diffraction enabled by default.
5. Confirm the old experimental diffraction config does not override the new final config.
6. Run `/cchqphysics diffraction status` (or the final equivalent command) and confirm diffraction is ON.
7. Play -> stop -> play one speaker to confirm ordinary playback still works.
8. Check one previously-known V7.1 opening/occlusion scenario to confirm the accepted diffraction behavior is present. This is a sanity check, not a new A/B campaign.
9. Start synchronized speakers and do one pause -> resume cycle to confirm final packaging/default cleanup did not disturb synchronized playback/lifecycle behavior.
10. Check `latest.log` / `debug.log` for version/config/OpenAL/EFX/lifecycle errors. If anything is suspicious, analyze the full relevant sequence before release acceptance.

If this short smoke test is clean, then proceed to the final stable/archive ref/tag and final SHA recording.

## Final release order

The intended order is now fixed:

1. Runtime-test lifecycle candidate `4b3c8c52...`.
2. If clean, apply the locked final-release transformation.
3. Build and run final CI/source/JAR invariant checks.
4. Run the short final-release smoke test above.
5. If clean, freeze the final release state and record its commit/JAR SHA.
6. Decide separately whether to merge/repoint `main`; do not alter the frozen V7.1 acoustic reference.
