# Latest session (Claude, Tivek87)

**Date:** 2026-10-17 23:25
**Request:** Re-organise the whole folder/file structure by franchise (like the Saw folder), fix all paths, commit + push.

**Done:**
- Viewers: `custom-models/mobs/<franchise>/<mob>/<mob>.html` + `custom-models/vehicles/...`; shared UI `custom-models/_shared/`.
- `references/`, `sounds/`, `archive/` mirror that path (`<kind>/<franchise>/<name>/`).
- 44 reference images renamed to clear names; 6 exact duplicates deleted.
- All paths fixed (viewers, tools, Java comments, README, CLAUDE.md, AGENTS.md). Checked: every `../` path resolves; 4 viewers load the shared UI locally (headless Brave).

**Files:** whole tree moved (git renames), `README.md`, `CLAUDE.md`, `AGENTS.md`, `docs/`, `tools/`, 5 Java comments.

**Next:** branch `tivek87/homelander-attacks` (unmerged WIP) must rebase onto the new layout; Saw viewers still named `_bad` (redo them).
