# Phase 1 binary-backed recheck — 2026-09-04

## Authority

This recheck was performed against the user-supplied tested artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

Authoritative SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The supplied JAR's SHA-256 matches the frozen Phase 1 baseline exactly. The JAR remains the runtime authority through Phases 3–5.

## Archive inventory recheck

Direct enumeration of the supplied JAR produced:

- 75 ZIP entries total;
- 10 directory entries;
- 65 file entries;
- 60 Java class files;
- 5 source-relevant non-class resources.

All 60 class files use class-file major version `65`, confirming Java 21 bytecode.

The SHA-256 of every one of the 65 file entries matches `docs/baseline/HOTFIX3_SHA256SUMS.txt` exactly. There are no missing, extra, or mismatched file fingerprints relative to the frozen Phase 1 inventory.

## Exact source-relevant resources

The following values were recomputed directly from the supplied JAR bytes on 2026-09-04:

| JAR entry | SHA-256 | Git blob SHA-1 |
| --- | --- | --- |
| `META-INF/MANIFEST.MF` | `3c2cfe2a82eae5330820aa3a472a83ece7307c672bb8d0c5f9222ac048926a52` | `eaaf2adf468022c856c6849a7a35d05a2fb27f29` |
| `META-INF/neoforge.mods.toml` | `f18e09d33ecac1185b254274c77e76d97c718bf58ee20aed8d8ef7e65cbd0220` | `0f7e93b65ec1acb93acb1023f3fc9b3d5c04c5df` |
| `META-INF/accesstransformer.cfg` | `f44a718305a547c39a73cf69d250dd3d2ff75fd010258289a48cb5b32ccd130a` | `1db2ac63c484ef2b7669744eeecf04ef35868b92` |
| `cchq_soundphysics_compat.mixins.json` | `28fab2e92908c86ce0d1651a52c320f2f281c6a5ae3ed055df54f3d3c194ef84` | `e13c939bb4771286374d3801e48e8ebca4685ce6` |
| `assets/cchq_soundphysics_compat/lang/en_us.json` | `8941c3be17b9394d4b883bd0ecb2c80d6ea6bb957b44a6062ec30715f67d1a19` | `704bb607f3bfb6898e857c8ebdc5e3a348b7d27f` |

These are the values that also appear in `docs/baseline/HOTFIX3_SHA256SUMS.txt` and `docs/baseline/PHASE1_FINAL_VERIFICATION.md`.

The manifest was checked as raw bytes and retains the Hotfix3 CRLF line termination and terminating blank CRLF line.

## Runtime metadata revalidated from the JAR

`META-INF/neoforge.mods.toml` in the supplied binary establishes the tested mod identity/version and dependency ranges for Java 21, Minecraft 1.21.1, NeoForge, HQ Speakers, Sound Physics Remastered and optional Cloth Config.

The exact access transformer widens the SPR methods/fields used by Hotfix3 source, including:

- `SoundPhysics.setEnvironment(...)`;
- `SoundPhysics.setSoundPos(...)`;
- `SoundPhysics.runOcclusion(Vec3, Vec3)`;
- the SPR config-holder fields consumed by the compat scheduler/environment logic.

The exact Mixin config still contains the 11 Phase 1 inventoried client mixins and Java 21 compatibility level.

## Documentation correction note

An earlier draft of this recheck document temporarily contained incorrect recomputed resource SHA-256/Git-blob values. The whole JAR hash and frozen `HOTFIX3_SHA256SUMS.txt` were always correct. This file is now corrected from the actual supplied JAR bytes above so the Phase 1 documents are internally consistent.

## Recheck result

**PASS — Phase 1 remains COMPLETE / JAR-RECHECKED.**

The supplied JAR independently confirms the existing frozen baseline: whole-artifact identity, archive inventory, class topology basis, Java bytecode level, per-file fingerprints and exact runtime resources all agree with the authoritative Phase 1 records.

No baseline class/resource correction is required.
