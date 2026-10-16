# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 21:31
**Request:** Improve Anton Chigurh without reworking him: fix bugs and clipping, add facial expressions, check the reference images.

**Done:**
- `mobs/anton_chigurh/anton_chigurh.html`: moving face (eyes, lids, brows, mouth) + shared expression menu, follow mouse, Show bones; 2-bone limbs, foot solver, hair springs; new Idle 2; collar / arm / prop / floor clipping fixed; test modes `#cliptest`, `#rest`, `#shot-...`.
- `references/anton_chigurh/`: the 3 reference images added to git.
- Checked: `#cliptest` 0 clips (chin dip 0.42, floor 0), face renders of all 5 expressions, all 5 animations run without errors.

**Files:** `mobs/anton_chigurh/anton_chigurh.html`, `references/anton_chigurh/*`, docs.

**Next:** Anton is viewer only (not in the mod, no release).
