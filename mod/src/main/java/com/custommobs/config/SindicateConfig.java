package com.custommobs.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* ───────── All Sindicate settings: one SERVER config (per world, synced to players) + one CLIENT config ─────────
   Files: <world>/serverconfig/sindicate-server.toml and config/sindicate-client.toml.
   Every option is also listed in ENTRIES (side, section, key, value, tooltip, default, range) for the settings screen.
   The old per-mob classes (KingConfig, HomelanderConfig, UpdateConfig) point at these values. */
public final class SindicateConfig {
    public enum Side { CLIENT, SERVER }

    /** One option for the settings screen. */
    public record Entry(Side side, String section, String key, ModConfigSpec.ConfigValue<?> value, String tooltip, Object def, Number min, Number max) {
        public String id() { return section + "." + key; }
    }

    private static final List<Entry> LIST = new ArrayList<>();
    public static final List<Entry> ENTRIES = Collections.unmodifiableList(LIST);

    /* ───────── Server (gameplay; the world's owner, or operators if allowed, can change it in game) ───────── */
    private static final B S = new B(Side.SERVER);
    public static final ModConfigSpec.BooleanValue OPS_CAN_EDIT = S.section("general")
            .bool("opsCanEditConfig", true, "Operators (permission level 2+) may change these server settings from the in-game settings screen. Off = only the server file (or the singleplayer owner).");

    public static final ModConfigSpec.BooleanValue KING_NATURAL_SPAWNS = S.section("theking")
            .bool("naturalSpawns", true, "The King spawns by himself in plains, savanna, forest and meadow (rare, never two close together).");
    public static final ModConfigSpec.BooleanValue KING_DROPKICK = S.bool("dropkick", true, "He leaps and dropkicks targets 3-5 blocks away.");
    public static final ModConfigSpec.DoubleValue KING_DROPKICK_DAMAGE = S.dbl("dropkickDamageMultiplier", 1.6, 0, 10, "Dropkick damage = attack damage x this.");
    public static final ModConfigSpec.BooleanValue KING_DANCE = S.bool("danceOnKill", true, "He does his victory dance 1 second after every kill.");
    public static final ModConfigSpec.BooleanValue KING_VOICE = S.bool("voiceLines", true, "He says a voice line now and then (mouth synced).");

    public static final ModConfigSpec.BooleanValue HL_NATURAL_SPAWNS = S.section("homelander")
            .bool("naturalSpawns", true, "Homelander spawns by himself in plains, meadows and badlands (very rare, never two close together).");
    public static final ModConfigSpec.DoubleValue HL_SPAWN_CHANCE = S.dbl("spawnChance", 0.25, 0, 1, "Chance that a natural spawn attempt really spawns him (lower = rarer).");
    public static final ModConfigSpec.BooleanValue HL_NEUTRAL = S.bool("neutral", true, "Neutral: he only fights whoever hits him first (players and mobs). Off = he hunts on his own (see the options below).");
    public static final ModConfigSpec.BooleanValue HL_HUNT_PLAYERS = S.bool("huntPlayers", true, "When not neutral: he hunts players.");
    public static final ModConfigSpec.BooleanValue HL_HUNT_VILLAGERS = S.bool("huntVillagers", true, "When not neutral: he hunts villagers, wandering traders and iron golems.");
    public static final ModConfigSpec.BooleanValue HL_HUNT_MOBS = S.bool("huntMobs", false, "When not neutral: he hunts every other mob too (never another Homelander).");
    public static final ModConfigSpec.IntValue HL_HEALTH = S.intv("maxHealth", 75, 1, 2048, "His health (an iron golem has 100).");
    public static final ModConfigSpec.IntValue HL_ARMOR = S.intv("armor", 20, 0, 30, "Armor points (20 = a full diamond set).");
    public static final ModConfigSpec.IntValue HL_TOUGHNESS = S.intv("armorToughness", 8, 0, 20, "Armor toughness (8 = a full diamond set).");
    public static final ModConfigSpec.DoubleValue HL_DAMAGE = S.dbl("damageMultiplier", 1.0, 0, 10, "All of his damage x this.");
    public static final ModConfigSpec.DoubleValue HL_RUN_SPEED = S.dbl("runSpeed", 11.0, 2, 20, "How fast he runs while chasing, in blocks per second (a sprinting player: 5.6).");
    public static final ModConfigSpec.BooleanValue HL_PUNCH = S.bool("punch", true, "Punch: one heavy punch at close range.");
    public static final ModConfigSpec.BooleanValue HL_HEAT_VISION = S.bool("heatVision", true, "Heat vision: laser beams from his eyes at targets 3-12 blocks away (sets them on fire).");
    public static final ModConfigSpec.DoubleValue HL_HEAT_RANGE = S.dbl("heatVisionRange", 12, 3, 32, "Farthest target for heat vision (blocks).");
    public static final ModConfigSpec.BooleanValue HL_LASER_FIRE = S.bool("heatVisionSetsBlocksOnFire", false, "Heat vision sets blocks on fire where it hits (needs the mobGriefing game rule).");
    public static final ModConfigSpec.BooleanValue HL_THROW = S.bool("throatGrabThrow", true, "Throat grab + throw: lifts the victim by the throat and flings it high into the air.");
    public static final ModConfigSpec.BooleanValue HL_BURN = S.bool("throatLaser", true, "Throat laser: holds the victim by the throat and burns its face with heat vision.");
    public static final ModConfigSpec.BooleanValue HL_CLAP = S.bool("headClap", true, "Head clap: \"I am god\", then he smacks both ears (stun, nausea, the victim falls).");
    public static final ModConfigSpec.BooleanValue HL_SMASH = S.bool("groundSmash", true, "Ground smash: smashes the victim into the ground, drags it by the feet and hurls it far away.");
    public static final ModConfigSpec.BooleanValue HL_GRAB_PLAYERS = S.bool("grabPlayers", true, "Grabs (throw, throat laser, smash) also work on players.");
    public static final ModConfigSpec.DoubleValue HL_THROW_POWER = S.dbl("throwPower", 1.0, 0.1, 3, "How high and far his throws fling victims (1 = like the viewer).");
    public static final ModConfigSpec.BooleanValue HL_ATTACK_WHILE_WALKING = S.bool("attackWhileWalking", true, "Punch and heat vision while he keeps walking (legs walk, arms and eyes attack).");
    public static final ModConfigSpec.BooleanValue HL_BOSS_BAR = S.bool("bossBar", true, "Show his health as a boss bar to players near him while he fights.");
    public static final ModConfigSpec.BooleanValue HL_PERSISTENT = S.bool("neverDespawn", true, "He never despawns once spawned.");
    public static final ModConfigSpec SERVER_SPEC = S.build();

