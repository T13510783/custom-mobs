package com.custommobs.homelander.client;

import com.custommobs.homelander.anim.HomelanderAnimator;
import com.custommobs.homelander.anim.HomelanderCloth;
import com.custommobs.homelander.anim.HomelanderData;
import com.custommobs.homelander.entity.HomelanderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/* ───────── Draws Homelander exactly like the HTML viewer ─────────
   Body: the viewer's own three.js boxes, exported 1:1 as quads (rig.json), each drawn under its bone's matrix.
   Cape: one closed cloth surface built every frame from the cloth points (viewer "Cape mesh": outside + lining
   CAPE_T on each side of the cloth, side edges, hem, top), textured from the cape's atlas rects.
   Eye glow + heat-vision beams: glowing quads in the head bone (viewer eyeGlow / lasers), drawn additive.
   No allocation per frame. */
public final class HomelanderModel extends EntityModel<HomelanderEntity> {
    private HomelanderClientState state;
    private HomelanderEntity entity;
    private float partial;
    /** Set by the renderer before each draw: the glow + beams need their own render type. */
    MultiBufferSource buffers;

    // Cape mesh layout (viewer CAPE_MESH): outside grid, lining grid, side edges, hem, top
    private final int NK, NP, N, G;
    private final float[] cpos, cnrm, cuv;
    private final int[] cquads;
    private final double[] cn, ca;

    public HomelanderModel() {
        super(RenderType::entityCutout); // back faces culled like the viewer
        HomelanderData d = HomelanderData.get();
        HomelanderData.Cloth c = d.cloth;
        NK = c.cols; N = c.n; NP = N + 1; G = NK * NP;
        int nv = 2 * G + 4 * NP + 4 * NK;
        cpos = new float[nv * 3]; cnrm = new float[nv * 3]; cuv = new float[nv * 2];
        cn = new double[G * 3]; ca = new double[G * 3];
        int nq = 2 * (NK - 1) * N + 2 * N + 2 * (NK - 1);
        cquads = new int[nq * 4];
        int q = 0;
        for (int k = 0; k < NK - 1; k++) for (int i = 0; i < N; i++) {
            q = quad(q, O(k, i), O(k + 1, i), O(k, i + 1), O(k + 1, i + 1));          // outside faces away from his back
            q = quad(q, I(k + 1, i), I(k, i), I(k + 1, i + 1), I(k, i + 1));          // lining faces his back
        }
        for (int i = 0; i < N; i++) {
            q = quad(q, SL(i, 1), SL(i, 0), SL(i + 1, 1), SL(i + 1, 0));              // -x edge
            q = quad(q, SR(i, 0), SR(i, 1), SR(i + 1, 0), SR(i + 1, 1));              // +x edge
        }
        for (int k = 0; k < NK - 1; k++) {
            q = quad(q, HM(k, 0), HM(k + 1, 0), HM(k, 1), HM(k + 1, 1));              // hem (faces down)
            q = quad(q, TP(k, 1), TP(k + 1, 1), TP(k, 0), TP(k + 1, 0));              // top (faces up)
        }
        // UVs: atlas rect f of the cape, fu / fv = 0..1 across / down it (v down, like Minecraft)
        for (int k = 0; k < NK; k++) for (int i = 0; i < NP; i++) {
            double a = k / (double) (NK - 1), b = i / (double) N;
            uv(d, 5, O(k, i), 1 - a, b); uv(d, 4, I(k, i), a, b);                 // back face = outside (mirrored), front = lining
        }
        for (int i = 0; i < NP; i++) for (int s = 0; s < 2; s++) { uv(d, 1, SL(i, s), s, i / (double) N); uv(d, 0, SR(i, s), 1 - s, i / (double) N); }
        for (int k = 0; k < NK; k++) for (int s = 0; s < 2; s++) {
            uv(d, 3, HM(k, s), k / (double) (NK - 1), s); uv(d, 2, TP(k, s), k / (double) (NK - 1), 1 - s);
        }
    }

