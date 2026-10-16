package com.custommobs.homelander.client;

import com.custommobs.homelander.HomelanderMod;
import com.custommobs.homelander.entity.HomelanderEntity;
import com.custommobs.theking.TheKingMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/* ───────── Homelander: client-only setup (never loaded on a dedicated server) ───────── */
@Mod(value = TheKingMod.MODID, dist = Dist.CLIENT)
public final class HomelanderClient {
    public HomelanderClient(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, e -> e.registerEntityRenderer(HomelanderMod.HOMELANDER.get(), HomelanderRenderer::new));
        // Dev runs only: a chat line "HLSHOT <name>" (from the test datapack) saves a game screenshot to run/screenshots/<name>.png.
        if (HomelanderEntity.DEBUG) NeoForge.EVENT_BUS.addListener(ClientChatReceivedEvent.class, e -> {
            String s = e.getMessage().getString();
            Minecraft mc = Minecraft.getInstance();
            if (s.startsWith("HLHUD ")) mc.options.hideGui = s.endsWith("off"); // "HLHUD off" = clean shots
            if (!s.startsWith("HLSHOT ")) return;
            Screenshot.grab(mc.gameDirectory, s.substring(7).trim() + ".png", mc.getMainRenderTarget(), c -> {});
        });
    }
}
