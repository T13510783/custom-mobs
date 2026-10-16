package com.custommobs.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* ───────── Sindicate settings screen ─────────
   Two tabs: Client (this player) and Server (the world's gameplay). A search box filters options by name or text.
   Hover any option for a short explanation with its default and range. Each row: name, control (ON/OFF button,
   number or text field; red = not valid), and ↺ = back to default. Changes apply on "Save"; "Cancel" drops them.
   Server tab: editable by the singleplayer owner, or by operators when the server allows it (opsCanEditConfig);
   the change goes to the server (ConfigNet), which checks the permission again. Opened from the Mods list or key K. */
public final class SindicateConfigScreen extends Screen {
    private final Screen parent;
    private SindicateConfig.Side tab = SindicateConfig.Side.CLIENT;
    private final Map<SindicateConfig.Entry, String> pending = new LinkedHashMap<>();
    private OptionList list;
    private EditBox search;
    private Button clientTab, serverTab;
    private String filter = "";

    public SindicateConfigScreen(Screen parent) {
        super(Component.literal("Sindicate Settings"));
        this.parent = parent;
    }

    /* ───────── Layout ───────── */
    @Override
    protected void init() {
        clientTab = addRenderableWidget(Button.builder(Component.literal("Client"), b -> switchTab(SindicateConfig.Side.CLIENT))
                .bounds(width / 2 - 154, 22, 100, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Settings for you only: sounds, looks, updates."))).build());
        serverTab = addRenderableWidget(Button.builder(Component.literal("Server"), b -> switchTab(SindicateConfig.Side.SERVER))
                .bounds(width / 2 - 50, 22, 100, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Gameplay of this world: spawns, health, attacks. Saved per world."))).build());
        search = addRenderableWidget(new EditBox(font, width / 2 + 58, 22, 96, 20, Component.literal("Search")));
        search.setHint(Component.literal("Search...").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(filter);
        search.setResponder(s -> { filter = s; rebuild(); });
        list = addRenderableWidget(new OptionList(minecraft, width, height - 84, 48));
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> { save(); onClose(); }).bounds(width / 2 - 154, height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Reset tab"), b -> resetTab()).bounds(width / 2 - 50, height - 28, 100, 20)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Every option on this tab back to its default (applies on Save)."))).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> { pending.clear(); onClose(); }).bounds(width / 2 + 54, height - 28, 100, 20).build());
        ConfigNet.onClientValues = () -> Minecraft.getInstance().execute(this::rebuild);
        rebuild();
    }

    private void switchTab(SindicateConfig.Side s) {
        tab = s;
        rebuild();
    }

    /** Is the server tab usable: a world is open and this player may edit. */
    private boolean serverEditable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return mc.hasSingleplayerServer() || ConfigNet.clientCanEdit;
    }

    private String serverNote() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "Open a world to change its server settings.";
        if (!serverEditable()) return SindicateConfig.OPS_CAN_EDIT.get() ? "Only operators can change the server settings." : "This server does not allow changing settings in game.";
        return "";
    }

    private void rebuild() {
        if (list == null) return;
        clientTab.active = tab != SindicateConfig.Side.CLIENT;
        serverTab.active = tab != SindicateConfig.Side.SERVER;
        list.children().clear();
        list.setScrollAmount(0);
        boolean editable = tab == SindicateConfig.Side.CLIENT || serverEditable();
        String note = tab == SindicateConfig.Side.SERVER ? serverNote() : "";
        if (!note.isEmpty()) list.add(new Header(note, ChatFormatting.RED));
        String f = filter.toLowerCase(Locale.ROOT).trim();
        String section = null;
        for (SindicateConfig.Entry e : SindicateConfig.ENTRIES) {
            if (e.side() != tab) continue;
            if (!f.isEmpty() && !(label(e).toLowerCase(Locale.ROOT).contains(f) || e.tooltip().toLowerCase(Locale.ROOT).contains(f) || e.section().contains(f))) continue;
            if (!e.section().equals(section)) {
                section = e.section();
                list.add(new Header(sectionName(section), ChatFormatting.GOLD));
            }
            list.add(new Option(e, editable && (tab == SindicateConfig.Side.CLIENT || Minecraft.getInstance().level != null)));
        }
    }

    /* ───────── Values ───────── */
    private String current(SindicateConfig.Entry e) {
        String p = pending.get(e);
        if (p != null) return p;
        try {
            return String.valueOf(e.value().get());
        } catch (IllegalStateException ex) {   // server settings are only loaded inside a world
            return String.valueOf(e.def());
        }
    }

    private static boolean valid(SindicateConfig.Entry e, String s) {
        try {
            if (e.def() instanceof Integer) { int v = Integer.parseInt(s.trim()); return v >= e.min().intValue() && v <= e.max().intValue(); }
            if (e.def() instanceof Double) { double v = Double.parseDouble(s.trim()); return v >= e.min().doubleValue() && v <= e.max().doubleValue(); }
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void resetTab() {
        for (SindicateConfig.Entry e : SindicateConfig.ENTRIES) if (e.side() == tab) pending.put(e, String.valueOf(e.def()));
        rebuild();
    }

    private void save() {
        List<String> ids = new ArrayList<>(), vals = new ArrayList<>();
        boolean client = false;
        for (var en : pending.entrySet()) {
            SindicateConfig.Entry e = en.getKey();
            if (!valid(e, en.getValue())) continue;
            if (e.side() == SindicateConfig.Side.CLIENT) {
                client |= SindicateConfig.setFromString(e, en.getValue());
            } else if (serverEditable()) {
                ids.add(e.id());
                vals.add(en.getValue());
            }
        }
        if (client) SindicateConfig.CLIENT_SPEC.save();
        if (!ids.isEmpty()) PacketDistributor.sendToServer(new ConfigNet.Edit(ids, vals));
        pending.clear();
    }

    @Override
    public void onClose() {
        ConfigNet.onClientValues = () -> {};
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
    }

    /* ───────── Names ───────── */
    static String label(SindicateConfig.Entry e) {
        String key = "sindicate.config." + e.id();
        if (I18n.exists(key)) return I18n.get(key);
        // camelCase -> "Camel case"
        String s = e.key().replaceAll("([a-z0-9])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String sectionName(String s) {
        return switch (s) {
            case "general" -> "General";
            case "theking" -> "The King";
            case "homelander" -> "Homelander";
            case "updates" -> "Updates";
            default -> s;
        };
    }

    private List<FormattedCharSequence> tooltip(SindicateConfig.Entry e) {
        List<FormattedCharSequence> t = new ArrayList<>(font.split(Component.literal(label(e)).withStyle(ChatFormatting.YELLOW), 240));
        t.addAll(font.split(Component.literal(e.tooltip()), 240));
        String range = e.min() != null ? "  Range: " + num(e.min()) + " - " + num(e.max()) : "";
        t.addAll(font.split(Component.literal("Default: " + num(e.def()) + range).withStyle(ChatFormatting.GRAY), 240));
        return t;
    }

    private static String num(Object o) {
        if (o instanceof Double d && d == Math.rint(d)) return String.valueOf(d.longValue());
        return String.valueOf(o);
    }

    /* ───────── The scrolling list ───────── */
    private final class OptionList extends ContainerObjectSelectionList<Row> {
        OptionList(Minecraft mc, int w, int h, int y) { super(mc, w, h, y, 24); }

        void add(Row r) { addEntry(r); }

        @Override
        public int getRowWidth() { return Math.min(340, width - 40); }

        @Override
        protected int getScrollbarPosition() { return width / 2 + getRowWidth() / 2 + 8; }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {}

    private final class Header extends Row {
        private final String text;
        private final ChatFormatting color;

        Header(String text, ChatFormatting color) { this.text = text; this.color = color; }

        @Override
        public void render(GuiGraphics g, int i, int top, int left, int w, int h, int mx, int my, boolean hover, float pt) {
            g.drawString(font, Component.literal(text).withStyle(color, ChatFormatting.BOLD), left, top + 8, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() { return List.of(); }

        @Override
        public List<? extends NarratableEntry> narratables() { return List.of(); }
    }

    private final class Option extends Row {
        private final SindicateConfig.Entry e;
        private final AbstractWidget control;
        private final Button reset;

        Option(SindicateConfig.Entry e, boolean editable) {
            this.e = e;
            if (e.def() instanceof Boolean) {
                control = Button.builder(onOff(current(e)), b -> {
                    String nv = String.valueOf(!Boolean.parseBoolean(current(e)));
                    pending.put(e, nv);
                    b.setMessage(onOff(nv));
                }).bounds(0, 0, 90, 20).build();
            } else {
                EditBox box = new EditBox(font, 0, 0, 90, 20, Component.literal(label(e)));
                box.setMaxLength(200);
                box.setValue(current(e));
                box.setResponder(s -> {
                    pending.put(e, s);
                    box.setTextColor(valid(e, s) ? 0xE0E0E0 : 0xFF5555);
                });
                control = box;
            }
            control.active = editable;
            reset = Button.builder(Component.literal("↺"), b -> {
                String d = String.valueOf(e.def());
                pending.put(e, d);
                if (control instanceof EditBox box) box.setValue(d); else control.setMessage(onOff(d));
            }).bounds(0, 0, 20, 20).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Back to default: " + num(e.def())))).build();
            reset.active = editable;
        }

        private Component onOff(String v) {
            return Boolean.parseBoolean(v) ? Component.literal("ON").withStyle(ChatFormatting.GREEN) : Component.literal("OFF").withStyle(ChatFormatting.RED);
        }

        @Override
        public void render(GuiGraphics g, int i, int top, int left, int w, int h, int mx, int my, boolean hover, float pt) {
            Font f = font;
            g.drawString(f, label(e), left, top + 6, pending.containsKey(e) ? 0xFFFF88 : 0xFFFFFF);
            control.setX(left + w - 116);
            control.setY(top);
            reset.setX(left + w - 22);
            reset.setY(top);
            control.render(g, mx, my, pt);
            reset.render(g, mx, my, pt);
            if (hover && mx < left + w - 24) setTooltipForNextRenderPass(tooltip(e));
        }

        @Override
        public List<? extends GuiEventListener> children() { return List.of(control, reset); }

        @Override
        public List<? extends NarratableEntry> narratables() { return List.of(control, reset); }
    }
}
