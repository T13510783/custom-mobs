package com.custommobs.homelander.client;

import com.custommobs.homelander.HomelanderMod;
import com.custommobs.homelander.anim.HomelanderData;
import com.custommobs.homelander.entity.HomelanderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/* ───────── Homelander's renderer: vanilla mob rendering (shadow, hurt flash, death fall, name tag) + HomelanderModel ───────── */
public final class HomelanderRenderer extends MobRenderer<HomelanderEntity, HomelanderModel> {
    private final ResourceLocation[] textures;

    public HomelanderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HomelanderModel(), 0.45F);
        String[] v = HomelanderData.get().variants; // classic, damaged, comic: one texture each, painted by the viewer
        textures = new ResourceLocation[v.length];
        for (int i = 0; i < v.length; i++) textures[i] = ResourceLocation.fromNamespaceAndPath(HomelanderMod.NS, "textures/entity/homelander_" + v[i] + ".png");
    }

    @Override
    public void render(HomelanderEntity e, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light) {
        model.buffers = buffers; // the eye glow and beams draw into their own render type
        super.render(e, yaw, partial, ps, buffers, light);
        model.buffers = null;
    }

    @Override
    public ResourceLocation getTextureLocation(HomelanderEntity e) {
        return textures[Math.floorMod(e.getVariant(), textures.length)];
    }
}
