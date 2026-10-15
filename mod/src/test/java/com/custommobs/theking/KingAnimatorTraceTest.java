package com.custommobs.theking;

import com.custommobs.theking.anim.KingAnimator;
import com.custommobs.theking.anim.KingData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/* ───────── 1:1 test: the mod's animation runtime against the HTML viewer ─────────
   modtrace.json = the viewer's own recording (#modtrace, written by tools/export_mod_assets.ps1):
   a fixed scene with animation switches, talking and look angles, every bone every 1/30 s.
   The Java runtime plays the same scene; every bone's rotation, position and scale must match. */
class KingAnimatorTraceTest {
    @Test
    void sameAsViewer() throws Exception {
        KingData d;
        try (InputStream rig = new FileInputStream("src/main/resources/assets/theking/king/rig.json");
             InputStream bin = new FileInputStream("src/main/resources/assets/theking/king/anims.bin")) {
            d = KingData.load(rig, bin);
        }
        JsonObject trace;
        try (InputStream in = getClass().getResourceAsStream("/modtrace.json")) {
            trace = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
        JsonArray frames = trace.getAsJsonArray("frames");
        JsonArray plan = trace.getAsJsonArray("plan");

        KingAnimator a = new KingAnimator(d, 7);
        a.followOn = true;
        a.followHas = true;
        double dt = 1.0 / 120, next = 0;
        int p = 0, frame = 0, nb = d.boneNames.length;
        double maxRot = 0, maxPos = 0, maxScl = 0;
        String worstRot = "", worstPos = "";
        double end = trace.has("end") ? trace.get("end").getAsDouble() : 17.5; // seconds the viewer recorded
        for (int i = 0; i <= end * 120; i++) {
            double t = i * dt;
            while (p < plan.size() && t >= plan.get(p).getAsJsonArray().get(0).getAsDouble() - 1e-9) {
                a.switchAnim(plan.get(p).getAsJsonArray().get(1).getAsString());
                p++;
            }
            a.lookYaw = 0.6 * Math.sin(0.7 * t);
            a.lookPitch = 0.1 * Math.sin(0.5 * t);
            boolean talking = t >= 1 && t < 2.5;
            a.talkActive = talking;
            a.talkLevel = talking ? 0.5 + 0.5 * Math.sin(Math.PI * 2 * 3 * t) : 0;
            a.talkShape = 0.5 + 0.5 * Math.sin(Math.PI * 2 * t);
            a.step(dt, 1);
            if (t >= next - 1e-9) {
                next += 1.0 / 30;
                JsonArray ref = frames.get(frame).getAsJsonArray();
                for (int b = 0; b < nb; b++) {
                    JsonArray r = ref.get(b).getAsJsonArray();
                    for (int k = 0; k < 3; k++) {
                        double er = Math.abs(a.rot[b * 3 + k] - r.get(k).getAsDouble());
                        double ep = Math.abs(a.pos[b * 3 + k] - r.get(3 + k).getAsDouble());
                        double es = Math.abs(a.scl[b * 3 + k] - r.get(6 + k).getAsDouble());
                        if (er > maxRot) { maxRot = er; worstRot = d.boneNames[b] + " axis " + k + " at " + String.format("%.3f", t) + " s (" + a.anim.name + ")"; }
                        if (ep > maxPos) { maxPos = ep; worstPos = d.boneNames[b] + " axis " + k + " at " + String.format("%.3f", t) + " s (" + a.anim.name + ")"; }
                        maxScl = Math.max(maxScl, es);
                    }
                }
                frame++;
            }
        }
        System.out.printf("Compared %d frames x %d bones. Max difference: rotation %.5f rad (%s), position %.5f texel (%s), scale %.5f%n",
                frame, nb, maxRot, worstRot, maxPos, worstPos, maxScl);
        assertTrue(frame == frames.size(), "frame count " + frame + " vs " + frames.size());
        assertTrue(maxRot < 0.001, "rotation differs " + maxRot + " at " + worstRot);
        assertTrue(maxPos < 0.005, "position differs " + maxPos + " at " + worstPos);
        assertTrue(maxScl < 0.001, "scale differs " + maxScl);
    }
}
