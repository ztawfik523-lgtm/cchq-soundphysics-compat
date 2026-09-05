# 0.1.0-beta11-rc1 Release Notes

## Highlights

- CC:HQ whole-file playback is processed as real positional Sound Physics audio.
- 17-path progressive wall obstruction provides wall-thickness muffling.
- Reflected source direction is stabilized for long-running speakers.
- Synchronized copies receive a bounded clarity correction only when their direct cutoffs diverge severely.
- Opening-aware vertical sound lets real nearby openings contribute to blocked elevated sound.
- Adaptive probing, exact-result/ray reuse, room-ray memoization and bounded opening checks reduce repeated acoustic work.
- Release-facing Cloth Config categories and TOML keys replace internal experimental names.
- Opening-aware sound and synchronized clarity balance are enabled by default using the approved tuned values.

## Configuration reset from internal builds

Experimental config filenames and keys are not migrated automatically. RC1 creates the release-facing config files documented in `docs/CONFIGURATION.md`.

## Validation note

The acoustic behavior used by RC1 is based on the frozen runtime-approved opening and synchronized-balance checkpoints plus behavior-preserving performance housekeeping. Cleanup itself is source/CI validated; final runtime promotion should wait until the user launches the resulting release-candidate JAR and checks for regressions.

The final stress benchmark currently covers 1- and 4-source cases. A 12-source stress run has not yet been completed.
