# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 14:37
**Request:** in-game updater: auto check every 5 min with a notification, manual check, changelog in game, "Update and Close Game" (manual restart); then commit + push + release.

**Done:**
- Already there: button (title + pause), 5-minute check, changelog screen, download + swap after close.
- New: toast + clickable chat link `[Changelog + Update]` while playing (client command `/sindicate update`), restart hint, scroll arrow, toast cleared on the screen.
- Tests: `gradlew build` 8 tests green; in game (`runHomelanderTest`, `CMTEST ui`): notice shown, link opens the update screen with the changelog.
- Not tested in game: the real download + jar swap (dev run has no jar); covered by `UpdateInstallerTest`.

**Files:** `mod/src/main/java/com/custommobs/update/UpdateClient.java`, `UpdateScreen.java`, lang, test timeline, `CLAUDE.md`, `README.md`, docs, version 0.0.3-alpha.

**Next:** v0.0.3-alpha released; players on v0.0.2 get the notice within 5 minutes.
