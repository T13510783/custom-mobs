# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 14:47
**Request:** Bateman: mouth like Homelander's type; make that mouth the default for every mob and put it in the root `CLAUDE.md`.

**Done:**
- `mobs/patrick_bateman/patrick_bateman.html`: thin lip line (smirk tilt, opens thicker, smile wider) + teeth plate for the grin; earlier today: new UI, 2-bone rig, moving face, Idle / Idle 2 / Walk, arms flush with the torso.
- `mobs/_shared/viewer_template.html`: same standard mouth for new mobs.
- `CLAUDE.md` (Face): Homelander-type mouth = default for every mob with a face.
- Tests: face renders (rest, smirk, grin, happy, sad, angry, afraid) checked; template renders with the grin; scripts syntax-checked; `#cliptest` 0 clips (body unchanged since).

**Files:** `mobs/patrick_bateman/patrick_bateman.html`, `mobs/_shared/viewer_template.html`, `CLAUDE.md`, docs (+ PR #31: archive, references, README).

**Next:** other mobs keep their own mouths until they are worked on (rule: only touch the mob asked about); merge PR #31 when the user says "merge".
