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
| Reverse-Flash | `mobs/reverse_flash/ReverseFlash.html` | not yet |
| Darth Vader | `mobs/darth_vader/DarthVader.html` | not yet |
| Thanos | `mobs/thanos/Thanos.html` | not yet |
| The Joker | `mobs/the_joker/the_joker.html` | not yet |

Viewer controls: number keys = animations, `B` = show bones, `R` = reset camera,
eye button = head follows the mouse, dots = color variants, top right = download `.bbmodel`.

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

Several people (and their AI assistants) work in this repo. **Read [`AGENTS.md`](AGENTS.md) first**:
own branch per person, pull/rebase + push after every change, PRs into `main`, no merge conflicts left behind.
The rules for making mobs are in [`CLAUDE.md`](CLAUDE.md).
