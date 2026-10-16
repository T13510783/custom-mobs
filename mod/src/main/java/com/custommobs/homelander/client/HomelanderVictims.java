package com.custommobs.homelander.client;

import com.custommobs.Sindicate;
import com.custommobs.homelander.anim.HomelanderAnimator;
import com.custommobs.homelander.anim.HomelanderData;
import com.custommobs.homelander.entity.HomelanderEntity;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.HashMap;
import java.util.Map;

/* ───────── Victims of the Clap, on the client (any mob, best on humanoids) ─────────
   The viewer's victimPose() for real mobs: Homelander syncs who his victim is (getVictimId); from then on this
   class remembers it with the start time of his attack and turns the victim's whole body around its feet while
   it is drawn (RenderLivingEvent.Pre), so it works for every mob without touching its model:
     Clap:  after the smack it falls onto its back (head away from him) and lies there, rolling its head,
            then gets up again. Humanoid models also put both hands on their ears (LivingEntityRendererMixin).
   Times come from the attack's own events (rig.json), so they stay 1:1 with the viewer. */
@EventBusSubscriber(modid = Sindicate.MODID, value = Dist.CLIENT)
public final class HomelanderVictims {
    private static final double TAU = Math.PI * 2;

    private static final class State {
        final String kind;      // "clap"
        final double start;     // client seconds when his attack started (t = 0)
        final int homelander;

        State(String kind, double start, int homelander) { this.kind = kind; this.start = start; this.homelander = homelander; }
    }

    private static final Map<Integer, State> STATES = new HashMap<>();

    private HomelanderVictims() {}

    private static double now(Entity e, float partial) { return (e.level().getGameTime() + partial) / 20.0; }

    private static double smooth01(double x) { x = Mth.clamp(x, 0, 1); return x * x * (3 - 2 * x); }

    /** Called every frame Homelander is drawn: remember the victim of a running clap. */
    static void track(HomelanderEntity h, HomelanderAnimator a, float partial) {
        String n = a.anim.name;
        int id = h.getVictimId();
        if (id < 0 || !n.equals("clap")) return;
        double start = now(h, partial) - a.t;
        State s = STATES.get(id);
        if (s == null || !s.kind.equals(n) || s.homelander != h.getId() || Math.abs(s.start - start) > 0.5) STATES.put(id, new State(n, start, h.getId()));
    }

    /* ───────── The pose at time u of the attack: lean back (rad, 0 = standing, PI/2 = flat on its back), which way
       its feet face (yaw, degrees, Minecraft), a head roll, how much the arms go up (ears). null = done. ───────── */
    private static final class Pose { double lean, roll, ears, lift; float yaw; boolean ownYaw; }
    private static final Pose P = new Pose();

    private static Pose pose(State s, LivingEntity v, double u, float partial) {
        HomelanderData d = HomelanderData.get();
        HomelanderData.Anim A = d.anim(s.kind);
        Entity he = v.level().getEntity(s.homelander);
        P.lean = 0; P.roll = 0; P.ears = 0; P.lift = 0; P.ownYaw = false;
        if (he instanceof HomelanderEntity h) {      // its feet toward him (head away), like the dummy
            double dx = Mth.lerp(partial, h.xo, h.getX()) - Mth.lerp(partial, v.xo, v.getX());
            double dz = Mth.lerp(partial, h.zo, h.getZ()) - Mth.lerp(partial, v.zo, v.getZ());
            if (dx * dx + dz * dz > 1e-6) { P.yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F; P.ownYaw = true; }
        }
        double hit = A.event("hit", 1.2), w = u - hit;
        if (w > 4.2) return null;
        if (w <= 0) return P;
        double down = smooth01((w - 0.95) / 0.4) * (1 - smooth01((w - 3.4) / 0.5));   // falls, lies, gets up
        P.lean = Math.PI / 2 * down;
        P.lift = down;
        P.roll = 0.35 * down * Math.sin(TAU * 1.4 * w);                                 // rolls its head in pain
        P.ears = smooth01((w - 0.18) / 0.22) * (1 - smooth01((w - 3.5) / 0.4));         // hands on its ears (after his are off)
        return P;
    }

    /* ───────── Drawing: turn the whole victim around its feet (works for every mob) ───────── */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> e) {
        LivingEntity v = e.getEntity();
        State s = STATES.get(v.getId());
        if (s == null) return;
        float partial = e.getPartialTick();
        Pose p = v.isAlive() ? pose(s, v, now(v, partial) - s.start, partial) : null;
        if (p == null) { STATES.remove(v.getId()); return; }
        if (HomelanderEntity.DEBUG && v.tickCount % 10 == 0 && partial < 0.2F)
            HomelanderEntity.LOG.info("[Homelander] victim {} {}: u {} lean {} yaw {} body {} ears {} at {} {} {} ground {}", v.getName().getString(), s.kind,
                    String.format("%.2f", now(v, partial) - s.start), String.format("%.2f", p.lean), String.format("%.0f", p.yaw), String.format("%.0f", v.yBodyRot),
                    String.format("%.2f", p.ears), String.format("%.2f", v.getX()), String.format("%.2f", v.getY()), String.format("%.2f", v.getZ()), v.onGround());
        if (p.lean == 0 && p.roll == 0) return;
        float body = Mth.rotLerp(partial, v.yBodyRotO, v.yBodyRot);
        float yaw = p.ownYaw ? p.yaw : body;
        var ps = e.getPoseStack();
        // lying: its back rests on the floor instead of half inside it (body half depth)
        ps.translate(0, v.getBbWidth() * 0.5 * Mth.clamp(p.lift, 0, 1), 0);
        // net turn = Ry(180 - yaw) x lean back x head roll; the renderer adds Ry(180 - body) itself, so undo that first
        ps.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        ps.mulPose(Axis.XP.rotation((float) p.lean));
        ps.mulPose(Axis.ZP.rotation((float) p.roll));
        ps.mulPose(Axis.YP.rotationDegrees(-(180F - body)));
    }

    /* ───────── Humanoid arms (LivingEntityRendererMixin, after the model's own setupAnim) ───────── */
    public static void poseHumanoid(LivingEntity v, HumanoidModel<?> m, float partial) {
        State s = STATES.get(v.getId());
        if (s == null) return;
        Pose p = pose(s, v, now(v, partial) - s.start, partial);
        if (p == null) return;
        // hands on its ears: both arms up beside the head, hands turned in over the ears (one-bone arms: no elbow)
        double e = p.ears;
        if (e > 0) {
            m.rightArm.xRot = (float) Mth.lerp(e, m.rightArm.xRot, -2.75);
            m.rightArm.yRot = (float) Mth.lerp(e, m.rightArm.yRot, 0);
            m.rightArm.zRot = (float) Mth.lerp(e, m.rightArm.zRot, 0.55);
            m.leftArm.xRot = (float) Mth.lerp(e, m.leftArm.xRot, -2.75);
            m.leftArm.yRot = (float) Mth.lerp(e, m.leftArm.yRot, 0);
            m.leftArm.zRot = (float) Mth.lerp(e, m.leftArm.zRot, -0.55);
        }
        // players: the sleeve layer was copied from the arms inside setupAnim, copy it again
        if (e > 0 && m instanceof PlayerModel<?> pm) {
            pm.rightSleeve.copyFrom(pm.rightArm);
            pm.leftSleeve.copyFrom(pm.leftArm);
        }
    }
}
