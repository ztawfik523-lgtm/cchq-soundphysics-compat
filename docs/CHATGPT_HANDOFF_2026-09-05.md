# CC:HQ Sound Physics Compat — ChatGPT handoff

Snapshot date: **2026-09-05**

This is the first file a new ChatGPT conversation should read before doing any more work on this repository.

Repository:

`ztawfik523-lgtm/cchq-soundphysics-compat`

## 0. What the next chat should do first

Do **not** restart reconstruction, do not merge anything, and do not assume the newest-sounding branch name is the accepted build.

The immediate task is **Phase 5 Issue A runtime A/B testing** using the already verified diagnostic JAR described below.

After the user returns the A/B result and `latest.log`, decide whether reflected-position redirection is actually responsible for the hard-to-name synchronized-playback coloration. If it is not implicated, the next diagnostic step is typed room/reverb-send telemetry, not an acoustic behavior change by guesswork.

A separate elevation/diffraction issue is tracked later in this document and must remain isolated from Issue A.

---

# 1. Non-negotiable project rules

- Hotfix3 is the behavioral authority for the frozen parity baseline.
- Preserve `phase4-hotfix3-parity` permanently.
- Preserve `archive-phase4-hotfix3-parity` permanently.
- Preserve `phase5-test-candidate-1` permanently as the known-good Phase-5 runtime rollback.
- Do **not** merge to `main` unless the user explicitly asks.
- The user performs Minecraft launch/listening tests. Do not claim runtime/audio validation unless the user actually performed it and supplied the result/logs.
- New experimental behavior must stay on isolated branches and must not silently change the known-good defaults.
- Defaults on maintained Phase-5 extensions should preserve Hotfix3 behavior unless the user explicitly chooses an experimental feature.
- Prefer compile-checked/package-local diagnostics over reflective inspection of private fields.
- Keep useful diagnostics until runtime stability and final behavior are settled.
- The user values exactness and durable Git/documentation checkpoints more than speed.

---

# 2. Authoritative Hotfix3 binary baseline

Authoritative JAR SHA-256:

`83500f182fc9829aa1a5a51fbfa11ba6cdfb645699b25d1c445167666dabc1ef`

Frozen inventory:

- 75 ZIP entries
- 10 directories
- 65 files
- 60 `.class`
- Java major 65 / Java 21
- 5 non-class runtime resources

Authoritative non-class hashes:

- `META-INF/MANIFEST.MF` — `3c2cfe2a82eae5330820aa3a472a83ece7307c672bb8d0c5f9222ac048926a52`
- `META-INF/neoforge.mods.toml` — `f18e09d33ecac1185b254274c77e76d97c718bf58ee20aed8d8ef7e65cbd0220`
- `META-INF/accesstransformer.cfg` — `f44a718305a547c39a73cf69d250dd3d2ff75fd010258289a48cb5b32ccd130a`
- `cchq_soundphysics_compat.mixins.json` — `28fab2e92908c86ce0d1651a52c320f2f281c6a5ae3ed055df54f3d3c194ef84`
- `assets/cchq_soundphysics_compat/lang/en_us.json` — `8941c3be17b9398d4b883bd0ecb2c80d6ea6bb957b44a6062ec30715f67d1a19`

Exact authoritative manifest bytes correspond to:

`Manifest-Version: 1.0\r\nCreated-By: 21.0.11 (Debian)\r\n\r\n`

---

# 3. Reconstruction status

## Phase 1

**COMPLETE / JAR-RECHECKED**

Binary baseline frozen and independently rechecked.

## Phase 2

**COMPLETE / JAR-RECHECKED**

Build project reconstructed and pinned.

## Phase 3

**COMPLETE / RECHECKED**

All authored Java source reconstructed.

## Phase 4

**COMPLETE / RECHECKED / FROZEN**

Permanent parity branch:

`phase4-hotfix3-parity`

Frozen ref:

`79eed29767343ee34022e8f6268b386f75e84c9f`

Permanent archival ref:

`archive-phase4-hotfix3-parity`

Same frozen ref.

Final audited Phase-4 code/build head recorded during closure:

`98e7dedb7ecf6fda22008b084b6bb41956edff78`

Phase-4 structural evidence established:

