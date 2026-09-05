# Configuration Guide

This guide describes the **release-facing settings** for CC:HQ Sound Physics Compat in plain language.

The defaults are the tuned release values. Most users should only need the first few categories. Settings under **Advanced / Troubleshooting** are mainly for diagnosis and performance tuning.

## Config files

- `cchq_soundphysics_compat-client.toml` — normal acoustic, smoothing and performance controls
- `cchq_soundphysics_compat-advanced.toml` — scheduler/cache/filter and diagnostic controls
- `cchq_soundphysics_compat-sync-balance.toml` — synchronized-copy clarity balance
- `cchq_soundphysics_compat-openings.toml` — opening-aware vertical sound

The RC1 schema intentionally uses release-facing names. Older experimental config files/keys are not migrated automatically.

---

## General

### Enable compatibility

**Default: ON**

Controls whether newly-started CC:HQ whole-file playback is intercepted by the compat and processed as positional Sound Physics audio.

- **ON:** CC:HQ playback uses the compatibility layer.
- **OFF:** newly-started sounds are left to CC:HQ normally.

Turning this off does not forcibly restart an already-playing compat source.

---

## Distance & Range

### Scale range above volume 1

**Default: ON**

For CC:HQ volume values above 1, uses the extra level to extend audible distance instead of making close-range gain exceed normal full volume.

- **ON:** extra volume mainly extends range.
- **OFF:** follows the more direct volume behavior.

Recommended: **ON**.

### Audible range multiplier

**Default: 1.0**

Multiplies the final audible distance.

- **Higher:** speakers can be heard farther away.
- **Lower:** speakers stop being audible sooner.
- **1.0:** tuned default distance behavior.

This changes audible reach, not the shape of wall obstruction.

---

## Occlusion & Muffling

### Progressive occlusion

**Default: ON**

Enables the 17-path wall-obstruction model for compat speakers.

- **ON:** wall thickness and nearby geometry progressively affect clarity/volume.
- **OFF:** disables this compat-specific progressive obstruction layer.

Recommended: **ON**.

### Muffling strength

**Default: 0.35**

Controls how strongly obstruction removes high frequencies.

- **Higher:** darker/more muffled behind walls.
- **Lower:** clearer through the same obstruction.
- **0:** removes the progressive high-frequency muffling contribution.

### Occluded volume loss

**Default: 0.50**

Controls how strongly obstruction reduces direct volume.

- **Higher:** blocked sound becomes quieter.
- **Lower:** less volume loss through obstruction.
- **0:** no progressive direct-volume attenuation from this model.

### Advanced probe model

These settings change how the 17 direct paths sample nearby geometry. Leave them at defaults unless deliberately tuning the acoustic model.

#### Inner probe offset

**Default: 0.20 blocks**

Distance of the inner 8-path ring from the exact center path.

- **Higher:** samples a wider nearby area.
- **Lower:** keeps the inner paths closer to the exact path.

#### Outer probe offset

**Default: 0.49 blocks**

Distance of the outer 8-path ring from the exact center path.

- **Higher:** samples farther around the direct route.
- **Lower:** keeps the outer ring tighter.

#### Center-path weight

**Default: 4.0**

Importance of the exact speaker-to-listener path in the combined result.

- **Higher:** the exact center path dominates more strongly.
- **Lower:** surrounding paths matter more relative to the center.

#### Inner-ring weight

**Default: 1.0**

Importance of the inner 8 paths.

- **Higher:** nearby surrounding geometry affects muffling more strongly.
- **Lower:** inner-ring geometry matters less.

#### Outer-ring weight

**Default: 0.5**

Importance of the outer 8 paths.

- **Higher:** wider surrounding geometry affects muffling more strongly.
- **Lower:** outer-ring geometry matters less.

#### Open-path ring influence

**Default: 20%**

Controls how much blocked surrounding geometry can matter while the exact center path is clear.

- **Higher:** nearby walls can still influence a clear center path more strongly.
- **Lower:** a clear center path dominates more completely.
- **0%:** a fully clear center path ignores ring obstruction at that point.

---

## Direction & Reflections

### Stabilize reflected direction

**Default: ON**

Smooths Sound Physics' reflected virtual-source position for long-running CC:HQ speakers.

- **ON:** less rapid left/right jumping while still allowing reflected direction.
- **OFF:** uses the reflected positioning without the compat stabilizer.

Recommended: **ON**.

### Reflection occlusion threshold

