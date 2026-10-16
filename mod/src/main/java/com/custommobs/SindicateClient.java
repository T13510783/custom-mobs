package com.custommobs;

import com.custommobs.config.SindicateConfigScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/* ───────── Sindicate: client setup ─────────
   The settings screen opens from Mods > Sindicate > Config, or with a key (K, change it in Controls > Sindicate). */
@Mod(value = Sindicate.MODID, dist = Dist.CLIENT)
public final class SindicateClient {
    public static final KeyMapping OPEN_SETTINGS = new KeyMapping("key.sindicate.settings", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.sindicate");

    public SindicateClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new SindicateConfigScreen(parent));
        modBus.addListener(RegisterKeyMappingsEvent.class, e -> e.register(OPEN_SETTINGS));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, e -> {
            Minecraft mc = Minecraft.getInstance();
            while (OPEN_SETTINGS.consumeClick()) if (mc.screen == null) mc.setScreen(new SindicateConfigScreen(null));
        });
    }
}
