package com.custommobs.theking.client;

import com.custommobs.theking.TheKingMod;
import com.custommobs.theking.anim.KingData;
import com.custommobs.theking.entity.KingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/* ───────── The King's renderer: vanilla mob rendering (shadow, hurt flash, death fall, name tag) + KingModel ───────── */
public final class KingRenderer extends MobRenderer<KingEntity, KingModel> {
    private final ResourceLocation[] textures;

    public KingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KingModel(), 0.45F);
        String[] v = KingData.get().variants; // classic, mint, royal: one texture each, painted by the viewer
        textures = new ResourceLocation[v.length];
        for (int i = 0; i < v.length; i++) textures[i] = ResourceLocation.fromNamespaceAndPath(TheKingMod.MODID, "textures/entity/the_king_" + v[i] + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(KingEntity e) {
        return textures[Math.floorMod(e.getVariant(), textures.length)];
    }
}
