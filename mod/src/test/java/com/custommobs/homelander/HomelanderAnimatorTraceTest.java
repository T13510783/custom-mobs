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
   a fixed 36 s scene with every animation, look angles, the Clap's voice line and the random Idle extras.
   The Java runtime plays the same scene; every bone's rotation, position and scale and the eye/beam values must match. */
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
        JsonArray plan = trace.getAsJsonArray("plan");

        HomelanderAnimator a = new HomelanderAnimator(d, 7);
        a.followOn = true;
        a.followHas = true;
        HomelanderData.VoiceLine line = d.voice[0];
        double dt = 1.0 / 120, next = 0, talkStart = -1;
        int p = 0, frame = 0, nb = d.boneNames.length;
        double maxRot = 0, maxPos = 0, maxScl = 0, maxFx = 0;
        String worstRot = "", worstPos = "", worstFx = "";
        double end = trace.get("end").getAsDouble();
        for (int i = 0; i <= end * 120; i++) {
            double t = i * dt;
            while (p < plan.size() && t >= plan.get(p).getAsJsonArray().get(0).getAsDouble() - 1e-9) {
                a.switchAnim(plan.get(p).getAsJsonArray().get(1).getAsString());
                p++;
            }
            a.lookYaw = 0.6 * Math.sin(0.7 * t);
            a.lookPitch = 0.1 * Math.sin(0.5 * t);
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
                    for (int q = 0; q < 3; q++) {
                        double er = Math.abs(a.rot[b * 3 + q] - r.get(q).getAsDouble());
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
        System.out.printf("Homelander: compared %d frames x %d bones. Max difference: rotation %.5f rad (%s), position %.5f texel (%s), scale %.5f, fx %.5f (%s)%n",
                frame, nb, maxRot, worstRot, maxPos, worstPos, maxScl, maxFx, worstFx);
        assertTrue(frame == frames.size(), "frame count " + frame + " vs " + frames.size());
        assertTrue(maxRot < 0.001, "rotation differs " + maxRot + " at " + worstRot);
        assertTrue(maxPos < 0.005, "position differs " + maxPos + " at " + worstPos);
        assertTrue(maxScl < 0.001, "scale differs " + maxScl);
        assertTrue(maxFx < 0.002, "eye/beam values differ " + maxFx + " at " + worstFx);
    }
}
