package com.custommobs.update;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/* ───────── Picking the update from GitHub's release list ───────── */
class UpdateCheckerTest {
    private static final String LIST = """
            [
              {"tag_name":"v0.1.0-alpha","name":"v0.1.0-alpha","draft":true,"body":"","assets":[{"name":"custom-mobs-0.1.0-alpha.jar","url":"u","browser_download_url":"b"}]},
              {"tag_name":"v0.0.3-alpha","name":"v0.0.3-alpha","draft":false,"body":"## Added\\n- x","assets":[{"name":"custom-mobs-0.0.3-alpha.jar","url":"api3","browser_download_url":"dl3"}]},
              {"tag_name":"v0.0.4-alpha","name":"v0.0.4-alpha","draft":false,"body":"","assets":[{"name":"notes.txt","url":"u","browser_download_url":"b"}]},
              {"tag_name":"v0.0.2-alpha","name":"","draft":false,"body":"","assets":[{"name":"custom-mobs-0.0.2-alpha.jar","url":"api2","browser_download_url":"dl2"}]},
              {"tag_name":"v9.0.0","name":"no alpha","draft":false,"body":"","assets":[{"name":"x.jar","url":"u","browser_download_url":"b"}]}
            ]""";

    @Test
    void picksHighestAlphaWithJarSkippingDrafts() {
        var r = UpdateChecker.newest(JsonParser.parseString(LIST).getAsJsonArray(), AlphaVersion.parse("0.0.1-alpha"));
        assertNotNull(r);
        assertEquals("0.0.3-alpha", r.version().toString());
        assertEquals("custom-mobs-0.0.3-alpha.jar", r.jarName());
        assertEquals("api3", r.jarApiUrl());
        assertEquals("dl3", r.jarDownloadUrl());
        assertTrue(r.notes().startsWith("## Added"));
    }

    @Test
    void nothingWhenUpToDate() {
        assertNull(UpdateChecker.newest(JsonParser.parseString(LIST).getAsJsonArray(), AlphaVersion.parse("0.0.3-alpha")));
    }
}
