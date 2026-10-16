# Changelog

## 2026-10-16 15:08
- Patrick Bateman (`mobs/patrick_bateman/patrick_bateman.html`): new film-based animations, all his own.
  - Idle "the mask": rigid posture, dead stare (blinks held back), left hand straightens the tie with two tugs, fake teeth smile, right-eye twitch (3 pulses), slow scan of the room.
  - Idle 2: "Hip to Be Square" groove on the beat (mouthing the words), freeze into the psycho stare, fake smile, mirror flex (double biceps with empty hands, one arm when holding a prop), impatient watch check with eye roll, sigh and foot taps.
  - Walk: stiff executive strut, head nod on each step, smug glance at his reflection.
  - New: 2-bone arm IK for hand gestures; face channels `twitch` and `stare`. Clip test 0 clips in all 5 outfits.

## 2026-10-16 14:49
- Repo cleanup: PR #31 (Patrick Bateman rework, standard mouth rule, archive + reference renames) merged into `main`; worktree and branch removed; scan found no leftovers (root, names, sizes, `docs/working_on.md`).

## 2026-10-16 14:47
- Patrick Bateman (`mobs/patrick_bateman/patrick_bateman.html`): mouth rebuilt as the standard Homelander type (thin lip line that tilts for the smirk, thicker when open, wider with a smile; teeth plate grows down for the grin); mouth corners painted skin shadow.
- Template (`mobs/_shared/viewer_template.html`): same standard mouth (`grin`, `smirk` face channels, teeth plate).
- Rule (`CLAUDE.md`, Face): the Homelander-type mouth is the default for every mob with a face.

## 2026-10-16 14:38
- Patrick Bateman (`mobs/patrick_bateman/patrick_bateman.html`):
  - Mouth: one connected pixel shape; corners step up (smile) / down (frown) beside the lip ends, mouth widens in a grin, teeth fill the opening, lips thin when open, no seam at rest.
  - Arms: shoulder pivots flush with the torso top (were 0.4 lower); head on a 1.0 neck lift, softer up-tilt, even walk arm swing (0 clips, all outfits).
  - Test stills (`#shot-...`) hide the UI; new `bust` camera.

## 2026-10-16 14:37
- Updater (`mod/src/main/java/com/custommobs/update/UpdateClient.java`, `UpdateScreen.java`): a new version found by the 5-minute check pops up a toast + a clickable chat line (`/sindicate update`, new client command) that opens the changelog with "Update and Close Game"; restart hint on the update screen; scroll arrow; toast cleared when the screen opens. Lang `en_us` + `nl_nl`.
- Test (`mod/testworld/homelandertest/.../timeline.mcfunction`, `CMTEST ui`): also shoots the update notice and the changelog screen.
- Version `0.0.3-alpha` (`mod/gradle.properties`, `mod/release_notes.md`, README).

## 2026-10-16 14:24
- Patrick Bateman (`mobs/patrick_bateman/patrick_bateman.html`, still one file):
  - UI: shared viewer UI (`mobs/_shared/`): dock with Idle / Idle 2 / Walk, follow-mouse, show bones, 5 outfit dots; expression menu + reset top right.
  - Rig: 2-bone arms and legs, coat panels split at the knee, prop on the forearm, head on a short recessed neck (no shoulder clipping).
  - Face: painted eyes, brows and mouth are now moving planes (same pixels at rest): blinks with eyelids, glances, brows, smile/frown corners, jaw, toothy grin; pupils centred (no cross-eye).
  - Animations: new Idle, Idle 2 (hair slick, watch check, "Hip to be square" grin) and Walk; Attack and Special (dance) removed. Springs: fringe lock, tie, coat panels.
  - Tests: `#cliptest` (0 clips, all outfits), `#rest`, `#shot-...` modes.
- Cleanup: old viewer copies `mobs/<mob>/<mob>-old.html` -> `archive/<mob>/<mob>_old.html` (bane, darth_vader, ghostface, pennywise, victor_perkins); hash-named reference images renamed by what they show (bane, black_noir, darth_vader, ghostface, michael_myers, pennywise, victor_perkins, the_king).
- README: mobs table lists Bane, Davy Jones, Ghostface, Pennywise, Victor Perkins.

## 2026-10-16 14:09
- Homelander: Smash attack removed everywhere (viewer `mobs/homelander/homelander.html`: button, animation, victim pose; mod: `HomelanderEntity`, `HomelanderAttackGoal`, `HomelanderVictims`, `HomelanderClientState`, `HomelanderData`, `SindicateConfig` option `groundSmash`, mods.toml text, test timeline).
- Homelander AI (`HomelanderAttackGoal.java`): every ready special joins one random pool (throw, throat laser, clap, point-blank heat vision), at most 2 punches in a row; lower first cooldowns; creative players can be grabbed (`HomelanderEntity.java`).
- Rules (`CLAUDE.md`): release after every in-game test; commit everything when asked.
- Release notes for v0.0.2-alpha (`mod/release_notes.md`); README attacks line.

## 2026-10-16 13:20
- Homelander (`mobs/homelander/homelander.html` + mod port in `mod/src/main/java/com/custommobs/homelander/`):
  - Hover: hands behind the back now rest ON the cape; the cape stays flat on his back and narrows between the arms (`anim/HomelanderCloth.java`, `anim/HomelanderData.java`).
  - Neck removed: head sits right on the torso, soft tilt limits (`anim/HomelanderAnimator.java`).
  - Smash: after the ankle grab he turns his back, drags the victim behind him over the floor (dust + blood), spins around and flings it (`entity/HomelanderEntity.java`, `client/HomelanderVictims.java`).
  - Walk-while-attacking button now starts the walk; spawn egg no longer dark (no tint over its texture, `HomelanderMod.java`).
  - Trace scene 56 s so the cape settles after the Hover (`HomelanderAnimatorTraceTest.java`); test world puts the egg in the hotbar.
- Rules (`CLAUDE.md`): eyelids always animated; no-neck head limits; cape under hands behind the back.

## 2026-10-16 12:13
- First release `v0.0.1-alpha`: release notes (`mod/release_notes.md`), download line (`README.md`).
- `tools/release.ps1`: works in Windows PowerShell 5.1 (native stderr from `gh` / `gradlew` no longer aborts the script).
- `docs/working_on.md`: finished claims removed (all branches merged or closed).

## 2026-10-16 12:09
- Homelander cape (`mobs/homelander/homelander.html`, `mod/.../anim/HomelanderCloth.java`): the "hands behind the back" block now only forms when both hands are behind the back. One arm swung back (walk, run, attacks) no longer lifts the cape into a flat shelf that sticks out sideways; the cape lies along that arm. 1:1 trace test green.

## 2026-10-16 11:58
- Homelander mod port of the new viewer work (`mod/src/main/java/com/custommobs/homelander/`):
  - Run: he sprints when the target is farther than 5 blocks (~10-11 blocks/s, `homelander.runSpeed`), clients play the Run loop at his real speed (`client/HomelanderClientState.java`).
  - Smash: hammer-fist slam, victim lies on its back, he grabs its ankles, drags it and flings it ~12 blocks (`entity/HomelanderEntity.java`); works on any mob up to 2.3 blocks tall.
  - Clap victims: hands on ears, fall onto their back, lie screaming, get up (`client/HomelanderVictims.java` + `mixin/LivingEntityRendererMixin.java`, `sindicate.mixins.json`).
  - Walk while attacking: legs walk under Punch / Heat vision.
  - Fixed: thrown / burned victims stayed stuck on his fist (own dismount block); he kept walking into victims after the chase; lying victims walked away; held mobs took over his movement.
  - Voice line `I am god` heard up to 32 blocks (`assets/homelander/sounds.json`).
  - Test world: smash + run scenes, debug target hook (`mod/testworld/homelandertest/`).

## 2026-10-16 10:00
- Mod renamed to **Sindicate** (`mod/gradle.properties`, `mod/src/main/java/com/custommobs/Sindicate*.java`): mod id `sindicate`, jar `sindicate-<version>.jar`, lang in `assets/sindicate/`.
- Config screen (`mod/src/main/java/com/custommobs/config/`): Client/Server tabs, search, hover tooltips, reset buttons, key `K`; server settings per world, synced, editable by operators only when `opsCanEditConfig` is on.
- Homelander (`HomelanderEntity.java`, `HomelanderAttackGoal.java`): 75 HP, 20 armor + 8 toughness, neutral by default, boss bar, many server options; American-flag spawn egg texture.
- `.env.example` is no longer in git (`.gitignore`); rules: README update at every push, repo is public (`CLAUDE.md`, `AGENTS.md`, `README.md`).