**Default: 0.45**

Controls how blocked the direct path must be before reflected positioning is allowed to move the apparent source.

- **Higher:** sound stays anchored to the real speaker until more strongly obstructed.
- **Lower:** reflected direction can activate with lighter obstruction.

### Reflection displacement strength

**Default: 35%**

Controls how much of Sound Physics' reflected position shift is retained.

- **Higher:** stronger apparent bending toward the reflection route.
- **Lower:** stays closer to the physical speaker.
- **0%:** no reflected displacement is retained.

### Maximum reflection offset

**Default: 2.5 blocks**

Maximum distance the stabilized virtual source may move away from the real speaker.

- **Higher:** allows stronger apparent displacement.
- **Lower:** keeps reflected direction closer to the real block.

### Advanced direction smoothing

#### Reflection follow smoothing

**Default: 22%**

How quickly the virtual source follows a changing reflected target.

- **Higher:** reacts faster.
- **Lower:** smoother/slower movement.

#### Return-to-speaker smoothing

**Default: 28%**

How quickly the virtual position returns to the physical speaker when redirection clears.

- **Higher:** returns faster.
- **Lower:** returns more gradually.

#### Opposite-side transition smoothing

**Default: 35%**

How quickly the stabilizer recentres when the preferred reflected route flips to the opposite side.

- **Higher:** faster transition.
- **Lower:** smoother transition.

---

## Smoothing

These settings control how quickly newly-calculated targets become audible. Higher percentages react faster; lower percentages smooth changes more heavily.

### Muffling response

**Default: 30%**

- **Higher:** new obstruction becomes audible sooner.
- **Lower:** muffling fades in more gradually.

### Unmuffling cutoff response

**Default: 18%**

- **Higher:** high frequencies return faster when a path clears.
- **Lower:** clarity returns more gradually.

### Unmuffling gain response

**Default: 16%**

- **Higher:** direct volume returns faster after clearing.
- **Lower:** volume recovery is smoother/slower.

### Reverb response

**Default: 22%**

- **Higher:** room/reverb sends react faster to changed surroundings.
- **Lower:** reverb transitions more gradually.

---

## Performance

### Adaptive probe cache

**Default: ON**

Keeps the center path fresh while alternating which exact 8-probe ring is recalculated. Meaningful movement or center-path changes force all 17 paths fresh again.

- **ON:** lower ray cost in stable situations.
- **OFF:** all 17 probes are recalculated on every progressive update.

Recommended: **ON**.

### Full SPR update interval

**Default: 100 ms**

Minimum interval between full Sound Physics environment calculations for each compat speaker.

- **Lower:** more responsive, higher CPU cost.
- **Higher:** lower CPU cost, slower full-environment response.

### Moving occlusion interval

**Default: 100 ms**

Minimum interval between progressive wall-obstruction calculations while moving.

- **Lower:** obstruction follows movement more frequently, higher CPU.
- **Higher:** cheaper, but movement response updates less often.

### Stationary occlusion interval

**Default: 200 ms**

Progressive obstruction refresh interval while effectively stationary.

- **Lower:** fresher stationary results, higher CPU.
- **Higher:** cheaper stationary processing.

### Movement threshold

**Default: 0.15 blocks**

Movement required to count as moving for the faster occlusion update rate.

- **Lower:** small movement switches to moving mode sooner.
- **Higher:** remains on the stationary cadence through more small movement.

### Full refresh movement

**Default: 0.50 blocks**

Cumulative listener movement since the last full 17-path refresh that forces both rings fresh.

- **Lower:** more frequent full refreshes, more raycasts.
- **Higher:** more ring reuse while moving.

### Center-change full refresh

**Default: 0.20**

Change in center-path obstruction that immediately forces both rings fresh.

- **Lower:** more sensitive to acoustic changes, more raycasts.
- **Higher:** allows more center-path change before a full refresh.

### Performance diagnostics

**Default: OFF**

Prints compact performance reports while compat speakers are active. This is useful for benchmarking but not needed for normal play.

---

## Openings & Vertical Sound

This system is intended for cases such as a speaker above a tunnel, lower floor or shaft where a real nearby opening should let some sound reach the listener.

The normal direct obstruction result remains in place. A verified opening adds a bounded secondary contribution.

### Enable opening-aware sound

**Default: ON**

- **ON:** nearby real openings can affect blocked vertical sound.
- **OFF:** only the normal direct obstruction result is used.

