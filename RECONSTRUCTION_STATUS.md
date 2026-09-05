# Reconstruction Status — Historical Notice

The reconstruction/audit project that produced the current source is **complete**. This file remains at the repository root only because older handoffs and frozen references point to it.

For current release information, use:

- [`README.md`](README.md) — release overview, requirements, installation and features
- [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md) — plain-language configuration guide
- [`docs/KNOWN_LIMITATIONS.md`](docs/KNOWN_LIMITATIONS.md) — current limitations and runtime-validation scope

## Completed reconstruction baseline

The previous Beta11 Hotfix3 binary was used as the authoritative reconstruction baseline. The completed audit established:

- exact 60/60 class-path topology for the original baseline
- structural ABI and constant-value checks
- mixin/accessor annotation reconciliation
- processed-resource parity
- method/control-flow auditing
- preserved OpenAL ordering, source lifetime, synchronized-start, EFX and cache invariants

The original baseline artifact SHA-256 is retained for traceability:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Detailed reconstruction evidence remains under `docs/` and the frozen Git refs. Those documents are historical audit records, not the current release status.

## Current development state

The current release-candidate line incorporates the reconstructed baseline plus the runtime-approved synchronized clarity correction, opening-aware vertical sound, and behavior-preserving performance housekeeping.

Acoustic behavior is frozen for release cleanup. Remaining release work is limited to correctness/stability checks, documentation, packaging and final CI unless a real bug is discovered.

No merge to `main` is implied by this status file; branch/release promotion remains a separate explicit action.
