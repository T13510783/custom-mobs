# Latest session

- **Date:** 2026-10-15 20:53
- **Request:** Fix sunken / disconnected shoulders and make held gun/props prominently visible in hand in Idle.
- **Done:** Raised arm pivots to `y = 12.0 * PX` and `x = ±6.0 * PX` flush with torso top; projected Glock slide forward from hand; enhanced slide metallic sheen, silver ejection port, white 3-dot sights, and guide rod; tuned overcoat idle arm angle (`rArmX = -0.22`) to showcase the gun silhouette clearly in front of the coat; synchronized `PatrickBateman.html` and `mobs/patrick_bateman/PatrickBateman.html`.
- **Tested:** Mathematical bounding-box analysis and Three.js hierarchy audit: shoulder continuous at 16 PX width, 0-pixel lateral gap, gun extends `1.0 * PX` to `5.8 * PX` forward in +Z outside arm box, 0 penetrations across all animations.
- **Files:** `PatrickBateman.html`, `mobs/patrick_bateman/PatrickBateman.html`, `docs/changelog.md`, `docs/latest.md`, `docs/working_on.md`.
- **Next step:** User review of the updated shoulders and held weapons/props.
