# Custom Mobs

Custom Minecraft mobs for **Minecraft 1.21.1 + NeoForge**, made in three steps:

1. **Viewer page**: every mob starts as one HTML page with a 3D model (three.js), its animations,
   face animation, cape/hair physics and color variants. Open it in a browser, no install needed.
2. **Blockbench model**: when a mob is final, it is saved as a `.bbmodel` file.
3. **Mod**: the final mob goes into the NeoForge mod (`mod/`), 1:1 with the viewer.

## Mobs

| Mob | Viewer | In the mod |
|---|---|---|
| The King | `mobs/the_king/thekingv3.html` | yes |
| Homelander | `mobs/homelander/homelander.html` | yes |
| Black Noir | `mobs/black_noir/black_noir.html` | not yet |
| Reverse-Flash | `mobs/reverse_flash/reverse_flash.html` | not yet |
| Darth Vader | `mobs/darth_vader/darth_vader.html` | not yet |
| Thanos | `mobs/thanos/thanos.html` | not yet |
| Patrick Bateman | `mobs/patrick_bateman/patrick_bateman.html` | not yet |
| Anton Chigurh | `mobs/anton_chigurh/anton_chigurh.html` | not yet |
| The Joker | `mobs/the_joker/the_joker.html` | not yet |
| Jason Voorhees | `mobs/jason_voorhees/jason_voorhees.html` | not yet |
| Hannibal Lecter | `mobs/hannibal/hannibal.html` | not yet |
| Michael Myers | `mobs/michael_myers/michael_myers.html` | not yet |

Viewer controls: number keys = animations, `B` = show bones, `R` = reset camera,
eye button = head follows the mouse, dots = color variants, top right = expression menu (Auto / Happy / Sad / Angry / Afraid) + reset camera.
Shared viewer UI for every mob: `mobs/_shared/` (`viewer.css`, `viewer.js`, `viewer_template.html` = start page for a new mob).

## Folders

| Folder | What is in it |
|---|---|
| `mobs/<mob>/` | viewer pages |
| `mod/` | the NeoForge mod (Gradle project) |
| `references/<mob>/` | reference photos and videos |
| `sounds/<mob>/` | sounds (`.ogg` for the mod, `.mp3` for the browser) |
| `archive/` | old versions |
| `tools/` | test and helper scripts (PowerShell) |
| `docs/` | changelog, latest session, who works on what |

## The Sindicate mod

All finished mobs live in one NeoForge mod: **Sindicate** (mod id `sindicate`, jar `sindicate-<version>.jar`).

| What | Where |
|---|---|
| Spawn eggs | Creative tab per mob ("The King", "Homelander") + vanilla Spawn Eggs tab |
| Settings | Mods list -> Sindicate -> Config, or key `K` in game. Tabs **Client** (your sounds, effects, updates) and **Server** (mob behaviour, per world). Hover any option for a short explanation. |
| Who may change server settings | singleplayer: you. Multiplayer: operators, only when `Operators can edit config` is on |
| Updates | "Check for Updates" button (title + pause screen) or `/sindicate update`; auto check every 5 min: a new version pops up (toast + chat link `[Changelog + Update]`, or the update screen on a menu). The update screen shows the changelog; "Update and Close Game" downloads it and closes the game: start Minecraft again yourself |

Homelander: 75 HP, diamond-armor protection (20 armor, 8 toughness), neutral by default (only fights back when hurt).
Attacks: punch, heat vision, throat grab + throw, throat grab + point-blank laser, ear clap (victim falls and lies screaming); he mixes all of them in a fight. Runs ~2x player speed when the target is far away; punches and lasers while walking. Boss bar while he fights.

## Run the mod

Needs JDK 21. In PowerShell:

```
cd mod
.\gradlew.bat runClient
```

`.\gradlew.bat build` makes `sindicate-<version>.jar` (copied to `builds/`, newest 5 kept) and runs the tests.
Download: newest `sindicate-<version>.jar` on the [Releases page](https://github.com/T13510783/custom-mobs/releases) (newest: `v0.0.3-alpha`), put it in your `mods` folder (NeoForge 1.21.1).
Releases: raise `mod_version` (`X.Y.Z-alpha`) + `mod/release_notes.md` in a PR, merge, then run `tools\release.ps1` on `main`; in game: "Check for Updates".
Local paths go in your own `.env` (git-ignored, never committed; `.env.example` is local too): `JAVA_HOME` (JDK 21), `BRAVE_EXE` (headless browser for `tools\cdp.ps1`), `MC_MODS_DIR` (your Minecraft `mods` folder).

## Working together

Repo: https://github.com/T13510783/custom-mobs (public, owner `T13510783`).
Several people (and their AI assistants) work in this repo. **Read [`AGENTS.md`](AGENTS.md) first.**
The rules for making mobs are in [`CLAUDE.md`](CLAUDE.md) (its "GitHub repo and teamwork" section has the short version).

**Join:** the owner invites you (repo Settings -> Collaborators). Then:

```
winget install --id Git.Git -e
winget install --id GitHub.cli -e
gh auth login --web
gh repo clone T13510783/custom-mobs
```

**Rules in short:**

| Rule | Why |
|---|---|
| Never commit to `main`; use your own branch `<github-name>/<topic>` (lowercase) | `main` has no branch protection |
| Write who works on what in `docs/working_on.md`; one person per mob page | big HTML pages conflict badly |
| After every change: commit -> fetch -> `pull --rebase` + `rebase origin/main` -> push | everyone stays in sync, small conflicts only |
| Done + tested -> pull request (PR) -> squash merge | `main` gets one clean commit per topic |
| Conflict -> backup branch first, keep both sides, re-test | no lost work |
| Binary file conflict (png, ogg, mp4, jar) -> keep both, ask | git can't merge those |
| No force push, no `reset --hard` / `clean -fd` on others' work | can delete someone's work |
| No `.env` / `.env.example`, tokens, build output, files over 50 MB | secrets stay private, GitHub limit is 100 MB |
| Update this README before every push | it is the repo's front page on GitHub |
