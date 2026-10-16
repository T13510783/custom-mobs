package com.custommobs.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/* ───────── The swap helper: waits for the game process to end, then replaces the old jar ─────────
   A short dummy process stands in for Minecraft; the helper must not touch the jar before it has ended. */
class UpdateInstallerTest {
    @Test
    void helperSwapsJarAfterProcessEnds(@TempDir Path dir) throws Exception {
        Path mods = Files.createDirectories(dir.resolve("mods"));
        Path oldJar = Files.writeString(mods.resolve("the-king-crowned-brawler-1.0.0.jar"), "old");
        Path tmp = Files.createDirectories(mods.resolve(".sindicate-update"));
        Path newJar = Files.writeString(tmp.resolve("sindicate-0.0.2-alpha.jar"), "PKnew");
        boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Process game = (win ? new ProcessBuilder("powershell.exe", "-NoProfile", "-Command", "Start-Sleep -Seconds 3")
                : new ProcessBuilder("sleep", "3")).start();

        UpdateInstaller.launch(UpdateInstaller.writeHelper(oldJar, newJar, game.pid()));
        Thread.sleep(1500);
        assertTrue(Files.exists(oldJar), "old jar must stay while the game still runs");

        assertTrue(game.waitFor(20, TimeUnit.SECONDS));
        Path target = mods.resolve("sindicate-0.0.2-alpha.jar");
        for (int i = 0; i < 60 && (Files.exists(oldJar) || !Files.exists(target) || Files.exists(tmp)); i++) Thread.sleep(250);
        assertFalse(Files.exists(oldJar), "old jar removed");
        assertEquals("PKnew", Files.readString(target), "new jar in the mods folder");
        assertFalse(Files.exists(tmp), "temp folder removed");
    }
}
