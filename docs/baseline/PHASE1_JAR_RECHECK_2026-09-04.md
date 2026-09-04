# Phase 1 binary-backed recheck — 2026-09-04

## Authority

This recheck was performed against the user-supplied tested artifact:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

Authoritative SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The supplied JAR's SHA-256 matches the Phase 1 frozen baseline exactly. The JAR, not historical prose or reconstructed source, remains the runtime authority through Phases 3–5.

## Archive inventory recheck

Direct enumeration of the supplied JAR produced:

- 75 ZIP entries total;
- 10 directory entries;
- 65 file entries;
- 60 Java class files;
- 5 source-relevant non-class resources.

All 60 class files have class-file major version `65`, confirming Java 21 bytecode.

The recomputed SHA-256 of every one of the 65 file entries matches `docs/baseline/HOTFIX3_SHA256SUMS.txt` exactly. There are no missing, extra, or mismatched file fingerprints relative to the frozen Phase 1 inventory.

## Exact source-relevant resources

The following SHA-256 values were recomputed directly from the supplied JAR:

| JAR entry | SHA-256 |
| --- | --- |
| `META-INF/MANIFEST.MF` | `0bdda6e4f02c84cc63b58d78a96327b1324168a9a525705424c3aa8fdabb71a` |
| `META-INF/neoforge.mods.toml` | `e6cba69aa6255d7eeb77da2f77d30a0889ba11334ae05e66ec8d2de5ef480562` |
| `META-INF/accesstransformer.cfg` | `5e78c7c7668e0d0bd5cee2ba84176f432997372634b28b614dc82dac2c272e4a` |
| `cchq_soundphysics_compat.mixins.json` | `6188829a7dc2b5c0b490da5bc005acb45059ec907500410d3f942ed7e6dda940` |
| `assets/cchq_soundphysics_compat/lang/en_us.json` | `89412c79262033e5e84adbeab381ede60068a63e32914187f1743aa8d7d65f9` |

Their Git blob identities computed from the exact JAR bytes are:

| JAR entry | Git blob SHA-1 |
| --- | --- |
| `META-INF/MANIFEST.MF` | `b28deb93fefb24b69060baca289567aae866c1e0` |
| `META-INF/neoforge.mods.toml` | `8002bf2100f04415743964e3a1d8a6cbe1694748` |
| `META-INF/accesstransformer.cfg` | `ebc3bc67c83c870c0736e06837477b28bb7edb84` |
| `cchq_soundphysics_compat.mixins.json` | `77e6228a7d2e8820e9389db8f2c4c8fa554fc77a` |
| `assets/cchq_soundphysics_compat/lang/en_us.json` | `62b7335adedb81f7dc59f2b62d77abcda17eca93` |

Those identities match the exact resource files retained under `src/main/resources` and the values already recorded by `PHASE1_FINAL_VERIFICATION.md`.

The manifest was also checked as raw bytes and retains its CRLF line termination.

## Runtime metadata revalidated from the JAR

`META-INF/neoforge.mods.toml` in the supplied binary establishes:

- mod id `cchq_soundphysics_compat`;
- mod version `0.1.0-beta11-hotfix3`;
- Java requirement `[21,)`;
- Minecraft `[1.21.1,1.22)`;
- NeoForge `[21.1.0,)`;
- HQ Speakers `[1.1.4,)`;
- Sound Physics Remastered `[1.21.1-1.5.1,)`;
- optional Cloth Config `[15.0.0,)`;
- client-side dependency scope for the runtime integration.

The exact access transformer widens:

- `SoundPhysics.setEnvironment(...)`;
- `SoundPhysics.setSoundPos(...)`;
- `SoundPhysics.runOcclusion(Vec3, Vec3)`;
- four public-final SPR config holder fields used by Hotfix3.

The exact Mixin config still contains the 11 Phase 1 inventoried client mixins and requires Java 21 compatibility.

## Recheck result

**PASS — Phase 1 remains COMPLETE.**

The newly supplied JAR independently confirms the existing frozen baseline: whole-artifact identity, entry inventory, nested-class topology basis, Java bytecode level, per-file fingerprints, and exact runtime resources all agree with the Phase 1 records.

No Phase 1 baseline correction was required.