## 2026-10-16 09:40
- Homelander in the mod (`mod/src/main/java/com/custommobs/homelander/`): entity, spawn egg, creative tab, sounds + subtitles, config, en_us + nl_nl, loot table, biome modifier (`mod/src/main/resources/assets|data/homelander/`).
  - 1:1 runtime: cloth cape port (`anim/HomelanderCloth.java`), new step() port (`anim/HomelanderAnimator.java`); trace test: body within 2e-5 rad, cape exact while calm and at rest.
  - Attacks on players, villagers and every other mob: punch, heat vision (beams to the target, fire), throat grab + throw sky high, throat laser, head clap with "I am god" (lip-synced); held victims ride his fist.
  - Renderer: body quads, continuous cape mesh, additive eye glow + beams (`client/`).
  - Test world `gradlew runHomelanderTest` (`mod/testworld/homelandertest/`), dev tags `hl_*`, game screenshots via `HLSHOT`.
- Release pipeline: version `0.0.1-alpha` (parts roll over at 99), jar `custom-mobs-<version>.jar`, newest 5 kept in `builds/` and `mod/build/libs/`, `tools/release.ps1`, changelog `mod/release_notes.md` (Added / Removed / Fixed).
- In-game updater (`mod/src/main/java/com/custommobs/update/`): "Check for Updates" button, check every 5 minutes, changelog screen, "Update and Close Game" (detached helper swaps the jar after closing). Tests: version rules, release pick, real jar swap.
- Rules + docs: `CLAUDE.md` (releases, updater, Homelander test world, game screenshots only), `run.txt`, `README.md`.

## 2026-10-16 08:48
- Homelander cape (`mobs/homelander/homelander.html`): 50 bones posed only by cloth physics (7 chains, gravity, air, breeze, body + hands collisions); drawn as one continuous cloth surface from the cloth points (no gaps, steps or box edges); lies over his hands in Hover.
- Homelander clap: he smacks both ears first, then the dummy grabs its own ears, screams and falls; I AM GOD plays to the end (lip-sync curve covers the whole 2.55 s file).
- Homelander: one file (no `js/` folder), dummy with 2-bone arms/legs, throat grabs at neck height, rarer random grin, dock wraps.
- Shared viewer UI (`mobs/_shared/viewer.css`, `viewer.js`, `viewer_template.html`): expression menu (Auto / Happy / Sad / Angry / Afraid, face icons, pill style); Homelander uses it.
- Sounds (`sounds/homelander/`, `tools/sfx/`): new `slap`, `crush` removed.
- Rules (`CLAUDE.md`): expression menu, shared viewer files, only touch the asked mob, 10000 lines per file, edit tool choice, own-branch cleanup, organise + commit + push after every request.

