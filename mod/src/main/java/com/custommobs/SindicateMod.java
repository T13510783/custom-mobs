package com.custommobs;

import com.custommobs.config.ConfigNet;
import com.custommobs.config.SindicateConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/* ───────── Sindicate: shared setup (both sides) ─────────
   The settings (one server file per world, one client file) and the network messages for editing the server
   settings in game. Each mob registers its own content in its own @Mod class (TheKingMod, HomelanderMod). */
@Mod(Sindicate.MODID)
public final class SindicateMod {
    public SindicateMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SindicateConfig.SERVER_SPEC, "sindicate-server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, SindicateConfig.CLIENT_SPEC, "sindicate-client.toml");
        modBus.addListener(RegisterPayloadHandlersEvent.class, e -> ConfigNet.register(e.registrar("1")));
        // every player gets the server settings (and whether they may edit them) when they join
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, e -> {
            if (e.getEntity() instanceof ServerPlayer p) ConfigNet.send(p);
        });
    }
}
