# Latest session

- **Date:** 2026-10-15 22:25
- **Request:** Clean up all branches/worktrees, add an always-clean-up rule, clean and organise the whole repo and root.
- **Done:**
  - Root cleaned: only docs, config, `run.txt` and folders left. Duplicates (same hash) deleted, 6 Bateman film stills sorted into `references/patrick_bateman/`.
  - `run.txt` / `CLAUDE.md`: relative `cd .\mod` instead of a hardcoded `C:\custom mobs` path.
  - References renamed/sorted (drop kick video -> `references/the_king/`, no spaces or camera names). New clean-up rules in `CLAUDE.md` + `AGENTS.md` section 7.
  - Checked: no file over 2500 lines or 50 MB, `.gitignore` covers build output, no untracked junk.
- **Tested:** `mobs/patrick_bateman/PatrickBateman.html` is unchanged; nothing referenced the removed files (grep).
- **Files:** root images, `test.txt`, root `PatrickBateman.html`, `references/patrick_bateman/`, `run.txt`, `CLAUDE.md`, `docs/`.
- **Open:** PR #18 (t13510783) also edits the root `PatrickBateman.html` -> they keep only `mobs/patrick_bateman/`. Stale example path in `tools/cdp.ps1` line 4 left for #18 (same lines).
- **Next step:** merge PR #19 and this cleanup PR.
