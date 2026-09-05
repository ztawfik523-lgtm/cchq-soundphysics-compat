# Phase 5 synchronized HF50 configurable candidate

## Status

- Exact user-approved automatic HF50 runtime remains frozen at `c1eff512194e5d9893227c0983afbf8c5510348a` on `phase5-sync-hf50-test-candidate`.
- User verdict for that automatic candidate on 2026-09-05: **sounds good**.
- Runtime dump confirmed the automatic candidate behaved as designed in a four-source synchronized group:
  - source 1 intrinsic cutoff ~0.949 -> unchanged
  - source 2 intrinsic cutoff ~0.101 -> adjusted ~0.550
  - source 3 intrinsic cutoff ~0.470 -> unchanged by the 0.35 dark-source gate
  - source 4 intrinsic cutoff 1.000 -> unchanged
- The configurable follow-up is isolated on `phase5-sync-hf50-configurable-candidate`.
- Configurable build source commit: `62d3a7a0a176c901402b913946d98f3cb455a8f4`.
- Configurable JAR SHA-256: `fe894a42eebeea37e77f63e9acf65df22bdac72897fb6ac1eb9def198dcd032a`.
- Configurable build verification passed: wrapper validation, frozen-ref checks, change-scope checks, HF50 algorithm/default checks, reconstruction classpath, resource wiring, clean Java compile, JAR build, artifact inspection, upload.
- Configurable JAR runtime/listening test is **not yet separately performed**. Do not promote it above the exact approved runtime until that quick sanity check is done.

## Configurable HF50 controls

Defaults reproduce the already approved HF50 behavior exactly:

- `enabled`: true
- `dark_source_cutoff`: 0.35
- `peer_clear_cutoff`: 0.75
- `min_peer_gap`: 0.40
- `clarity_floor_ratio` / UI `HF lift strength`: 0.50 / 50%
- `max_cutoff_lift`: 0.55

The Cloth Config category is now **Synchronized HF Balance** and exposes:

1. Enable synchronized HF balance
2. Dark-source cutoff gate
3. Clear-peer cutoff gate
4. Minimum peer cutoff gap
5. HF lift strength (0-100%, default 50%)
6. Maximum cutoff lift

The correction still changes only the final direct low-pass cutoff of eligible synchronized copies. It does not change source gain, source position, reverb sends, PCM/playback timing, synchronized-start behavior, reflection behavior, or the progressive occlusion model.

## Issue-A conclusion

Current evidence supports the direct-HF spectral disparity hypothesis as the primary cause of the reproduced painful skew/doubled/phasey perception:

- persistent OpenAL cursor micro-desync was not observed in the diagnostic snapshots;
- disabling reverb sends did not change the problem;
- reflection redirection was inactive in the reproduced synchronized snapshot and improved localization in standalone testing;
- direct-HF bypass reduced the bad skew but degraded spatial quality;
- 25/50/75/bypass dose testing selected **50%** as the best balance;
- the guarded automatic HF50 candidate then sounded good and selectively corrected only the severe synchronized outlier in the dump.

## Next isolated problem

After one quick configurable-build sanity check, preserve Issue A and move to the separate elevation/diffraction over-occlusion case. Do not mix that work back into HF50.