- 65/65 file topology
- 60/60 class paths exact
- 60/60 structural ABI exact
- 69/69 compiled `ConstantValue` exact
- zero bootstrap/string-concat recipe mismatches
- 11/11 Mixin/accessor semantic annotation sets reconciled
- 5/5 non-class resources byte-exact
- 550 methods audited
- 478 normalized instruction-equivalent
- 72 reviewed compiler-shape differences in 13 classes
- no unresolved proven behavioral drift

## Phase 5

**IN PROGRESS — CORE RUNTIME PASSED / ISSUE A AWAITING USER A/B TEST**

Do not mark complete yet.

---

# 4. Exact build/toolchain contract

Pinned/tested stack:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.248
- ModDevGradle 2.0.144
- Gradle 9.2.1
- CC:Tweaked 1.120.2
- CC:HQ Speakers runtime reports `1.1.4-neoforge-1.21.1`
- Sound Physics Remastered 1.21.1-1.5.1
- SPR pinned Modrinth version ID `Dd2tmpsk`
- HQ Speakers dependency pin historically recorded as project/version `ygA78R8l/u5PEI5Ax`
- Cloth Config 15.0.140

Important compile/runtime contract:

- runtime uses the untouched tested SPR JAR;
- compilation uses an isolated SPR copy transformed with the compat access transformer;
- transformed compile artifact is `build/reconstruction/compile/sound-physics-remastered-at.jar`;
- raw SPR must not silently become the effective compile target for private members widened by the compat AT;
- Java 21 is required;
- Phase-5 Gradle config disables configuration cache because `prepareSprCompileJar` resolves project/configuration objects at execution time.

---

# 5. Known-good Phase-5 runtime candidate — rollback authority

Frozen branch:

`phase5-test-candidate-1`

Exact source commit:

`44612192d875e43ecef66ca51798cab7adb17020`

