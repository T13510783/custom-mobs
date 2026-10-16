package com.custommobs.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/* ───────── Asks GitHub Releases for a newer -alpha jar ─────────
   GET https://api.github.com/repos/<repo>/releases: the highest vX.Y.Z-alpha tag with a .jar asset that is newer
   than the running mod is "the update". Runs off the game thread (a small daemon thread): manual checks from the
   button and an automatic check every 5 minutes. A private repo needs a token (SindicateConfig.UPDATE_TOKEN). */
public final class UpdateChecker {
    /** One GitHub release: version, title, changelog (Added / Removed / Fixed), its jar. */
    public record Release(AlphaVersion version, String title, String notes, String jarName, String jarApiUrl, String jarDownloadUrl) {}

    public static final long AUTO_CHECK_MINUTES = 5;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL).build();
    private static ScheduledExecutorService timer;

    private UpdateChecker() {}

    /** Background check: the newest release if it is newer than `current`, else null. Errors complete exceptionally. */
    public static CompletableFuture<Release> check(String repo, String token, AlphaVersion current) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest.Builder rq = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + repo + "/releases?per_page=30"))
                        .timeout(Duration.ofSeconds(15)).header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28").header("User-Agent", "sindicate-updater");
                if (token != null && !token.isBlank()) rq.header("Authorization", "Bearer " + token.trim());
                HttpResponse<String> r = HTTP.send(rq.GET().build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 404) throw new IllegalStateException("repo " + repo + " not found (private repo: set githubToken in the config)");
                if (r.statusCode() != 200) throw new IllegalStateException("GitHub answered " + r.statusCode());
                return newest(JsonParser.parseString(r.body()).getAsJsonArray(), current);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        });
    }

    /** From GitHub's release list: the highest -alpha release with a jar, if newer than current. Public for the unit test. */
    public static Release newest(JsonArray releases, AlphaVersion current) {
        Release best = null;
        for (JsonElement el : releases) {
            JsonObject o = el.getAsJsonObject();
            if (o.has("draft") && o.get("draft").getAsBoolean()) continue;
            AlphaVersion v = AlphaVersion.parse(str(o, "tag_name"));
            if (v == null || (current != null && v.compareTo(current) <= 0) || (best != null && v.compareTo(best.version()) <= 0)) continue;
            for (JsonElement a : o.getAsJsonArray("assets")) {
                JsonObject ao = a.getAsJsonObject();
                String name = str(ao, "name");
                if (name == null || !name.endsWith(".jar")) continue;
                String title = str(o, "name");
                best = new Release(v, title == null || title.isBlank() ? v.tag() : title, str(o, "body") == null ? "" : str(o, "body"),
                        name, str(ao, "url"), str(ao, "browser_download_url"));
                break;
            }
        }
        return best;
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : null;
    }

    /** Every AUTO_CHECK_MINUTES (first one after 20 s): found(release) for each newer release seen. */
    public static synchronized void startAuto(java.util.function.Supplier<String[]> repoAndToken, AlphaVersion current,
                                              Consumer<Release> found, Consumer<String> failed) {
        if (timer != null) return;
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sindicate-update-check");
            t.setDaemon(true);   // never keeps the game from closing
            return t;
        });
        // everything inside the try: one exception thrown out of a scheduled task silently stops all later runs
        timer.scheduleAtFixedRate(() -> {
            try {
                String[] rt = repoAndToken.get();
                if (rt == null) return;  // auto checks turned off
                Release r = check(rt[0], rt[1], current).get(30, TimeUnit.SECONDS);
                if (r != null) found.accept(r);
            } catch (Throwable e) {
                failed.accept(e.getCause() != null ? e.getCause().getMessage() : String.valueOf(e.getMessage()));
            }
        }, 20, AUTO_CHECK_MINUTES * 60, TimeUnit.SECONDS);
    }
}
