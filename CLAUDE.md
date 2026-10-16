# Rules for making custom Minecraft mobs and mods

**Team repo: before your first change in a session, read `AGENTS.md` (git workflow for every person and AI).**
@AGENTS.md

Rule set for Claude. It works in any project, for anyone who makes custom mobs and mods.
Put this file in the root of your project and name it `CLAUDE.md` (or paste it into your existing `CLAUDE.md`).
These rules count for every mob and every mod, unless the user clearly says otherwise for one mob or mod.

## Permanent project rules (user rules, 2026-10-16)

Always apply these, every session, without being reminded:

- **No sub-agents.** Work directly in the main loop. Spawn a sub-agent only when the user explicitly asks for one.
- **Commit + push always**, with a clear, clean, short, accurate title (`<area>: <what changed>`) and a short description (details: GitHub section below).
- **Co-Authored-By trailer on every commit and every squash-merge**: the last line names Claude and the exact model, e.g. `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. `.claude/settings.json` keeps `"includeCoAuthoredBy": true`.
- **Viewer buttons:** no "Download .bbmodel" / "Download Blockbench Model" button in any HTML viewer, ever; the "Show bones" toggle (key `B`) is in every HTML viewer, always (rule 10).
- **Expression menu, ALWAYS, every character** (user rule, 2026-10-16): every HTML viewer has an expression menu next to the camera-reset button (top right): `Auto`, `Happy`, `Sad`, `Angry`, `Afraid`, each with its icon, in the viewer's own pill style. A pick forces that face (eyes, brows, mouth eased in, never snapped) until `Auto` gives the face back to the animations (rule 10). A viewer gets it when its mob is worked on.
- **Shared viewer files** (user rule, 2026-10-16): `mobs/_shared/viewer.css` (UI look), `mobs/_shared/viewer.js` (UI parts every mob shares: expression menu) and `mobs/_shared/viewer_template.html` (start page for a new mob). A viewer links the css + js; shared UI is changed there once, never copied into the pages (rule 10).
- **Only touch the mob you were asked about** (user rule, 2026-10-16): never edit another character's viewer, files or mod code unless the user names it (other people and agents work on them).
- **Every request ends with:** re-organise + optimise all files and folders (rule 15), then commit + push yourself (GitHub section). Never ask first, never skip.
- **Edits** (user rule, 2026-10-16): use the `Edit` tool (the user sees the diff) or a script (`node`, `sed`), whichever gives the best quality and is fastest at that moment.
- **Other characters' viewers are never edited by hand** (user rule, 2026-10-16): only the mob the user asks about; shared UI changes go into `mobs/_shared/` (html/css/js), never into the other pages.
- **Branch cleanup:** only your own branches (`tivek87/*`, own `backup/*`); a branch of `T13510783` or anyone else only when the user explicitly allows it (2026-10-16: allowed for all branches: finish/merge their work into `main`, then delete, never lose it).
- **Hygiene:** root, folders and git branches stay clean: files in logical folders, finished branches merged and deleted, no leftover or obsolete files, no stale branches or worktrees (rule 15, `AGENTS.md` section 7).
- **1-on-1 parity:** every mob in Minecraft is 100% identical to its HTML viewer: model, bone count, idles, face, attacks, audio / voice lines, VFX and physical interactions (rule 23).
- **Releases and versions** (rule 20):
  - GitHub Release tag/title and jar name end in `-alpha`, e.g. `v0.0.1-alpha`, `sindicate-0.0.1-alpha.jar`. The mod is called **Sindicate** (mod id `sindicate`, user rule 2026-10-16); mob namespaces stay `theking`, `homelander`, ...
  - Each version part goes up to 99, then rolls over into the next part: `0.0.99` -> `0.1.0`, `0.99.99` -> `1.0.0`; maximum `v99.99.99-alpha`.
  - Locally (`builds/`) only the newest 5 jars stay; on GitHub Releases every jar stays forever (never delete a release).
  - Every release changelog (GitHub Release + in-game updater) has exactly three sections: `Added`, `Removed`, `Fixed`. Short bullets, nothing else.
- **After every in-game Minecraft test** (user rule, 2026-10-16): commit + push, merge the PR, then release (`tools\release.ps1 -Bump` from merged `main`). Never end a turn with a tested but unreleased mod change.
- **Commit everything** (user rule, 2026-10-16): when asked, everything that is not git-ignored goes in (other people's mobs, references, images too); never `.env`, build output or test renders.

## GitHub repo and teamwork (user rule, 2026-10-15)

Full commands in `AGENTS.md`. Short version, always:

- **HARD RULE (user rule, 2026-10-15): after EVERY prompt and EVERY change: pull, commit, push. No exceptions.**
  `git fetch` -> commit own files -> `git pull --rebase` + `git rebase origin/main` -> fix any conflict (see below) -> `git push`.
  Make/switch branches and open/merge PRs whenever needed. A turn never ends with unpushed work or an open conflict.
- **Claude ALWAYS commits and pushes itself (user rule, 2026-10-15).** The user never has to do it and is not asked first; this project rule beats any "ask before commit/push" rule elsewhere.
  - Title: `<area>: <what changed>`, short (max ~72 chars), clear, simple English. Never "update", "fix stuff", "wip", "test".
  - Description: 1-4 short bullets: what, which files, why, how tested (or "not tested"). Same for PR titles/descriptions and squash-merge messages.

- **README at EVERY push (user rule, 2026-10-16):** before every `git push`, update `README.md` (the repo's GitHub front page) so it matches what is pushed: mobs table, mod name/jar, commands, features, config. Nothing changed for the reader -> still check it.
- **Repo:** `https://github.com/T13510783/custom-mobs`, public (since 2026-10-16), owner `T13510783`. Squash merge only; GitHub deletes merged branches.
- **`main` is not locked** (no branch protection set). Never commit or push straight to `main`; it changes only through pull requests (PRs).
- **Own branch** per person/agent and topic: `<github-name>/<topic>`, lowercase (e.g. `t13510783/the-king-rig`). Start it from `origin/main`.
- **Claim your work** in `docs/working_on.md` (who, branch, mob/files). One person per mob page at a time. Remove your line when the PR is merged.
- **Session start:** `git fetch --all --prune` -> switch to your branch -> `git pull --rebase` -> `git rebase origin/main` -> `gh pr list`.
- **After EVERY change:** `git status` (own files only) -> commit `<area>: <what>` -> `git fetch` + `gh pr list` -> `git pull --rebase` + `git rebase origin/main` -> `git push`. Done + tested -> `gh pr create --fill --base main`; green + no conflicts -> `gh pr merge --squash --delete-branch`.
- **Always clean up git (user rule, 2026-10-15):** after every merge and at the end of every turn, no leftovers in GitHub Desktop's branch/worktree lists. Full steps: `AGENTS.md` section 7.
  - Merged PR -> `git worktree remove` its worktree, `git branch -d` its local branch, `git fetch --all --prune`, remove your `docs/working_on.md` line.
  - Delete `backup/*` branches once the rebase/merge is verified and pushed. Never leave an unpushed worktree or branch behind.
  - Only your own branches/worktrees; someone else's stale branch -> tell the user, never delete it.
- **Commit messages (user rule, 2026-10-15):** every commit and every squash-merge always has a good message: clear, short, clean.
  - Subject: `<area>: <what changed>`, simple English, max ~72 chars, no "update"/"fix stuff"/"wip".
  - Body (after a blank line): 1-4 short bullets: what changed, which files, why, how tested (or "not tested").
  - Last line: the AI that did the work, the normal co-author trailer with its model, e.g. `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
  - Squash-merge: pass the same clean subject + body + trailer (`gh pr merge --squash --subject "..." --body "..."`), never GitHub's raw commit list.
  - Project setting `.claude/settings.json` has `"includeCoAuthoredBy": true`.
- **Merge conflicts:** never leave or push them.
  - First a safety copy: `git branch backup/<github-name>-<yyyyMMdd-HHmm>`.
  - Keep both sides' intent, never blindly "mine" or "theirs".
  - `docs/changelog.md`: keep all entries, newest on top. `docs/latest.md`: keep the newest. `docs/working_on.md`: keep all lines.
  - Binary files (png, jpg, ogg, mp3, mp4, jar) can't be merged: keep both, ask the user which one wins.
  - Re-test what you touched (`#cliptest`, `gradlew build`), then `git rebase --continue`.
  - Not safely solvable -> stop, keep the backup branch, tell the user.
- **Safety:**
  - Push rejected -> `git pull --rebase`, never force. `--force-with-lease` only on your own branch, never on `main`.
  - Uncommitted work before a pull or branch switch -> commit or stash it first.
  - Never `git reset --hard`, `git clean -fd`, `git checkout -- .`, or delete someone else's branch or commits.
  - Never commit `.env`, tokens, passwords, build output (`mod/build/`, `mod/run/`, `builds/`) or test renders. `.env.example` is local only too (user rule, 2026-10-16: git-ignored, never commit it); the settings it holds are listed in `README.md`.
  - No file over 50 MB (GitHub refuses 100 MB). Bigger media -> shrink it with ffmpeg first, or ask.
  - Never without the user's OK: change repo visibility, settings or collaborators, delete the repo, merge someone else's PR.
- **What is in git:** viewer pages, references, sounds (`.ogg` + `.mp3`), archive, tools, docs, the mod's source + Gradle wrapper. Text files are LF (`.gitattributes`), `.bat`/`.ps1` CRLF.
- **Login / tools:** git + GitHub CLI (`gh`) are installed. Login (only if `gh auth status` fails): the user runs `& "C:\Program Files\GitHub CLI\gh.exe" auth login --web` in a new PowerShell window. Claude may run any `git` / `gh` command without asking (user settings).
- **New teammate:** owner invites them (GitHub repo -> Settings -> Collaborators). They run `gh repo clone T13510783/custom-mobs`; their AI reads `CLAUDE.md` + `AGENTS.md` first.

## How Claude talks to the user

- Use very simple language. Short sentences. One idea per line. Lists and tables, not long paragraphs.
- Explain step by step, in depth, but easy: like explaining to someone who has never done it.
- Every technical word gets a plain explanation right after it. Example: "pivot (the point a part turns around)".
- Always say WHAT you did, WHERE (the full file path) and WHY.
- Done and tested -> say it plainly. Not tested -> say that too.

## Workflow: from idea to mod

Every mob goes through these steps, in this order:

1. **HTML first.** Every new mob starts as one HTML page: a 3D viewer of the model (three.js) with its animations and color variants. Start from a copy of the base mob and keep its systems.
2. **Change the HTML when asked.** All changes to the model, textures or animations go into that HTML page first. Check them there.
3. **Final version confirmed -> save as `.bbmodel`.** When the user says the mob is final, always also save it as a Blockbench file (`.bbmodel`, the file format of the Blockbench model editor) in the `models/` folder: same cubes, same pivots, same textures, same animations.
4. **Then into the mod.** Only after that, and only when the user asks for it, put the mob into the mod (the `.jar`, the file Minecraft loads), using the confirmed model. Until then: HTML only, no Java, no mod. (2026-10-14)
5. Never skip a step, never change the mod model without changing the HTML and `.bbmodel` too.

`.bbmodel` notes (it is a JSON text file): use the "Modded Entity" format with box UV and whole-texel cube sizes (the Java model uses `texOffs`); every bone = a group with its pivot at the joint; cubes with `from` / `to` / `origin` in texels; the textures embedded as PNG; the animations as keyframes. Exception: a model the user gave with non-whole cube sizes keeps them -> "Generic Model" (free) format with per-face UV, never rounded. Check the file by its own export test, not in Blockbench (see Testing).

## Merging versions (2026-10-14)

When the user says "model from A, animations from B":
- The model of A stays exactly as it is: same cubes, sizes, pivots, rest rotations, textures, colors. Only the smallest edits that are truly needed.
- Make eyes, brows and mouth movable without changing the look: thin planes on the exact painted face pixels, plain skin painted under them, so the rest pose looks the same as A.
- Take B's animations, face system, springs, UI and tests; map B's bone names onto A's rig. New bones only as empty groups or as a parent around an existing cube at the same spot.
- Prove it: render A and the merge in the rest pose with the same camera and compare them pixel by pixel.
- The merge replaces B's file, under the name the user gives.

## Animations

1. Every mob always has exactly **Idle, Idle 2, Walk** (keys `1`, `2`, `3`).
2. **Never an attack or special animation** (or any other extra) unless the user asks for it for that mob. Never add one on your own, not even when it seems to fit. Asked-for extras go after Walk (keys `4`, `5`, ...).
3. The 2 idles must clearly differ (e.g. calm breathing vs. looking around / fidget), not the same motion with other numbers.
4. **Every animation is unique to its mob.** Each mob's walk is its own walk, never copied from another mob (no shared keyframes, timing, poses or rhythm). Only exception: the same base model with small tweaks (other outfit, color, prop) may share animations.
5. Design each animation for that mob's body, weight and character.
   - A run is not a fast walk (2026-10-15): the body is lowest at mid-stance and highest in a flight phase (both feet off the ground), never lowest with the legs spread (that reads as walking). Build it with a foot path + 2-bone IK (hip and knee solved from where the foot must be): stance slides back on the ground, swing kicks the heel up and drives the knee forward.
   - No hard corners in motion curves: `abs()`, a bare `max(0, x)`, hard clamps. Use `x*x`, `max(0, x)**2`, softplus limits. Corners look jerky and break the mod's 1:1 test.

## Face

6. **Eyes, eyebrows and mouth (whatever the mob has) move in every animation**, idles included:
   - eyes: blink at random intervals (now and then a double blink) + glance left/right;
   - eyelids (user rule, 2026-10-16): ALWAYS make and animate them whenever a mob gets eyes / brows / a face: real lid parts that slide over the eye in every blink, squint and closed-eye moment, in every animation, high quality (see rule 7);
   - eyebrows: raise / frown with the motion;
   - mouth: open/close or change shape (breath, snarl, shout, yawn, grin).
7. **Always very smooth, never clunky or teleporting:**
   - no instant 0/1 steps, no parts popping in at full size, no snapping to whole texels;
   - a blink is the lid sliding down and up (scale from a pivot on the lid's top edge, ~0.18 s sine bump);
   - eyes glide between glance positions (filtered value);
   - brows, mouth and eyes-closed run through a per-frame smoothing filter (`v += (target - v) * (1 - exp(-dt * 16))`), so keyframe jumps, loop wraps and crossfades never snap;
   - closed-eye channels ramp (e.g. `clamp((x - 0.3) * 3, 0, 1)`), never `x > 0.4 ? 1 : 0`;
   - face moves take at least ~0.08 s; talking/chanting mouth flaps at most ~4 per second.
   - pupils rest in the middle of the eye white and move together; never rest on the inner pixels (looks cross-eyed). (2026-10-14)
   - voice lines: mouth from a loudness curve made beforehand with `tools/lipsync.html` (30 values/s) and read at `audio.currentTime`; live audio analysis gives zeros on `file://` pages. Sound only starts after the first click/key (browser rule). (2026-10-14)
   - a mask / bandana / scarf on the face = one object on the head bone, its parts never move apart. (2026-10-14)

## Physics and clipping

8. **Everything loose has physics:** beard, hair, cape, cloak, robe flaps, sleeves, tails, ears, vines, straps, hanging gear. Spring-driven from body velocity/rotation, always moving a little, even in idle. The beard moves in every animation. Always clamp spring results.
9. **No clipping:** no part may pass through another in any animation, frame or color variant. Only exception: the beard, or a spot where a little is truly unavoidable, kept minimal.
   - Check it numerically, not by eye: per frame, sample a 5x5 grid of points on every face of part A, transform them into part B's local space, count points inside B by more than 0.06. Test every animation with the mouse pointer at the center, edges and corners (head-follow extremes).
   - Rig lessons that avoid most clipping:
     - arm pivot on the arm's top edge (not 2 below it), arms only rotate outward on Z;
     - head on top of the body: lift it ~0.7 on a recessed neck block and clamp head pitch to about ±0.08 rad; arm tops ~0.4 below the body top;
     - no neck wanted (Homelander, 2026-10-16): head sits right on the torso top, never lifted; soft tilt limits instead (`limit * tanh(x / limit)`, pitch ~0.12, roll ~0.06 rad) keep the chin dip into the torso under ~0.5;
     - a cape with hands behind the back: the hands rest ON the cape (forearms push cloth toward the back, upper arms toward the spine, torso pushed last) and the cape's top edge narrows to fit between the arms;
     - a long beard below the chin hangs from the torso (following the chin), not from the head;
     - poses with arms beside the head fade head-follow out before the arms move;
     - hanging back hair only swings backward and ends above the sleeve tops; hands end above the leg tops (2026-10-14);
     - eyelids rest just inside the head (hidden) and step out only while closing, so the default pose has open eyes (2026-10-14).

## Look and UI

10. **Same UI for every mob, always:** dark background (`#0f1115`), same font and color tokens, floating pill bar at the bottom center (animation buttons, follow toggle, color-variant dots), small camera-reset button top right, orbit camera controls, number keys + `R` for reset. No new layouts, no other libraries.
    - **No "Download .bbmodel" button** (user rule, 2026-10-15). The page keeps its `.bbmodel` export code: Claude saves the file to `models/` with the page's headless export mode (workflow step 3).
    - **Always the expression menu** (pill button `#expr` with face icon + word, glass list, next to the reset button, from `mobs/_shared/viewer.js`): Auto / Happy / Sad / Angry / Afraid. The page registers its face with `MobViewer.init({ ... })` and reads `MobViewer.expression` each frame; the forced face goes through the same smoothing filter as the rest of the face.
    - **Shared files:** `mobs/_shared/viewer.css` + `mobs/_shared/viewer.js` (classic `<link>` / `<script src>`, loaded before the page's own module). New shared UI goes there, not into each page.
    - **Always a "Show bones" button, in every viewer page, every mob, no exceptions** (bone icon in the dock, key `B`, user rule 2026-10-15): draws the rig on top of the model (a dot on every bone pivot, a line to its parent), updated live in every animation.
11. **Follow-mouse toggle in every mob:** head and eyes follow the mouse pointer; an eye-icon button in the dock turns it on/off. On by default. When off, head and eyes ease back to the animation's own pose, never snap.
12. **Two humanoid body styles** (style unclear for a new mob -> ask):
    - **humanoid** = player model: head 8x8x8, body 8x12x4, arms + legs 4x12x4, no nose;
    - **villageroid** = villager model: head 8x10x8 with the 2x4x2 nose, body 8x12x6, legs 4x12x4, two separate arms 4x12x4 - not the crossed villager arms unless asked.
    - **Limb rig, every mob, always (user rule, 2026-10-15):** each arm = exactly 2 bones (`arm_<side>` upper arm, pivot at the shoulder; `forearm_<side>` pivot at the elbow); each leg = exactly 2 bones (`leg_<side>` thigh, pivot at the hip; `knee_<side>` shin, pivot at the knee). The shoe/boot is a cube on the shin bone. No hand, ankle or other extra limb bones. Vanilla friendly: plain boxes per segment, nothing bends inside a box.
    - Animations use both bones: elbows and knees bend in every walk/run/idle. Without an ankle the foot tilts with the shin (hip + knee = foot angle): that gives heel-strike / toe-off for free. Keep feet on the ground by solving the root height from the leg angles (lowest boot corner, smooth min), not by a hard clamp.
13. Minecraft / vanilla+ style: 1 unit = 1 texel, 16 texels per block, feet at `y = 0`, vanilla proportions, crisp procedural pixel-art textures (`NearestFilter`; roughness >= 0.7, glossy faces mirror the blue rim light as a blue strip and the mod has no gloss anyway (2026-10-15)), outer layers (hair, sleeves, armor) slightly bigger boxes with cutout pixels. Color variants via a palette per variant.

## Performance

14. **Everything always runs at 120 FPS when the screen can, otherwise 60 FPS.** Viewers, tools, mods: never below 60 on a normal PC.
    - Viewer loop: measure the `requestAnimationFrame` interval (time between screen refreshes) over the first ~30 frames; >= ~110 Hz -> lock to 120, else 60. All motion uses delta time (seconds since the last frame), so the speed is the same at both rates.
    - Stay cheap: pixel ratio max 2, one shadow-casting light, reuse geometry and materials, no new objects inside the per-frame update, repaint textures only when the color variant changes.

## Files and folders

15. **Always organised and sorted.** Clean folders grouped by kind, clear names, no loose test or temp files in the project. **At every request** (end of the turn): re-check all folders and files, re-organise and optimise what needs it (sort new files, delete duplicates and leftovers, shrink oversized files, fix stale paths and docs). Layout (2026-10-14):
    - `mobs/<mob>/` viewer pages (only there, no copy in the root) · `archive/<mob>/` old versions (old sounds in `archive/<mob>/sounds/`)
    - `references/<mob>/` only material the user gave (photos, videos); not yet tied to a mob -> `references/_unsorted/`
    - `sounds/<mob>/` every sound as `.ogg` (the mod) + `.mp3` (browser backup); pages pick with `canPlayType` · `tools/` test and helper scripts · `docs/` changelog + session log
    - Root holds only `README.md`, `CLAUDE.md`, `AGENTS.md`, `run.txt`, `.gitignore`, `.gitattributes`, `.env` + `.env.example` (both local, git-ignored), `.vscode/`, `.claude/` and the folders. Never a copy of a viewer or reference in the root, even if asked for "in the root": sort it in. (2026-10-15)
    - New files from the user in the root -> sort them in; exact duplicates (same hash) -> delete.
    - File names: lowercase, `_` instead of spaces, say what it is (`drop_kick_slow_motion.mp4`, not `VID_2026....mp4` or a hash). Fix every path that points to a renamed file. (2026-10-15)
    - Test output (renders, clip reports, `shot_*.txt/.png`) goes to the session scratchpad / `%TEMP%`, never into the project. (2026-10-14)
    - Deleting project files can be blocked by the permission check: then list them for the user to delete; never work around it.
16. **Max 10000 lines per file** (user rule, 2026-10-16). A mob viewer page stays ONE html file (no `js/` folder per mob); shared UI goes into `mobs/_shared/`. A file bigger than 10000 lines -> split it into a folder with parts by section (e.g. `style.css`, `painters.js`, `rig.js`, `anims.js`, `ui.js`). A split viewer page loads its parts with classic `<script src>` (no ES modules: they break when the page is opened as a local file).
17. **Every file has sections and comments.** A header comment per section (e.g. `/* ───────── Painters ───────── */`) and short comments on anything not obvious (pivots, clamps, why a value). Comments in simple English.
18. One viewer page per mob in `mobs/<mob>/` (e.g. `mobs/the_king/thekingv3.html`).
19. Every new mob starts as a copy of the base mob and keeps its systems (texture system, loop, camera, UI); only the model, colors, animations, face parts and springs change.

## Mods (.jar)

20. **Keep the last 5 builds** (user rule, 2026-10-14). `gradlew build` copies the jar to `builds/sindicate-<version>-<yyyyMMdd-HHmmss>.jar`; `builds/` and `mod/build/libs/` each keep only their newest 5 (Gradle task `copyToBuilds`, in `build.gradle`). GitHub Releases keep every version.
    - **Releases (2026-10-16):** version `X.Y.Z-alpha` in `mod/gradle.properties` (`mod_version`), every part 0..99 and rolls over at 99 (`0.0.99` -> `0.1.0`, `0.99.99` -> `1.0.0`, last `99.99.99`). Changelog = `mod/release_notes.md` with only `## Added`, `## Removed`, `## Fixed`. Publish from merged `main`: `tools\release.ps1 -Bump` (bump, build + tests, tag `vX.Y.Z-alpha`, GitHub release with the jar); `-DryRun` checks without publishing. Release after every in-game test (user rule, 2026-10-16), and whenever the user asks.
    - In-game updater (`com.custommobs.update`): "Check for Updates" button (title + pause screen), auto check every 5 min, changelog screen, "Update and Close Game" (downloads the jar, a detached helper swaps it after the game closed). The repo is public, so no token is needed (`githubToken` in `config/sindicate-client.toml` only for a private fork).
