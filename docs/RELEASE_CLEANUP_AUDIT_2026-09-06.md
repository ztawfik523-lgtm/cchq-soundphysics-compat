# CC:HQ Sound Physics Compat — release cleanup audit

**Date:** 2026-09-06 (Africa/Cairo)

This is the release-presentation/source audit started after the lifecycle source audit. It does **not** rename or repackage the mod yet. The current runtime candidate remains the audited lifecycle build from source commit `2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7` with JAR SHA-256 `4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`.

The runtime lifecycle gate is still pending. This document inventories what should be cleaned after that gate and separates harmless presentation cleanup from changes that can affect compatibility or default behavior.

---

## 1. Production identity is currently inconsistent

Three independent version strings currently disagree:

- `gradle.properties` / artifact version: `0.1.0-beta11-phase5-v7-1-performance-test`
- Java `CCHQSoundPhysicsCompat.VERSION`: `0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test`
- `META-INF/neoforge.mods.toml` version: `0.1.0-beta11-phase5-diffraction-v7-1-spreading-only-test`

The final release must use one intentional version identity.

### Version strategy choices

**A. Conservative manual alignment**

Pick one final version string and set the three current version declarations to the same value.

- smallest release diff;
- lowest build-system risk;
- future releases can drift again if one location is forgotten.

**B. Centralize release metadata**

Make `mod_version` the build source of truth and expand/derive the metadata version instead of maintaining several independent literals.

- cleaner long-term maintenance;
- prevents future metadata drift;
- requires additional build/code wiring immediately before release.

Do not choose between A/B without an explicit release decision.

The actual final version number is also intentionally unresolved. Plausible naming directions include retaining the Beta11 lineage, graduating to a clean `0.1.0`, or using a new beta identifier. No new release number should be invented implicitly.

---

## 2. Diffraction default is a real release-behavior decision

`DiffractionConfig.ENABLED` still defaults to `false` and the current metadata/startup text describes V7.1 as an experiment that is OFF by default.

At the same time, V7.1 is the frozen, user-accepted acoustic reference for the opening/diffraction behavior.

That creates two valid release approaches:

**A. Keep diffraction OFF by default**

- most conservative for fresh installs;
- preserves the current default exactly;
- users do not receive the accepted V7.1 opening behavior unless they enable it.

**B. Enable accepted V7.1 diffraction by default**

- fresh installs get the accepted stable opening behavior automatically;
- changes the default behavior of the release;
- existing installations with an already-written config may keep their stored value instead of adopting the new default.

This must be an explicit release choice after runtime lifecycle acceptance. The audit does not change the default.

---

## 3. Diffraction config naming is compatibility-sensitive

Current config filename:

`cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml`

Current root key:

`portal_diffraction_v7_1_spreading_only_test`

These names are ugly for a final release, but they are not merely cosmetic. Renaming the file and/or schema root without migration can cause existing beta/test users to lose their stored settings and receive newly generated defaults.

### Config-name choices

**A. Preserve the legacy filename and root key**

- zero migration risk;
- existing user settings continue to load;
- final config retains obvious experimental naming.

**B. Rename with explicit migration/compatibility handling**

- clean final config naming;
- can preserve existing settings;
- adds migration code and another runtime path to validate before release.

**C. Rename without migration**

- smallest source change;
- existing settings are silently abandoned;
- not appropriate unless losing beta configuration is explicitly accepted.

No config filename/root rename should be done during cosmetic cleanup without resolving this tradeoff.

The other existing config filenames are already neutral enough to keep:

- `cchq_soundphysics_compat-client.toml`
- `cchq_soundphysics_compat-advanced.toml`
- `cchq_soundphysics_compat-sync-hf50.toml`

The HF50 filename is implementation-flavored but already represents user-approved behavior and does not contain `test`/`candidate`; keeping it avoids unnecessary migration.

---

## 4. Safe production-facing wording cleanup after the runtime gate

The following are presentation-only or comment-only and can be normalized without changing acoustic equations or config values:

### `CCHQSoundPhysicsCompat.java`

Current stale wording includes:

- Java `VERSION` with `spreading-only-test`;
- comment saying the fresh V6 filename protects the portal-energy test;
- startup message saying `V7.1 spreading-only aperture-energy diffraction test available and OFF by default`;
- `Phase 5 advanced config` startup label.

Final wording should describe the shipping compatibility features rather than the experiment history.

### `META-INF/neoforge.mods.toml`

The description currently reads like an experiment report: Phase 5, V6/V7.1 history, V3-V5 comparison, and `The experiment is OFF by default`.

For release it should become a concise user-facing description of what the mod does, while detailed provenance remains in the repository docs.

### `DebugCommands.java`

The command itself (`/cchqphysics diffraction ...`) is fine, but messages currently say `diffraction test` and the class is described as `Phase-5 validation commands`.

Those strings can become neutral diagnostics wording.

### `DiffractionConfig.java`

Source/config comments still use:

- `Experimental V7.1...`;
- `Experimental Phase-5...`;
- `Narrow experiment scope`;
- `isolated test`;
- V6/V7.1 provenance wording.