    private int O(int k, int i) { return k * NP + i; }
    private int I(int k, int i) { return G + k * NP + i; }
    private int SL(int i, int s) { return 2 * G + i * 2 + s; }
    private int SR(int i, int s) { return 2 * G + 2 * NP + i * 2 + s; }
    private int HM(int k, int s) { return 2 * G + 4 * NP + k * 2 + s; }
    private int TP(int k, int s) { return 2 * G + 4 * NP + 2 * NK + k * 2 + s; }

    /** Viewer quad(a, b, c, d) = triangles (a, b, c), (b, d, c): as one Minecraft quad that is a, b, d, c. */
    private int quad(int q, int a, int b, int c, int d) {
        cquads[q] = a; cquads[q + 1] = b; cquads[q + 2] = d; cquads[q + 3] = c;
        return q + 4;
    }

    private void uv(HomelanderData d, int f, int v, double fu, double fv) {
        int[] r = d.cloth.rects[f];
        cuv[v * 2] = (float) ((r[0] + fu * r[2]) / d.atlasW);
        cuv[v * 2 + 1] = (float) ((r[1] + fv * r[3]) / d.atlasH);
    }

    @Override
    public void setupAnim(HomelanderEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        entity = e;
        partial = ageInTicks - e.tickCount;
        state = HomelanderClientState.of(e);
        state.update(e, ageInTicks, netHeadYaw, headPitch);
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
        if (state == null) return;
        HomelanderAnimator a = state.a;
        HomelanderData d = a.d;
        ps.pushPose();
        // LivingEntityRenderer flipped X/Y and moved down 1.501 for vanilla models; undo that and turn
        // 180 degrees so the viewer's axes (+z = his front, +y = up, feet at 0) line up with the entity.
        ps.translate(0F, 1.501F, 0F);
        ps.scale(1F, -1F, -1F);
        PoseStack.Pose pose = ps.last();
        double[] m = a.mat;
        for (HomelanderData.Cube c : d.cubes) {
            int o = c.bone * 12;
            for (HomelanderData.Quad q : c.quads) {
                float nx = (float) (m[o] * q.n[0] + m[o + 1] * q.n[1] + m[o + 2] * q.n[2]);
                float ny = (float) (m[o + 4] * q.n[0] + m[o + 5] * q.n[1] + m[o + 6] * q.n[2]);
                float nz = (float) (m[o + 8] * q.n[0] + m[o + 9] * q.n[1] + m[o + 10] * q.n[2]);
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) { nx /= len; ny /= len; nz /= len; }
                for (int k = 0; k < 4; k++) vertex(vc, pose, m, o, q, k, color, overlay, light, nx, ny, nz);
                if (c.twoSided) for (int k = 3; k >= 0; k--) vertex(vc, pose, m, o, q, k, color, overlay, light, -nx, -ny, -nz);
            }
        }
        drawCape(vc, pose, a.cloth, d.cloth.thick, light, overlay, color);
        // Glow + beams last: asking for another buffer finishes the body's batch (vc is not used after this).
        if (buffers != null && (a.glowOpacity > 0.003 || a.laserOpacity > 0.003)) drawGlow(buffers.getBuffer(RenderType.lightning()), pose, a, d);
        ps.popPose();
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, double[] m, int o, HomelanderData.Quad q, int k,
                               int color, int overlay, int light, float nx, float ny, float nz) {
        double x = q.v[k * 3] / 16, y = q.v[k * 3 + 1] / 16, z = q.v[k * 3 + 2] / 16; // texels -> blocks
        float px = (float) (m[o] * x + m[o + 1] * y + m[o + 2] * z + m[o + 3]);
        float py = (float) (m[o + 4] * x + m[o + 5] * y + m[o + 6] * z + m[o + 7]);
        float pz = (float) (m[o + 8] * x + m[o + 9] * y + m[o + 10] * z + m[o + 11]);
        vc.addVertex(pose, px, py, pz).setColor(color).setUv(q.uv[k * 2], q.uv[k * 2 + 1]).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

    /* ───────── Cape: viewer updateCapeMesh, from the cloth points (texels, model space) ───────── */
    private void drawCape(VertexConsumer vc, PoseStack.Pose pose, HomelanderCloth cl, double T, int light, int overlay, int color) {
        double[] P = cl.P;
        // normal = across x down (central differences, one-sided at the edges): points away from his back
        for (int k = 0; k < NK; k++) for (int i = 0; i < NP; i++) {
            int j = (k * NP + i) * 3;
            int ka = Math.min(NK - 1, k + 1), kb = Math.max(0, k - 1), ia = Math.min(N, i + 1), ib = Math.max(0, i - 1);
            int a1 = (ka * NP + i) * 3, a0 = (kb * NP + i) * 3, d1 = (k * NP + ia) * 3, d0 = (k * NP + ib) * 3;
            double ax = P[a1] - P[a0], ay = P[a1 + 1] - P[a0 + 1], az = P[a1 + 2] - P[a0 + 2];
            double dx = P[d1] - P[d0], dy = P[d1 + 1] - P[d0 + 1], dz = P[d1 + 2] - P[d0 + 2];
            double nx = ay * dz - az * dy, ny = az * dx - ax * dz, nz = ax * dy - ay * dx;
            double nl = Math.sqrt(nx * nx + ny * ny + nz * nz), al = Math.sqrt(ax * ax + ay * ay + az * az);
            if (nl == 0) nl = 1;
            if (al == 0) al = 1;
            cn[j] = nx / nl; cn[j + 1] = ny / nl; cn[j + 2] = nz / nl;
            ca[j] = ax / al; ca[j + 1] = ay / al; ca[j + 2] = az / al;
        }
        for (int k = 0; k < NK; k++) for (int i = 0; i < NP; i++) {
            int j = (k * NP + i) * 3;
            put(O(k, i), P, j, T, 1, cn[j], cn[j + 1], cn[j + 2]);
            put(I(k, i), P, j, T, -1, -cn[j], -cn[j + 1], -cn[j + 2]);
        }
        for (int i = 0; i < NP; i++) {
            int l = i * 3, r = ((NK - 1) * NP + i) * 3;
            for (int s = 0; s < 2; s++) {
                put(SL(i, s), P, l, T, s == 1 ? -1 : 1, -ca[l], -ca[l + 1], -ca[l + 2]);
                put(SR(i, s), P, r, T, s == 1 ? -1 : 1, ca[r], ca[r + 1], ca[r + 2]);
            }
        }
        for (int k = 0; k < NK; k++) {
            int h = (k * NP + N) * 3, h0 = h - 3, t = k * NP * 3, t1 = t + 3;
            double dx = P[h] - P[h0], dy = P[h + 1] - P[h0 + 1], dz = P[h + 2] - P[h0 + 2], dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dl == 0) dl = 1;
            for (int s = 0; s < 2; s++) put(HM(k, s), P, h, T, s == 1 ? -1 : 1, dx / dl, dy / dl, dz / dl);
            dx = P[t] - P[t1]; dy = P[t + 1] - P[t1 + 1]; dz = P[t + 2] - P[t1 + 2]; dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dl == 0) dl = 1;
            for (int s = 0; s < 2; s++) put(TP(k, s), P, t, T, s == 1 ? -1 : 1, dx / dl, dy / dl, dz / dl);
        }
        for (int q = 0; q < cquads.length; q++) {
            int v = cquads[q];
            vc.addVertex(pose, cpos[v * 3], cpos[v * 3 + 1], cpos[v * 3 + 2]).setColor(color).setUv(cuv[v * 2], cuv[v * 2 + 1])
                    .setOverlay(overlay).setLight(light).setNormal(pose, cnrm[v * 3], cnrm[v * 3 + 1], cnrm[v * 3 + 2]);
        }
    }

    /** Vertex v at cloth point j, s = +1 outside / -1 lining (texels -> blocks). */
    private void put(int v, double[] P, int j, double T, int s, double nx, double ny, double nz) {
        cpos[v * 3] = (float) ((P[j] + cn[j] * T * s) / 16);
        cpos[v * 3 + 1] = (float) ((P[j + 1] + cn[j + 1] * T * s) / 16);
        cpos[v * 3 + 2] = (float) ((P[j + 2] + cn[j + 2] * T * s) / 16);
        cnrm[v * 3] = (float) nx; cnrm[v * 3 + 1] = (float) ny; cnrm[v * 3 + 2] = (float) nz;
    }

    /* ───────── Eye glow + heat-vision beams (viewer eyeGlow / lasers) ───────── */
    private final double[] eo = new double[3], ed = new double[3];

    private void drawGlow(VertexConsumer g, PoseStack.Pose pose, HomelanderAnimator a, HomelanderData d) {
        int head = d.bone("head");
        double[] m = a.mat;
        int o = head * 12;
        for (int e = 0; e < d.eyesX.length; e++) {
            double x = d.eyesX[e];
            // red eye white, hot core on the pupil (moves with the glance), faint halo
            plane(g, pose, m, o, x, d.eyeY, d.glowZ[0], d.glowSize[0], d.glowColors[0], a.glowOpacity);
            plane(g, pose, m, o, x + a.glance, d.eyeY, d.glowZ[1], d.glowSize[1], d.glowColors[1], a.coreOpacity);
            plane(g, pose, m, o, x, d.eyeY, d.glowZ[2], d.glowSize[2], d.glowColors[2], a.haloOpacity);
        }
        if (a.laserOpacity <= 0.003) return;
        // beams: from the front of each eye along the head's +z, or onto his heat-vision target
        Entity target = entity == null ? null : entity.getLaserTarget();
        double len0 = a.anim.beam;   // texels (Throat laser: the victim's face is right there)
        for (int e = 0; e < d.eyesX.length; e++) {
            double ex = d.eyesX[e] / 16, ey = d.eyeY / 16, ez = d.beamZ / 16;
            eo[0] = m[o] * ex + m[o + 1] * ey + m[o + 2] * ez + m[o + 3];
            eo[1] = m[o + 4] * ex + m[o + 5] * ey + m[o + 6] * ez + m[o + 7];
            eo[2] = m[o + 8] * ex + m[o + 9] * ey + m[o + 10] * ez + m[o + 11];
            double hx = m[o + 2], hy = m[o + 6], hz = m[o + 10], hl = Math.sqrt(hx * hx + hy * hy + hz * hz);
            ed[0] = hx / hl; ed[1] = hy / hl; ed[2] = hz / hl;
            double len;
            if (len0 > 0) len = len0 / 16;
            else {
                if (target != null) aimAt(target, a);
                len = clipLength(a, 16);
            }
            double w = a.beamW;
            beam(g, pose, len, d.beamR[0] * w / 16, d.beamSides[0], d.beamColors[0], a.laserOpacity);
            beam(g, pose, len, d.beamR[1] * w / 16, d.beamSides[1], d.beamColors[1], a.laserCoreOpacity);
        }
    }

    /** Turn the beam direction (ed) toward the target's chest if it is roughly in front of his face. */
    private void aimAt(Entity t, HomelanderAnimator a) {
        double wx = Mth.lerp(partial, t.xo, t.getX()) - a.worldX, wy = Mth.lerp(partial, t.yo, t.getY()) + t.getBbHeight() * 0.6 - a.worldY;
        double wz = Mth.lerp(partial, t.zo, t.getZ()) - a.worldZ;
        double c = Math.cos(a.worldYaw), s = Math.sin(a.worldYaw);
        double x = c * wx + s * wz - eo[0], y = wy - eo[1], z = -s * wx + c * wz - eo[2];   // world -> his model space
        double l = Math.sqrt(x * x + y * y + z * z);
        if (l < 1e-4) return;
        x /= l; y /= l; z /= l;
        if (x * ed[0] + y * ed[1] + z * ed[2] < 0.25) return; // behind / far to the side: keep the head's direction
        ed[0] = x; ed[1] = y; ed[2] = z;
    }

    /** Beam length (blocks): to the first block or entity in the way, at most max. */
    private double clipLength(HomelanderAnimator a, double max) {
        double c = Math.cos(a.worldYaw), s = Math.sin(a.worldYaw);
        Vec3 from = new Vec3(c * eo[0] - s * eo[2] + a.worldX, eo[1] + a.worldY, s * eo[0] + c * eo[2] + a.worldZ);
        Vec3 dir = new Vec3(c * ed[0] - s * ed[2], ed[1], s * ed[0] + c * ed[2]);
        Vec3 to = from.add(dir.scale(max));
        BlockHitResult hit = entity.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        double len = hit.getType() == HitResult.Type.MISS ? max : hit.getLocation().distanceTo(from);
        Entity t = entity.getLaserTarget();
        if (t != null) {
            var box = t.getBoundingBox().move(Mth.lerp(partial, t.xo, t.getX()) - t.getX(), Mth.lerp(partial, t.yo, t.getY()) - t.getY(), Mth.lerp(partial, t.zo, t.getZ()) - t.getZ());
            var e = box.clip(from, to);
            if (e.isPresent()) len = Math.min(len, e.get().distanceTo(from));
        }
        return len;
    }

    /** A flat glowing rectangle facing the head's +z (viewer PlaneGeometry), center (x, y, z) texels. */
    private static void plane(VertexConsumer g, PoseStack.Pose pose, double[] m, int o, double x, double y, double z, double[] size, int rgb, double alpha) {
        if (alpha <= 0.003) return;
        double w = size[0] / 2, h = size[1] / 2;
        double[][] c = { { x - w, y - h }, { x + w, y - h }, { x + w, y + h }, { x - w, y + h } }; // counter-clockwise seen from the front
        int r = rgb >> 16 & 255, gg = rgb >> 8 & 255, b = rgb & 255, al = (int) (Math.min(1, alpha) * 255);
        for (double[] p : c) {
            double px = p[0] / 16, py = p[1] / 16, pz = z / 16;
            g.addVertex(pose, (float) (m[o] * px + m[o + 1] * py + m[o + 2] * pz + m[o + 3]), (float) (m[o + 4] * px + m[o + 5] * py + m[o + 6] * pz + m[o + 7]),
                    (float) (m[o + 8] * px + m[o + 9] * py + m[o + 10] * pz + m[o + 11])).setColor(r, gg, b, al);
        }
    }

    /** A glowing prism from eo along ed (blocks, model space), radius rad, n sides (viewer CylinderGeometry). */
    private void beam(VertexConsumer g, PoseStack.Pose pose, double len, double rad, int n, int rgb, double alpha) {
        if (alpha <= 0.003 || len <= 0) return;
        // U, V perpendicular to the beam with U x V = ed: the side quads then face outward (counter-clockwise)
        double ux = ed[1], uy = -ed[0], uz = 0;
        if (Math.abs(ed[2]) > 0.9) { ux = 0; uy = ed[2]; uz = -ed[1]; }
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul; uy /= ul; uz /= ul;
        double vx = ed[1] * uz - ed[2] * uy, vy = ed[2] * ux - ed[0] * uz, vz = ed[0] * uy - ed[1] * ux;   // V = ed x U
        int r = rgb >> 16 & 255, gg = rgb >> 8 & 255, b = rgb & 255, al = (int) (Math.min(1, alpha) * 255);
        double ex = eo[0] + ed[0] * len, ey = eo[1] + ed[1] * len, ez = eo[2] + ed[2] * len;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            double c0 = Math.cos(a0) * rad, s0 = Math.sin(a0) * rad, c1 = Math.cos(a1) * rad, s1 = Math.sin(a1) * rad;
            double ox0 = c0 * ux + s0 * vx, oy0 = c0 * uy + s0 * vy, oz0 = c0 * uz + s0 * vz;
            double ox1 = c1 * ux + s1 * vx, oy1 = c1 * uy + s1 * vy, oz1 = c1 * uz + s1 * vz;
            g.addVertex(pose, (float) (eo[0] + ox0), (float) (eo[1] + oy0), (float) (eo[2] + oz0)).setColor(r, gg, b, al);
            g.addVertex(pose, (float) (eo[0] + ox1), (float) (eo[1] + oy1), (float) (eo[2] + oz1)).setColor(r, gg, b, al);
            g.addVertex(pose, (float) (ex + ox1), (float) (ey + oy1), (float) (ez + oz1)).setColor(r, gg, b, al);
            g.addVertex(pose, (float) (ex + ox0), (float) (ey + oy0), (float) (ez + oz0)).setColor(r, gg, b, al);
        }
    }
}
