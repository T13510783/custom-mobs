# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 19:52
**Request:** Improve and add facial expressions and better animations for the Joker; keep Special as Dance (same dance, higher quality); model/face only small changes.

**Done:**
- `mobs/the_joker/the_joker.html` rebuilt on the current viewer system (shared UI, expression menu + Grin/Laugh/Smug/Manic, follow mouse, bones, test modes), same cubes and paint.
- Moving face (eyes, blue lids, brows, standard mouth + teeth), 2-bone limbs, hair/coat springs.
- Animations: Idle, Idle 2 (laughing fit), Walk (strut), Attack (revolver), Dance (old Special, smoother).
- Checked: `#cliptest` max 0.36 texel (was 1.9), feet on the floor, per-frame step probe, renders of every expression and animation.

**Files:** `mobs/the_joker/the_joker.html`, `references/the_joker/*` (renamed), docs.

**Next:** Joker is not in the mod yet (viewer only, no release).
