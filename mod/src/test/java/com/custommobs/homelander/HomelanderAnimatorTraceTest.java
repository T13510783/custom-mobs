package com.custommobs.homelander;

import com.custommobs.homelander.anim.HomelanderAnimator;
import com.custommobs.homelander.anim.HomelanderData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/* ───────── 1:1 test: the mod's Homelander runtime against the HTML viewer ─────────
   homelander_modtrace.json = the viewer's own recording (#modtrace, written by tools/export_mod_assets.ps1 -mob homelander):
   a fixed 56 s scene with every animation, look angles, the Clap's voice line and the random Idle extras (a Hover,
   then calm Idle long enough for the cape to settle).
   The Java runtime plays the same scene; every bone's rotation, position and scale and the eye/beam values must match.
   The cape bones (cloth physics) are measured on their own: the baked curves differ a hair (~2e-5) from the viewer's
   exact ones, and cloth with collisions is chaotic: a 1e-5 nudge of one cloth point gives the same error curve
   Java-vs-Java as Java-vs-viewer (checked 2026-10-16). So the cape must match while the motion is calm (first second)
   and again once it settles (last 2 s, idle); in fast attacks it may fold a little differently. */
class HomelanderAnimatorTraceTest {
    @Test
    void sameAsViewer() throws Exception {
        HomelanderData d;
        try (InputStream rig = new FileInputStream("src/main/resources/assets/homelander/homelander/rig.json");
             InputStream bin = new FileInputStream("src/main/resources/assets/homelander/homelander/anims.bin")) {
            d = HomelanderData.load(rig, bin);
        }
        JsonObject trace;
        try (InputStream in = getClass().getResourceAsStream("/homelander_modtrace.json")) {
            trace = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonArray frames = trace.getAsJsonArray("frames"), fx = trace.getAsJsonArray("fx");
        JsonArray plan = trace.getAsJsonArray("plan"), walkAtk = trace.getAsJsonArray("walkAtk");

        HomelanderAnimator a = new HomelanderAnimator(d, 7);
        a.followOn = true;
        a.followHas = true;
        HomelanderData.VoiceLine line = d.voice[0];
        double dt = 1.0 / 120, next = 0, talkStart = -1;
        int p = 0, frame = 0, nb = d.boneNames.length;
        double maxRot = 0, maxPos = 0, maxScl = 0, maxFx = 0, maxCape = 0, sumCape = 0, capeStart = 0, capeEnd = 0;
        int nCape = 0;
        String worstRot = "", worstPos = "", worstFx = "", worstCape = "", firstCape = "none";
        java.util.Set<Integer> cape = new java.util.HashSet<>();
        for (int b : d.cloth.bones) cape.add(b);
        double end = trace.get("end").getAsDouble();
        for (int i = 0; i <= end * 120; i++) {
            double t = i * dt;
            while (p < plan.size() && t >= plan.get(p).getAsJsonArray().get(0).getAsDouble() - 1e-9) {
                a.switchAnim(plan.get(p).getAsJsonArray().get(1).getAsString());
                p++;
            }
            a.lookYaw = 0.6 * Math.sin(0.7 * t);
            a.lookPitch = 0.1 * Math.sin(0.5 * t);
            a.walkAtk = t >= walkAtk.get(0).getAsDouble() && t < walkAtk.get(1).getAsDouble() ? 1 : 0;   // walk-while-attacking window
            // the viewer's voice clock: mouth from the curve, 30 values per second since the "voice" event
            int k = talkStart >= 0 ? (int) Math.floor((a.time - talkStart) * 30) : -1;
            if (k >= line.env.length()) talkStart = -1;
            a.talkActive = k >= 0 && k < line.env.length();
            if (a.talkActive) {
                a.talkLevel = Math.min(1, Character.digit(line.env.charAt(k), 36) / 35.0 * 1.1);
                a.talkShape = Character.digit(line.zcr.charAt(k), 10) / 9.0;
            }
            a.step(dt, 1);
            if (a.firedEvent("voice")) talkStart = a.time;
            if (t >= next - 1e-9) {
                next += 1.0 / 30;
                JsonArray ref = frames.get(frame).getAsJsonArray();
                for (int b = 0; b < nb; b++) {
                    JsonArray r = ref.get(b).getAsJsonArray();
                    if (cape.contains(b)) {
                        // cape bones: Euler angles are ambiguous near gimbal lock, so compare the real rotation (angle between quaternions)
                        double er = quatAngle(a.rot[b * 3], a.rot[b * 3 + 1], a.rot[b * 3 + 2], r.get(0).getAsDouble(), r.get(1).getAsDouble(), r.get(2).getAsDouble());
                        sumCape += er; nCape++;
                        if (t < 1) capeStart = Math.max(capeStart, er);
                        if (t > end - 2) capeEnd = Math.max(capeEnd, er);                        if (er > 0.001 && firstCape.equals("none")) firstCape = d.boneNames[b] + " " + String.format("%.5f", er) + " at " + String.format("%.3f", t) + " s (" + a.anim.name + ")";
                        if (er > maxCape) { maxCape = er; worstCape = d.boneNames[b] + " at " + String.format("%.3f", t) + " s (" + a.anim.name + ")"; }
                        continue;
                    }
                    for (int q = 0; q < 3; q++) {
                        // angle difference wrapped to [-pi, pi]: +pi and -pi are the same angle
                        double dr = a.rot[b * 3 + q] - r.get(q).getAsDouble();
                        double er = Math.abs(dr - 2 * Math.PI * Math.rint(dr / (2 * Math.PI)));
                        double ep = Math.abs(a.pos[b * 3 + q] - r.get(3 + q).getAsDouble());
                        double es = Math.abs(a.scl[b * 3 + q] - r.get(6 + q).getAsDouble());
                        if (er > maxRot) { maxRot = er; worstRot = d.boneNames[b] + " axis " + q + " at " + String.format("%.3f", t) + " s (" + a.anim.name + ")"; }
                        if (ep > maxPos) { maxPos = ep; worstPos = d.boneNames[b] + " axis " + q + " at " + String.format("%.3f", t) + " s (" + a.anim.name + ")"; }
                        maxScl = Math.max(maxScl, es);
                    }
                }
                JsonArray f = fx.get(frame).getAsJsonArray();
                double[] mine = {a.glowOpacity, a.coreOpacity, a.haloOpacity, a.laserOpacity, a.beamW, a.glance};
                String[] names = {"glow", "core", "halo", "laser", "beamW", "glance"};
                for (int q = 0; q < mine.length; q++) {
                    double e = Math.abs(mine[q] - f.get(q).getAsDouble());
                    if (e > maxFx) { maxFx = e; worstFx = names[q] + " at " + String.format("%.3f", t) + " s (" + a.anim.name + ")"; }
                }
                frame++;
            }
        }
        System.out.printf("Homelander: compared %d frames x %d bones. Max difference: rotation %.5f rad (%s), position %.5f texel (%s), scale %.5f, fx %.5f (%s); cape first second %.5f, last 2 s %.5f, max %.5f rad (%s), mean %.6f, first > 0.001: %s%n",
                frame, nb, maxRot, worstRot, maxPos, worstPos, maxScl, maxFx, worstFx, capeStart, capeEnd, maxCape, worstCape, sumCape / Math.max(1, nCape), firstCape);
        assertTrue(frame == frames.size(), "frame count " + frame + " vs " + frames.size());
        assertTrue(maxRot < 0.001, "rotation differs " + maxRot + " at " + worstRot);
        assertTrue(maxPos < 0.005, "position differs " + maxPos + " at " + worstPos);
        assertTrue(maxScl < 0.001, "scale differs " + maxScl);
        assertTrue(capeStart < 0.001, "cape differs in the first second: " + capeStart);
        assertTrue(capeEnd < 0.001, "cape does not settle back to the viewer's: " + capeEnd);
        assertTrue(sumCape / Math.max(1, nCape) < 0.1, "cape differs on average " + sumCape / Math.max(1, nCape));
        assertTrue(maxFx < 0.002, "eye/beam values differ " + maxFx + " at " + worstFx);
    }

    /** Angle (rad) between two ZYX Euler rotations, via their quaternions (three.js order 'ZYX'). */
    private static double quatAngle(double ax, double ay, double az, double bx, double by, double bz) {
        double[] p = quat(ax, ay, az), q = quat(bx, by, bz);
        double dot = Math.min(1, Math.abs(p[0] * q[0] + p[1] * q[1] + p[2] * q[2] + p[3] * q[3]));
        return 2 * Math.acos(dot);
    }

    private static double[] quat(double x, double y, double z) {
        double c1 = Math.cos(x / 2), c2 = Math.cos(y / 2), c3 = Math.cos(z / 2);
        double s1 = Math.sin(x / 2), s2 = Math.sin(y / 2), s3 = Math.sin(z / 2);
        return new double[] {
            s1 * c2 * c3 - c1 * s2 * s3, c1 * s2 * c3 + s1 * c2 * s3,
            c1 * c2 * s3 - s1 * s2 * c3, c1 * c2 * c3 + s1 * s2 * s3 };
    }
}
