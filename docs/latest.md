# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 14:09
**Request:** Homelander: drag at walk speed, AI uses all attacks; then remove the drag (Smash) attack completely; commit + push + release everything.

**Done:**
- Took over the unfinished AI work of session "homelander" (user's choice): random pool of all ready specials, max 2 punches in a row.
- Smash removed from viewer + mod (anim, button, key 0, victim pose, config `groundSmash`, events, data, test scenes, docs).
- Tests: viewer cliptest ok, `gradlew build` 8 tests green, in-game `runHomelanderTest`: free fights use clap, laser, throw, punch; no errors.
- Rule: release after every in-game test (`CLAUDE.md`).
- Committed everything not git-ignored (also other mobs and references).

**Files:** `mobs/homelander/homelander.html`, `mod/src/main/java/com/custommobs/**`, assets, test datapack, `CLAUDE.md`, `README.md`, docs, + other people's untracked mobs/references.

**Next:** release v0.0.2-alpha is published by `tools\release.ps1 -Bump`.
