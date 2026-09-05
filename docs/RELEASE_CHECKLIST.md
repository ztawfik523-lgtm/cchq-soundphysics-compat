# Release Checklist

This checklist replaces the old phase-specific active workflows on the release branch.

## Before calling a build final

- [ ] Java 21 clean build passes.
- [ ] `gradle.properties`, `CCHQSoundPhysicsCompat.VERSION`, and `neoforge.mods.toml` use the same release identity.
- [ ] JAR contains the expected 81 classes and required runtime resources.
- [ ] Required mixin config and access transformer are packaged and registered.
- [ ] The source eligibility expression still uses the required non-short-circuit `&` form.
- [ ] No Lua files changed from the approved baseline.
- [ ] Synchronized clarity defaults remain 0.35 / 0.75 / 0.40 / 0.50 / 0.55.
- [ ] Opening-aware defaults remain 0.25 / 1.5 / 8 / 0.25 / 2 / 4 / 1.5 / 3 / 0.75 / 2 / 0.35 / 1000 ms / 0.75 / 5000 ms.
- [ ] Opening acoustic formulas remain the runtime-approved V7.1 formulas.
- [ ] No playback timing, OpenAL cursor/buffer-offset, or source-position feature has been added during cleanup.
- [ ] Temporary cleanup scripts/workflows are absent.
- [ ] Release-facing README/config/metadata do not describe the build as an experiment/test candidate.
- [ ] Known limitations accurately state runtime validation scope.
- [ ] Final JAR SHA-256 is recorded.
- [ ] `game_launch_performed=false` is recorded for CI-only validation; runtime approval must only be claimed after an actual user launch/test.

## Runtime checks when testing resumes

A full acoustic retune is not required. The remaining release check should focus on regressions:

- config screen opens and saves all four config specs
- opening-aware sound defaults ON in a fresh config
- synchronized clarity balance defaults ON
- normal one-speaker playback starts/stops cleanly
- synchronized multi-speaker start/stop remains correct
- pause/resume and stopAll remain correct
- sound/resource reload recreates state cleanly
- disconnect/rejoin and dimension change do not preserve stale source/cache state
- no repeated errors or growing cache/source counts during a short soak

A 12-source stress test is optional before RC promotion but should be completed before claiming 12-source runtime validation.