## 2026-10-16 08:28
- Michael Myers (`mobs/michael_myers/michael_myers.html`):
  - Built 16x16 Vanilla+ 3D viewer based on the 3 reference photos in `references/michael_myers/` (`images.jpg`, `MichaelMyers2018.jpg`, `MICHAELMYERSHD.png`), with a 2-bone arm and leg rig (`armR`/`forearmR`, `armL`/`forearmL`, `legR`/`kneeR`, `legL`/`kneeL`), recessed hollow eye sockets with pointer tracking and smooth blinks, spring-physics collar/sheet drape and hair tufts, and a 3-bevel 12-inch Lamson rosewood butcher knife.
  - Added 4 selectable outfits in the floating bottom pill bar: `2018 Haddonfield` (weathered cracked grey-ivory latex mask, charcoal-navy coveralls with open collar, grey undershirt, square right pocket + flapped left pocket, and bandaged left hand), `1978 The Shape` (stark bleached-white mask with chiaroscuro shadow, high upturned collar wings, and slate-spruce Tex-Green coveralls), `Scorched 2021` (fire-charred left mask & bloodied coveralls), and `Ghost Sheet` (1978 white bedsheet ghost disguise with Bob's 3D wire-rimmed glasses).
  - Implemented 4 selectable animations (`Idle`, `Walk`, `Attack`, `Special` head-tilt & glowing Jack-O'-Lantern pumpkin), camera reset (`R`), and `Show bones` (`B`) overlay.
- Hannibal Lecter (`mobs/hannibal/hannibal.html`):
  - Refined Mads Mikkelsen's 8x8 pixel-art face: moved deep-set hooded eyes to row 4 (`y = 3.5`) with inner-anchored dark irises and upper orbital hood fold, overhanging bone brow ridge without dark eyebrows, high cheekbone highlights, tear-trough eye bags, Cupid's-bow mouth, and a pixel-grid-aligned stepped diagonal ash-brown hair sweep.

## 2026-10-16 08:11
- Hannibal Lecter (`mobs/hannibal/hannibal.html`):
  - Built 16x16 Vanilla+ 3D viewer based on the 5 reference photos in `references/hannibal/`, featuring Mads Mikkelsen likeness (high cheekbones, hollow cheek shading, faint ash-blonde brow ridge, swept sandy-ash hair with silver temples and spring-physics diagonal forehead strands, smooth blinking eyelids, and pointer head/eye tracking).
  - Implemented 4 selectable outfits matching the reference photos: `Plaid Suit` (brown/burgundy/teal 3-piece windowpane plaid suit, pale blush shirt, burgundy/silver paisley tie, pocket square, Chianti wine glass & scalpel), `Kill Suit` (translucent clear vinyl zip-up coverall with double white zipper tape and upturned plastic collar over charcoal glen-plaid suit + #10 surgical scalpel), `Chef Flambé` (rolled-sleeve white dress shirt, striped butcher's waist apron, copper/steel skillet with animated 3D voxel flambé flames + brandy bottle), and `Psychiatrist` (burnt rust-orange shirt, dark slate-teal check waistcoat, teal/copper paisley tie).
  - Added 2-bone arm and leg rig (`armR`/`forearmR`, `armL`/`forearmL`, `legR`/`kneeR`, `legL`/`kneeL`), 4 animations (`Idle`, `Walk`, `Attack`, `Special`), camera reset (`R`), and `Show bones` (`B`) overlay.

## 2026-10-16 08:01
- References (`references/darth_vader/`, `references/hannibal/`):
  - Sorted and renamed 5 new Darth Vader reference photos from root into `references/darth_vader/` (`vader_full_body_grey_bg.png`, `vader_full_body_red_bg.png`, `vader_storm_clouds_portrait.png`, `vader_full_body_lightsaber.png`, `vader_helmet_closeup_bw.png`).
  - Sorted and renamed 5 new Hannibal Lecter reference photos from root into `references/hannibal/` (`hannibal_cooking_flambe.png`, `hannibal_kill_suit_scalpel.png`, `hannibal_waistcoat_portrait.png`, `hannibal_plaid_suit_full.png`, `hannibal_kill_suit_dark.png`).

## 2026-10-16 07:53
- Jason Voorhees (`mobs/jason_voorhees/jason_voorhees.html`, `references/jason_voorhees/`):
  - Sorted the three Jason Voorhees reference images from root into `references/jason_voorhees/` (`jason_standing_machete_rain.png`, `jason_rooftop_machete.png`, `jason_mask_spear_closeup.png`).
  - Built the 16x16 Vanilla+ 3D viewer with 2-bone arms/legs (`arm_R`/`forearm_R`, `arm_L`/`forearm_L`, `leg_R`/`knee_R`, `leg_L`/`knee_L`), oval fiberglass hockey mask with forehead axe crack, top strap with silver rivets, red chevron, 3D breather vents, sunken shadowed eyes with smooth blinks and pointer tracking, olive work shirt with chest pockets and bullet tears, open tattered hunting coat with spring-physics lower flaps, bloodied 3-bevel Machete, and Part VI iron Fence-Post Spear.
  - Added 4 selectable animations (`Idle`, `Walk`, `Attack`, `Special`), locked 60 FPS delta-time loop, camera reset button, and `Show bones` (`B`) overlay.

## 2026-10-15 22:36
- Organized files into subfolders without deleting or editing any contents:
  - `mobs/anton_chigurh/AntonChig.html`: sorted Anton Chigurh viewer into its own mob directory (`mobs/anton_chigurh/`).
  - `archive/patrick_bateman/patrickbateman3final.html`: archived earlier Bateman viewer into `archive/patrick_bateman/`.
  - Preserved existing root files and verified byte-for-byte SHA256 integrity.

## 2026-10-15 22:30
- Rule (`CLAUDE.md` GitHub section): Claude always commits and pushes itself, with a short clear `<area>: <what>` title and a 1-4 bullet description.
- PR #19 (I AM GOD voice line + clap reference) and PR #20 (repo cleanup) merged; branch `tivek87/homelander-attacks` rebased onto `main`.

## 2026-10-15 22:25
- References sorted and renamed (lowercase, no spaces): `references/_unsorted/Drop kick - Extreme Slow Motion.mp4` -> `references/the_king/drop_kick_slow_motion.mp4` (used by The King's Attack; comment in `mobs/the_king/thekingv3.html` updated), `The Burger King Plane Guy Original MEME.mp4` -> `plane_guy_meme.mp4`, `VID_20261014_065424_395.mp4` -> `the_king_clip_20261014.mp4`, `references/homelander/homelander 1.png` -> `homelander_1.png`.
- New rules: always clean up git after a merge / at session end (`CLAUDE.md` teamwork section, `AGENTS.md` section 7); root holds only docs/config + folders, clear file names (`CLAUDE.md` rule 15).

## 2026-10-15 22:22
- Repo cleanup (branch `tivek87/repo-cleanup`):
  - Root: removed `test.txt`, the root copy `PatrickBateman.html` (same bytes as `mobs/patrick_bateman/PatrickBateman.html`), 9 `media_*.png` and 5 `reference_bateman_*.png` (all exact duplicates of files in `references/patrick_bateman/`).
  - `references/patrick_bateman/`: removed `reference_bateman_gun_aim.png` (same bytes as `reference_bateman_gun.png`); moved the 6 hash-named film stills from the root in as `bateman_film_*.jpg`.
  - `run.txt` + `CLAUDE.md` (rules 21, 24): no hardcoded `C:\custom mobs` path, the command now works from any clone folder.

## 2026-10-15 22:20
- Viewers: Download .bbmodel button + toast removed (`mobs/the_king/thekingv3.html`, `mobs/homelander/homelander.html`, `mobs/black_noir/black_noir.html`); the export code stays for the headless `.bbmodel` save.
- Show bones button (key `B`) added to `mobs/darth_vader/DarthVader.html`, `mobs/thanos/Thanos.html`, `mobs/patrick_bateman/PatrickBateman.html` (+ root copy), `mobs/reverse_flash/ReverseFlash.html`, `mobs/the_joker/the_joker.html`: one self-contained rig-overlay block that hooks the scene update.
- `tools/cdp.ps1`: new `-shot <file.jpg>` option (page screenshot after the expression returns).

## 2026-10-15 22:06
- Patrick Bateman (`PatrickBateman.html` & `mobs/patrick_bateman/PatrickBateman.html`):
  - Removed teeth from raincoat skin face: restored the standard deadpan serious face (`px(ctx, 3, 6, 2, 1, skinDeep)`) matching all other skins.
  - Re-anchored Paul Allen axe hold to the bottom of the handle hanging down:
    - Updated `buildPaulAllenAxe()` so the origin grip point is at the hand hold near the top butt knob (`y = +0.9 * PX`).
    - The handle hangs naturally downward at his side (`handle.position.y = -5.0 * PX`) with the mirror-chrome axe head mounted at the bottom (`y = -10.2 * PX`, 3.3 PX above the floor), blade facing forward (`+Z`).
    - Eliminates all no-clipping through the arm, torso, and raincoat—the only point of contact is the handle held in his hand.
  - Adjusted raincoat idle arm to `rArmX = -0.06, rArmZ = 0.10` and walk swing to `-0.10 - swing * 0.28, rArmZ = 0.10` so the axe hangs and swings freely in open air beside his leg.
  - Synchronized `PatrickBateman.html` directly to `mobs/patrick_bateman/PatrickBateman.html` (verified identical SHA256 hashes).

## 2026-10-15 22:04
- Rules: every commit and squash-merge needs a clear, short, clean message with the AI co-author trailer (`CLAUDE.md` GitHub section, `AGENTS.md` section 6).
- New `.claude/settings.json` with `"includeCoAuthoredBy": true`.
- Viewer UI rule 10 (`CLAUDE.md`): no Download .bbmodel button; Show bones button in every viewer, always.

## 2026-10-15 21:52
- Patrick Bateman (`PatrickBateman.html` & `mobs/patrick_bateman/PatrickBateman.html`):
  - Fixed legs clipping in Special: replaced rotated scissor stance (`rLegX = -0.12, lLegX = 0.12`) with an authentic upright planted shooting stance (`rLegX = 0, lLegX = 0, rLegZ = 0.03, lLegZ = -0.03`). Completely eliminates the disjointed coat protrusion at the back of the waist and front torso clipping shown in user screenshot.
  - Fixed legs clipping in Walk: tailored walking stride for overcoat (`swing * 0.28`) and raincoat (`swing * 0.32`), refined coat overlay depth to `4.08 * PX` flush with the torso, and made the inner thigh faces transparent (`empty4x12`) so the two leg skirts never collide or slice through each other.
  - Made overcoat DARKER brown overall: transformed sandy tan palette (`#bfa07a`) to rich dark chocolate / camel wool brown (`coatBase = '#604229'`, `coatLight = '#785334'`, `coatDark = '#48311e'`, `coatDeep = '#342315'`) across torso, 3D lapels, sleeves, cuff straps, and skirt overlay.
  - Placed uploaded reference screenshot in root (`media_1792093345142_d3f364d8.png`) and backed up to `references/patrick_bateman/`.
  - Synchronized `PatrickBateman.html` directly to `mobs/patrick_bateman/PatrickBateman.html` (verified identical SHA256 hashes).

## 2026-10-15 21:38
- Patrick Bateman (`PatrickBateman.html` & `mobs/patrick_bateman/PatrickBateman.html`):
  - Adjusted Glock pistol position forward along barrel axis and slightly outward (`y = -2.2 * PX`, `x = -0.35 * PX` in prop group): completely resolved the issue where the slide was sunken/no-clipped inside the black glove box; the rear sight and beavertail now sit cleanly at the knuckles, with the entire slide, ejection port, and sights clearly visible outside the glove.
  - Held Paul Allen axe normally at the bottom of the handle: updated `buildPaulAllenAxe()` so the origin grip point is at the lower fawn-foot knob (`y = 0`), with the long hickory handle rising upwards (`15.5 * PX`) and the mirror-chrome axe head held high at chest level (`y = 13.6 * PX`).
  - Movie lore-accurate "It's Hip to Be Square" dance for Raincoat Special: choreographed an authentic 2-phase dance routine alternating smoothly between both movie stills:
    - Phase 1 (Pic 4): Sassy lean-back moonwalk glide with tight 80s power-fist pump, left hand behind back, head tilted back singing along, and axe held over shoulder.
    - Phase 2 (Pic 3): Iconic two-handed axe presentation diagonally across chest (right hand at bottom of handle, left hand gripping upper shaft) with beaming manic grin!
  - Updated raincoat face texture: gave Bateman a wide ecstatic open-mouthed grin showing white teeth matching both movie stills.
  - Placed reference photos in root and backed up to `references/patrick_bateman/`.
  - Synchronized `PatrickBateman.html` directly to `mobs/patrick_bateman/PatrickBateman.html` (verified identical SHA256 hashes).

## 2026-10-15 21:11
- Patrick Bateman (`PatrickBateman.html` & `mobs/patrick_bateman/PatrickBateman.html`):
  - Fixed Glock pistol hold angle so it points correctly and never points upwards: re-anchored gun pivot to the hand grip and oriented with `rotation.x = Math.PI / 2`, so in Special (`rArmX = -Math.PI / 2`) the barrel aims dead level horizontally forward (`0.00 rad` elevation) directly at the target, and in Idle (`rArmX = -0.08`) the barrel points safely downward towards the ground at his side.
  - Fixed Paul Allen chrome axe hold: re-anchored axe grip to the hand hold point right below the head, mounting the heavy mirror-chrome axe head upright at waist height with cutting edge facing forward (`+Z`) and the long hickory shaft extending cleanly down past the knee.
  - Added the iconic "It's Hip to Be Square" dance for Raincoat Special animation: 140 BPM upbeat pop groove with high-stepping knee bounce, dynamic hip sway and torso rock, enthusiastic head bobbing, left arm pumping and pointing with 80s swagger, and right arm swaying the axe rhythmically at his side (zero clipping).
  - Synchronized `PatrickBateman.html` directly to `mobs/patrick_bateman/PatrickBateman.html` (verified identical SHA256 hashes).

## 2026-10-15 20:53
- Patrick Bateman (`PatrickBateman.html` & `mobs/patrick_bateman/PatrickBateman.html`):
  - Fixed sunken / disconnected shoulder geometry: updated arm group pivots to `x = Â±6.0 * PX, y = 12.0 * PX, z = 0` (was previously `y = 10.0 * PX`), making the top of the arms flush with the top of the torso and collar line, forming a seamless 16-pixel shoulder line without gaps.
  - Made held Glock pistol, Paul Allen chrome axe, and 1980s brick mobile phone prominently visible in the hand in Idle:
    - Glock 19: refined tactical gunmetal slide (`0x2d323b`, metallic sheen), polished steel ejection port, bright white 3-dot aiming sights, recoil guide rod, and forward projection (`z = 1.6 * PX`, slide extending to `5.8 * PX` in +Z).
    - In Idle pose for tan overcoat: angled the right arm forward (`rArmX = -0.22, rArmZ = 0.09, rArmY = -0.06`) so the gun silhouette clearly projects in front of the coat and is immediately visible from all camera angles.
    - Synchronized `PatrickBateman.html` directly to `mobs/patrick_bateman/PatrickBateman.html` (verified identical SHA256 hashes).

## 2026-10-15 19:51
- Patrick Bateman (`PatrickBateman.html` & `mobs/patrick_bateman/PatrickBateman.html`):
  - Fixed clipping bug on special animation with tan overcoat: corrected arm pivot centering (y = -6*PX instead of -4*PX so shoulder rotates around top of arm without swinging backwards into the torso), refined outer torso/arm/leg coat overlay dimensions, and tuned arm angles. Clip audit: 0 penetrations across all 16 variant/animation combinations.
  - Tan overcoat skin: added 3D voxel Glock 19 semi-automatic pistol with black polymer frame, trigger guard, metallic trigger, gunmetal slide with serrations and ejection port, and white 3-dot sights; in Special animation, points the gun straight forward in dramatic shootout stance matching Reference Pic 2.
  - Raincoat skin: added Paul Allen murder axe with long American hickory curved wood handle and polished mirror-chrome wedge blade with razor-sharp cutting edge matching Reference Pics 3 & 4; in Special animation, brings the axe up to inspect the gleaming chrome blade.
  - Suspenders skin: added authentic 1980s Motorola DynaTAC brick mobile phone with beige chassis, red LED segment display, 3x4 keypad, and long flexible rubber antenna matching Reference Pic 5; in Special animation, holds the phone up to his right ear while smirking behind sunglasses.
  - Keyboard shortcuts added: 1-4 for animations (Idle, Walk, Attack, Special), 5-8 for outfit variants, R for camera reset.
  - Reference photos placed directly in project root as requested: `reference_bateman_special_clip_issue.png`, `reference_bateman_gun.png`, `reference_bateman_axe_inspect.png`, `reference_bateman_axe_standing.png`, `reference_bateman_phone.png`, and backed up to `references/patrick_bateman/`.

## 2026-10-15 17:51
- The King (`mobs/the_king/thekingv3.html`) on the 2-bone rig rule:
  - Removed the empty `hand_*` bones and the `ankle_*` bones; the shoe now sits on the shin bone (same spot).
  - New ground solve (`legHeight()` / `footLow()`, smooth min): Idle 2 (relaxed knee forward), Walk (heel strike / toe-off, swing knee 1.15 so the long shoe clears), Dance (crouch on the balls of the feet, feet stay put), Attack 2 (back foot pivots on its toe, solved per key: no slide, no float).
  - Run IK reads the real shoe pitch (thigh + knee) for the contact height (6 passes). Attack 1 keeps its keys (ground check lifts it as before).
  - Show bones button (key B).
  - Cliptest: no new clips (only the 4 old bandana contacts), ground 0-0.015, ground lift needed <= 0.07 (was up to 5.4 in Idle 2).
- Mod: re-exported (`rig.json` 26 bones, `anims.bin`, trace); `gradlew build` OK, 1:1 test passed (max 0.00011 rad / 0.00031 texel).
- `AGENTS.md`: shared folder -> work in your own git worktree, keep the shared folder on `main`.

## 2026-10-15 17:37
- Homelander (`mobs/homelander/homelander.html`):
  - Red glow fixed: no more red light in the middle of the face; now only the eyes glow (red eye whites, hot orange core on the pupil, faint halo; a blink hides it). Attack: eyes flare in the wind-up. Special: eyes heat up fully before the beams.
  - Laser beams now start at the eyes (were starting inside the head); length per animation.
  - Cape: 10 bones of 2 texels (was 5 x 4), same look; waves run smoothly down the cape, springs chain (each bone follows the one above).
  - New attacks: 6 Throw (neck grab, lift, fling up, watch them fall), 7 Clap (from `references/homelander/homelander_i_am_god.mp4`: arms wide + scream, palms slam onto the sides of the head), 8 Throat Laser (grab, 1 s heat-up, point-blank lasers to the face).
  - Cliptest: 0 clips, ground OK in all 9 animations, 0 bad UVs.
- Hard git rule in `CLAUDE.md` + `AGENTS.md`: pull, commit, push after every prompt and change; check the current branch before every commit (shared folder).

## 2026-10-15 17:12
- GitHub repo `T13510783/custom-mobs` (private) created and pushed; squash merge only, merged branches auto-deleted.
- New "GitHub repo and teamwork" section in `CLAUDE.md`; repo info, join steps and rules table in `README.md`; extra safety rules in `AGENTS.md` (binary conflicts, rejected push, 50 MB limit, no settings changes without OK).

## 2026-10-15 17:05
- Git repo set up: `.gitignore`, `.gitattributes`, `.env.example`, `README.md`, `AGENTS.md` (team git rules, read by every AI), `docs/working_on.md` (who works on what). `claude.md` -> `CLAUDE.md` (imports `AGENTS.md`).
- Files sorted: Reverse-Flash viewer -> `mobs/reverse_flash/ReverseFlash.html` (exact duplicate `the_reverse_flash_3d_viewer.html` deleted); its 5 photos -> `references/reverse_flash/reverse_flash_1..5.jpg`; `mobs/the_joker/the joker.html` -> `the_joker.html`.

## 2026-10-15 16:30
- New rig rule (`CLAUDE.md` rule 10 + 12): exactly 2 bones per arm (`arm`, `forearm`) and per leg (`leg`, `knee`), boot on the shin, no hand/ankle bones; "Show bones" button (key `B`) in every viewer.
- Homelander (`mobs/homelander/homelander.html`): 2-bone rig, ground solve, bones overlay; orange collar removed; nose box removed, player-style painted face with moving brows, mouth (smirk/grin + teeth) and blinking eyes; red eye glow at random; new Hover idle (floats up, hands behind back, looks around) that starts at random from Idle; Idle 2 = PR grin + peace sign, then cold stare with red flicker. Cliptest: 0 clips.
- Black Noir (`mobs/black_noir/black_noir.html`): 2-bone rig (knife on the forearm, boot on the shin), new idle/idle2/walk, bones overlay. Cliptest: only pouch vs gauntlet 0.078.

## 2026-10-15 14:23
- Homelander look matched to the photos (`mobs/homelander/homelander.html`), same cubes except a shorter bracer:
  - Suit royal blue with a soft scale texture (was dark navy checkerboard); copper collar on top of the chest.
  - Red piping: one line across the upper chest that turns down his right chest to the belt.
  - Forearms: copper scale bracers (4.2x3.6x4.2, was 3.8 tall), blue sleeve above, red glove below. No red band at the elbow.
  - Antique gold pauldrons/belt (belt keeps the red square), red leather boots with plain gold rim + 2x2 gold medallion, matte boot toes (blue glare gone).
  - Cape: star yoke ends in a V, equal red/white stripes, inside striped too (was plain white).
  - Hair: natural golden blonde, combed strands on back/sides (was lemon-yellow checkerboard), short dark nape.
- New **Idle 2** (key 2, 7.0 s): smug peace sign from the photo, small wave, grin, then looks around. Keys now Idle 1, Idle 2 2, Walk 3, Attack 4, Special 5.
- Walk T 1.74 -> 1.75 s (whole 1/120 s frames); `abs()` / bare `max()` corners removed. Idle loop wrap jumps fixed. Random double blinks; face filter 16.
- `#cliptest`: 0 clips, ground >= 0.04 in all 5 anims, 0 bad UVs.
- Files: `homelander_peace_sign.jpg` + `homelander_suit_closeup.png` -> `references/homelander/`; deleted root jpg (same photo as `homelander_front_back.png`), `scratch/` and `references/homelander/shot_*.png` (old test renders).

## 2026-10-15 13:49
- Homelander: textured face with rich skin tones matching The King and added a 3D nose bridge in `mobs/homelander/homelander.html` and `homelander.html`:
  - Face skin micro-shading: updated `paintHead` with `skinLight` forehead highlights, `hash2D` micro-shading across `skinBase` and `skinWarm`, `skinRosy` cheekbones, `skinShadow` cheek/jawline contours, and philtrum depth, matching The King reference.
  - Added 3D nose bridge: created `nose_bridge` box ($2 \times 2 \times 1.2$, center $0, 4.2, 4.4$) on bone `head` between the eyes with `paintNose` (light bridge highlight and warm rosy tip).
  - Hair and suit strictly untouched per user instruction.
  - Verified via headless Brave (`#cliptest`): 0 clips (`clips: {}`), 0 bad UVs (`badUV: []`), 0 ground penetrations, 35 cubes, all 3 color variants and 4 animations verified clean.

## 2026-10-15 13:23
- Homelander: replaced yellow center of belt with a red square buckle in `mobs/homelander/homelander.html` and `homelander.html`:
  - Updated `paintTorso` rows $y=10, 11$: painted a shaded red square (`crimsonLight`, `crimsonBase`, `crimsonWarm`) on columns $x=3, 4, 5$ in the middle of the belt, framed symmetrically by 3 gold pixels on either side ($x=0..2$ and $x=6..8$).
  - Strictly built directly into the suit texture with zero 3D box overlays per user instruction.
  - Verified via headless Brave (`#cliptest`): 0 clips (`clips: {}`), 0 bad UVs (`badUV: []`), 0 ground penetrations, 34 cubes, and visual verified via `#shot-idle-0-1`.

## 2026-10-15 13:18
- Homelander: cleaned chest, removed shoe gaps to single-shoe Minecraft boots, and built belt directly into suit in `mobs/homelander/homelander.html` and `homelander.html`:
  - Chest cleanup: removed the two vertical red chest lines and yellow collar bar; sculpted blue micro-weave suit with defined superhero pectorals and six-pack abdominals matching reference photos.
  - Seamless boots: removed `boot_cuff_right` and `boot_cuff_left`, eliminating the 1-texel ankle gap so each foot has exactly 1 solid, floor-flush Minecraft shoe (`boot_right`, `boot_left`) connected seamlessly to the tall crimson calf boot shaft.
  - Built-in belt: removed extruded 3D overlay boxes `belt_strap` and `eagle_buckle`; painted the golden eagle buckle and gold belt strap directly into the waist rows ($y=10, 11$) of `paintTorso`, wrapping seamlessly around front, sides, and back.
  - Reduced cube count from 38 to 34 cubes. Verified via headless Brave (`#cliptest`): 0 clips (`clips: {}`), 0 bad UVs (`badUV: []`), 0 ground penetrations, all 3 color variants and 4 animations verified clean.

## 2026-10-15 12:34
- Fixed: the King sometimes stood next to a monster without punching. His path (`moveTo(entity)`, accuracy 1) ended ~1.5 blocks away, just outside his punch reach (1.48). Now he paths all the way (`createPath(target, 0)`) and steps straight in when the path ends too early (`KingAttackGoal.java`).
- Fixed: punches that started but did not land. The target slid out of reach during the 0.42 s wind-up; the hit now has +0.5 block reach (step-in + arm) (`KingEntity.java`).
- Husk test (200 HP husk) in the test world (`mod/testworld/.../fight_zombie.mcfunction`); dev-only state log once a second. Before: 2 punches landed, 3 missed, stood still up to ~9 s. After: 14 landed, 0 missed, 2/2 dropkicks, never stood still more than 1 s.
- New jar copied into the Modrinth instance. Rule added to `claude.md` (rule 23: melee AI).

## 2026-10-15 12:19
- Fixed the 358 VS Code problems ("import net.minecraft cannot be resolved"): the Java extension had synced the project at 12:04, before the NeoForge 21.1.251 Minecraft jar existed. Reloaded the project; now 0 problems.
- `.vscode/settings.json`: `java.configuration.updateBuildConfiguration` = `automatic` (the project reloads by itself when build files change). `mod/build.gradle`: note on the order (build first).
- `KingEntity.java`: silenced the false "deprecated" warning on `finalizeSpawn` (NeoForge: override-only, overriding is correct).
- Build + 1:1 test pass; new jar also copied into the Modrinth instance. Rule added to `claude.md` (rule 23).

## 2026-10-15 12:10
- Mod name: **The King: Crowned Brawler** (`mod/gradle.properties`); new description + logo `mod/src/main/resources/theking_logo.png` (viewer render, crouched before the dropkick) in `neoforge.mods.toml`.
- Built against NeoForge 21.1.251 (the user's Modrinth pack) with range `[21.1.251,)`: the 21.1.259 build would not load there (`gradle.properties`, `build.gradle`, `neoforge.mods.toml`).
- Installed in Modrinth instance "take a guess": `%APPDATA%\ModrinthApp\profiles\SuperPowers test 1.0.0\mods\the-king-crowned-brawler-1.0.0.jar`.
- Spawn egg (already there) tested: right-click spawns a King. Test datapack: egg step at 100 s (`mod/testworld/.../tick.mcfunction`).
- Tested together with the pack's mods (Sodium, Iris, EMF, ETF, JEI, Jade, Mutant Monsters, Multiverse Powers...): loads, renders, fights. 1:1 test passes.
- Rules: `claude.md` rule 23 (build for the pack's NeoForge, Modrinth path, test with pack mods) and 24 (process filter, server lag).

## 2026-10-15 11:56
- The King: new **Run** animation (key 7, 1.125 s loop) in `mobs/the_king/thekingv3.html`. Legs are solved from a running foot path (2-bone IK): stance slide, heel kick, knee drive, flight with both feet off the ground; forward lean, pumping arms, effort face. First version looked like speed-walking (body lowest with legs spread) and was rebuilt. No new clipping, feet exactly on the ground.
- Viewer: `#modexport` refuses lengths that are not whole 1/120 s frames; `#modtrace` now records Run too (19.7 s, length stored in the trace).
- Mod: he sprints (+30% speed, synced flag) while his target is more than 4.5 blocks away; the client plays Run, sped up to his real ground speed (`KingAttackGoal.java`, `KingClientState.java`, `KingEntity.java`). Teleports no longer count as speed. Test reads the trace length (`KingAnimatorTraceTest.java`).
- Test world: run test at 80 s and 90 s (`mod/testworld/.../tick.mcfunction`).
- Docs: `docs/the_king.md` (Run row, running behavior). Rules: `claude.md` rule 5 (how to build a run, no hard corners) and rule 23 (whole-frame lengths, base animations from synced flags).
- Tested: 1:1 test passes (592 frames x 30 bones, max 0.00012 rad); in game the log shows `idle -> run` on both run tests, then a dropkick hit.

## 2026-10-15 01:57
- Black Noir: fixed leg clipping / z-fighting speckles and resolved multiple bugs in `mobs/black_noir/black_noir.html`:
  - Knee pad & shin guard z-fighting fix: resolved coplanar face overlap at $Z=2.5$ by layering knee pads proud at $Z=2.55$ ($3.6 \times 2.2 \times 0.8$, center $z=2.15$) and shin guards recessed at $Z=2.37$ ($3.6 \times 2.6 \times 0.7$, center $z=2.02$). Removed $0.7$-texel vertical overlap so both meet cleanly at seam $y=-1.1$.
  - Ankle boot collision fix: raised shin guard lower edge to $y=-3.7$, creating $0.5$ texels of dynamic clearance above boot cuffs ($y=-4.2$) and eliminating walking collisions (`shin_guard in boot: 0`).
  - Texture cleanup: removed redundant front `armorBevel` line from calf undersuit fabric in `paintCalf`.
  - Debugging API: exposed `setVariant`, `selectAnim`, and `buildBBModel` on `window` and `window.blackNoirDebug` for clean headless inspection.
  - Verification: `#cliptest` clean (0 unwanted clips across all frames, 0 ground penetrations, 0 bad UVs, 39 cubes). Verified all 3 color variants, all 3 animations, keyboard shortcuts, and .bbmodel export.

## 2026-10-15 01:13
- Black Noir: added gray composite armor to sides and back of legs in `mobs/black_noir/black_noir.html`:
  - Thighs (`paintThigh`): added gray composite armor plates (`armorBevel` and `armorEdge`) to hamstring (back) and lateral quads (sides) between the tactical harness straps.
  - Drop-leg holster (`paintHolster`): added gray armor bevel rims to sheath sides and back.
  - Knee pads (`paintKneePad`): added wrap-around gray armor bevels to the sides and edges.
  - Calves & Shins (`paintCalf`, `paintShinGuard`): added rear calf muscle armor plates, lateral calf armor guards, and outer calf sheath plates with gray bevels on front, back, and sides.
  - Strictly touched ONLY the legs per user instruction (head, torso, arms, daggers, animations untouched).
  - Verified via headless Brave (`#cliptest`): 0 clips (`clips: {}`), 0 bad UVs (`badUV: []`), 0 ground penetrations (`ground: { idle: 0, idle2: 0, walk: 0 }`), 39 cubes. Visual verified via `#shot-idle-0-1` front/side and rear camera renders.

## 2026-10-15 01:05
- Black Noir: added gray composite armor to torso back and arms in `mobs/black_noir/black_noir.html`:
  - Torso back: replaced plain fabric on `spine_armor` with `paintBackArmor`, featuring segmented scapular armor plates with gray bevels (`armorBevel`) and silver rivets (`armorRivet`), plus lumbar plates, matching the front chest armor 1:1.
  - Arm armor (back and sides): updated `paintUpperArm`, `paintPauldron`, and `paintForearm` to add gray tactical armor plates to the triceps, lateral upper arm, shoulder pauldron rim bevels, and gauntlet back/sides.
  - Flank panels: extended athletic charcoal flank stretch panels around the back flanks in `paintTorso`.
  - Zero changes to head, legs, knives, or animations.
  - Verified via headless Brave (`#cliptest`): 0 clips (`clips: {}`), 0 bad UVs (`badUV: []`), 0 ground penetrations (`ground: { idle: 0, idle2: 0, walk: 0 }`), 39 cubes. Visual verified via `#shot-idle-0-1` front/side and rear camera renders.

## 2026-10-15 00:54
- Black Noir: reworked head model and textures to faithfully match user reference photos (`ref1_standing.png`, `ref2_daggers.png`, `ref3_fist.png`) in `mobs/black_noir/black_noir.html`:
  - Recessed dark tinted goggle lenses: replaced bright specular white glints with stealth pitch-black tinted lenses (`visorBase`, `visorTint`, `visorGloss`) sitting recessed 0.22 texels inside the outer helmet shell.
  - Menacing composite brow & nose bridge: sculpted a cohesive down-turned armored brow ridge and angular nose bridge in `paintMaskOuter` without square highlight artifacts.
  - Smooth aerodynamic cowl & ear cups: contoured the helmet dome and painted circular molded communication ear covers on temple sides.
  - Sleek balaclava lower face: kept rows 6-7 transparent on `mask_outer` so the form-fitting balaclava mouth and tapered jawline remain sleek without bulky square jaw clipping.
  - Zero changes to body, arms, legs, or animations (strictly head-only per user instruction).
  - Verified via headless Brave (`#cliptest`): 0 clips (`clips: {}`), 0 bad UVs (`badUV: []`), 0 ground penetrations (`ground: { idle: 0, idle2: 0, walk: 0 }`), 39 cubes. Visual verified via `#shot-idle-0-1`.

## 2026-10-14 23:59
- The King: new **Attack** (key 5, 3.4 s: crouch, leap, two-footed dropkick, back landing, stand up; made from `references/_unsorted/Drop kick - Extreme Slow Motion.mp4`) and **Attack 2** (key 6, 1.1 s punch) in `mobs/the_king/thekingv3.html`. Voice line key moved to `V`. Camera follows the leap; ground clamp over all cubes; `#cliptest` clean (only the 4 old hidden overlaps).
- New docs page `docs/the_king.md`: look, animations, behavior (dropkick, punch, dance 1 s after every kill, random voice lines), stats, spawns, config, how the mod stays 1:1.
- New mod `mod/` (NeoForge 1.21.1): The King with the exact viewer model and animations (exported by `tools/export_mod_assets.ps1`, checked by a 1:1 JUnit trace test: max 0.00012 rad), own creative tab, spawn egg, AI (dropkick re-aims at takeoff, range = leap reach), dance on kill with music, lip-synced voice lines, 3 variants, config + config screen, en/nl lang, loot, natural spawns.
- `gradlew build` copies the jar to `builds/` (newest 5 kept). New `run.txt` (root). Test world: `gradlew runTestWorld` + datapack `mod/testworld/` (stage with glass walls, scripted zombie and player fights).
- New tool `tools/winshot.ps1` (screenshot of one window).
- `claude.md`: rule 20 = keep 5 builds; new rules 23 (mod = viewer 1:1, export pipeline) and 24 (fast in-game test); later rules renumbered 25-28.
- Tested in game: dropkick hit the zombie, punches killed it, dance started exactly 1.0 s after the kill, voice lines play, dropkicks hit the player.

## 2026-10-14 23:41
- Improved Black Noir 3D model and textures to faithfully match user reference photos (`ref1_standing.png`, `ref2_daggers.png`, `ref3_fist.png`) in `mobs/black_noir/black_noir.html`:
  - Head & cowl: removed protruding snout mouth box; shaped aerodynamic helmet dome, tinted almond ocular visor lenses with specular light glint (`paintVisorLens`), smooth fabric balaclava lower face, and bunched accordion-pleated fabric neck gaiter (`paintNeckGaiter`).
  - Chest & torso armor: built dual rounded pentagonal pectoral plates with graphite bevel outlines and silver corner rivets (`paintPectorals`, matching `ref3_fist.png`), 6-pack abdominal trauma pads (`paintAbs`), athletic charcoal flank stretch panels (`paintTorso`, matching `ref1_standing.png` V-taper), and shield codpiece.
  - Tactical gear: upgraded duty belt with quick-release gunmetal buckle and double modular ammo pouches on both hips (`pouch_right_front/back`, `pouch_left_front/back`).
  - Legs & boots: painted dual wrap-around tactical harness straps with front snap buckles on each thigh (`paintThigh`), molded composite knee pads with flex-joints, lateral outer calf armor guards (`calf_outer_right/left`), and heavy treaded combat boots.
  - Knives & Idle 2 animation: refined tactical combat daggers (5.2 texels with steel fuller and cutting edge) and posed Idle 2 to directly recreate `ref2_daggers.png` (tightly tucked elbows, fists at collarbones, dual blades raised upward and angled inward in a defensive V-guard).
- Verified in headless Brave via `#cliptest`: 0 clips (`clips: {}`), 0 bad UVs (`badUV: []`), zero ground penetration across all animations (`ground: { idle: 0, idle2: 0, walk: 0 }`), and 41 cubes exported to `.bbmodel`.
- Cleaned test renders from `references/black_noir/` per Rule 15.

## 2026-10-14 22:48
- Organised folders: `Drop kick - Extreme Slow Motion.mp4` (root) -> `references/_unsorted/`; old non-final `take_ur_stuff_and_get_out_of_my_seat_please.mp3/.ogg` -> `archive/the_king/sounds/`; `scratch/save_shot.ps1` -> `tools/save_shot.ps1` (with header comment).
- New `sounds/the_king/take_ur_stuff_and_get_out_of_my_seat_please_final.ogg` (from the final mp3, 112 kbps like the others): every King sound now exists as .ogg + .mp3.
- `mobs/the_king/thekingv3.html`: voice lines + music pick .ogg (or .mp3 backup) with one `AUDIO_EXT` check; `tools/lipsync.html` uses the new .ogg. All 4 sounds load (tested).
- `claude.md` rule 15: layout for `references/_unsorted/`, `archive/<mob>/sounds/`, .ogg + .mp3 pairs, test output never in the project.
- Not done (delete blocked by the permission check): root `homelander.html` (exact copy of `mobs/homelander/homelander.html`), `scratch/` (old test renders), `references/homelander/shot_*.png` (test renders).

## 2026-10-14 22:39
- Built more muscular proportions and slightly bigger shoes for Homelander in `mobs/homelander/homelander.html` and `homelander.html`:
  - Torso & chest: broadened from 8x12x4 to 9x12x5 for a wide heroic chest and deeper torso; scaled belt (9.5x1.8x5.5), buckle, and collar (5.2x1.0x5.2) proportionally.
  - Arms: thickened upper arms to 4.0x5.6x4.0, lower arms to 3.8x5.6x3.8, gauntlets to 4.2x3.8x4.2, and pauldrons to 4.4x1.4x4.4; shifted shoulder pivots outward (sx * 6.8) to align with broad torso.
  - Legs: enlarged thighs to 4.2x5.8x4.2 and calves to 4.0x5.0x4.0; widened hip stance (sx * 2.2).
  - Shoes / boots: scaled boots from 3.8x2.0x4.6 to 4.2x2.2x5.0 and boot cuffs to 4.6x2.8x4.6 for a slightly bigger, grounded look.
  - Cape: adjusted attachment z-depth (-2.65) and mantle width (10.0) to drape naturally around the deeper muscular torso.
- Verified in headless Brave via `#cliptest`: 0 clips (`clips: {}`), 0 bad UVs, positive ground contact, and saved updated renders in `references/homelander/shot_idle.png` and `shot_walk.png`.

## 2026-10-14 22:37
- King dance rebuilt as a 1:1 copy of the reference video 0:14.0-0:19.0 (5.0 s, key 4) in `mobs/the_king/thekingv3.html`: keyframes per 0.1 s from the video frames (napkin swing, hand on hip, two steps in, napkin wave, crouched hops on the music hits, wiping, deep crouch with dangling arms, hands to the face). Crossfades on loop (`loopFade`).
- Dance music: `sounds/the_king/backgroundmusic.ogg` (mp3 fallback) plays during the dance, as the master clock (dance eases onto it, jumps back on loop), fades out after. Muted by the speaker button.
- Voice lines 65% quieter (volume 0.35) + volume slider in the dock (saved in the browser).
- Ground clamp in `step()`: soles never sink below y = 0 (crossfades from a crouch used to dip 0.37).
- Head-follow fades out while the hands are at the face (no side hair into the arms). Clip test: no new clips; ground 0 in all 4 anims; rest pose 0 pixels changed.
- New test mode `#strip-<anim>-<variant>-<from>-<to>-<step>[-<camDeg>]` (filmstrip JPEG); `#cliptest` also reports when the lowest foot happened.
- `tools/imgdiff.ps1` creates its out folder. `claude.md`: re-organise + optimise all folders at every request (rule 15); PowerShell alias, ffmpeg path and filmstrip notes.

## 2026-10-14 22:32
- Removed hanging sideburn strips from hair in `mobs/homelander/homelander.html` and `homelander.html`:
  - Front face: removed vertical sideburn pixel columns ($x = 0$ and $x = 7$ for $y \ge 1$), keeping only the combed-back hairline along the top forehead edge ($y = 0$).
  - Side faces: cleaned up front column ($x = 0$) below crown level so cheeks, temples, and ears are completely unobstructed.
- Verified in headless Brave via `#cliptest`: 0 clips (`clips: {}`), 0 bad UVs, positive ground contact, and saved updated renders in `references/homelander/shot_idle.png` and `shot_walk.png`.

## 2026-10-14 22:28
- Applied single uniform skin tone and fixed slickback hair in `mobs/homelander/homelander.html` and `homelander.html`:
  - Single skin color: removed multi-tone noise, cheek/jaw shading, and brow gradients in `paintHead`; every skin pixel on the head cranium and eyelids now uses exactly one uniform tone (`#f4c7b3`).
  - Slickback hair: removed protruding `hair_quiff` 3D block; configured `hair_main` as an 8x5x8 outer shell with 0.25 inflate; updated `paintHair` with combed front-to-back crown strands, subtle honey-amber parting groove, clean unobstructed forehead hairline, and horizontal side sweep.
- Verified in headless Brave via `#cliptest`: 0 clips (`clips: {}`), 0 bad UVs, positive ground contact, and saved updated renders in `references/homelander/shot_idle.png` and `shot_walk.png`.

## 2026-10-14 22:14
- Fixed arm clipping, head top clipping, and closed mouth in `mobs/homelander/homelander.html` and `homelander.html`:
  - Fixed arm clipping: resized upper arm to 3.6x5.2x3.6 and pauldron to 4.0x1.2x4.0; positioned pauldron strictly below head height (11.7 < 12.0) and outside torso (4.2 > 4.0); increased elbow joint gap to prevent gauntlet penetration.
  - Fixed head top clipping: enlarged outer hair box to 8.5x5.2x8.5 (top at 8.30 > 8.0 cranium top) and added solid combed hair top face painting in `paintHair`, eliminating transparent roof and z-fighting.
  - Closed mouth: replaced bright open ruby-red mouth rectangle with a thin closed sinister lip line in natural skin lip tones (`#a36456` / `#b57366`), removing white teeth patches.
- Verified in headless Brave via `#cliptest`: 0 clips (`clips: {}`), positive ground clearance, and re-captured clean visual renders in `references/homelander/shot_idle.png` and `shot_walk.png`.

## 2026-10-14 22:04
- Fixed cape clipping and face texture artifact in `mobs/homelander/homelander.html` and `homelander.html`:
  - Damped 5-segment cape physics and animation wave rotations so cumulative bend stays natural (~0.25 rad total) and never curls over 90 degrees.
  - Clamped sideways spring swing (`z: [-0.015, 0.015]`), completely preventing the cape from curling through the hips or arms.
  - Restricted spring pitch to non-negative angles (`x: [0.0, ...]`), preventing the cape from swinging forward into thighs or buttocks.
  - Cleared dark brow shadow patch on nose bridge so only eye sockets are shaded.
- Verified in headless Brave via `#cliptest`: 0 clips (`clips: {}`), positive ground clearance, and re-captured clean visual renders in `references/homelander/`.

## 2026-10-14 21:55
- Refined Homelander model and animations per request in `mobs/homelander/homelander.html` and `homelander.html`:
  - Removed neck box: head mounted directly on torso collar at y = 12.0 for vanilla humanoid player proportions.
  - Fluent 5-segment cape: upgraded from 3 to 5 articulated segments with cascading phase delays and tapering physics springs.
  - Upgraded hair: combed golden blonde hair with left side part, sideburns, and 3D extruded front quiff volume.
  - Menacing facial look: heavy under-brow shadow, cold cyan/navy eyes, sharp inward V-furrow eyebrows, chiselled jaw contours, and psychopathic smirk.
  - Eliminated all geometry clipping (0 clips reported in `#cliptest`), tuned pauldron height and outward arm swing flare.
- Captured visual render shots in `references/homelander/shot_idle.png`, `shot_walk.png`, `shot_attack.png`, `shot_special.png`, and `shot_back.png`.

## 2026-10-14 21:13
- Built single-file HTML/CSS/JS 3D Vanilla+ Minecraft model viewer for Homelander in `mobs/homelander/homelander.html` and `homelander.html`.
- Strict 16x16 humanoid box model with layered 3D details:
  - Slicked-back golden blonde hair outer layer with cutout face window.
  - Sculpted golden eagle shoulder pauldrons and eagle belt buckle.
  - Red superhero gauntlets with gold chevron patterns and tall red boots with golden eagle shin insignia.
  - 3-segment articulated American flag cape (navy star mantle + vertical red/white stripes).
  - Glowing red heat-vision laser eye VFX emitters and point light.
- 4 selectable animations with smooth easing, secondary motion, and spring-driven cape physics:
  - `Idle` (key 1): Arrogant power stance, deep superhero breathing, chin raised high, subtle blinks and smirks.
  - `Walk` (key 2): Authoritative 1.15 strides/s superhero stride with grounded foot contact and cape billowing.
  - `Attack` (key 3): Superhuman wind-up and explosive snap right punch with combat recoil.
  - `Special` (key 4): Levitation hover with fierce heat-vision laser eyes sweeping targets.
- Minimalist dark UI (#0f1115) with glassmorphic bottom pill bar, eye-follow toggle, color variants (Classic, Battle Damaged, Comic Book), camera reset (key R), and Blockbench (.bbmodel) export.
- Verified 0 clips and positive ground clearance in headless Brave via `tools/cdp.ps1`.

## 2026-10-14 20:26
- Improved walk cycle animation in `3d_voxel_character_viewer.html`, `mobs/fictional_leader/index.html`, and `fictional_leader/index.html`:
  - Cadence refined to a deliberate 1.15 strides/s for authoritative presence.
  - Pelvic step-contact dip keeps feet grounded without dipping below the floor.
  - Lateral weight transfer (`pelvisX`) shifts center of gravity onto the supporting stance foot.
  - Pelvic list (`pelvisRotZ`) with lateral spine counter-roll (`torsoSideRoll`) for natural pelvic motion.
  - Thoracic counter-twist (`torsoTwist`) rotates shoulders opposite to hips.
  - Stabilized head tracking with horizon lock and subtle step nod.
  - Asymmetric forward-biased arm swing (carrying angle inward on forward stroke, flare on sides for coat clearance).

## 2026-10-14 20:20
- Replaced `mobs/fictional_leader/index.html` (and `fictional_leader/index.html`) with `3d_voxel_character_viewer.html`.
- Improved suit styling to match reference photo:
  - Tailored warm olive-khaki / field-tan wool palette across jacket, articulated lapel plates, and matching trousers.
  - Double-breasted jacket front with diagonal fold and two columns of 3 cream/ivory buttons (6 buttons total).
  - High-collar white dress shirt with champagne / golden-tan silk necktie.
  - Left-chest historical military decorations (circular gold pin, Iron Cross cross-pattÃ©e, oval badge) with no political insignia or extremist symbols.
  - Sleeve cuffs with matching cream buttons.
- Preserved head meshes, hair, mustache, facial features, and all animations (Idle, Walk, Attack, Special) unchanged.

## 2026-10-14 20:08
- Built single-file HTML/CSS/JS Minecraft 3D model viewer for fictional 1930s European authoritarian leader in `mobs/fictional_leader/index.html` (and `fictional_leader/index.html`).
- Hierarchical box geometry (Blockbench standard), pivot rigging, strict 16x16 texels per block density.
- Procedural pixel-art canvas textures with NearestFilter: neatly side-parted dark hair, small rectangular dark mustache, period gray wool suit, white dress shirt collar, dark necktie, charcoal trousers with center crease, black dress shoes. Neutral presentation with no insignia, symbols, slogans, or glorification.
- 4 selectable animations: Idle (breathing + gentle head movement), Walk (alternating leg strides + arm swings + torso counter-twist), Attack (generic arm swing with no weapon/target), Special (lapel adjustment returning to idle; no political gestures).
- Minimalist dark UI (#0f1115) with floating glassmorphic bottom pill bar and camera reset button.

## 2026-10-14 19:35
- Project reorganized: `mobs/the_king/thekingv3.html`, `archive/the_king/thekingv2.html`, `references/the_king/` (photos, videos), `sounds/the_king/` (voice lines), `tools/`, `docs/`. Deleted exact duplicate `cussing_long_final (1).ogg`; removed empty `files/`.
- King bandana: one object on the head bone (fold + new thin pointed tip, cutout triangle); old separate tail block + spring removed. No more bandana/shirt/collar clipping on head turns.
- Pupils rest centered and move together (was cross-eyed).
- New Dance animation (key 4, from reference video 0:15-0:19); also plays by itself now and then from Idle / Idle 2.
- Voice lines (3, from `sounds/the_king/`) with lip sync: speaker toggle in the dock, key 5 = say one now, random lines when on. Mouth opens above the bandana, jaw drops, brows frown while talking.
- `.bbmodel` export now also bakes Dance. `#shot` takes an optional talk level.
- New `tools/lipsync.html`; `tools/cdp.ps1` takes extra browser flags.
- ffmpeg 9.0.2 installed (winget `Gyan.FFmpeg`). Global rules: keep projects organized; use ffmpeg for media. Project `claude.md`: folder layout, pupils, voice/lip-sync, one-piece mask rules.

## 2026-10-14 19:27
- Global rules (`~/.claude/CLAUDE.md`): new "Minecraft" section: all Minecraft work targets 1.21.1 + NeoForge.

## 2026-10-14 19:15
- New `files/thekingv3.html`: v2 model (`thekingv2.html`) exactly + v1 animations, face, springs, UI, tests. Replaces `files/TheKing.html` (removed).
- Face parts as thin planes on v2's face pixels (brows, eyes + pupils, lids, mouth); bandana works as a jaw.
- Rig: new empty `root` bone; `bandana_tail` own bone hanging from the neck; head yaw 0.35 / 0.25 (walk), pitch 0.08 up / 0.03 down; head lifts slightly on pitch/turn.
- Walk: knees bend in the swing, ankles keep shoes flat, bounce moved to the torso (no floating feet). Idle 2: real hand-on-hip with the elbow.
- Export: free format, per-face UV, v2 sizes kept. Atlas 128x128 with edge gutters.
- New `tools/cdp.ps1` (headless Brave via DevTools) and `tools/imgdiff.ps1` (pixel compare); `#rest` test mode.
- `claude.md`: mod only when asked; new "Merging versions" section; never Blockbench; testing via `tools/`; dead end `--dump-dom`; three.js zero-size box normal note.

## 2026-10-14 19:08
- Global rule added in `~/.claude/CLAUDE.md`: max 3 tries per approach, then log it under the new `# Dead ends` section and find another way. Next time: check if the dead end works now or is fixable first.

## 2026-10-14 18:23
- Rewrote `files/TheKing.html` to follow `claude.md` (1218 lines, was 1761).
- Animations now Idle / Idle 2 / Walk (keys 1-3). Removed Attack, Special and the car-wheel prop (not asked for).
- Face parts: blinking lids, gliding pupils, moving brows, shape-changing mouth, all smoothed.
- Springs on back hair, side locks, bandana knot, crown. Head + eyes follow the mouse (eye toggle in dock).
- 3 color variants (Classic, Mint, Royal), dots in the dock. One 128x128 box-UV texture atlas.
- Player-style body (8x12x4 body, 3x11x3 arms, 4x12x4 legs), head lifted 0.7 on the scarf. Removed nose, elbows, knees.
- 120/60 FPS lock, delta-time motion, no Tailwind (plain CSS), rules UI (download + reset top right).
- `.bbmodel` export: Modded Entity, box UV (fits the Java `texOffs` style), animations baked with springs and face.
- Self-tests in the page: `#cliptest` (clipping + export check), `#shot-<anim>-<variant>-<sec>` (one rendered frame).

## 2026-10-14 18:20
- Sharpened Edit/Write rule in global `C:\Users\quepr\.claude\CLAUDE.md`: Grep + `Edit` for existing files, `Write` only when truly needed (new file / full rewrite); tools used freely but only when needed. No project files changed.

## 2026-10-14 17:41
- Added "Work & autonomy" section to global `C:\Users\quepr\.claude\CLAUDE.md`: research + web search, free tool/library use and installs, start apps to test, always test + fix, ask only when needed.
- Sharpened subagent rule: never unless heavily needed, max 2 at once. No project files changed.

## 2026-10-14 17:40
- Set context window to 500k + auto-compact at 500k in `C:\Users\quepr\.claude\settings.json` (`autoCompactEnabled`, `autoCompactWindow: 500000`).
- Added matching rule to global `C:\Users\quepr\.claude\CLAUDE.md` (Context & tokens). No project files changed.

## 2026-10-14 17:31
- Added caveman-talk rule to global `C:\Users\quepr\.claude\CLAUDE.md` (Answer economy). No project files changed.

## 2026-10-14 17:28
- Analysed all project files and explained the rules in `claude.md`. No code changed.
- Started this changelog (`docs/changelog.md`) and the session log (`docs/latest.md`).
