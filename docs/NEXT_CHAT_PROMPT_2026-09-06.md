# Prompt for the next ChatGPT session

Continue my CC:HQ Sound Physics Compat project from the existing repository state:

`https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Use branch:

`phase5-v7-1-lifecycle-state-finish`

Before doing implementation work, read these files in this order:

1. `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`
2. `docs/LIFECYCLE_SOURCE_AUDIT_2026-09-06.md`
3. `docs/RELEASE_CLEANUP_AUDIT_2026-09-06.md`
4. `docs/RELEASE_POLICY_LOCK_2026-09-06.md`
5. `docs/FINAL_RELEASE_EXECUTION_PLAN_2026-09-06.md`
6. `RECONSTRUCTION_STATUS.md`
7. `README.md`
8. `docs/BETA11_RECONSTRUCTION_HANDOFF.md`

The first handoff contains the detailed project history. The lifecycle source-audit continuation supersedes it for the current lifecycle source checkpoint/fixes/candidate. The release-policy lock and final-release execution plan are newer still and are authoritative for what to do after lifecycle runtime acceptance.

Important orientation:

- Beta11 Hotfix3 reconstruction and the Phase 4 equivalence audit are complete.
- V7.1 is the frozen stable acoustic reference. Its commit is `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`.
- The approved HF50 synchronized-speaker behavior is part of the stable sound result.
- The clean performance pass is `962eab8b052466ca984496a7dec0767dc65803f4`.
- A source-only lifecycle audit fixed source-0 registration after failed allocation, teardown steps being skipped after one cleanup failure, and a pause/resume bug where pending synchronized `AL_INITIAL` sources could be mistaken for paused sources and started early.
- The clean audited production-source checkpoint is `2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`.
- Successful audit CI run: `33996243988`; job `101387190106`; artifact `9978159512`.
- Current audited lifecycle candidate JAR SHA-256: `4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`.
- The old lifecycle candidate `6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18` is superseded.
- CI proved the frozen acoustic/config algorithm files and Lua remained unchanged from the intended baseline; class count remains 81.
- User runtime lifecycle validation is still pending because I chose to postpone it. Do not call the audited candidate runtime-approved yet and do not pressure me to test immediately.

Locked final-release policy after lifecycle runtime approval:

- final private-mod version: `0.1.0`;
- centralize version identity so `mod_version` is the source of truth and Gradle/NeoForge/runtime reporting agree;
- V7.1 diffraction ships **ON by default**;
- clean break from the old experimental diffraction config with **no migration**;
- final diffraction config filename: `cchq_soundphysics_compat-diffraction.toml`;
- final diffraction root key: `portal_diffraction`;
- old `cchq_soundphysics_compat-diffraction-v7-1-spreading-only-test.toml` / `portal_diffraction_v7_1_spreading_only_test` are intentionally ignored by the final release;
- clean user-facing Beta/Phase/test/candidate terminology while preserving persisted internal keys/log continuity where useful;
- remove the stale static manifest `Created-By: 21.0.11 (Debian)` line;
- preserve approved HF50 values, frozen V7.1 equations/parameters, the deliberate bitwise eligibility `&`, and lifecycle fixes.

CRITICAL two-stage runtime rule:

1. First runtime-test the audited lifecycle candidate `4b3c8c52...` using the full lifecycle sequence.
2. Only after that passes, apply the final release transformation above and rebuild/CI it.
3. The final cleaned build then **must receive a short final smoke test** before release freeze because its fresh-install behavior differs from the lifecycle candidate: diffraction becomes ON by default and the old diffraction config identity is discarded.
4. Do not mistake a clean lifecycle-candidate test for validation of the final release config/default behavior.
5. Do not redo the full historical acoustic campaign unless the short final smoke test reveals a regression. Use the targeted smoke sequence in `docs/RELEASE_POLICY_LOCK_2026-09-06.md`.

From now on, make routine technical/tradeoff choices yourself unless I explicitly change that instruction. Aim for a middle ground: correct, maintainable and reasonably performant without overengineering or unnecessary risk.

The project is in finishing mode. Prioritize concrete bugs, lifecycle/state correctness, evidence-based performance work, runtime validation and release cleanup. Treat V7.1 as the stable comparison point when evaluating later changes.

When I upload `latest.log` and `debug.log`, reconstruct the full relevant runtime sequence before concluding anything. The assistant cannot hear the game, so keep subjective listening results separate from source/log evidence.

If I have not yet run the audited lifecycle candidate, I may choose to keep postponing the runtime test. Continue only with work that does not invalidate the candidate/source gate. If I ask to test or upload logs, use the lifecycle sequence from the main handoff but use the `4b3c8c52...` candidate. If lifecycle validation is clean, execute `docs/FINAL_RELEASE_EXECUTION_PLAN_2026-09-06.md`, run final CI, then require the short Gate-2 smoke test before freezing the release.
