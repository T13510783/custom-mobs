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
| Homelander | `mobs/homelander/homelander.html` | not yet |
| Black Noir | `mobs/black_noir/black_noir.html` | not yet |
| Reverse-Flash | `mobs/reverse_flash/reverse_flash.html` | not yet |
| Darth Vader | `mobs/darth_vader/darth_vader.html` | not yet |
| Thanos | `mobs/thanos/thanos.html` | not yet |
| Patrick Bateman | `mobs/patrick_bateman/patrick_bateman.html` | not yet |
| Anton Chigurh | `mobs/anton_chigurh/anton_chigurh.html` | not yet |
| The Joker | `mobs/the_joker/the_joker.html` | not yet |

Viewer controls: number keys = animations, `B` = show bones, `R` = reset camera,
eye button = head follows the mouse, dots = color variants, top right = reset camera.

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

## Run the mod

Needs JDK 21. In PowerShell:

```
cd mod
.\gradlew.bat runClient
```

`.\gradlew.bat build` makes the jar (copied to `builds/`) and runs the 1:1 animation test.
Copy `.env.example` to `.env` for your own local paths.

## Working together

Repo: https://github.com/T13510783/custom-mobs (private, owner `T13510783`).
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
| Never commit to `main`; use your own branch `<github-name>/<topic>` (lowercase) | `main` is not locked by GitHub (free private repo) |
| Write who works on what in `docs/working_on.md`; one person per mob page | big HTML pages conflict badly |
| After every change: commit -> fetch -> `pull --rebase` + `rebase origin/main` -> push | everyone stays in sync, small conflicts only |
| Done + tested -> pull request (PR) -> squash merge | `main` gets one clean commit per topic |
| Conflict -> backup branch first, keep both sides, re-test | no lost work |
| Binary file conflict (png, ogg, mp4, jar) -> keep both, ask | git can't merge those |
| No force push, no `reset --hard` / `clean -fd` on others' work | can delete someone's work |
| No `.env`, tokens, build output, files over 50 MB | secrets stay private, GitHub limit is 100 MB |
