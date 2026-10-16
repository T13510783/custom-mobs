package com.custommobs.theking.client;

import com.custommobs.Sindicate;
import com.custommobs.theking.TheKingMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/* ───────── Client-only setup (never loaded on a dedicated server) ───────── */
@Mod(value = Sindicate.MODID, dist = Dist.CLIENT)
public final class TheKingClient {
    public TheKingClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, e -> e.registerEntityRenderer(TheKingMod.KING.get(), KingRenderer::new));
    }
}
