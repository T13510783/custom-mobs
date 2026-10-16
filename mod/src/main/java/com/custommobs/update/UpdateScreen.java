package com.custommobs.update;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/* ───────── The update screen: new version, its changelog (Added / Removed / Fixed), Update and Close Game ─────────
   Shown by the "Check for Updates" button, or by the automatic 5-minute check (on the next title / pause screen).
   The changelog scrolls with the mouse wheel. "Update and Close Game" downloads the jar, arms the swap helper and
   closes Minecraft (see UpdateInstaller). */
public final class UpdateScreen extends Screen {
    private final Screen parent;
    private final UpdateChecker.Release release;
    private final String current;
    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private double scroll;
    private Button updateButton, laterButton;
    private volatile String status = "";
    private volatile boolean busy;

    public UpdateScreen(Screen parent, UpdateChecker.Release release, String current) {
        super(Component.translatable("custommobs.update.title"));
        this.parent = parent;
        this.release = release;
        this.current = current;
    }

    @Override
    protected void init() {
        lines.clear();
        int w = Math.min(360, width - 40);
        for (String raw : release.notes().replace("\r", "").split("\n")) {
            String s = raw.strip();
            if (s.isEmpty()) { lines.add(FormattedCharSequence.EMPTY); continue; }   // keep the gap between sections
            Component c;
            if (s.startsWith("#")) c = Component.literal(s.replaceFirst("^#+\\s*", "")).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            else if (s.startsWith("- ") || s.startsWith("* ")) c = Component.literal("  • " + s.substring(2));
            else c = Component.literal(s);
            lines.addAll(font.split(c, w));
        }
        int by = height - 32;
        updateButton = addRenderableWidget(Button.builder(Component.translatable("custommobs.update.install"), b -> install())
                .bounds(width / 2 - 154, by, 150, 20).build());
        laterButton = addRenderableWidget(Button.builder(Component.translatable("custommobs.update.later"), b -> onClose())
                .bounds(width / 2 + 4, by, 150, 20).build());
        updateButton.active = laterButton.active = !busy;
    }

    private void install() {
        busy = true;
        updateButton.active = laterButton.active = false;
        status = Component.translatable("custommobs.update.downloading", "0").getString();
        UpdateClient.install(release, p -> status = Component.translatable("custommobs.update.downloading", p < 0 ? "..." : String.valueOf((int) (p * 100))).getString(),
                err -> {   // back on the game thread
                    busy = false;
                    status = Component.translatable("custommobs.update.failed", err).getString();
                    updateButton.active = laterButton.active = true;
                });
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visibleLines()), scroll - dy * 3));
        return true;
    }

    private int visibleLines() { return Math.max(1, (height - 32 - 12 - 62) / 10); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("custommobs.update.versions", current, release.version().toString()), width / 2, 28, 0xA0A0A0);
        g.drawCenteredString(font, Component.literal(release.title()).withStyle(ChatFormatting.WHITE), width / 2, 42, 0xFFFFFF);
        int x = width / 2 - Math.min(360, width - 40) / 2, y = 60, n = visibleLines();
        for (int i = (int) scroll; i < lines.size() && i < (int) scroll + n; i++, y += 10) g.drawString(font, lines.get(i), x, y, 0xE0E0E0);
        if (!status.isEmpty()) g.drawCenteredString(font, status, width / 2, height - 46, 0xFFD166);
    }

    @Override
    public boolean shouldCloseOnEsc() { return !busy; }

    @Override
    public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
