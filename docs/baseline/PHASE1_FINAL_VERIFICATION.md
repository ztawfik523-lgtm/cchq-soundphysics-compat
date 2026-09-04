# Phase 1 final verification — Beta11 Hotfix3

Phase 1 is considered complete only against the tested binary:

`cchq_soundphysics_compat-0.1.0-beta11-hotfix3.jar`

SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

## Binary inventory verification

Direct inspection of the authoritative JAR produced:

- 75 ZIP entries total;
- 65 non-directory files;
- 60 `.class` files;
- 10 directory entries;
- package class counts:
  - root compat package: 1;
  - `audio`: 44 including nested classes;
  - `mixin`: 11;
  - `config`: 4.

`HOTFIX3_SHA256SUMS.txt` contains exactly 65 per-file SHA-256 entries, one for every non-directory file in the JAR.

## Exact source-relevant resource verification

The following repository blobs were compared against the exact bytes extracted from Hotfix3. The Git blob SHA-1 values match the extracted bytes:

| Resource | Verified Git blob SHA-1 |
| --- | --- |
| `META-INF/MANIFEST.MF` | `eaaf2adf468022c856c6849a7a35d05a2fb27f29` |
| `META-INF/accesstransformer.cfg` | `1db2ac63c484ef2b7669744eeecf04ef35868b92` |
| `META-INF/neoforge.mods.toml` | `0f7e93b65ec1acb93acb1023f3fc9b3d5c04c5df` |
| `cchq_soundphysics_compat.mixins.json` | `e13c939bb4771286374d3801e48e8ebca4685ce6` |
| `assets/cchq_soundphysics_compat/lang/en_us.json` | `704bb607f3bfb6898e857c8ebdc5e3a348b7d27f` |

The manifest required one correction during this audit: the Hotfix3 manifest uses CRLF line endings and a terminating blank CRLF line. The repository copy was corrected so its blob now matches the extracted bytes exactly.

## Phase 1 exit criterion

Satisfied:

- authoritative whole-JAR hash frozen;
- every class/resource inventoried;
- every non-directory entry fingerprinted;
- package/nested-class topology recorded;
- all source-relevant runtime resources copied byte-for-byte into `src/main/resources`.

Phase 1 is therefore closed. Any later reconstruction discrepancy must be resolved in favor of the Hotfix3 binary and these fingerprints.
