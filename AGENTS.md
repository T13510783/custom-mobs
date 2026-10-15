# AGENTS.md: read this first (every person, every AI agent)

This repo is shared by several people. Each person may use their own AI (Claude, Codex, Cursor, ...)
that commits, pulls and pushes by itself. These rules keep all of them working together without
lost work or broken merges. **Read this whole file before your first change in a session.**
Project rules for making mobs and the mod are in `CLAUDE.md` (also read it).

## 1. Branches

- Repo: `https://github.com/T13510783/custom-mobs` (private; owner `T13510783`). Merging = squash only; merged branches are deleted by GitHub.
- `main` = always working. Nobody commits straight to `main`; it only changes through pull requests (PRs).
  GitHub can't lock `main` on a free private repo, so this rule is the only guard: follow it.
- Every person/agent works on **their own branch**: `<github-name>/<topic>` in lowercase, e.g. `t13510783/homelander`.
- One topic per branch. Small, frequent PRs beat big ones.

## 2. Start of every session

```
git fetch --all --prune
git switch <your-branch>            # or: git switch -c <github-name>/<topic> origin/main
git pull --rebase                   # your branch from GitHub
git rebase origin/main              # get everyone else's merged work
gh pr list                          # see open PRs: who is changing what
```

Then check `docs/working_on.md` (who works on which mob/file) and add your own line.

**Shared folder?** If another person or AI works in the same project folder at the same time, don't switch
branches there (you would switch theirs too). Work in your own git worktree (a second checkout of the repo):
`git worktree add "C:\cm-worktrees\<topic>" <your-branch>`; test and build there; remove it after the merge
(`git worktree remove "C:\cm-worktrees\<topic>"`). The shared folder stays on `main`.

## 3. After EVERY change (each finished edit)

**HARD RULE: after EVERY prompt and EVERY change: pull, commit, push. No exceptions.**
A session/turn never ends with unpushed work, an open merge conflict or a half-done rebase.
Make or switch branches and open/merge PRs whenever needed.

1. `git status` + `git branch --show-current` - only commit your own files, and only on your own branch.
   The folder is shared (your user's editor or another agent can switch branches or pull in between):
   on the wrong branch -> switch first. Never `git add -A` blindly: check for stray/temp files first.
2. `git commit -m "<mob or area>: <what changed>"` (e.g. `homelander: hover idle with hands behind back`).
3. `git fetch` + `gh pr list` - new PRs or new commits on `main`?
4. `git pull --rebase` (your branch), then `git rebase origin/main` if `main` moved.
5. `git push` (first time: `git push -u origin <your-branch>`).
6. Work is done and tested -> open or update the PR: `gh pr create --fill --base main`.
7. A PR is approved/green and has no conflicts -> `gh pr merge --squash --delete-branch`.

## 4. Merge conflicts: never leave them, never lose work

- Before resolving anything big: `git branch backup/<github-name>-<yyyyMMdd-HHmm>` (a safety copy).
- Resolve by keeping **both** sides' intent. Never just pick "mine" or "theirs" for code you did not write.
- After resolving: re-run the tests for what you touched (viewer `#cliptest`, `gradlew build`), then `git rebase --continue`.
- Can't resolve it safely? Stop, keep the backup branch, tell your user; don't push a guess.
- Known conflict hot spots:
  - `docs/changelog.md`: keep **all** entries, newest on top (sort by the time in the heading).
  - `docs/latest.md`: keep the newest one.
  - `docs/working_on.md`: keep everyone's lines.
  - Viewer pages (`mobs/<mob>/*.html`): only one person per mob at a time (see `docs/working_on.md`).
  - Binary files (png, jpg, ogg, mp3, mp4, jar) can't be merged: keep both versions, ask your user which one wins.

## 5. Never

- Never `git push --force` to `main`. On your own branch only `git push --force-with-lease`.
- Never rewrite or delete someone else's commits or branches.
- Never commit secrets or local paths: `.env` is ignored; use `.env.example` for shared settings.
- Never commit build output (`mod/build/`, `mod/run/`, `builds/*.jar`) or test renders: see `.gitignore`.
- Never `git reset --hard` / `git clean -fd` / `git checkout -- .` with uncommitted work from someone else.
- Never force a rejected push: `git pull --rebase` first. Uncommitted work before a pull or branch switch: commit or stash it.
- Never commit a file over 50 MB (GitHub refuses 100 MB): shrink media with ffmpeg first, or ask.
- Never without the repo owner's OK: change repo visibility, settings or collaborators, delete the repo, merge someone else's PR.

## 6. Commits and PR text

- Message: `<area>: <what>` in simple English. One logical change per commit.
- PR description: what changed, which files, how it was tested (or "not tested").