### Opening effect strength

**Default: 25%**

Overall strength of the opening contribution.

- **Higher:** a usable opening makes blocked sound clearer/louder more strongly.
- **Lower:** subtler opening effect.
- **0%:** detection may still occur, but it adds no opening contribution.

### Opening influence distance

**Default: 3.0**

Controls how quickly an explicit opening's effect weakens as you move away from it inside the enclosed area.

- **Higher:** effect stays noticeable farther away.
- **Lower:** effect fades sooner.

This does not increase the maximum search radius by itself.

### Opening search radius

**Default: 8 blocks**

Maximum listener-side radius used to discover nearby ceiling openings.

- **Higher:** can find farther openings but requires more block checks.
- **Lower:** cheaper scans but ignores openings sooner.

Current hard maximum: **8 blocks**.

### Advanced sound shape

#### Minimum vertical separation

**Default: 0.25 blocks**

How far above the listener the source must be before this model is considered.

- **Higher:** limits the feature to stronger vertical separation.
- **Lower:** allows it in shallower above/below setups.

#### Opening clearance

**Default: 1.5 blocks**

How far above the detected opening/ceiling plane the source-side route point is placed.

- **Higher:** clears thicker roof edges more aggressively.
- **Lower:** route stays closer to the opening plane.

#### Full-effect obstruction

**Default: 2.0**

How blocked the normal direct path must be before the opening contribution reaches full activation.

- **Higher:** requires stronger direct obstruction.
- **Lower:** reaches full effect sooner.

#### Bass carry around openings

**Default: 4.0**

How well low frequencies survive an indirect detour through/around an opening.

- **Higher:** more bass survives longer detours.
- **Lower:** indirect bass falls off more strongly.

#### Clarity carry around openings

**Default: 1.5**

How well high frequencies survive an indirect detour.

- **Higher:** brighter/clearer sound through longer indirect routes.
- **Lower:** highs fall off more strongly and the indirect path sounds darker.

#### Search-edge fade start

**Default: 75% of search radius**

Where the contribution starts fading near the outer edge of the finite search area.

- **Higher:** stays stronger until closer to the edge.
- **Lower:** begins fading earlier.

#### Opening separation

**Default: 2.0 blocks**

Minimum spacing between the two candidates that may receive the expensive full path check.

- **Higher:** avoids spending both verification slots on adjacent cells of one hole.
- **Lower:** allows nearby opening cells to occupy both slots.

#### Opening switching stability

**Default: 0.35**

How much better another opening must become before the selected route switches to it.

- **Higher:** steadier selection, less switching.
- **Lower:** responds sooner to small route advantages.

### Opening performance

#### Opening scan interval

**Default: 1000 ms**

Minimum interval between topology scans while the listener remains in the same block cell.

- **Lower:** notices block changes sooner, higher CPU.
- **Higher:** fewer scans, slower response to changed geometry.

#### Movement recheck distance

**Default: 0.75 blocks**

How far the listener must move before a cached listener-to-opening acoustic leg is checked again.

- **Lower:** more checks, higher CPU.
- **Higher:** more reuse while moving.

#### Opening path cache time

**Default: 5000 ms**

Maximum age of a verified opening path while endpoints remain stable.

- **Higher:** fewer extra Sound Physics path checks.
- **Lower:** geometry is rechecked more frequently.

---

## Synchronized Sound Balance

This system only activates when synchronized copies have a large direct-clarity mismatch. It does not alter source gain, position, start timing or reverb sends.

### Enable synchronized clarity balance

**Default: ON**

Recommended: **ON**.

### How muffled a copy must be

**Default: 0.35**

The direct cutoff uses a 0-to-1 scale: 0 is extremely muffled, 1 is clear.

Only copies at or below this threshold can receive correction.

- **Higher:** more synchronized copies can qualify.
- **Lower:** only more severely muffled copies qualify.

### How clear another copy must be

**Default: 0.75**

At least one synchronized peer must be this clear to act as the comparison/reference.

- **Higher:** requires a clearer peer.
- **Lower:** correction can activate with a less-clear peer.

### Minimum clarity difference

**Default: 0.40**

Minimum cutoff gap between the muffled copy and the clearest synchronized peer.

- **Higher:** only larger mismatches are corrected.
- **Lower:** smaller differences can be corrected.

### Clarity correction strength

**Default: 50%**

How far an eligible muffled copy moves toward the clear peer.

