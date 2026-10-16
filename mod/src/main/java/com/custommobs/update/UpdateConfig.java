package com.custommobs.update;

import net.neoforged.neoforge.common.ModConfigSpec;

/* ───────── Updater settings (client, config/theking-updates-client.toml) ───────── */
public final class UpdateConfig {
    private static final ModConfigSpec.Builder C = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue AUTO_CHECK = C
            .comment("Look for a new Custom Mobs release on GitHub every 5 minutes while the game runs.")
            .define("autoCheck", true);
    public static final ModConfigSpec.ConfigValue<String> REPO = C
            .comment("GitHub repository with the releases (owner/name).")
            .define("repo", "T13510783/custom-mobs");
    public static final ModConfigSpec.ConfigValue<String> GITHUB_TOKEN = C
            .comment("Only for a private repository: a GitHub token with read access to it (fine-grained, Contents: read).",
                    "Stored as plain text in this file: never share this file. Empty = public repository.")
            .define("githubToken", "");
    public static final ModConfigSpec SPEC = C.build();

    private UpdateConfig() {}
}
