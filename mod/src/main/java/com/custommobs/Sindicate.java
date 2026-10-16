package com.custommobs;

/* ───────── Sindicate: the mod (one jar, every mob) ─────────
   Mod id "sindicate". Each mob keeps its own namespace for its entity, egg, sounds and textures ("theking", "homelander"),
   so worlds made with the older "The King" jar keep their mobs. */
public final class Sindicate {
    public static final String MODID = "sindicate";
    public static final String NAME = "Sindicate";

    /** Dev runs only (gradlew run* sets -Dtheking.debug=true): mob actions go to the log, test hooks are on. */
    public static final boolean DEBUG = Boolean.getBoolean("theking.debug");
    public static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private Sindicate() {}
}
