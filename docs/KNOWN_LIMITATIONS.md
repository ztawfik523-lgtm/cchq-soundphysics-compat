# Known Limitations and Validation Scope

This file documents the current **0.1.0-beta11-rc1** limitations without changing the approved acoustic behavior to chase edge cases late in the release cycle.

## Opening discovery has a finite radius

Opening-aware vertical sound currently searches up to **8 blocks** around the listener for a usable ceiling opening.

The contribution fades near the edge of that radius, but the discovery area is still finite. In some geometry, moving from a position where an opening is barely inside the search area to one where it is outside can still produce a noticeable transition.

This is intentionally left unchanged for RC1 rather than increasing scan cost or introducing another acoustic model change.

## Opening-aware sound is primarily a vertical/elevation correction

By default, the source must be at least **0.25 blocks above the listener** before the opening model is considered.

The feature is designed for cases such as:

- speaker above a tunnel
- lower floor beneath an opening
- shaft/open-top geometry
- terrain where a plausible sound route exists around a ceiling edge/opening

It is not intended to be a general-purpose same-height portal/wave-propagation solver.

## It is an approximation, not a full wave simulation

The opening model uses bounded geometric path checks, obstruction, path-length difference and two-band energy combination. It does not simulate phase-accurate wave diffraction or create delayed secondary playback sources.

That limitation is deliberate: source timing, synchronized starts and OpenAL playback position remain unchanged.

## Final stress validation currently covers 1 and 4 active sources

The performance benchmark completed for the approved behavior included controlled **1-source** and **4-source** cases, stationary and moving.

The portal/opening portion remained a small fraction of the measured acoustic work, and listener-side opening topology scanning stayed shared rather than multiplying per speaker.

A final **12-source stress run has not yet been completed** for RC1. Do not claim 12-source performance as runtime-validated until that test is actually performed.

## Cleanup/RC1 has been source- and build-validated, not yet re-listened

The release cleanup changes configuration names/descriptions, diagnostics, documentation and other release-facing text. The cleanup pass is guarded so the approved opening formulas and critical eligibility logic remain unchanged, and clean Java 21 builds are required.

However, no new listening claim should be made for the final cleanup JAR until it is launched and tested in Minecraft again.

## Config migration is intentionally not preserved

RC1 replaces experimental config filenames/section names with release-facing ones. Old experimental values may reset to the tuned release defaults.

This is intentional for the first release candidate. Users upgrading from internal/test builds should review the new config files rather than expecting old experimental keys to migrate.

## Diagnostic command name `diffraction` is retained

The user-facing feature is now described as **Openings & Vertical Sound** / **opening-aware sound**, but the client command:

`/cchqphysics diffraction ...`

is retained for diagnostic compatibility with existing test instructions and logs.

## Historical internal names remain in source

Some internal classes/methods still carry development-era names such as `Beta9Optimizer`, `Beta10Optimizer` and `Beta11RoomRayCache`.

They are intentionally not renamed during release cleanup because those names are woven through mixins, reflection handles and audited call paths. They do not represent separate user-selectable beta modes, and leaving them in place avoids a cosmetic refactor that could introduce runtime risk.