21. **Always a `run.txt`** in the project root with one command the user can copy, paste and run to test the mod (now: `cd .\mod; .\gradlew.bat runClient`, for PowerShell in the project folder). Keep it correct after every change. No absolute paths: each teammate clones to another folder (`C:\custom mobs`, `C:\custom-mobs`, ...). (2026-10-15)
22. Test the mod before saying it is done. Not tested -> say so. Claude may start Minecraft to test, without asking (user rule, 2026-10-14): `gradlew runClient` (dev client, mod already loaded) or the user's own Minecraft.
23. **Mod = the viewer, 1:1** (2026-10-14). The HTML stays the source of truth; the mod never gets its own model or animation edits.
    - Setup: NeoForge 21.1.x + ModDevGradle 2.0.x (`mod/`). JDK 21: `C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot` (machine `JAVA_HOME`).
    - VS Code red errors like "import net.minecraft cannot be resolved" (2026-10-15) = the Java extension synced before the Minecraft jar existed (e.g. right after a `neo_version` change), not a code bug. Fix: `gradlew build` first, then a build-file change reloads the project (`.vscode/settings.json` has `updateBuildConfiguration: automatic`). Check with the VS Code diagnostics tool, aim for 0 problems.
    - Build against the NeoForge version of the user's pack, not the newest (2026-10-15): a jar that needs a newer NeoForge does not load. The pack's version is in its `logs/latest.log` (`neoforge-21.1.xxx`). Range in `neo_version_range`.
    - The user's Modrinth instance "take a guess" = `%APPDATA%\ModrinthApp\profiles\SuperPowers test 1.0.0\` (NeoForge 21.1.251, Sodium, Iris, EMF/ETF, JEI, Jade, Multiverse Powers). Install = copy `mod/build/libs/sindicate-<version>.jar` to its `mods\` and delete the old `the-king-crowned-brawler-*.jar` / `sindicate-*.jar` (one jar holds every mob).
    - Test with the pack's mods: copy its jars (not Essential: needs a login) into `mod/run/mods/` (1.21.1 runs Mojang names, so release jars load in dev), run, then empty that folder again.
    - After every viewer change: `tools\export_mod_assets.ps1` (headless `#modexport`: rig.json, anims.bin baked at 120 fps, textures, mono `.ogg`, trace), then `gradlew build` (runs the 1:1 trace test).
    - The Java animator is a line-by-line port of the viewer's `step()`; `#modtrace` records the viewer, the JUnit test replays it (limits: rotation < 0.001 rad, position < 0.005 texel).
    - Baked delta data: decode with a 16-bit wrap (`(short)`), or big jumps overflow.
    - Every animation length `T` is a whole number of 1/120 s frames (e.g. 1.125, not 1.12); `#modexport` throws otherwise.
    - Melee AI (2026-10-15): never `getNavigation().moveTo(entity, speed)` to close in (accuracy 1: the path ends ~1.5 blocks away, outside melee reach, the mob just stands there). Use `createPath(entity, 0)` + `moveTo(path, speed)`, and `getMoveControl().setWantedPosition(...)` for the last step when the path is done but not in reach. Wind-up attacks: check the hit with a little extra reach (the target slides away during the wind-up).
    - Find AI bugs with a scripted fight + a once-a-second state log (target, distance, melee, action, path done), not by guessing.
    - Base animations (walk/run) are picked on the client from synced vanilla flags (e.g. `isSprinting()` = Run, +30% speed) and sped up by real ground speed / the animation's design speed.
    - Sounds: mono `.ogg` (stereo does not get a position in Minecraft). Actions start on the server and reach the client as entity events (`broadcastEntityEvent`, ids 100+).
    - Server-side root motion (leaps): the server moves the entity along the root curve; the client renderer subtracts it from the root bone.
    - Every mob: own creative tab + egg also in the vanilla Spawn Eggs tab, `en_us` + `nl_nl`, subtitles, loot table, biome modifier.
    - Config (2026-10-16): one shared system in `com.custommobs.config.SindicateConfig` (`config/sindicate-client.toml` + per-world `sindicate-server.toml`), one section per mob, every option with an English hover tooltip. Screen: `SindicateConfigScreen` (Mods list -> Config, or key `K`), Client/Server tabs; server edits go over `ConfigNet` and only work for the singleplayer owner or, when `opsCanEditConfig` is on, operators.
