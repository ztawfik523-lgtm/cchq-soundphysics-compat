# Development and Audit History

The current release-candidate source was produced through a reconstruction and validation process against the previous known-good Beta11 Hotfix3 binary, followed by isolated runtime-tested acoustic fixes and performance cleanup.

This page is an index for maintainers. Normal users should start with the repository `README.md` and `docs/CONFIGURATION.md`.

## Preserved historical evidence

The detailed records remain in this repository, including:

- bytecode/class inventory and original artifact hashes
- build/classpath reconstruction audits
- structural ABI verification
- mixin/accessor annotation audits
- method/control-flow verification
- runtime test notes for synchronized clarity correction
- runtime test notes for vertical/opening behavior

Relevant historical files include the existing `PHASE*`, `RECONSTRUCTION*`, and `BETA11*` documents under `docs/` and `docs/baseline/`.

## Frozen Git checkpoints

Important runtime/audit states are preserved on dedicated frozen/archive refs. Release cleanup must not move those refs.

Notable checkpoints include:

- Phase 4 Hotfix3 parity
- approved synchronized clarity/HF correction
- approved earlier diffraction diagnostic checkpoints
- approved V7.1 opening-aware runtime behavior

The release branch intentionally keeps internal historical class names where renaming would create unnecessary risk. Those names are implementation history, not user-facing modes.

## Release cleanup policy

For the current release candidate:

- acoustic behavior is frozen
- user-facing config names and descriptions are release-facing
- obsolete reconstruction workflows are removed from the release branch
- historical audit documents remain available for traceability
- final CI checks release invariants directly instead of replaying every reconstruction phase

No merge to `main` is part of this historical cleanup unless explicitly requested separately.
