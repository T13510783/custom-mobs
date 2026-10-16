package com.custommobs.homelander;

import net.neoforged.neoforge.common.ModConfigSpec;

/* ───────── Homelander settings (Mods screen > config, or config/theking-homelander-common.toml) ─────────
   Gameplay, decided by the server. Sound volume follows the game's "Hostile Creatures" slider. */
public final class HomelanderConfig {
    private static final ModConfigSpec.Builder C = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue NATURAL_SPAWNS = C
            .comment("Homelander spawns by himself in plains, meadows and badlands (very rare, never two close together).")
            .define("naturalSpawns", true);
    public static final ModConfigSpec.BooleanValue ATTACK_PLAYERS = C
            .comment("He attacks players.")
            .define("attackPlayers", true);
    public static final ModConfigSpec.BooleanValue ATTACK_VILLAGERS = C
            .comment("He attacks villagers, wandering traders and iron golems.")
            .define("attackVillagers", true);
    public static final ModConfigSpec.BooleanValue ATTACK_ALL_MOBS = C
            .comment("He attacks every other mob too (animals, monsters), never another Homelander.")
            .define("attackAllMobs", true);
    public static final ModConfigSpec.BooleanValue GRABS = C
            .comment("Throat grabs: he lifts a victim by the throat, then throws it high into the air or burns it with heat vision.")
            .define("grabs", true);
    public static final ModConfigSpec.BooleanValue HEAT_VISION = C
            .comment("Heat vision: laser beams from his eyes at targets 3-12 blocks away (sets them on fire).")
            .define("heatVision", true);
    public static final ModConfigSpec.BooleanValue HEAD_CLAP = C
            .comment("Head clap: he says \"I am god\" and smacks both ears of the victim (stuns, deafens, knocks back).")
            .define("headClap", true);
    public static final ModConfigSpec.DoubleValue DAMAGE = C
            .comment("All of his damage x this.")
            .defineInRange("damageMultiplier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.BooleanValue LASER_FIRE = C
            .comment("Heat vision sets blocks on fire where it hits (needs mobGriefing).")
            .define("laserSetsBlocksOnFire", false);
    public static final ModConfigSpec COMMON_SPEC = C.build();

    private HomelanderConfig() {}
}
