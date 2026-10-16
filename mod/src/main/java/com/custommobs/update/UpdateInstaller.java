package com.custommobs.update;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.function.DoubleConsumer;

/* ───────── "Update and Close Game": download the new jar, swap it in after the game has closed ─────────
   1. Download the release jar to <mods>/.sindicate-update/<name>.jar (checked: a real zip file).
   2. Write a small helper script next to it (PowerShell on Windows, sh elsewhere).
   3. A JVM shutdown hook starts that helper as its own process (detached): it waits until this game process has
      ended (Windows keeps a running jar locked), deletes the old jar, moves the new one into the mods folder
      and removes the temp folder. Then the game closes normally. */
public final class UpdateInstaller {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER).build();   // redirects by hand: the asset store refuses our token

    private UpdateInstaller() {}

    /** Downloads the release jar; progress 0..1 (or -1 when the size is unknown). Returns the downloaded file. */
    public static Path download(UpdateChecker.Release r, String token, Path modsDir, DoubleConsumer progress) throws IOException, InterruptedException {
        Path dir = modsDir.resolve(".sindicate-update");
        Files.createDirectories(dir);
        Path out = dir.resolve(r.jarName());
        boolean auth = token != null && !token.isBlank();
        // private repo: the API asset URL with the token answers with a redirect to a signed download link
        URI uri = URI.create(auth ? r.jarApiUrl() : r.jarDownloadUrl());
        for (int hop = 0; hop < 6; hop++) {
            HttpRequest.Builder rq = HttpRequest.newBuilder(uri).timeout(Duration.ofMinutes(2)).header("User-Agent", "sindicate-updater");
            if (auth && hop == 0) rq.header("Authorization", "Bearer " + token.trim()).header("Accept", "application/octet-stream");
            HttpResponse<InputStream> res = HTTP.send(rq.GET().build(), HttpResponse.BodyHandlers.ofInputStream());
            int code = res.statusCode();
            if (code >= 300 && code < 400) {
                res.body().close();
                uri = uri.resolve(res.headers().firstValue("Location").orElseThrow(() -> new IOException("redirect without Location")));
                continue;
            }
            if (code != 200) { res.body().close(); throw new IOException("download answered " + code); }
            long total = res.headers().firstValueAsLong("Content-Length").orElse(-1);
            try (InputStream in = res.body(); var os = Files.newOutputStream(out)) {
                byte[] buf = new byte[65536];
                long done = 0;
                for (int n; (n = in.read(buf)) > 0; ) {
                    os.write(buf, 0, n);
                    done += n;
                    progress.accept(total > 0 ? (double) done / total : -1);
                }
            }
            try (InputStream in = Files.newInputStream(out)) {
                byte[] magic = in.readNBytes(2);
                if (magic.length < 2 || magic[0] != 'P' || magic[1] != 'K') throw new IOException("the download is not a jar");
            }
            return out;
        }
        throw new IOException("too many redirects");
    }

    /** After the download: when the game closes, a detached helper replaces oldJar with newJar (same folder as oldJar). */
    public static void replaceOnExit(Path oldJar, Path newJar) throws IOException {
        Path helper = writeHelper(oldJar, newJar, ProcessHandle.current().pid());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                launch(helper);
            } catch (IOException ignored) {
                // nothing left to tell: the game is closing; the new jar stays in .sindicate-update
            }
        }, "sindicate-apply-update"));
    }

    private static boolean windows() { return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"); }

    /** The helper script: waits for process pid to end, deletes oldJar, moves newJar next to it, removes the temp folder. */
    public static Path writeHelper(Path oldJar, Path newJar, long pid) throws IOException {
        Path mods = oldJar.getParent();
        Path target = mods.resolve(newJar.getFileName().toString());
        boolean win = windows();
        Path helper = newJar.resolveSibling(win ? "apply_update.ps1" : "apply_update.sh");
        String script = win ? """
                # Sindicate updater: waits for Minecraft (pid %d) to close, then swaps the mod jar.
                $ErrorActionPreference = 'SilentlyContinue'
                Wait-Process -Id %d -Timeout 600
                Start-Sleep -Seconds 1
                $old = '%s'; $new = '%s'; $dst = '%s'
                for ($i = 0; $i -lt 60 -and (Test-Path -LiteralPath $old); $i++) { Remove-Item -LiteralPath $old -Force; if (Test-Path -LiteralPath $old) { Start-Sleep -Milliseconds 500 } }
                Move-Item -LiteralPath $new -Destination $dst -Force
                Remove-Item -LiteralPath '%s' -Recurse -Force
                """.formatted(pid, pid, ps(oldJar), ps(newJar), ps(target), ps(newJar.getParent()))
                : """
                #!/bin/sh
                # Sindicate updater: waits for Minecraft (pid %d) to close, then swaps the mod jar.
                while kill -0 %d 2>/dev/null; do sleep 0.5; done
                sleep 1
                rm -f '%s'
                mv -f '%s' '%s'
                rm -rf '%s'
                """.formatted(pid, pid, sh(oldJar), sh(newJar), sh(target), sh(newJar.getParent()));
        Files.writeString(helper, script, StandardCharsets.UTF_8);
        return helper;
    }

    /** Starts the helper as its own process (detached: it outlives this one). */
    public static void launch(Path helper) throws IOException {
        ProcessBuilder pb = windows()
                ? new ProcessBuilder("cmd.exe", "/c", "start", "\"\"", "/min", "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-WindowStyle", "Hidden", "-File", helper.toString())
                : new ProcessBuilder("sh", "-c", "nohup sh '" + sh(helper) + "' >/dev/null 2>&1 &");
        pb.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
    }

    private static String ps(Path p) { return p.toAbsolutePath().toString().replace("'", "''"); }

    private static String sh(Path p) { return p.toAbsolutePath().toString().replace("'", "'\\''"); }
}
