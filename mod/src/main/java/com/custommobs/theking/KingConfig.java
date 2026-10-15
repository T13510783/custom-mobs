package com.custommobs.theking;

import net.neoforged.neoforge.common.ModConfigSpec;

/* ───────── Settings (Mods screen > The King > Config, or config/theking-*.toml) ─────────
   COMMON = gameplay (server decides), CLIENT = sound volumes (each player). */
public final class KingConfig {
    /* ───────── Gameplay ───────── */
    private static final ModConfigSpec.Builder C = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue NATURAL_SPAWNS = C
            .comment("The King spawns by himself in plains, savanna, forest and meadow (rare, never two close together).")
            .define("naturalSpawns", true);
    public static final ModConfigSpec.BooleanValue DROPKICK = C
            .comment("He leaps and dropkicks targets 3-5 blocks away.")
            .define("dropkick", true);
    public static final ModConfigSpec.DoubleValue DROPKICK_DAMAGE = C
            .comment("Dropkick damage = attack damage x this.")
            .defineInRange("dropkickDamageMultiplier", 1.6, 0.0, 10.0);
    public static final ModConfigSpec.BooleanValue DANCE_ON_KILL = C
            .comment("He does his victory dance 1 second after every kill.")
            .define("danceOnKill", true);
    public static final ModConfigSpec.BooleanValue VOICE_LINES = C
            .comment("He says a voice line now and then (mouth synced).")
            .define("voiceLines", true);
    public static final ModConfigSpec COMMON_SPEC = C.build();

    /* ───────── Sound (per player) ───────── */
    private static final ModConfigSpec.Builder K = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue VOICE_VOLUME = K
            .comment("Voice line volume (0-1).")
            .defineInRange("voiceVolume", 0.6, 0.0, 1.0);
    public static final ModConfigSpec.DoubleValue MUSIC_VOLUME = K
            .comment("Dance music volume (0-1).")
            .defineInRange("musicVolume", 0.45, 0.0, 1.0);
    public static final ModConfigSpec CLIENT_SPEC = K.build();

    private KingConfig() {}
}
