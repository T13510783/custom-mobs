package com.custommobs.theking;

import com.custommobs.config.SindicateConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/* ───────── The King's settings: names kept for the King code, values live in SindicateConfig ─────────
   Server (per world): spawns, dropkick, dance, voice lines. Client (each player): sound volumes. */
public final class KingConfig {
    public static final ModConfigSpec.BooleanValue NATURAL_SPAWNS = SindicateConfig.KING_NATURAL_SPAWNS;
    public static final ModConfigSpec.BooleanValue DROPKICK = SindicateConfig.KING_DROPKICK;
    public static final ModConfigSpec.DoubleValue DROPKICK_DAMAGE = SindicateConfig.KING_DROPKICK_DAMAGE;
    public static final ModConfigSpec.BooleanValue DANCE_ON_KILL = SindicateConfig.KING_DANCE;
    public static final ModConfigSpec.BooleanValue VOICE_LINES = SindicateConfig.KING_VOICE;
    public static final ModConfigSpec.DoubleValue VOICE_VOLUME = SindicateConfig.KING_VOICE_VOLUME;
    public static final ModConfigSpec.DoubleValue MUSIC_VOLUME = SindicateConfig.KING_MUSIC_VOLUME;

    private KingConfig() {}
}