Verified JAR:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-test.jar`

SHA-256:

`6d782812f7915de1870a8b5ae0f619556e7ec1d24ef2eaffa7b5b225aa00bd93`

This is the rollback authority for Phase-5 experiments.

It contains 65 classfiles: the original reconstructed 60 plus five Phase-5 support classes:

- `DebugCommands`
- `DebugControl`
- `DebugDiagnostics`
- `ExtendedClientConfig`
- `ExtendedClientConfigAccess`

The original `ClientConfig.java` is unchanged from the frozen Phase-4 source.

### Runtime evidence already established by the user

The user performed the actual Minecraft/audio tests. Evidence includes:

- correct Phase-5 identity loaded;
- compat Mixins applied;
- OpenAL and SPR EFX initialized successfully with four auxiliary sends;
- compat private per-source EFX created successfully;
- no observed ordinary-playback private-EFX failure/native fallback fault;
- `efxApplies == efxReattachPasses` across reported telemetry windows;
- 12-source stress playback without compat crash;
- listener/source movement worked;
- doorway clearing transitions worked;
- room/direct reuse and Beta9/Beta10/Beta11 telemetry worked;
- `/cchqphysics reset_caches`, `refresh_rooms`, and `reset_efx` executed while playback survived;
- a brief quiet/static artifact was heard only during explicit `reset_efx` teardown/recreate and is not treated as ordinary-playback failure;
- source cleanup/unregister and normal shutdown worked;
- synchronized playback was heard starting together;
- HQ consistently advertised one more expected sync member than compat audio sources actually registered (for example 4/5), with the coordinator using the existing partial-flush grace; the user still heard all physical speakers, so this is treated as HQ sync-metadata semantics rather than proven source loss;
- user reported the known-good candidate generally sounded correct.

---

# 6. Phase-5 advanced/debug controls on the known-good candidate

Advanced config file:

`cchq_soundphysics_compat-advanced.toml`

There are 35 verified advanced/debug defaults. Important Hotfix3-equivalent defaults include:

Scheduler:

- room slot 50 ms
- min hard stale 500 ms
- max hard stale 2000 ms
- recent source 1000 ms
- teleport distance 4.0 blocks
- source urgent movement 0.10 blocks

Clearing sentinel:

- move 0.05
- raw occluded 0.075
- rearm center 0.12
- open center 0.035
- center drop 0.15
- confirm raw drop 0.035
- confirm cutoff rise 0.055
- cooldown 300 ms

Sync:

- partial flush 100 ms
- stale group 5000 ms

Optimization/features:

- private EFX true
- Beta9 whole-direct reuse true
- Beta9 room backoff true
- Beta9 adaptive controller true
- Beta10 exact ray cache true
- Beta11 room ray memo true
- performance report 10000 ms

Targeted logs default false.

Useful commands:

- `/cchqphysics status`
- `/cchqphysics dump`
- `/cchqphysics refresh_rooms`
- `/cchqphysics reset_caches`
- `/cchqphysics reset_efx`
- `/cchqphysics config`

Mutating debug requests are consumed on the existing sound-thread executor path, not by directly mutating OpenAL from the command callback.

---

# 7. Important Hotfix/runtime invariants to preserve

Do not casually rewrite these:

- no Lua changes;
- `SoundSource.BLOCKS` distance behavior;
- center + 8 inner + 8 outer direct geometry;
- full direct refresh 17 / adaptive alternating 9;
- per-source private EFX isolation;
- never skip required EFX reattachments;
- no private EFX before PLAYING/PAUSED;
- `PositionStabilizer` behavior unless an experiment specifically tests it;
- do not inject/cancel/replace SPR `calculateOcclusion()`;
- no worker-thread SPR world/geometry raycasts;
- strict source lifetime generation/identity;
- scheduling must not intentionally alter PCM/OpenAL clock/buffer offset/sync;
- partial sync grace;
- pending `AL_INITIAL` protection;
- Beta10 exact direct reuse / bit-identical AL suppression;
- Beta11 same-clone room-ray memo;
- cross-clone room reuse telemetry only;
- lifecycle pause/resume/stopAll/destroy/emergencyShutdown/reload;
- preserve non-short-circuit eligibility exactly:

`Beta9Optimizer.isAudibleAndRecord(sourceId) & beta9EligibleReal(...)`

The `&` is intentional; do not change it to `&&`.

---

# 8. Synchronized-mix experiments

## V1 amplitude suppression — REJECTED

Historical/frozen experiment branch name includes:

`phase5-final-feature-test-candidate`

Recorded source commit:

`323d0e34651ae086dcd96ebe608b3149f5f0d73a`

The experiment applied extra whole-source gain attenuation to occluded synchronized copies.

User result:

- combined spectral balance could sound somewhat improved;
- spatial balance became wrong;
- clear off-axis speaker could pull the image strongly left/right;
- heavily occluded speaker could nearly disappear.

Conclusion:

**Reject the algorithm, not merely the tuning.** It fixed spectral summation by altering amplitude and therefore interfered with OpenAL spatial weighting.

Do not resurrect this as the final solution.

## V2 spectral-only compensation — FROZEN EXPERIMENT / NOT FINAL

Frozen test branch:

`phase5-mix-v2-test-candidate`

Exact source commit:

`ab1e1e70a13ebb6f3dadd30581b069f06a15142a`

JAR SHA-256:

`bba8d93e696403ae857dd155db2969c7591886aa1e8734b0b949f1a749c8319c`

V2 safety rule:

- no source gain change;
- no source position change;
- no reverb-send-filter change;
- only bounded direct low-pass cutoff lift for extremely dark synchronized copies when a genuinely clear peer exists.

Two independent clean builds produced byte-for-byte identical JARs.

Runtime result:

- technically healthy;
- did not reproduce V1's amplitude/panning failure;
- user still noticed a hard-to-name reverb/treble/spatial coloration;
- importantly, the user had already noticed that coloration before V2 was enabled in the same session.

Therefore V2 is preserved but not accepted or blamed for the coloration.

See:

`docs/PHASE5_SYNC_MIX_EXPERIMENTS.md`

---

# 9. Issue A — CURRENT ACTIVE INVESTIGATION

Working/docs branch:

`phase5-issue-a-reflection-diagnostics`

Current working branch head at this handoff:

`ed564a0e930d86a3e50c7aebd09c314c84be8186`

The working branch has later documentation-only commits beyond the exact reviewed runtime source.

Known-good base:

`44612192d875e43ecef66ca51798cab7adb17020`

Reviewed frozen runtime-test branch:

`phase5-issue-a-test-candidate-2`

Exact reviewed/test source commit:

`973f1df7dad886fb0f5fffd4264015fecac2e786`

Build identity:

`0.1.0-beta11-phase5-issuea-test`

Verified JAR:

`cchq_soundphysics_compat-0.1.0-beta11-phase5-issuea-test.jar`

SHA-256:

`d649f14cdce89db21a79c396dbdecca681daf3d0389dc794a7ad52929f8c8451`

Verification:

- workflow run `33935819269`
- job `101223434623`
- result **SUCCESS**
- artifact `cchq-phase5-issue-a-reflection-diagnostics`
- artifact id `9960138065`
- artifact digest `sha256:26f2a427795f8fd69e4c9459d165cd748f57471dadc27bdb54c96dda162e4989`
- 67 classfiles
- `game_launch_performed=false` in build metadata; the runtime test is the user's job

Issue-A contains **neither V1 nor V2**.

## Why Issue A exists

In a prior synchronized run, the user described the music as having an unclear extra reverb/treble/spatial coloration. Logs around one such moment showed several nearly clear synchronized copies and one substantially muffled copy with a meaningful reflected-position offset.

Leading hypothesis:

`PositionStabilizer` can move an occluded source toward SPR's reflected point. One displaced/muffled synchronized copy mixed with several bright/direct correlated copies may be perceived as spatial smear, phase-like coloration, extra ambience, or timbre change.

This is **not yet proven**.

## Review feedback already incorporated

An external code review correctly identified weaknesses in the first draft. The current reviewed candidate fixes them:

1. **Global-only switch was too blunt**
   - now there is per-source ON/OFF/AUTO isolation.

2. **Reflective inspection of private fields was fragile**
   - removed;
   - focused diagnostics now use compile-checked package-local paths.

3. **Frozen base inherited stale docs and obsolete one-shot patch scripts**
   - current branch status docs are updated;
   - `phase5_apply_batch1..4.py` were removed from this branch.

4. The diagnostic preserves both `requestedRedirect` and actual `redirectActive` so the log distinguishes "SPR/occlusion wanted a redirect" from "the diagnostic permitted the redirect."

Per-source overrides clear on source unregister so reused OpenAL source IDs cannot inherit stale test state.

## Issue-A commands

Global:

- `/cchqphysics reflection_redirect on`
- `/cchqphysics reflection_redirect off`
- `/cchqphysics reflection_redirect status`

Per source:

- `/cchqphysics reflection_redirect source <sourceId> on`
- `/cchqphysics reflection_redirect source <sourceId> off`
- `/cchqphysics reflection_redirect source <sourceId> auto`
- `/cchqphysics reflection_redirect source <sourceId> status`

Every launch starts global redirect **ON**, matching known-good behavior. Overrides are runtime-only, not persisted.

`/cchqphysics dump` plus Issue-A dump paths expose real/applied/reflected position, offset, occlusion, requested/active redirect state, source status, and private-EFX direct state.

Current Issue-A intentionally does not yet add invasive typed room-send telemetry. If reflection is exonerated, add compile-checked room/send snapshot accessors for `r0..r3` and `h0..h3` next.

## Exact Issue-A test matrix

### Test A — standalone speaker

Goal: learn whether reflected-position redirection itself audibly creates the reported coloration with only one source.

1. Use one non-synchronized speaker in geometry where a dump shows `requestedRedirect=true` and a meaningful non-zero offset.
2. Listen with redirect ON.
3. Run `/cchqphysics dump`.
4. Without moving, set global redirect OFF.
5. Give the stabilizer time to return toward the real position.
6. Listen again and dump again.
7. Restore global redirect ON.

Interpretation:

- strong repeatable change on one source → reflected positioning itself is audibly coloring the sound;
- little/no change → multi-source correlation becomes more likely.

### Test B — synchronized group, global A/B

This is a coarse sanity check only.

1. Start the known 4-speaker synchronized scene.
2. Reproduce the coloration with global redirect ON.
3. Dump.
4. Toggle global redirect OFF without moving.
5. Listen and dump.
6. Restore ON.

Do **not** conclude "correlated copies are the cause" from a positive Test B alone, because every source changed simultaneously.

### Test C — synchronized group, one-source isolation — PRIMARY DECIDING TEST

1. With the group playing and global redirect ON, run `/cchqphysics dump`.
2. Identify a source with `requestedRedirect=true`, `redirectActive=true`, and the largest meaningful offset.
3. Disable reflection only for that source:

`/cchqphysics reflection_redirect source <id> off`

4. Leave every other source on `auto`.
5. Do not move if possible.
6. Listen and dump again.
7. Return that source to `auto` before stopping playback.

Interpretation:

- strong change / symptom disappears → source-specific reflected-position interaction strongly implicated;
- partial change → test complementary combinations across the redirected sources;
- no repeatable change → reflection likely not the main cause; next step is typed room/reverb-send telemetry.

For subtle effects, repeat trials and use a low-cost blind/unknown-state check before making a permanent behavior change.

Detailed source of truth:

`docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md`

Build record:

`docs/PHASE5_ISSUE_A_BUILD_RECORD.md`

---

# 10. Separate elevation / diffraction issue

This is **not Issue A** and must not be folded into the same test build yet.

User scenario:

- speaker only a few blocks away horizontally;
- listener several Y-levels lower in an open-topped hole / terrain depression;
- sound becomes absurdly muffled, as if many solid walls are present;
- directly underneath with only one actual separating block can sound more reasonable.

Logs showed very high direct/progressive occlusion values, roughly into the 7–15 range, pushing direct cutoff extremely low.

Source inspection explains the limitation:

- progressive model uses center + 8 inner + 8 outer probes;
- the probes vary X/Y/Z;
- but every path remains a straight source-to-listener ray;
- therefore a diagonal line through terrain can accumulate many blocks even when a plausible acoustic route exists around the rim.

This is a diffraction/path-around-edge limitation, not simply "Y is ignored."

Future isolated experiment concept:

1. keep the normal direct occlusion unchanged as the primary path;
2. only when direct occlusion is very high and vertical separation is meaningful, probe a small number of alternate two-segment escape paths around/above the obstruction;
3. each segment still uses SPR's real block occlusion raycast;
4. if an alternate route is genuinely much clearer, reduce only the absurd over-occlusion and apply a diffraction penalty;
5. sealed floors/ceilings must remain strongly occluded;
6. do not alter source position or source gain to fake the result;
7. keep feature experimental/default OFF until tested.

Do this only after Issue A is resolved or explicitly parked.

---

# 11. Documentation state

Read these after this handoff:

1. `docs/PHASE5_CURRENT_STATUS_2026-09-05.md`
2. `docs/PHASE5_ISSUE_A_REFLECTION_DIAGNOSTICS.md`
3. `docs/PHASE5_ISSUE_A_BUILD_RECORD.md`
4. `RECONSTRUCTION_STATUS.md`
5. `docs/PHASE5_RUNTIME_VALIDATION.md` if present on the relevant finalization/history branch
6. `docs/BUILD_FROM_SOURCE.md` and `docs/SOURCE_HANDOVER.md` on the finalization/history branch
7. baseline/audit docs for Phase 1–4 when exact parity evidence is needed

Be aware that some historical branches were created from older frozen commits and can contain stale status prose. Prefer this handoff plus the current Issue-A working branch docs for present-tense state.

---

# 12. The final closure work is still required

Once Issue A and the elevation/V2 decisions are made, the project still owes the final closure sequence:

1. write/finalize the Phase-5 runtime-validation report;
2. mark Phase 5 **COMPLETE / RECHECKED** only after evidence supports it;
3. keep Phase-4 Hotfix3 parity refs permanent;
4. freeze the final extended/configurable maintained-source candidate;
5. ensure obsolete mutation/patch scaffolding is cleaned while useful diagnostics remain;
6. finalize reproducible build instructions;
7. document the definitive final JAR/hash and supported stack;
8. finalize source handover/architecture/invariants/known limitations;
9. update README/status so the reconstructed source is clearly the maintainable authority.

Do not perform the final merge to `main` without explicit user approval.

---

# 13. User preference / working style

The user is intentionally cautious about a large reconstruction task and does not want a fast, hand-wavy "looks right" conclusion.

Preferred approach:

- preserve known-good checkpoints before experiments;
- make experiments reversible through Git branches;
- verify builds independently;
- keep docs current so another chat can recover context;
- separate subjective audio observations from what logs/source actually prove;
- never claim a runtime result the user did not report;
- prefer a narrow A/B experiment over changing several acoustic systems at once;
- if an experiment fails, preserve it as evidence and revert by branch rather than patching over it blindly.

---

# 14. Immediate handoff sentence

**Current action:** give the user the verified Issue-A JAR (`SHA-256 d649f14c...c8451`), have them perform Tests A/B/C from the Issue-A matrix, then inspect the returned `latest.log` and subjective result. Do not start the elevation/diffraction feature and do not promote V2 until Issue A has a conclusion.
