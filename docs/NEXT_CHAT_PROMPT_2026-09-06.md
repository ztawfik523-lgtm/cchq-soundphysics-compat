# Prompt for the next ChatGPT session

Continue my CC:HQ Sound Physics Compat project from the existing repository state:

`https://github.com/ztawfik523-lgtm/cchq-soundphysics-compat`

Use branch:

`phase5-v7-1-lifecycle-state-finish`

Before doing implementation work, read these files in this order:

1. `docs/NEXT_CHAT_HANDOFF_2026-09-06.md`
2. `RECONSTRUCTION_STATUS.md`
3. `README.md`
4. `docs/BETA11_RECONSTRUCTION_HANDOFF.md`

The detailed handoff is intentionally concrete and contains the exact branches, commits, JAR hashes, benchmark numbers, accepted/rejected diffraction iterations, performance work, lifecycle fixes and the pending runtime test.

Important orientation:

- Beta11 Hotfix3 reconstruction and the Phase 4 equivalence audit are complete.
- V7.1 is the frozen stable acoustic reference. Its commit is `ffcf5f6e05d85b69f1f1dff8cfae1b082b71604d`.
- The approved HF50 synchronized-speaker behavior is part of the stable sound result.
- The performance pass is `962eab8b052466ca984496a7dec0767dc65803f4`.
- The clean lifecycle source checkpoint before documentation-only commits is `be03d30efe98ca03bdf27764bcea567df5ef3875`.
- The current lifecycle candidate JAR SHA-256 is `6d0fa98ee6c76d23a3e0764501d16dc5c993149e0de77181cdab6fc0a9abdc18`.
- The lifecycle source/CI audit passed; user runtime lifecycle validation is the next unfinished checkpoint.
- The lifecycle artifact still reports the older internal `phase5-v7-1-performance-test` version string, so final release naming cleanup comes later.

From now on, make routine technical/tradeoff choices yourself unless I explicitly change that instruction. Aim for a middle ground: correct, maintainable and reasonably performant without overengineering or taking unnecessary risks.

The project is in finishing mode. Prioritize concrete bugs, lifecycle/state correctness, performance where evidence supports it, runtime validation and release cleanup. Treat V7.1 as the stable comparison point when evaluating later changes.

When I upload `latest.log` and `debug.log`, reconstruct the full relevant runtime sequence before concluding anything. The assistant cannot hear the game, so keep subjective listening results separate from source/log evidence.

If I have not yet run the lifecycle candidate, give me the exact lifecycle test sequence from the handoff. If I upload the logs, analyze them first and decide the next step from the evidence. If lifecycle validation is clean, continue into release cleanup and the final audit.