24. **Test in game, fast** (2026-10-14): `gradlew runTestWorld` opens `mod/run/saves/kingtest`; its datapack (source `mod/testworld/`, copy it into the save after edits) builds a stage and scripts fights. Dev runs log actions with `-Dtheking.debug=true` -> check `mod/run/logs/latest.log` for `[TheKing]`. Screenshots: `tools\winshot.ps1`. The dev world has cheats off: chat commands fail, script tests in the datapack instead. Stop the client by matching the project's `mod` folder path (e.g. `custom-mobs\mod`) in the java command line (the client's own command line also contains "gradle": never filter on `*Gradle*`, only on `*GradleDaemon*` / `*GradleWrapperMain*`). The server ticks a bit behind the wall clock: wait for a scripted step to show (screenshot/log) before simulating input.
    - A dark mob in a screenshot: check F3 light and the player's own hand first (the player may stand in shadow) before blaming normals.
    - Scripted tests: place mob and player with absolute `tp` spots inside the walled stage, never relative to a mob (they fall off and the test lies).
    - Homelander (2026-10-16): `gradlew runHomelanderTest` opens `mod/run/saves/homelandertest` (datapack source `mod/testworld/homelandertest`). Dev tags `hl_punch/hl_laser/hl_throw/hl_clap/hl_burn` force an attack, `hl_hold` blocks random ones. Chat `HLSHOT <name>` saves a game screenshot to `mod/run/screenshots/`, `HLHUD off` hides the HUD, `CMTEST ui` shoots the pause menu + update screen.
    - Screenshots: use the game's own (`HLSHOT`), never `tools\winshot.ps1` while the user works: it grabs whatever window is on top (2026-10-16 it caught a private chat).
    - `hurt()` with an entity source already knocks back 0.4: for burns/grabs/own knockback save and restore `getDeltaMovement()` around it (victims flew away otherwise).
    - A pause screen in singleplayer stops the server ticks (datapack timeline): UI tests run on the client.

