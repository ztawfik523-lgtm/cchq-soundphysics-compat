# Prompt for the next ChatGPT session

Continue my CC:HQ Sound Physics Compat project from the existing repository state:

`https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Use branch:

`phase5-v7-1-lifecycle-state-finish`

Before doing implementation work, read these files in this order:

1. `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`
2. `docs/LIFECYCLE_SOURCE_AUDIT_2026-09-06.md`
3. `RECONSTRUCTION_STATUS.md`
4. `README.md`
5. `docs/BETA11_RECONSTRUCTION_HANDOFF.md`

The first handoff contains the detailed project history. The lifecycle source-audit continuation is newer and supersedes it for the current lifecycle source checkpoint, fixes, CI run and candidate JAR hash.

Important orientation:

- Beta11 Hotfix3 reconstruction and the Phase 4 equivalence audit are complete.
- V7.1 is the frozen stable acoustic reference. Its commit is `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`.
- The approved HF50 synchronized-speaker behavior is part of the stable sound result.
- The clean performance pass is `962eab8b052466ca984496a7dec0767dc65803f4`.
- The earlier lifecycle source checkpoint was `be03d30efe98ca03bdf27764bcea567df5ef3875`.
- A later source-only lifecycle audit found and fixed: OpenAL source-0 registration after failed allocation, teardown steps being skipped after one cleanup failure, and a pause/resume bug where pending synchronized `AL_INITIAL` sources could be mistaken for paused sources and started early.
- The new clean audited production-source checkpoint is `2a6a2f4ecd9e2faad51de9818797f5a16c14b0f7`.
- Successful audit CI run: `33996243988`; job `101387190106`; artifact `9978159512`.
- Current audited lifecycle candidate JAR SHA-256: `4b3c8c52cc00a37274d5829cff93933d6e548b733b83918fcc5570ab8d6ad3c5`.
- The old lifecycle candidate `6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18` is superseded.
- CI proved the frozen acoustic/config files and Lua remained unchanged from the intended baseline; class count remains 81.
- User runtime lifecycle validation is still pending because I chose to postpone it for now. Do not call the audited candidate runtime-approved yet.
- Release-facing naming still contains Phase 5/test terminology. Final naming/presentation cleanup comes after the runtime gate unless I explicitly decide otherwise.

From now on, make routine technical/tradeoff choices yourself unless I explicitly change that instruction. Aim for a middle ground: correct, maintainable and reasonably performant without overengineering or taking unnecessary risks.

The project is in finishing mode. Prioritize concrete bugs, lifecycle/state correctness, performance where evidence supports it, runtime validation and release cleanup. Treat V7.1 as the stable comparison point when evaluating later changes.

When I upload `latest.log` and `debug.log`, reconstruct the full relevant runtime sequence before concluding anything. The assistant cannot hear the game, so keep subjective listening results separate from source/log evidence.

If I have not yet run the audited lifecycle candidate, I may choose to keep postponing the runtime test. Do not pressure me to test immediately. If I ask to test or upload logs, use the lifecycle sequence from the main handoff but use the new `4b3c8c52...` candidate. If lifecycle validation is clean, continue into release cleanup and the final audit rather than reopening earlier reconstruction/acoustic experimentation.
