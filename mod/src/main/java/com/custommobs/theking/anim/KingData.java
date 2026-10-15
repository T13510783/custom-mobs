package com.custommobs.theking.anim;

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

/* ───────── The King's data, exported 1:1 from mobs/the_king/thekingv3.html (#modexport) ─────────
   assets/theking/king/rig.json  : bones (rest pose), cubes as finished quads, ground boxes, springs, voice curves, animation list
   assets/theking/king/anims.bin : every animation channel at 120 fps, 16-bit steps stored as frame-to-frame differences
   Units: texels and radians, axes like the viewer: +x = his left, +y = up, +z = his front.
   Plain Java (no Minecraft classes): the server, the client and the 1:1 test all use it. */
public final class KingData {
    public static final String[] FACE_KEYS = {"browUp", "frown", "open", "wide", "glance", "squint"};

    /* ───────── Types ───────── */
    public static final class Quad {
        public final double[] v = new double[12];  // 4 corners x (x, y, z), texels, bone space
        public final float[] uv = new float[8];    // 4 corners x (u, v), 0..1, v down
        public final float[] n = new float[3];     // face normal, bone space
    }

    public static final class Cube {
        public String name;
        public int bone;
        public boolean twoSided;   // cutout parts (crown, bandana tip) show their inside too
        public Quad[] quads;
        public double[] boxCenter, boxRot, boxHalf; // for the ground check (texels, radians)
    }

    public static final class Spring {
        public int bone;
        public double k, d, accel, x0, x1, z0, z1, bx, bz, breeze, f;
    }

    public static final class Anim {
        public String name;
        public double T, yawLimit;
        public int frames;
        public boolean loopFade, rootMotion;
        public final Map<String, Double> events = new HashMap<>();
        public double[][] rot, pos;    // [bone*3 + axis] -> samples (null = always 0)
        public double[][] face = new double[6][];
        public double[] follow, rmZ, rmY;

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

    /* ───────── Data ───────── */
    public double fps;
    public int atlasW, atlasH;
    public String[] variants;
    public String[] boneNames;
    public int[] parent;
    public double[] restPos, restRot;  // [bone*3 + axis]
    public Cube[] cubes;
    public Spring[] springs;
    public VoiceLine[] voice;
    public double pitchUp, pitchDown, gravity;
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

    /* ───────── Loading ───────── */
    private static KingData instance;

    /** Loaded once from the mod jar (works on a dedicated server too: the files are in the jar). */
    public static synchronized KingData get() {
        if (instance == null) {
            try (InputStream rig = KingData.class.getResourceAsStream("/assets/theking/king/rig.json");
                 InputStream bin = KingData.class.getResourceAsStream("/assets/theking/king/anims.bin")) {
                if (rig == null || bin == null) throw new IOException("King data missing in the jar");
                instance = load(rig, bin);
            } catch (IOException e) {
                throw new RuntimeException("Could not load The King's model/animations", e);
            }
        }
        return instance;
    }

    public static KingData load(InputStream rigJson, InputStream animsBin) throws IOException {
        JsonObject j = JsonParser.parseReader(new InputStreamReader(rigJson, StandardCharsets.UTF_8)).getAsJsonObject();
        short[] deltas = readShorts(animsBin);
        KingData d = new KingData();
        d.fps = j.get("fps").getAsDouble();
        JsonArray atlas = j.getAsJsonArray("atlas");
        d.atlasW = atlas.get(0).getAsInt();
        d.atlasH = atlas.get(1).getAsInt();
        JsonArray vars = j.getAsJsonArray("variants");
        d.variants = new String[vars.size()];
        for (int i = 0; i < vars.size(); i++) d.variants[i] = vars.get(i).getAsString();

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
            double[] p = doubles(o.getAsJsonArray("pos")), r = doubles(o.getAsJsonArray("rot"));
            System.arraycopy(p, 0, d.restPos, b * 3, 3);
            System.arraycopy(r, 0, d.restRot, b * 3, 3);
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
                JsonArray v = qo.getAsJsonArray("v"), uv = qo.getAsJsonArray("uv");
                for (int k = 0; k < 4; k++) {
                    JsonArray p = v.get(k).getAsJsonArray(), t = uv.get(k).getAsJsonArray();
                    for (int a = 0; a < 3; a++) quad.v[k * 3 + a] = p.get(a).getAsDouble();
                    quad.uv[k * 2] = t.get(0).getAsFloat();
                    quad.uv[k * 2 + 1] = t.get(1).getAsFloat();
                }
                JsonArray n = qo.getAsJsonArray("n");
                for (int a = 0; a < 3; a++) quad.n[a] = n.get(a).getAsFloat();
                cube.quads[q] = quad;
            }
            JsonObject box = o.getAsJsonObject("box");
            cube.boxCenter = doubles(box.getAsJsonArray("c"));
            cube.boxRot = doubles(box.getAsJsonArray("r"));
            cube.boxHalf = doubles(box.getAsJsonArray("h"));
            d.cubes[c] = cube;
        }

        JsonArray springs = j.getAsJsonArray("springs");
        d.springs = new Spring[springs.size()];
        for (int s = 0; s < springs.size(); s++) {
            JsonObject o = springs.get(s).getAsJsonObject();
            Spring sp = new Spring();
            sp.bone = o.get("bone").getAsInt();
            sp.k = o.get("k").getAsDouble();
            sp.d = o.get("d").getAsDouble();
            sp.accel = o.get("accel").getAsDouble();
            sp.x0 = o.getAsJsonArray("x").get(0).getAsDouble();
            sp.x1 = o.getAsJsonArray("x").get(1).getAsDouble();
            sp.z0 = o.getAsJsonArray("z").get(0).getAsDouble();
            sp.z1 = o.getAsJsonArray("z").get(1).getAsDouble();
            sp.bx = o.has("bx") ? o.get("bx").getAsDouble() : 0;
            sp.bz = o.has("bz") ? o.get("bz").getAsDouble() : 0;
            sp.breeze = o.get("breeze").getAsDouble();
            sp.f = o.get("f").getAsDouble();
            d.springs[s] = sp;
        }

        JsonObject consts = j.getAsJsonObject("consts");
        d.pitchUp = consts.get("pitchUp").getAsDouble();
        d.pitchDown = consts.get("pitchDown").getAsDouble();
        d.gravity = consts.get("gravity").getAsDouble();

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
        for (JsonElement nameEl : j.getAsJsonArray("animOrder")) {
            String name = nameEl.getAsString();
            JsonObject o = anims.getAsJsonObject(name);
            Anim a = new Anim();
            a.name = name;
            a.T = o.get("T").getAsDouble();
            a.frames = o.get("frames").getAsInt();
            a.yawLimit = o.get("yawLimit").getAsDouble();
            a.loopFade = o.get("loopFade").getAsBoolean();
            a.rootMotion = o.get("rootMotion").getAsBoolean();
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("events").entrySet()) a.events.put(e.getKey(), e.getValue().getAsDouble());
            a.rot = new double[nb * 3][];
            a.pos = new double[nb * 3][];
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
                    String fk = key.substring(2);
                    for (int k = 0; k < 6; k++) if (FACE_KEYS[k].equals(fk)) a.face[k] = s;
                } else if (key.equals("follow")) a.follow = s;
                else if (key.equals("rm:z")) a.rmZ = s;
                else if (key.equals("rm:y")) a.rmY = s;
            }
            d.anims.put(name, a);
        }
        return d;
    }

    private static double[] doubles(JsonArray a) {
        double[] out = new double[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
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