- **0%:** no correction.
- **50%:** halfway toward the peer before the safety cap.
- **100%:** attempts to match the peer before the safety cap.

### Maximum clarity increase

**Default: 0.55**

Absolute cap on how much one source's direct cutoff may be raised.

- **Higher:** permits a larger correction.
- **Lower:** limits correction more aggressively.

---

## Advanced / Troubleshooting

Most users should leave these settings at their defaults.

### Private per-source filters

**Default: ON**

Uses compat-owned isolated OpenAL filters per source.

- **ON:** normal/recommended behavior; prevents cross-source filter contamination.
- **OFF:** diagnostic fallback to native Sound Physics environment writes.

### Reuse unchanged direct acoustics

**Default: ON**

Reuses an exact matching direct acoustic result instead of recomputing it.

- **ON:** saves CPU when inputs are unchanged.
- **OFF:** forces more direct recomputation for diagnosis.

### Slow room updates when stable

**Default: ON**

Lets stable/less-relevant room/reverb updates run less often while stale-state and movement rules still force freshness.

- **ON:** saves CPU.
- **OFF:** stays closer to the base room-update cadence.

### Use load-aware room scheduling

**Default: ON**

Allows current acoustic load to contribute to safe room-update slowdown.

- **ON:** reduces pressure/spikes under load.
- **OFF:** keeps only the non-load-based scheduling behavior.

### Reuse unchanged occlusion rays

**Default: ON**

Reuses exact matching direct/Sound Physics obstruction ray results.

- **ON:** saves repeated raycasts when inputs are identical.
- **OFF:** useful only for diagnosis/reference testing.

### Reuse room rays within one calculation

**Default: ON**

Reuses identical environment/bounce raycasts within the same Sound Physics world snapshot.

- **ON:** saves repeated room raycasts.
- **OFF:** disables that memoization for diagnosis.

### Room scheduler

- **Scheduler slot — 50 ms:** base spacing between room scheduling opportunities. Lower = more responsive/more CPU.
- **Minimum hard stale — 500 ms:** earliest age at which an old room result can be forced fresh. Lower = fresher/more CPU.
- **Maximum hard stale — 2000 ms:** maximum allowed stale age after fairness scaling. Lower = fresher/more CPU.
- **Recent-source window — 1000 ms:** how long a recently seen source stays scheduler-eligible. Higher = tracked longer.
- **Teleport distance — 4 blocks:** listener jump that forces room refreshes. Lower = smaller jumps count as teleports.
- **Urgent source movement — 0.1 blocks:** source movement that marks room state urgent. Lower = more sensitive.

### Stable-room slowdown

- **Recent movement window — 400 ms:** time after movement during which slowdown is suppressed. Higher = stays responsive longer.
- **Listener movement reset — 0.05 blocks:** movement that resets stable-room tracking. Lower = more sensitive.
- **Maximum room backoff — 2.0×:** largest combined slowdown multiplier.
- **Maximum room interval — 1500 ms:** absolute maximum backed-off room interval.

### Clearing detection

These thresholds help detect blocked-to-open transitions quickly instead of waiting only for normal room scheduling. Defaults are tuned; change them only for diagnosis.

- **Movement trigger — 0.05 blocks:** movement before the fast-clear detector samples again.
- **Raw occluded threshold — 0.075:** obstruction level required to arm clearing detection.
- **Re-arm center threshold — 0.12:** center obstruction required to re-arm after a trigger.
- **Open center threshold — 0.035:** center value treated as effectively open.
- **Center drop trigger — 0.15:** center-path improvement required for a clearing candidate.
- **Confirm raw drop — 0.035:** overall obstruction improvement required for confirmation.
- **Confirm cutoff rise — 0.055:** clarity improvement required for confirmation.
- **Clear-trigger cooldown — 300 ms:** minimum time between confirmed fast-clear triggers.

### Synchronized starts

- **Partial group flush — 100 ms:** how long an incomplete synchronized group waits for missing members before starting anyway. Higher = waits longer.
- **Stale group cleanup — 5000 ms:** age at which an abandoned pending group is discarded.

---

## Debug & Validation

All targeted logs are OFF by default.

Available toggles cover:

- source lifecycle
- room scheduler decisions
- clearing detection
- private filter lifecycle
- cache scope resets
- synchronized grouping
- transition timing
- startup config summary

Performance-report cadence is configurable separately.

For one-off inspection, `/cchqphysics dump` is usually more useful than enabling every diagnostic category.
