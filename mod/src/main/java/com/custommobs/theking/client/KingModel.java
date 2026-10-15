package com.custommobs.theking.client;

import com.custommobs.theking.anim.KingAnimator;
import com.custommobs.theking.anim.KingData;
import com.custommobs.theking.entity.KingEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;

/* ───────── Draws The King exactly like the HTML viewer ─────────
   The quads (corners, UVs, normals) are the viewer's own three.js boxes, exported 1:1 (rig.json).
   Each frame the animator poses the bones; every quad is drawn under its bone's matrix.
   ~190 quads per King, no allocation per frame. */
public final class KingModel extends EntityModel<KingEntity> {
    private KingClientState state;
    private final int root;

    public KingModel() {
        super(RenderType::entityCutout); // back faces culled like the viewer (cutout parts get their inside drawn separately)
        root = KingData.get().bone("root");
    }

    @Override
    public void setupAnim(KingEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        state = KingClientState.of(e);
        state.update(e, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
        if (state == null) return;
        KingAnimator a = state.a;
        KingData d = a.d;
        // Root motion is real movement of the entity (dropkick leap): take it out of the drawn pose.
        double rz = a.pos[root * 3 + 2], ry = a.pos[root * 3 + 1];
        a.pos[root * 3 + 2] = rz - a.rmZ;
        a.pos[root * 3 + 1] = ry - a.rmY;
        a.computeMatrices();
        a.pos[root * 3 + 2] = rz;
        a.pos[root * 3 + 1] = ry;

        ps.pushPose();
        // LivingEntityRenderer flipped X/Y and moved down 1.501 for vanilla models; undo that and turn
        // 180 degrees so the viewer's axes (+z = his front, +y = up, feet at 0) line up with the entity.
        ps.translate(0F, 1.501F, 0F);
        ps.scale(1F, -1F, -1F);
        PoseStack.Pose pose = ps.last();
        double[] m = a.mat;
        for (KingData.Cube c : d.cubes) {
            int o = c.bone * 12;
            for (KingData.Quad q : c.quads) {
                float nx = (float) (m[o] * q.n[0] + m[o + 1] * q.n[1] + m[o + 2] * q.n[2]);
                float ny = (float) (m[o + 4] * q.n[0] + m[o + 5] * q.n[1] + m[o + 6] * q.n[2]);
                float nz = (float) (m[o + 8] * q.n[0] + m[o + 9] * q.n[1] + m[o + 10] * q.n[2]);
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) { nx /= len; ny /= len; nz /= len; }
                for (int k = 0; k < 4; k++) vertex(vc, pose, m, o, q, k, color, overlay, light, nx, ny, nz);
                if (c.twoSided) for (int k = 3; k >= 0; k--) vertex(vc, pose, m, o, q, k, color, overlay, light, -nx, -ny, -nz);
            }
        }
        ps.popPose();
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, double[] m, int o, KingData.Quad q, int k,
                               int color, int overlay, int light, float nx, float ny, float nz) {
        double x = q.v[k * 3] / 16, y = q.v[k * 3 + 1] / 16, z = q.v[k * 3 + 2] / 16; // texels -> blocks
        float px = (float) (m[o] * x + m[o + 1] * y + m[o + 2] * z + m[o + 3]);
        float py = (float) (m[o + 4] * x + m[o + 5] * y + m[o + 6] * z + m[o + 7]);
        float pz = (float) (m[o + 8] * x + m[o + 9] * y + m[o + 10] * z + m[o + 11]);
        vc.addVertex(pose, px, py, pz).setColor(color).setUv(q.uv[k * 2], q.uv[k * 2 + 1]).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
