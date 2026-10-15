# The King

A loud, proud man with a crown, long hair and a red bandana.
He picks fights, dropkicks people, and dances on every kill.

| | |
|---|---|
| Viewer (source of truth) | `mobs/the_king/thekingv3.html` |
| Mod | **The King: Crowned Brawler** in `mod/` (NeoForge 1.21.1, 21.1.251 or newer, mod id `theking`) |
| In Modrinth | instance "take a guess": `mods/the-king-crowned-brawler-1.0.0.jar` |
| Built jars | `builds/theking-<version>-<date-time>.jar` (newest 5 kept) |
| Test the mod | `run.txt` in the project root |

## Look

- Body style: humanoid (player model: head 8x8x8, body 8x12x4, arms and legs 4x12x4).
- Gold crown with a spring (it wobbles), long hair, red bandana over the mouth and neck.
- Moving face: eyes blink and glance, brows raise and frown, mouth talks (lip sync).
- Physics: hair, bandana tips and crown swing on springs, also while standing still.
- 3 color variants: **classic**, **mint**, **royal**. A spawned King gets a random one; it is saved with the world.

## Animations

| Key | Name | Length | What he does |
|---|---|---|---|
| 1 | Idle | 4 s | Calm breathing, small sway. |
| 2 | Idle 2 | 6 s | Looks around, fidgets. Plays now and then instead of Idle. |
| 3 | Walk | 2 s loop | His own swagger walk. Speeds up when he runs. |
| 4 | Dance | 5 s | Victory dance, 1:1 with the reference video, with music. |
| 5 | Attack | 3.4 s | Leap + dropkick (made from the slow-motion dropkick video). |
| 6 | Attack 2 | 1.1 s | A simple, heavy punch. |
| 7 | Run | 1.125 s loop | A heavy sprint: leaned forward, heels kick up, knees drive, both feet off the ground between steps, arms pumping, face full of effort. |

- Attack timing: takeoff at 0.45 s, feet hit at 1.02 s, lands on his back at 1.5 s, stands up after.
- Attack 2 timing: the fist hits at 0.42 s.

## Behavior (as a real mob)

- **Hostile.** He hunts players within 32 blocks and fights back against anything that hurts him.
- **Runs** at his target while it is more than 4.5 blocks away (30% faster, Run animation); walks the last few blocks.
- **Dropkick (Attack):**
  - When the target is 3 to about 5 blocks away (as far as his leap reaches), on about the same height, in sight and with a clear path.
  - He crouches, leaps and kicks with both feet: damage = attack damage x 1.6 (= 9.6), strong knockback.
  - At takeoff he aims again at where the target will be, so stepping aside a little does not save you.
  - After the kick he lies on the ground for a moment: the best time to hit him back.
  - Cooldown: 7 to 10 seconds.
- **Punch (Attack 2):** when the target is in melee range. 6 damage. About 1.5 seconds between punches.
- **Dance on a kill:** **always exactly 1 second after a kill** he does his victory dance, with music.
  - Hitting him while he dances stops the dance (and the music).
- **Voice lines:** **sometimes**, at random moments (on average about every 18 seconds), he says one of his lines:
  - "cussing (short)", "cussing (long)", "take your stuff".
  - Never the same line twice in a row. His mouth moves with the words.

## Stats

| Stat | Value |
|---|---|
| Health | 40 (20 hearts) |
| Attack damage | 6 (punch), 9.6 (dropkick) |
| Armor | 2 |
| Speed | 0.2 (a bit slower than a zombie; strolls at 70%, runs at 130% when the target is far) |
| Knockback resistance | 30% |
| Follow range | 32 blocks |
| Size | 0.7 wide, 1.95 tall |
| XP when killed | 10 |
| Drops | 1-4 gold nuggets (+1 per Looting level) |

## Spawning

- In plains, sunflower plains, savanna, forest and meadow. Rare (weight 5, then a 1 in 3 chance).
- Always alone: never within 48 blocks of another King.
- Day and night (light does not matter). Not on Peaceful.
- Spawn egg: in his own creative tab **"The King"**, and also in the vanilla Spawn Eggs tab.

## Settings (in game: Mods -> The King -> Config)

| Setting | Default | What it does |
|---|---|---|
| naturalSpawns | on | He spawns by himself in the world. |
| dropkick | on | He uses the dropkick. Off = punch only. |
| dropkickDamageMultiplier | 1.6 | Dropkick damage = attack damage x this. |
| danceOnKill | on | Victory dance 1 s after a kill. |
| voiceLines | on | Random voice lines. |
| voiceVolume (client) | 0.6 | Voice line volume. |
| musicVolume (client) | 0.45 | Dance music volume. |

Sounds play in the "Hostile Creatures" volume slider and have subtitles (English and Dutch).

## How the mod stays 1:1 with the viewer

1. The HTML viewer is the source. All model and animation changes happen there first.
2. `tools/export_mod_assets.ps1` opens the viewer headless (`#modexport`) and writes:
   - `mod/src/main/resources/assets/theking/king/rig.json`: bones, cubes, UVs, springs, face settings, voice curves;
   - `anims.bin`: every animation sampled 120 times per second;
   - the 3 variant textures, and the sounds as mono `.ogg` (mono = the sound comes from his position);
   - `mod/src/test/resources/modtrace.json`: a recording of the viewer's own runtime.
3. The Java animator (`KingAnimator.java`) is a line-by-line port of the viewer's runtime: crossfades, head follow, blinks, springs, ground clamp.
4. `gradlew test` replays the recording in Java and compares every bone in every frame.
   Last result: 592 frames x 30 bones (all 7 animations), max difference 0.00012 rad rotation and 0.0003 texel position (invisible).

Performance: no new objects per frame, one animator per King, faces culled, nothing done for Kings off screen.