    /* ───────── Client (this player only) ───────── */
    private static final B C = new B(Side.CLIENT);
    public static final ModConfigSpec.BooleanValue UPDATE_CHECK = C.section("updates")
            .bool("autoCheck", true, "Look for a new Sindicate release on GitHub every 5 minutes while the game runs.");
    public static final ModConfigSpec.BooleanValue UPDATE_BUTTON = C.bool("showButton", true, "Show the \"Check for Updates\" button on the title screen and the pause menu.");
    public static final ModConfigSpec.ConfigValue<String> UPDATE_REPO = C.str("repo", "T13510783/custom-mobs", "GitHub repository with the releases (owner/name).");
    public static final ModConfigSpec.ConfigValue<String> UPDATE_TOKEN = C.str("githubToken", "", "Only for a private repository: a GitHub token with read access. Stored as plain text: never share this file.");

    public static final ModConfigSpec.DoubleValue KING_VOICE_VOLUME = C.section("theking")
            .dbl("voiceVolume", 0.6, 0, 1, "Volume of The King's voice lines.");
    public static final ModConfigSpec.DoubleValue KING_MUSIC_VOLUME = C.dbl("musicVolume", 0.45, 0, 1, "Volume of The King's dance music.");

    public static final ModConfigSpec.DoubleValue HL_VOICE_VOLUME = C.section("homelander")
            .dbl("voiceVolume", 1.0, 0, 1, "Volume of Homelander's voice line (\"I am god\").");
    public static final ModConfigSpec.DoubleValue HL_SFX_VOLUME = C.dbl("effectsVolume", 1.0, 0, 1, "Volume of his punches, grabs, lasers and smacks.");
    public static final ModConfigSpec.BooleanValue HL_EYE_GLOW = C.bool("eyeGlow", true, "Draw his glowing red eyes.");
    public static final ModConfigSpec.DoubleValue HL_BEAM_BRIGHTNESS = C.dbl("beamBrightness", 1.0, 0.2, 1, "Brightness of the heat-vision beams (lower if they are too bright).");
    public static final ModConfigSpec CLIENT_SPEC = C.build();

    private SindicateConfig() {}

    /* ───────── Builder that also records every option ───────── */
    private static final class B {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        final Side side;
        String section;

        B(Side side) { this.side = side; }

        ModConfigSpec build() {
            if (section != null) b.pop();
            return b.build();
        }

        B section(String s) {
            if (section != null) b.pop();
            b.push(s);
            section = s;
            return this;
        }

        ModConfigSpec.BooleanValue bool(String k, boolean d, String tip) {
            var v = b.comment(tip).translation("sindicate.config." + section + "." + k).define(k, d);
            LIST.add(new Entry(side, section, k, v, tip, d, null, null));
            return v;
        }

        ModConfigSpec.DoubleValue dbl(String k, double d, double lo, double hi, String tip) {
            var v = b.comment(tip).translation("sindicate.config." + section + "." + k).defineInRange(k, d, lo, hi);
            LIST.add(new Entry(side, section, k, v, tip, d, lo, hi));
            return v;
        }

        ModConfigSpec.IntValue intv(String k, int d, int lo, int hi, String tip) {
            var v = b.comment(tip).translation("sindicate.config." + section + "." + k).defineInRange(k, d, lo, hi);
            LIST.add(new Entry(side, section, k, v, tip, d, lo, hi));
            return v;
        }

        ModConfigSpec.ConfigValue<String> str(String k, String d, String tip) {
            var v = b.comment(tip).translation("sindicate.config." + section + "." + k).define(k, d);
            LIST.add(new Entry(side, section, k, v, tip, d, null, null));
            return v;
        }
    }

    /** The option with this id ("homelander.maxHealth") on this side, or null. */
    public static Entry find(Side side, String id) {
        for (Entry e : LIST) if (e.side() == side && e.id().equals(id)) return e;
        return null;
    }

    /** Sets an option from its text form; false if the text is not valid for it. Caller saves the spec. */
    @SuppressWarnings("unchecked")
    public static boolean setFromString(Entry e, String text) {
        try {
            Object d = e.def();
            Object v;
            if (d instanceof Boolean) v = Boolean.parseBoolean(text.trim());
            else if (d instanceof Integer) v = Integer.parseInt(text.trim());
            else if (d instanceof Double) v = Double.parseDouble(text.trim());
            else v = text;
            if (e.min() != null && (((Number) v).doubleValue() < e.min().doubleValue() || ((Number) v).doubleValue() > e.max().doubleValue())) return false;
            ((ModConfigSpec.ConfigValue<Object>) e.value()).set(v);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