## Testing a viewer (no Node/Python here) (2026-10-14)

- Headless Brave via DevTools: `tools\cdp.ps1 -url "file:///.../Mob.html#cliptest" -expr "(document.getElementById('cliptest')||{}).textContent||''" -out <scratch>\clip.txt` (~15 s). Every viewer keeps the `#cliptest`, `#rest` and `#shot-<anim>-<variant>-<sec>[-<talk 0-1>]` modes (`#shot` / `#rest` fill `<pre id="shot">`). Reading local video/audio in a page needs `-extra '--allow-file-access-from-files'`.
- Reference video frames, cuts, audio info: ffmpeg / ffprobe (installed).
- Live-page checks via `cdp.ps1`: wait until the page's debug handle exists (e.g. `window.kingDebug`) before sending keys; three.js loads from the CDN first.
- `tools\imgdiff.ps1 a.txt b.txt <dir>` compares two saved renders pixel by pixel (red = differs).
- Dead end (2026-10-14): `--dump-dom` on Brave 155 hangs, even for a 1-line page; `--screenshot` hangs on the render loop. Use `tools\cdp.ps1`.
- PowerShell variables ignore case: `$a` and `$A` are the same variable. Never name a helper function with one letter (`R` = alias of Invoke-History): use e.g. `Rep`.
- ffmpeg may not be on PATH in an old shell: use the full path `$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Gyan.FFmpeg_*\ffmpeg-*\bin\ffmpeg.exe`.
- Compare an animation with a reference video: `#strip-<anim>-<variant>-<from>-<to>-<step>[-<camDeg>]` gives one JPEG filmstrip; tile the video frames with ffmpeg (`select`, `tile`) and stack both (`vstack`).
- three.js `BoxGeometry` with a 0 size flips that side's normal (lit from behind). Face planes: set the normals by hand.
- Never use Blockbench (user rule, 2026-10-14). Check `.bbmodel` exports with the page's own `#cliptest` export check. Stop only processes started with your own `--user-data-dir`.
- `#rest` mode renders the rest pose; compare it with the source model's rest render (same window size and camera) to prove the model did not change.

## Changelog and session log (after every request)

25. **After every request**, update both files in `docs/`:
    - `docs/changelog.md`: newest entry on top, heading `## YYYY-MM-DD HH:MM` with the real date and time (read the clock, never guess), short bullets: what changed + which file.
    - `docs/latest.md`: overwrite it (no history): date, the request in one line, what was done, files changed, next step. Max ~15 lines.

## Self-improvement

26. **Claude updates its own rules.** When the user repeats the same instruction, fix or preference (2 times or more), add it as a rule to `CLAUDE.md` right away, dated.
27. A mistake or workaround that cost time -> write a short rule so it does not happen again. A faster way found -> write it down so it is reused.
28. A rule that turns out wrong or outdated -> fix or remove it. Merge rules, never duplicate. Keep the file short and clear.
