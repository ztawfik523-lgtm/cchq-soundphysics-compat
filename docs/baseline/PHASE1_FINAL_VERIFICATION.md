# Phase 1 final verification — Beta11 Hotfix3

Phase 1 is complete only against the tested binary:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

The exact binary was independently rechecked again on 2026-09-04. See `PHASE1_JAR_RECHECK_2026-09-04.md`.

## Binary inventory verification

Direct inspection of the authoritative JAR produced:

- 75 ZIP entries total;
- 65 non-directory files;
- 60 `.class` files;
- 10 directory entries;
- all classfiles use Java 21 / major version 65;
- package class counts:
  - root compat package: 1;
  - `audio`: 44 including nested classes;
  - `mixin`: 11;
  - `config`: 4.

`HOTFIX3_SHA256SUMS.txt` contains exactly 65 per-file SHA-256 entries, one for every non-directory file in the JAR. The 2026-09-04 JAR recheck reproduced those fingerprints exactly.

## Exact source-relevant resource verification

The following repository blobs were compared against the exact bytes extracted from Hotfix3. The Git blob SHA-1 values match the extracted bytes:

| Resource | SHA-256 | Verified Git blob SHA-1 |
| --- | --- | --- |
| `META-INF/MANIFEST.MF` | `3c2cfe2a82eae5330820aa3a472a83ece7307c672bb8d0c5f9222ac048926a52` | `eaaf2adf468022c856c6849a7a35d05a2fb27f29` |
| `META-INF/accesstransformer.cfg` | `f44a718305a547c39a73cf69d250dd3d2ff75fd010258289a48cb5b32ccd130a` | `1db2ac63c484ef2b7669744eeecf04ef35868b92` |
| `META-INF/neoforge.mods.toml` | `f18e09d33ecac1185b254274c77e76d97c718bf58ee20aed8d8ef7e65cbd0220` | `0f7e93b65ec1acb93acb1023f3fc9b3d5c04c5df` |
| `cchq_soundphysics_compat.mixins.json` | `28fab2e92908c86ce0d1651a52c320f2f281c6a5ae3ed055df54f3d3c194ef84` | `e13c939bb4771286374d3801e48e8ebca4685ce6` |
| `assets/cchq_soundphysics_compat/lang/en_us.json` | `8941c3be17b9394d4b883bd0ecb2c80d6ea6bb957b44a6062ec30715f67d1a19` | `704bb607f3bfb6898e857c8ebdc5e3a348b7d27f` |

The Hotfix3 manifest uses CRLF line endings and a terminating blank CRLF line. The repository copy preserves those bytes exactly.

## Phase 1 exit criterion

Satisfied:

- authoritative whole-JAR hash frozen and reverified;
- every class/resource inventoried;
- every non-directory entry fingerprinted;
- package/nested-class topology recorded;
- all source-relevant runtime resources copied byte-for-byte into `src/main/resources`;
- a second exact-JAR recheck on 2026-09-04 agrees with the original frozen records.

**Phase 1 is COMPLETE / JAR-RECHECKED.**

Any later reconstruction discrepancy must be resolved in favor of the Hotfix3 binary and these fingerprints.
