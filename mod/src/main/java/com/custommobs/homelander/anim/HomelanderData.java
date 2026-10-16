package com.custommobs.homelander.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/* ───────── Homelander's data, exported 1:1 from mobs/homelander/homelander.html (#modexport) ─────────
   assets/homelander/homelander/rig.json  : bones (rest pose), cubes as finished quads, cape cloth, eye glow + beam
                                            geometry, victim spots, voice curve, animation list
   assets/homelander/homelander/anims.bin : every animation channel at 120 fps, 16-bit steps stored as frame-to-frame differences
   Units: texels and radians, axes like the viewer: +x = his left, +y = up, +z = his front.
   Plain Java (no Minecraft classes): the server, the client and the 1:1 test all use it. */
public final class HomelanderData {
    /* ───────── Types ───────── */
    public static final class Quad {
        public final double[] v = new double[12];  // 4 corners x (x, y, z), texels, bone space
        public final float[] uv = new float[8];    // 4 corners x (u, v), 0..1, v down
        public final float[] n = new float[3];     // face normal, bone space
    }

    public static final class Cube {
        public String name;
        public int bone;
        public boolean twoSided;   // cutout parts (hair shell) show their inside too
        public Quad[] quads;
    }

    /** A body box the cape collides with (viewer CLOTH.colliders): bone, center (texels), half size incl. inflate, rotation. */
    public static final class Collider {
        public String name;
        public int bone;
        public double[] c, h, r;
        public boolean hand;       // forearm / fist: pushes the cape toward his back when both hands are behind it
        public boolean upper;      // upper arm: pushes the cape toward his spine then
    }

    /** The cape cloth (viewer CLOTH + CAPE_*): physics settings, size, bones, colliders and the mesh texture rects. */
    public static final class Cloth {
        public double sub, gravity, drag, margin, wind, handsWidth, seg, width, thick;
        public int iters, cols, n;
        public double[] top;
        public int[] bones;
        public Collider[] colliders;
        public int[][] rects;      // atlas rects [x, y, w, h] of faces right, left, top, bottom, front (lining), back (outside)
    }

    public static final class Anim {
        public String name;
        public double T, yawLimit, follow, beam, look, mood, move;   // look / mood = director weights, move = walk speed (texels/s)
        public int frames;
        public final Map<String, Double> events = new LinkedHashMap<>();
        public double[][] rot, pos;    // [bone*3 + axis] -> samples (null = always 0)
        public double[][] face;        // [face key] -> samples

        /** Value of a channel at time t (seconds): Catmull-Rom through the baked samples. */
        public static double sample(double[] s, double t, double fps) {
            if (s == null) return 0;
            double u = t * fps;
            int n = s.length;
            if (u <= 0) return s[0];
            if (u >= n - 1) return s[n - 1];
            int i = (int) Math.floor(u);
            double a = u - i;
            double p0 = s[Math.max(0, i - 1)], p1 = s[i], p2 = s[Math.min(n - 1, i + 1)], p3 = s[Math.min(n - 1, i + 2)];
            return 0.5 * (2 * p1 + (-p0 + p2) * a + (2 * p0 - 5 * p1 + 4 * p2 - p3) * a * a + (-p0 + 3 * p1 - 3 * p2 + p3) * a * a * a);
        }

        public double event(String key, double fallback) {
            Double v = events.get(key);
            return v == null ? fallback : v;
        }
    }

    public static final class VoiceLine {
        public String id, env, zcr;
    }

    /** Where a victim stands for one attack (texels, his model space) and, for grabs, where it is let go. */
    public static final class Victim {
        public double[] spot, release;
        public boolean grab;
    }

    /* ───────── Data ───────── */
    public double fps;
    public int atlasW, atlasH;
    public String[] variants, faceKeys, boneNames;
    public int[] parent;
    public double[] restPos, restRot;  // [bone*3 + axis]
    public Cube[] cubes;
    public Cloth cloth;
    public VoiceLine[] voice;
    // Eye glow planes + heat-vision beams (viewer: eyeGlow / lasers, in the head bone)
    public double[] eyesX, glowZ, beamR;
    public double[][] glowSize;
    public int[] glowColors, beamColors, beamSides;
    public double eyeY, beamZ, laserLen;
    // Victims: hand point (forearm_right space), dummy neck height, throw speed (blocks/s), grab lift time (s)
    public double[] hand;
    public double neck, scale, hangUp, hangOut, throwVy, grabLift;
    /** Walk while attacking: bones the Walk drives under an attack, and the root (only its height). */
    public int[] walkBones;
    public int walkRoot;
    public final Map<String, Victim> victims = new HashMap<>();
    public final Map<String, Anim> anims = new LinkedHashMap<>();
    private final Map<String, Integer> boneIndex = new HashMap<>();

