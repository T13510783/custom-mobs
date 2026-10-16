# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 14:38
**Request:** Patrick Bateman viewer: check references, new UI, expressive face, Idle / Idle 2 / Walk (no attack, no special); keep one file; reorganise the repo; commit, push, release. Follow-up: fix the weird mouth, arms higher, check all faces.

**Done:**
- `mobs/patrick_bateman/patrick_bateman.html` (one file): shared UI + expression menu, 2-bone rig, moving face (lids, glances, brows, smile, jaw, toothy grin), new Idle / Idle 2 (hair slick, watch check, grin) / Walk, springs (fringe, tie, coat).
- Follow-up: mouth is one connected pixel shape (stepped corners, wider grin, teeth); arms flush with the torso top; head lift 1.0.
- Tests: `#cliptest` 0 clips in all 5 outfits (7 mouse spots), feet on the floor; renders of every animation + expression + outfit face checked.
- Cleanup: `*-old.html` pages -> `archive/<mob>/<mob>_old.html`; hash-named references renamed by content; README mobs table complete.
- No new release: v0.0.3-alpha (updater session) is the latest; this work is viewer-only (the jar does not change).

**Files:** `mobs/patrick_bateman/patrick_bateman.html`, `archive/*`, `references/*`, `README.md`, docs.

**Next:** Bateman is HTML only; `.bbmodel` + mod port when the user confirms him as final.
