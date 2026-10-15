# Latest session

- **Date:** 2026-10-15 17:51
- **Request:** Hard git rule; Homelander glow/cape/3 new attacks; then continue: The King on the 2-bone rig + Show bones.
- **Done:** Homelander (see changelog 17:37). The King: hand + ankle bones removed (shoe on the shin), ground solve for Idle 2 / Walk / Dance / Attack 2, Run IK on the real foot pitch, Show bones button; mod assets re-exported.
- **Tested:** King `#cliptest`: no new clips, ground OK. `gradlew build` OK, 1:1 trace test passed. Renders checked. Not tested in game yet.
- **Files:** `mobs/the_king/thekingv3.html`, `mod/src/main/resources/assets/theking/king/*`, `mod/src/test/resources/modtrace.json`, `AGENTS.md`, `docs/*`.
- **Next step:** In-game check of The King (test world). Darth Vader, Thanos, The Joker, Reverse Flash still have 1-bone limbs (older viewer engine): ask the user before converting.