    public int bone(String name) {
        Integer i = boneIndex.get(name);
        if (i == null) throw new IllegalStateException("No bone " + name);
        return i;
    }

    public Anim anim(String name) {
        Anim a = anims.get(name);
        if (a == null) throw new IllegalStateException("No animation " + name);
        return a;
    }

    public int faceKey(String name) {
        for (int k = 0; k < faceKeys.length; k++) if (faceKeys[k].equals(name)) return k;
        throw new IllegalStateException("No face key " + name);
    }

    /* ───────── Loading ───────── */
    private static HomelanderData instance;

    /** Loaded once from the mod jar (works on a dedicated server too: the files are in the jar). */
    public static synchronized HomelanderData get() {
        if (instance == null) {
            try (InputStream rig = HomelanderData.class.getResourceAsStream("/assets/homelander/homelander/rig.json");
                 InputStream bin = HomelanderData.class.getResourceAsStream("/assets/homelander/homelander/anims.bin")) {
                if (rig == null || bin == null) throw new IOException("Homelander data missing in the jar");
                instance = load(rig, bin);
            } catch (IOException e) {
                throw new RuntimeException("Could not load Homelander's model/animations", e);
            }
        }
        return instance;
    }

    public static HomelanderData load(InputStream rigJson, InputStream animsBin) throws IOException {
        JsonObject j = JsonParser.parseReader(new InputStreamReader(rigJson, StandardCharsets.UTF_8)).getAsJsonObject();
        short[] deltas = readShorts(animsBin);
        HomelanderData d = new HomelanderData();
        d.fps = j.get("fps").getAsDouble();
        JsonArray atlas = j.getAsJsonArray("atlas");
        d.atlasW = atlas.get(0).getAsInt();
        d.atlasH = atlas.get(1).getAsInt();
        d.variants = strings(j.getAsJsonArray("variants"));
        d.faceKeys = strings(j.getAsJsonArray("faceKeys"));

        JsonArray bones = j.getAsJsonArray("bones");
        int nb = bones.size();
        d.boneNames = new String[nb];
        d.parent = new int[nb];
        d.restPos = new double[nb * 3];
        d.restRot = new double[nb * 3];
        for (int b = 0; b < nb; b++) {
            JsonObject o = bones.get(b).getAsJsonObject();
            d.boneNames[b] = o.get("name").getAsString();
            d.parent[b] = o.get("parent").getAsInt();
            System.arraycopy(doubles(o.getAsJsonArray("pos")), 0, d.restPos, b * 3, 3);
            System.arraycopy(doubles(o.getAsJsonArray("rot")), 0, d.restRot, b * 3, 3);
            d.boneIndex.put(d.boneNames[b], b);
        }

        JsonArray cubes = j.getAsJsonArray("cubes");
        d.cubes = new Cube[cubes.size()];
        for (int c = 0; c < cubes.size(); c++) {
            JsonObject o = cubes.get(c).getAsJsonObject();
            Cube cube = new Cube();
            cube.name = o.get("name").getAsString();
            cube.bone = o.get("bone").getAsInt();
            cube.twoSided = o.get("twoSided").getAsBoolean();
            JsonArray qs = o.getAsJsonArray("quads");
            cube.quads = new Quad[qs.size()];
            for (int q = 0; q < qs.size(); q++) {
                JsonObject qo = qs.get(q).getAsJsonObject();
                Quad quad = new Quad();
                JsonArray v = qo.getAsJsonArray("v"), uv = qo.getAsJsonArray("uv"), n = qo.getAsJsonArray("n");
                for (int k = 0; k < 4; k++) {
                    JsonArray p = v.get(k).getAsJsonArray(), t = uv.get(k).getAsJsonArray();
                    for (int a = 0; a < 3; a++) quad.v[k * 3 + a] = p.get(a).getAsDouble();
                    quad.uv[k * 2] = t.get(0).getAsFloat();
                    quad.uv[k * 2 + 1] = t.get(1).getAsFloat();
                }
                for (int a = 0; a < 3; a++) quad.n[a] = n.get(a).getAsFloat();
                cube.quads[q] = quad;
            }
            d.cubes[c] = cube;
        }

        JsonObject cl = j.getAsJsonObject("cloth");
        Cloth cloth = new Cloth();
        cloth.sub = cl.get("sub").getAsDouble();
        cloth.gravity = cl.get("gravity").getAsDouble();
        cloth.drag = cl.get("drag").getAsDouble();
        cloth.iters = cl.get("iters").getAsInt();
        cloth.cols = cl.get("cols").getAsInt();
        cloth.margin = cl.get("margin").getAsDouble();
        cloth.wind = cl.get("wind").getAsDouble();
        cloth.handsWidth = cl.get("handsWidth").getAsDouble();
        cloth.n = cl.get("n").getAsInt();
        cloth.seg = cl.get("seg").getAsDouble();
        cloth.width = cl.get("width").getAsDouble();
        cloth.thick = cl.get("thick").getAsDouble();
        cloth.top = doubles(cl.getAsJsonArray("top"));
        JsonArray cb = cl.getAsJsonArray("bones");
        cloth.bones = new int[cb.size()];
        for (int i = 0; i < cloth.bones.length; i++) cloth.bones[i] = cb.get(i).getAsInt();
        JsonArray cc = cl.getAsJsonArray("colliders");
        cloth.colliders = new Collider[cc.size()];
        for (int i = 0; i < cloth.colliders.length; i++) {
            JsonObject o = cc.get(i).getAsJsonObject();
            Collider c = new Collider();
            c.name = o.get("name").getAsString();
            c.bone = o.get("bone").getAsInt();
            c.c = doubles(o.getAsJsonArray("c"));
            c.h = doubles(o.getAsJsonArray("h"));
            c.r = doubles(o.getAsJsonArray("r"));
            c.hand = c.name.startsWith("lower_arm_") || c.name.startsWith("gauntlet_");
            c.upper = c.name.startsWith("upper_arm_");
            cloth.colliders[i] = c;
        }
        JsonArray cr = cl.getAsJsonArray("rects");
        cloth.rects = new int[cr.size()][];
        for (int i = 0; i < cloth.rects.length; i++) {
            JsonArray r = cr.get(i).getAsJsonArray();
            cloth.rects[i] = new int[] { r.get(0).getAsInt(), r.get(1).getAsInt(), r.get(2).getAsInt(), r.get(3).getAsInt() };
        }
        d.cloth = cloth;

        JsonObject fx = j.getAsJsonObject("fx");
        d.eyesX = doubles(fx.getAsJsonArray("eyesX"));
        d.eyeY = fx.get("eyeY").getAsDouble();
        d.glowZ = doubles(fx.getAsJsonArray("glowZ"));
        JsonArray gs = fx.getAsJsonArray("glowSize");
        d.glowSize = new double[gs.size()][];
        for (int i = 0; i < gs.size(); i++) d.glowSize[i] = doubles(gs.get(i).getAsJsonArray());
        d.glowColors = colors(fx.getAsJsonArray("glowColors"));
        d.beamZ = fx.get("beamZ").getAsDouble();
        d.beamR = doubles(fx.getAsJsonArray("beamR"));
        JsonArray bs = fx.getAsJsonArray("beamSides");
        d.beamSides = new int[bs.size()];
        for (int i = 0; i < bs.size(); i++) d.beamSides[i] = bs.get(i).getAsInt();
        d.beamColors = colors(fx.getAsJsonArray("beamColors"));
        d.laserLen = fx.get("laserLen").getAsDouble();

        JsonObject victim = j.getAsJsonObject("victim");
        d.hand = doubles(victim.getAsJsonArray("hand"));
        d.neck = victim.get("neck").getAsDouble();
        d.scale = victim.get("scale").getAsDouble();
        d.hangUp = victim.get("hangUp").getAsDouble();
        d.hangOut = victim.get("hangOut").getAsDouble();
        d.throwVy = victim.get("throwVy").getAsDouble();
        d.grabLift = victim.get("grabLift").getAsDouble();
        for (Map.Entry<String, JsonElement> e : victim.getAsJsonObject("spots").entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            Victim v = new Victim();
            v.spot = doubles(o.getAsJsonArray("spot"));
            v.grab = o.has("grab") && o.get("grab").getAsBoolean();
            v.release = o.has("release") ? doubles(o.getAsJsonArray("release")) : null;
            d.victims.put(e.getKey(), v);
        }
        JsonObject wl = j.getAsJsonObject("walkLayer");
        JsonArray wb = wl.getAsJsonArray("bones");
        d.walkBones = new int[wb.size()];
        for (int i = 0; i < wb.size(); i++) d.walkBones[i] = wb.get(i).getAsInt();
        d.walkRoot = wl.get("root").getAsInt();

        JsonArray voice = j.getAsJsonArray("voice");
        d.voice = new VoiceLine[voice.size()];
        for (int i = 0; i < voice.size(); i++) {
            JsonObject o = voice.get(i).getAsJsonObject();
            VoiceLine l = new VoiceLine();
            l.id = o.get("id").getAsString();
            l.env = o.get("env").getAsString();
            l.zcr = o.get("zcr").getAsString();
            d.voice[i] = l;
        }

        // Animations: decode each channel (16-bit level = running sum of the stored differences).
        JsonObject anims = j.getAsJsonObject("anims");
        int nf = d.faceKeys.length;
        for (JsonElement nameEl : j.getAsJsonArray("animOrder")) {
            String name = nameEl.getAsString();
            JsonObject o = anims.getAsJsonObject(name);
            Anim a = new Anim();
            a.name = name;
            a.T = o.get("T").getAsDouble();
            a.frames = o.get("frames").getAsInt();
            a.yawLimit = o.get("yawLimit").getAsDouble();
            a.follow = o.get("follow").getAsDouble();
            a.beam = o.get("beam").getAsDouble();
            a.look = o.get("look").getAsDouble();
            a.mood = o.get("mood").getAsDouble();
            a.move = o.get("move").getAsDouble();
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("events").entrySet()) a.events.put(e.getKey(), e.getValue().getAsDouble());
            a.rot = new double[nb * 3][];
            a.pos = new double[nb * 3][];
            a.face = new double[nf][];
            for (JsonElement chEl : o.getAsJsonArray("channels")) {
                JsonObject ch = chEl.getAsJsonObject();
                String key = ch.get("key").getAsString();
                double lo = ch.get("lo").getAsDouble(), hi = ch.get("hi").getAsDouble();
                int at = ch.get("at").getAsInt();
                double[] s = new double[a.frames];
                int level = 0;
                for (int f = 0; f < a.frames; f++) {
                    level = (short) (level + deltas[at + f]); // 16-bit wrap, like the viewer's Int16Array: big jumps decode exactly
                    s[f] = lo + (level + 32768) / 65535.0 * (hi - lo);
                }
                if (key.startsWith("r:") || key.startsWith("p:")) {
                    String[] parts = key.split(":");
                    int idx = Integer.parseInt(parts[1]) * 3 + Integer.parseInt(parts[2]);
                    (key.charAt(0) == 'r' ? a.rot : a.pos)[idx] = s;
                } else if (key.startsWith("f:")) {
                    a.face[d.faceKey(key.substring(2))] = s;
                }
            }
            d.anims.put(name, a);
        }
        return d;
    }

    private static String[] strings(JsonArray a) {
        String[] out = new String[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsString();
        return out;
    }

    private static double[] doubles(JsonArray a) {
        double[] out = new double[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
        return out;
    }

    private static int[] colors(JsonArray a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = Integer.parseInt(a.get(i).getAsString().substring(1), 16); // "#rrggbb"
        return out;
    }

    private static short[] readShorts(InputStream in) throws IOException {
        byte[] all = in.readAllBytes();
        short[] out = new short[all.length / 2];
        DataInputStream ds = new DataInputStream(new java.io.ByteArrayInputStream(all));
        for (int i = 0; i < out.length; i++) out[i] = ds.readShort(); // big-endian, as written by the viewer
        return out;
    }
}
