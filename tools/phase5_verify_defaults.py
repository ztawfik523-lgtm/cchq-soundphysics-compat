from pathlib import Path
import re

root = Path(__file__).resolve().parents[1]
source = (root / "src/main/java/dev/cchqphysics/compat/config/ExtendedClientConfig.java").read_text(encoding="utf-8")

# These are the exact Hotfix3 values exposed during Phase 5. The extended build
# may change behavior only when the user deliberately moves away from them.
expected = {
    "room_slot_ms": "50",
    "min_hard_stale_ms": "500",
    "max_hard_stale_ms": "2000",
    "recent_source_ms": "1000",
    "teleport_distance": "4.0D",
    "source_move_urgent_distance": "0.10D",
    "move_distance": "0.05D",
    "raw_occluded": "0.075D",
    "rearm_center": "0.12D",
    "open_center": "0.035D",
    "center_drop": "0.15D",
    "confirm_raw_drop": "0.035D",
    "confirm_cutoff_rise": "0.055D",
    "clear_trigger_cooldown_ms": "300",
    "partial_flush_ms": "100",
    "stale_group_ms": "5000",
    "private_efx": "true",
    "beta9_direct_reuse": "true",
    "beta9_room_backoff": "true",
    "beta9_adaptive_controller": "true",
    "beta9_recent_movement_ms": "400",
    "beta9_listener_move_distance": "0.05D",
    "beta9_max_room_factor": "2.0D",
    "beta9_max_room_interval_ms": "1500",
    "beta10_ray_cache": "true",
    "beta11_room_ray_memo": "true",
    "performance_report_ms": "10000",
    "source_lifecycle": "false",
    "room_scheduler": "false",
    "sentinel": "false",
    "efx": "false",
    "cache": "false",
    "sync": "false",
    "transitions": "false",
    "config": "false",
}

failures = []
for key, value in expected.items():
    # Accept both define(key, value) and defineInRange(key, value, ...), with
    # normal Java whitespace/newlines between tokens.
    pattern = rf'\"{re.escape(key)}\"\s*,\s*{re.escape(value)}(?:\s*[,\)])'
    if not re.search(pattern, source):
        failures.append(f"{key} expected default {value}")

# Runtime normalization must remain present for the order-sensitive pairs.
required_snippets = [
    "return Math.min(a, b) * 1_000_000L;",
    "return Math.max(a, b) * 1_000_000L;",
    "return Math.max(partial, stale);",
]
for snippet in required_snippets:
    if snippet not in source:
        failures.append(f"missing safety normalization: {snippet}")

# Lightweight markers complement the workflow's exact git diff of ClientConfig.
main_config = (root / "src/main/java/dev/cchqphysics/compat/config/ClientConfig.java").read_text(encoding="utf-8")
for marker in [
    'define("enabled", true)',
    'define("progressive", true)',
    'defineInRange("audible_range_multiplier", 1.0D',
]:
    if marker not in main_config:
        failures.append(f"primary config parity marker missing: {marker}")

if failures:
    print("Phase 5 parity-default audit: FAIL")
    for failure in failures:
        print(" -", failure)
    raise SystemExit(1)

print(f"Phase 5 parity-default audit: PASS ({len(expected)} advanced/debug defaults checked)")
