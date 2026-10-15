package com.custommobs.theking.client;

import com.custommobs.theking.KingConfig;
import com.custommobs.theking.TheKingMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/* ───────── Client-only setup (never loaded on a dedicated server) ───────── */
@Mod(value = TheKingMod.MODID, dist = Dist.CLIENT)
public final class TheKingClient {
    public TheKingClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, KingConfig.CLIENT_SPEC);
        // QoL: settings screen under Mods > The King > Config.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, e -> e.registerEntityRenderer(TheKingMod.KING.get(), KingRenderer::new));
    }
}
