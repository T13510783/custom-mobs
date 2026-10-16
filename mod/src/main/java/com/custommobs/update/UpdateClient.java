package com.custommobs.update;

import com.custommobs.Sindicate;
import com.custommobs.config.SindicateConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/* ───────── In-game updater (client only) ─────────
   - "Check for Updates" button on the title screen and the pause menu (top left).
   - Automatic check every 5 minutes (SindicateConfig.UPDATE_CHECK): a newer release shows a toast during play and
     opens the update screen on the next title / pause screen (once per version).
   - The update screen shows the release's changelog and "Update and Close Game". */
@Mod(value = Sindicate.MODID, dist = Dist.CLIENT)
public final class UpdateClient {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static AlphaVersion current;
    private static String currentText = "?";
    private static volatile UpdateChecker.Release pending;   // found by the automatic check, not shown yet
    private static AlphaVersion announced;                   // last version we told the player about

    public UpdateClient(ModContainer container) {
        currentText = container.getModInfo().getVersion().toString();
        current = AlphaVersion.parse(currentText);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Init.Post.class, UpdateClient::addButton);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Opening.class, UpdateClient::showPending);
        UpdateChecker.startAuto(() -> SindicateConfig.UPDATE_CHECK.get() ? new String[]{SindicateConfig.UPDATE_REPO.get(), SindicateConfig.UPDATE_TOKEN.get()} : null,
                current, UpdateClient::foundByTimer, err -> LOG.info("[Sindicate] update check failed: {}", err));
        // Dev runs only (test datapack): chat "CMTEST ui" opens the pause menu, then a demo update screen, with a game
        // screenshot of each (run/screenshots/cm_ui_*.png). Client-side timing: a pause screen stops the server's ticks.
        if (Sindicate.DEBUG) NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.client.event.ClientChatReceivedEvent.class, e -> {
            if (!e.getMessage().getString().equals("CMTEST ui")) return;
            Minecraft mc = Minecraft.getInstance();
            UpdateChecker.Release demo = new UpdateChecker.Release(current == null ? new AlphaVersion(0, 0, 2) : current.next(), "Demo release",
                    "## Added\n- Homelander\n- Updater\n\n## Removed\n- Nothing\n\n## Fixed\n- Cape clipping", "demo.jar", "", "");
            Runnable shot = () -> net.minecraft.client.Screenshot.grab(mc.gameDirectory, "cm_ui_" + (mc.screen instanceof UpdateScreen ? "update" : "pause") + ".png",
                    mc.getMainRenderTarget(), c -> {});
            Thread t = new Thread(() -> {
                try {
                    mc.execute(() -> { mc.options.hideGui = false; mc.pauseGame(false); });
                    Thread.sleep(1500);
                    mc.execute(shot);
                    Thread.sleep(300);
                    mc.execute(() -> mc.setScreen(new UpdateScreen(mc.screen, demo, currentText)));
                    Thread.sleep(1500);
                    mc.execute(shot);
                    Thread.sleep(300);
                    mc.execute(() -> mc.setScreen(null));
                } catch (InterruptedException ignored) {}
            }, "cm-ui-test");
            t.setDaemon(true);
            t.start();
        });
    }

    /* ───────── "Check for Updates" button ───────── */
    private static void addButton(ScreenEvent.Init.Post e) {
        Screen s = e.getScreen();
        if (!SindicateConfig.UPDATE_BUTTON.get() || (!(s instanceof TitleScreen) && !(s instanceof PauseScreen))) return;
        if (s instanceof PauseScreen ps && !ps.showsPauseMenu()) return;   // F3+Esc: no menu, no button
        Button[] self = new Button[1];
        self[0] = Button.builder(Component.translatable("sindicate.update.check"), b -> checkNow(s, self[0]))
                .bounds(4, 4, 110, 20).build();
        e.addListener(self[0]);
    }

    private static void checkNow(Screen from, Button b) {
        Minecraft mc = Minecraft.getInstance();
        b.active = false;
        b.setMessage(Component.translatable("sindicate.update.checking"));
        UpdateChecker.check(SindicateConfig.UPDATE_REPO.get(), SindicateConfig.UPDATE_TOKEN.get(), current).whenComplete((r, err) -> mc.execute(() -> {
            b.active = true;
            b.setMessage(Component.translatable("sindicate.update.check"));
            if (err != null) {
                Throwable c = err.getCause() != null ? err.getCause() : err;
                toast(Component.translatable("sindicate.update.check_failed"), Component.literal(String.valueOf(c.getMessage())));
            } else if (r == null) {
                toast(Component.translatable("sindicate.update.up_to_date"), Component.literal("v" + currentText));
            } else {
                announced = r.version();
                pending = null;
                mc.setScreen(new UpdateScreen(from, r, currentText));
            }
        }));
    }

    /* ───────── Automatic check (every 5 minutes) ───────── */
    private static void foundByTimer(UpdateChecker.Release r) {
        if (announced != null && r.version().compareTo(announced) <= 0) return; // already told
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            announced = r.version();
            if (mc.screen instanceof TitleScreen || mc.screen instanceof PauseScreen) mc.setScreen(new UpdateScreen(mc.screen, r, currentText));
            else {
                pending = r;
                toast(Component.translatable("sindicate.update.available", r.version().tag()), Component.translatable("sindicate.update.available_hint"));
            }
        });
    }

    private static void showPending(ScreenEvent.Opening e) {
        UpdateChecker.Release r = pending;
        Screen s = e.getNewScreen();
        if (r == null || !(s instanceof TitleScreen || s instanceof PauseScreen ps && ps.showsPauseMenu())) return;
        pending = null;
        e.setNewScreen(new UpdateScreen(s, r, currentText));
    }

    private static void toast(Component title, Component body) {
        Minecraft mc = Minecraft.getInstance();
        SystemToast.addOrUpdate(mc.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, body);
    }

    /* ───────── Update and Close Game ───────── */
    /** Download (background thread), arm the swap helper, close the game. onError runs on the game thread. */
    static void install(UpdateChecker.Release r, DoubleConsumer progress, Consumer<String> onError) {
        Minecraft mc = Minecraft.getInstance();
        Path oldJar = ModList.get().getModFileById(Sindicate.MODID).getFile().getFilePath();
        if (!Files.isRegularFile(oldJar)) {   // dev run: the mod is a classes folder, nothing to swap
            onError.accept("not installed from a jar (dev run)");
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Path jar = UpdateInstaller.download(r, SindicateConfig.UPDATE_TOKEN.get(), oldJar.getParent(), progress);
                UpdateInstaller.replaceOnExit(oldJar, jar);
                LOG.info("[Sindicate] {} downloaded, swapping {} -> {} after the game closes", r.version().tag(), oldJar.getFileName(), jar.getFileName());
                mc.execute(mc::stop);
            } catch (Exception ex) {
                LOG.warn("[Sindicate] update failed", ex);
                mc.execute(() -> onError.accept(String.valueOf(ex.getMessage())));
            }
        }, "sindicate-update-download");
        t.setDaemon(true);
        t.start();
    }
}
