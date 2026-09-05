# Phase 5 synchronized HF50 configurable candidate

This checkpoint exists to preserve the already user-approved automatic HF50 runtime behavior while exposing its gates and lift strength as explicit configuration.

Baseline runtime candidate: `c1eff512194e5d9893227c0983afbf8c5510348a` on `phase5-sync-hf50-test-candidate`.

The configurable follow-up must not change source gain, source position, reverb sends, playback timing, sync-start behavior, reflection behavior, or the progressive occlusion model. Defaults must reproduce the approved HF50 behavior exactly:

- enabled: true
- dark source cutoff gate: 0.35
- clear peer cutoff gate: 0.75
- minimum peer gap: 0.40
- peer blend / lift strength: 0.50
- maximum cutoff lift: 0.55

The old V2 Cloth Config labels/defaults are stale and must be replaced by HF50-specific labels/defaults.
