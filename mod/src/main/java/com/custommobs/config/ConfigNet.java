package com.custommobs.config;

import com.custommobs.Sindicate;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/* ───────── Server settings over the network ─────────
   Client -> server: Edit (option ids + new values as text). The server checks the player may edit
   (singleplayer owner, or operator level 2+ when opsCanEditConfig is on), applies, saves, and sends
   Values (every server option + "may this player edit") to all players. Values also go to each player on login. */
public final class ConfigNet {
    public record Edit(List<String> ids, List<String> values) implements CustomPacketPayload {
        public static final Type<Edit> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Sindicate.MODID, "config_edit"));
        public static final StreamCodec<ByteBuf, Edit> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), Edit::ids,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), Edit::values, Edit::new);

        @Override
        public Type<Edit> type() { return TYPE; }
    }

    public record Values(boolean canEdit, List<String> ids, List<String> values) implements CustomPacketPayload {
        public static final Type<Values> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Sindicate.MODID, "config_values"));
        public static final StreamCodec<ByteBuf, Values> CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Values::canEdit,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), Values::ids,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), Values::values, Values::new);

        @Override
        public Type<Values> type() { return TYPE; }
    }

    /** Client side: may this player change the server settings (from the last Values packet). */
    public static volatile boolean clientCanEdit;
    /** Client side: called after new server values arrived (the settings screen refreshes). */
    public static volatile Runnable onClientValues = () -> {};

    private ConfigNet() {}

    public static void register(PayloadRegistrar r) {
        r.playToServer(Edit.TYPE, Edit.CODEC, ConfigNet::onEdit);
        r.playToClient(Values.TYPE, Values.CODEC, ConfigNet::onValues);
    }

    public static boolean canEdit(ServerPlayer p) {
        MinecraftServer s = p.getServer();
        return s != null && (s.isSingleplayerOwner(p.getGameProfile()) || (SindicateConfig.OPS_CAN_EDIT.get() && p.hasPermissions(2)));
    }

    private static void onEdit(Edit e, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            if (!canEdit(p)) {
                send(p);  // tell the client again that it may not edit (and the real values)
                return;
            }
            int n = Math.min(e.ids().size(), e.values().size());
            for (int i = 0; i < n; i++) {
                SindicateConfig.Entry en = SindicateConfig.find(SindicateConfig.Side.SERVER, e.ids().get(i));
                if (en != null) SindicateConfig.setFromString(en, e.values().get(i));
            }
            SindicateConfig.SERVER_SPEC.save();
            Sindicate.LOG.info("[Sindicate] {} changed {} server setting(s)", p.getGameProfile().getName(), n);
            for (ServerPlayer o : p.server.getPlayerList().getPlayers()) send(o);
        });
    }

    /** Sends every server option (and whether this player may edit) to one player. */
    public static void send(ServerPlayer p) {
        List<String> ids = new ArrayList<>(), vals = new ArrayList<>();
        for (SindicateConfig.Entry en : SindicateConfig.ENTRIES) {
            if (en.side() != SindicateConfig.Side.SERVER) continue;
            ids.add(en.id());
            vals.add(String.valueOf(en.value().get()));
        }
        PacketDistributor.sendToPlayer(p, new Values(canEdit(p), ids, vals));
    }

    private static void onValues(Values v, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            clientCanEdit = v.canEdit();
            // a remote client holds a copy of the server settings: keep it equal to the server's
            int n = Math.min(v.ids().size(), v.values().size());
            for (int i = 0; i < n; i++) {
                SindicateConfig.Entry en = SindicateConfig.find(SindicateConfig.Side.SERVER, v.ids().get(i));
                try {
                    if (en != null) SindicateConfig.setFromString(en, v.values().get(i));
                } catch (IllegalStateException ignored) {
                    // settings not loaded on this side (should not happen in a world)
                }
            }
            onClientValues.run();
        });
    }

}