Comments can be rewritten as stable behavior descriptions while keeping numeric defaults and keys unchanged.

### `SpectralMixConfig.java`

Comments still refer to `Phase-5 HF50 A/B`, `candidate`, and `Issue-A candidate`.

Those can be rewritten as the approved synchronized-copy spectral compensation behavior. Numeric defaults and schema keys should remain unchanged.

### `ExtendedClientConfig.java`

The class header still says `Phase-5 test/extended controls`. That is source-only wording and can be normalized.

---

## 5. Internal diagnostic/schema names should not be casually renamed

There are many names such as:

- `beta9_*`
- `beta10_*`
- `beta11_*`
- `[phase5/... ]` log tags
- `portalV7_1=true`
- Cloth Config labels such as `Beta9 room backoff`, `Beta10 exact ray cache`, `Beta11 room-ray memo`
- older `beta1`, `beta1b`, and `beta3` explanatory UI text.

They fall into different risk classes.

### Config keys (`beta9_*`, `beta10_*`, `beta11_*`)

These are persisted schema identifiers. Renaming them would be a configuration migration, not a visual cleanup. Keep them unless a migration is deliberately implemented.

### Log/summary tokens

Changing `[phase5/... ]`, `syncHf50`, `portalV7_1`, and Beta-number summary names does not alter acoustics, but it breaks continuity with existing log analysis instructions and historical diagnostics. There is little release value in changing them unless a clean public diagnostic format is desired.

### Cloth Config display strings

These are user-facing and can be made descriptive while preserving underlying config keys. Example directions:

- `Beta9 room backoff` -> `Adaptive room backoff`
- `Beta10 exact ray cache` -> `Exact occlusion ray cache`
- `Beta11 room-ray memo` -> `Room ray memoization`
- `Reference preset: beta1 / alpha20 acoustics` -> a neutral `Reference acoustic preset` description.

This is safe presentation cleanup, but it is optional. Keeping historical implementation names is also useful for technical support. Choose whether the final UI should favor user-facing clarity or debugging continuity.

---

## 6. Stale JAR manifest metadata

`META-INF/MANIFEST.MF` currently contains:

`Created-By: 21.0.11 (Debian)`

The successful lifecycle audit CI used Temurin Java 21.0.12, so this static line is already misleading.

Safe release choices:

**A. Remove the static `Created-By` line** and keep only the manifest version.

**B. Generate build/JDK provenance dynamically** if reproducible build metadata is desired.

The static Debian/JDK 21.0.11 claim should not be left in a final artifact built elsewhere.

---

## 7. Historical workflows and documents should not be mass-cleaned

The repository intentionally contains old Phase 1-5 audit documents and experiment workflows. Their historical names are evidence, not production presentation bugs.

Do **not** rewrite historical documents to pretend V3/V4/V5/V6/V7 experiments never existed.

Obsolete workflows can be considered separately for repository tidiness, but deleting or rewriting them is not necessary for the runtime JAR and can remove convenient provenance. Treat workflow cleanup as repository maintenance, not release correctness.

Likewise, branch names such as `phase5-v7-1-lifecycle-state-finish` do not need to be renamed. The final stable/archive ref or release tag can carry the clean public identity.

---

## 8. Active docs were stale after the lifecycle source audit

Before this audit, `README.md` and `RECONSTRUCTION_STATUS.md` still pointed to the superseded lifecycle source checkpoint/JAR (`be03d30e...` / `6d0fa98e...`) rather than the audited pause/resume/source-hardening candidate.

Those active status documents should be synchronized now because that is documentation-only and does not need the runtime gate.

Current audited production-source checkpoint:

`2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`

Current lifecycle candidate JAR SHA-256:

`4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`

Successful audit CI run:

`33996243988`

Artifact:

`9978159512`

---

## 9. Release cleanup execution order

After the audited lifecycle candidate passes the user's runtime test:

1. Resolve final version naming strategy and exact version value.
2. Resolve whether V7.1 diffraction ships enabled or disabled by default.
3. Resolve legacy diffraction config naming: keep legacy names or implement migration.
4. Apply safe presentation cleanup: Java startup text/comments, mod description, command messages, config comments and optionally Cloth UI labels.
5. Remove/fix stale static manifest build metadata.
6. Build from a clean checkout with Java 21.
7. Verify only intended release-presentation/default/migration changes occurred relative to audited source `2a6a2f4e...`.
8. Re-run acoustic invariant checks against frozen V7.1 and lifecycle invariant checks against the audited source.
9. Inspect packaged mod metadata/resources/classes and record the final JAR SHA-256.
10. Perform a short final smoke test if any default/config migration behavior changed.
11. Create final stable/archive ref and/or release tag while keeping the frozen V7.1 acoustic ref untouched.
12. Decide separately whether/when to merge to `main`.

---

## 10. Current stop point

No production release naming/default/config-schema change has been made by this audit.

Source/CI candidate remains:

- source checkpoint: `2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`
- candidate SHA-256: `4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`
- lifecycle runtime acceptance: **pending**

The audit can continue with documentation synchronization now. Actual release transformation stays behind the runtime gate unless the user explicitly changes that policy.
