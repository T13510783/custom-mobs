# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 12:13
**Request:** rename the mod to Sindicate, config screen, Homelander stats + neutral, flag egg, then cape/clap/voice/run/smash/hybrid fixes, clean all branches, release.

**Done (all 4 steps):**
- Steps 1-2 (earlier): Sindicate rename, config screen, stats, flag egg; viewer: cape, clap, voice, neck, run, smash, walk + attack.
- Mod port: run (~10-11 blocks/s measured), smash (grab, drag, ~12-block fling), clap victims lie down with hands on ears, walk while attacking.
- Fixed in game: victims stuck on his fist after throw/burn, chase step shoving victims away, lying victims walking off.
- Cape: no flat shelf when one arm swings back (hands block only with both hands behind the back); viewer + mod, trace test green.
- In-game test (`gradlew runHomelanderTest`) passes all scenes; `gradlew build` 8 tests green. Player as smash victim: fling resend added, not seen in a test yet.

**Files:** `mod/src/main/java/com/custommobs/homelander/**`, `mod/src/main/java/com/custommobs/mixin/`, `sindicate.mixins.json`, `neoforge.mods.toml`, `sounds.json`, test datapack, docs.

- Step 4: PR #25 merged into main; PRs #16/#18 closed (already in main via #24); all other branches deleted; release v0.0.1-alpha prepared (`mod/release_notes.md`, `tools/release.ps1` fixed for PowerShell 5.1).

**Next:** user checks in game (see handover); next releases with `tools\release.ps1 -Bump`.
