# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 16:00
**Request:** All buttons gone on the Bateman page: fix, then commit and push.

**Done:**
- Cause: the page could not load `mobs/_shared/viewer.css` + `viewer.js` (page opened where `../_shared/` is missing): buttons fall below the 3D view, no expression menu. Reproduced headless.
- Fix: `onerror` fallback to the same files on jsDelivr (public repo). Tested: a copy without `_shared` and the normal path both show the dock, reset button and expression menu.
- Same fallback in the template; rule in `CLAUDE.md`.

**Files:** `mobs/patrick_bateman/patrick_bateman.html`, `mobs/_shared/viewer_template.html`, `CLAUDE.md`, docs.

**Next:** other mobs get the fallback when they are worked on. No release (viewer only).
