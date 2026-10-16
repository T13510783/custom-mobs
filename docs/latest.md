# Latest session (Claude, Tivek87)

**Date:** 2026-10-16 18:52
**Request:** Fix the clipping, especially at Bateman's shoulders.

**Done:**
- Cause: the arm IK twisted the upper-arm box ~45 degrees around the arm (it followed the elbow bend), so its corners went up to 1.7 texel into the chest.
- Fix in `mobs/patrick_bateman/patrick_bateman.html`: twist-free arm IK, gestures fade along a forward-bowed path with rotation blending, all sleeves flush on the inner side, pistol arm straight forward.
- Checked: clip test all 10 outfits (shoulder in torso: none over 0.3 texel, was 0.85-1.7; shoulder top under the head max 0.29), per-frame step probe, renders.

**Files:** `mobs/patrick_bateman/patrick_bateman.html`, docs.

**Next:** PR #36 still waits for "merge". No release (viewer only).
