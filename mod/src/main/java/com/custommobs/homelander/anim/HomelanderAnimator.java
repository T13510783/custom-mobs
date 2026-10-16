package com.custommobs.homelander.anim;

import java.util.Arrays;

/* ───────── Homelander's animation runtime: a 1:1 port of step() in mobs/homelander/homelander.html ─────────
   Same order, same numbers: random extras (glow pulse, Hover, Idle 2), the random director (moods, look-around,
   eye darts, micro twitches), animation events, crossfade, talk, face filter, blinks, head-follow, pupils/lids/
   brows/mouth/teeth, eye glow + beam values, and the cape cloth (HomelanderCloth). Only the animation curves come
   baked (120 fps) instead of as code. The 1:1 test (src/test) replays the viewer's recording (#modtrace).
   Math like the browser: StrictMath (fdlibm) and three.js-style matrices. Units: bone positions in texels,
   rotations in radians (order Z, then Y, then X), world tracking in blocks. Runs on the client and the server. */
public final class HomelanderAnimator {
    private static final double TAU = Math.PI * 2;

    /* ───────── Pose buffer (one animation frame) ───────── */
    private static final class Pose {
        final double[] r, p, f;

        Pose(int nb, int nf) { r = new double[nb * 3]; p = new double[nb * 3]; f = new double[nf]; }

        void clear() { Arrays.fill(r, 0); Arrays.fill(p, 0); Arrays.fill(f, 0); }

        void copyTo(Pose d) {
            System.arraycopy(r, 0, d.r, 0, r.length);
            System.arraycopy(p, 0, d.p, 0, p.length);
            System.arraycopy(f, 0, d.f, 0, f.length);
        }
    }

    /* ───────── Random director (viewer MOOD_*): smug smirk / angry / deliberate fake smile ───────── */
    private static final double[] MOOD_PICK = { 0.37, 0.45, 0.18 };
    private static final double[][] MOOD_TIME = { { 1.5, 2.5 }, { 2.0, 2.5 }, { 1.0, 1.4 } };
    private static final String[] MOOD_KEYS = { "frown", "browUp", "squint", "grin", "wide", "smirk", "open" };
    private static final double[][] MOOD_FACE = {
        { 0.25, 1.0, 0.0 }, { 0.15, 0.0, 0.6 }, { 0.2, 0.5, 0.3 }, { 0.0, 0.15, 1.0 }, { 0.15, 0.05, 0.85 }, { 0.9, 0.0, 0.25 }, { 0.0, 0.04, 0.0 }
    };

    public final HomelanderData d;
    private final int nb, nf;
    private final Pose snap, cur, out;
    // Face channel indexes (rig.json faceKeys order)
    private final int BROW_UP, FROWN, OPEN, WIDE, GLANCE, SQUINT, LASER, GRIN, SMIRK, GLOW;
    private final int[] moodIdx = new int[MOOD_KEYS.length];
    private final int HEAD, CHEST, PUPIL_R, PUPIL_L, LID_R, LID_L, BROW_R, BROW_L, MOUTH, TEETH;

    /* ───────── State (same names as the viewer's sim) ───────── */
    public HomelanderData.Anim anim;
    public double t, time, blend = 1;
    private final double[] face;
    private double yawLimit = 0.45;
    private int rngState;
    private double blinkWait = 1.4, blinkT = -1;
    /** Random extras (viewer S.events): red glow pulses, now and then a Hover or an Idle 2 from Idle. */
    public boolean eventsOn = true;
    private double glowWait, glowT = -1, glowDur = 1.5, glowAmp = 0.5, hoverWait, idle2Wait;
    private String auto = "";
    // Director (viewer S.rd)
    private double lookWait, lookTgt, pitchTgt, look, pitch, dartWait, dartTgt, dart, moodWait, lookW, moodW;
    private int mood;
    private final double[] mw = { 1, 0, 0 }, rph = new double[7];
    /** Head + eyes follow: on/has like the viewer; lookYaw/lookPitch = where to look (radians, + yaw = his left, + pitch = down). */
    public boolean followOn = true, followHas;
    public double lookYaw, lookPitch;
    private double fw = 1, fyaw, fpitch;
    /** Voice line: level = loudness 0..1, shape = brightness 0..1 (from the line's curve). */
    public boolean talkActive;
    public double talkLevel, talkShape = 0.5;
    private double talkW;
    /** The cape. move = how far his body moved this frame (texels, model space); null = the animation's own walk speed. */
    public final HomelanderCloth cloth;
    public double[] move;
    private final double[] animMove = new double[3];

    /** Events the last step() passed (names from the animation's `events`), e.g. "voice", "grab", "hit". */
    public final String[] fired = new String[8];
    public int firedCount;

    /* Outputs for the eye glow / beams (viewer: material opacities and beam scale). */
    public double glowOpacity, coreOpacity, haloOpacity, laserOpacity, laserCoreOpacity, beamW, glance;

    /* Outer transform: the entity in the world (blocks, body yaw in radians, Minecraft yaw). 0 = viewer (no motion). */
    public double worldX, worldY, worldZ, worldYaw;

    /* ───────── Bones: local transform (texels, radians), three.js world matrices (blocks, column-major 4x4)
       and model-space matrices for drawing (blocks, 3x4 row-major) ───────── */
    public final double[] pos, rot, scl;
    public final double[][] world;
    public final double[] mat;
    private final double[] tmpLocal = new double[16];

    public HomelanderAnimator(HomelanderData d, int seed) {
        this.d = d;
        nb = d.boneNames.length;
        nf = d.faceKeys.length;
        snap = new Pose(nb, nf);
        cur = new Pose(nb, nf);
        out = new Pose(nb, nf);
        face = new double[nf];
        pos = new double[nb * 3];
        rot = new double[nb * 3];
        scl = new double[nb * 3];
        world = new double[nb][16];
        mat = new double[nb * 12];
        BROW_UP = d.faceKey("browUp"); FROWN = d.faceKey("frown"); OPEN = d.faceKey("open"); WIDE = d.faceKey("wide");
        GLANCE = d.faceKey("glance"); SQUINT = d.faceKey("squint"); LASER = d.faceKey("laser"); GRIN = d.faceKey("grin");
        SMIRK = d.faceKey("smirk"); GLOW = d.faceKey("glow");
        for (int k = 0; k < MOOD_KEYS.length; k++) moodIdx[k] = d.faceKey(MOOD_KEYS[k]);
        HEAD = d.bone("head"); CHEST = d.bone("chest"); PUPIL_R = d.bone("pupil_right"); PUPIL_L = d.bone("pupil_left");
        LID_R = d.bone("lid_right"); LID_L = d.bone("lid_left"); BROW_R = d.bone("brow_right"); BROW_L = d.bone("brow_left");
        MOUTH = d.bone("mouth"); TEETH = d.bone("teeth");
        // makeSim(seed): the same random numbers in the same order as the viewer
        rngState = seed;
        glowWait = 3 + rng() * 4;
        hoverWait = 8 + rng() * 3;
        idle2Wait = 12 + rng() * 10;
        for (int i = 0; i < 7; i++) rph[i] = rng() * TAU;
        lookWait = 0.5 + rng() * 1.5;
        dartWait = 0.3 + rng() * 0.8;
        moodWait = 0.8 + rng() * 1.2;
        cloth = new HomelanderCloth(d.cloth, this::rng);
        anim = d.anim("idle");
        System.arraycopy(d.restPos, 0, pos, 0, nb * 3);
        System.arraycopy(d.restRot, 0, rot, 0, nb * 3);
        Arrays.fill(scl, 1);
        computeMatrices();
    }

    /* mulberry32, same bits as the viewer. */
    private double rng() {
        rngState = rngState + 0x6D2B79F5;
        int x = (rngState ^ (rngState >>> 15)) * (1 | rngState);
        x = (x + ((x ^ (x >>> 7)) * (61 | x))) ^ x;
        return Integer.toUnsignedLong(x ^ (x >>> 14)) / 4294967296.0;
    }

    private static double filt(double dt, double rate) { return 1 - StrictMath.exp(-dt * rate); }
    private static double clamp(double v, double a, double b) { return v < a ? a : v > b ? b : v; }

    /** Crossfade to another animation from whatever is on screen now (viewer: switchAnim). */
    public void switchAnim(String name) {
        HomelanderData.Anim a = d.anim(name);
        if (a == anim) return;
        out.copyTo(snap);
        anim = a;
        t = 0;
        blend = 0;
        auto = "";   // a picked animation is never the auto one
    }

    /* One step of the random director (viewer directorStep): new random targets when their timers run out, then filters. */
    private void directorStep(double dt) {
        lookWait -= dt;
        if (lookWait <= 0) {
            boolean back = rng() < 0.35;
            lookTgt = back ? 0 : (rng() * 2 - 1) * 0.36;
            pitchTgt = -rng() * 0.04;
            lookWait = 1.2 + rng() * 2.8;
        }
        dartWait -= dt;
        if (dartWait <= 0) {
            dartTgt = clamp(1.2 * lookTgt + (rng() * 2 - 1) * 0.25, -0.45, 0.45);
            dartWait = 0.5 + rng() * 1.7;
        }
        moodWait -= dt;
        if (moodWait <= 0) {
            double r = rng();
            int m = r < MOOD_PICK[0] ? 0 : r < MOOD_PICK[0] + MOOD_PICK[1] ? 1 : 2;
            if (m == mood) m = (m + 1 + (rng() < 0.5 ? 0 : 1)) % 3;   // never the same mood twice in a row
            mood = m;
            moodWait = MOOD_TIME[m][0] + rng() * MOOD_TIME[m][1];
        }
        double kl = filt(dt, 2.5), kd = filt(dt, 14), km = filt(dt, 3.5);
        look += (lookTgt - look) * kl;
        pitch += (pitchTgt - pitch) * kl;
        dart += (dartTgt - dart) * kd;
        for (int i = 0; i < 3; i++) mw[i] += ((mood == i ? 1 : 0) - mw[i]) * km;
    }

    /** One frame. dt = real seconds; rate = animation speed (1 = viewer speed; walk follows the ground speed). */
    public void step(double dt, double rate) {
        double dta = dt * rate;
        time += dt;
        t += dta;
        firedCount = 0;

        // Random extras of the calm animations (Idle, Idle 2, Hover, Walk): a red glow pulse at random times.
        // From Idle, now and then one Hover or one Idle 2 plays by itself, then crossfades back.
        if (eventsOn) {
            String n = anim.name;
            if (n.equals("idle") || n.equals("hover") || n.equals("idle2") || n.equals("walk")) {
                glowWait -= dt;
                if (glowWait <= 0 && glowT < 0) {
                    glowT = 0; glowDur = 1.2 + rng() * 1.2; glowAmp = 0.35 + rng() * 0.3;
                    glowWait = 6 + rng() * 9;
                }
            }
            if (anim.name.equals("idle") && blend >= 1) {
                hoverWait -= dt;
                idle2Wait -= dt;
                if (hoverWait <= 0) {
                    switchAnim("hover"); auto = "hover";
                    hoverWait = 16 + rng() * 14; idle2Wait = Math.max(idle2Wait, 4);
                } else if (idle2Wait <= 0) {
                    switchAnim("idle2"); auto = "idle2";
                    idle2Wait = 14 + rng() * 16; hoverWait = Math.max(hoverWait, 4);
                }
            }
            if ((anim.name.equals("hover") || anim.name.equals(auto)) && !anim.name.equals("idle") && t >= anim.T) switchAnim("idle");
        }
        directorStep(dt);
        double evGlow = 0;
        if (glowT >= 0) {
            glowT += dt;
            double u = clamp(glowT / glowDur, 0, 1);
            double sg = StrictMath.sin(Math.PI * u);
            evGlow = glowAmp * sg * sg; // smooth in and out
            if (u >= 1) glowT = -1;
        }

        HomelanderData.Anim A = anim;
        double loopT = t % A.T;

        // Animation events passed in this step (t0 < 0 right after a loop wrap: the end of the last loop counts too)
        double t0 = loopT - dta;
        for (var e : A.events.entrySet()) {
            double v = e.getValue();
            if (((v > t0 && v <= loopT) || (t0 < 0 && v > t0 + A.T)) && firedCount < fired.length) fired[firedCount++] = e.getKey();
        }

        // 1. Animation pose (baked), crossfaded from the snapshot.
        cur.clear();
        double fps = d.fps;
        for (int i = 0; i < nb * 3; i++) {
            cur.r[i] = HomelanderData.Anim.sample(A.rot[i], loopT, fps);
            cur.p[i] = HomelanderData.Anim.sample(A.pos[i], loopT, fps);
        }
        for (int k = 0; k < nf; k++) cur.f[k] = HomelanderData.Anim.sample(A.face[k], loopT, fps);
        if (blend < 1) {
            blend = Math.min(1, blend + dt * 4.2);
            double w = blend * blend * (3 - 2 * blend);
            for (int i = 0; i < nb * 3; i++) {
                out.r[i] = snap.r[i] + (cur.r[i] - snap.r[i]) * w;
                out.p[i] = snap.p[i] + (cur.p[i] - snap.p[i]) * w;
            }
            for (int k = 0; k < nf; k++) out.f[k] = snap.f[k] + (cur.f[k] - snap.f[k]) * w;
            yawLimit = 0.45; // viewer: S.cur.yawLimit (the pose buffer default) while blending
        } else {
            cur.copyTo(out);
            yawLimit = A.yawLimit;
        }

        // 2. Random director on the face: the mood blends over the animation's face by its `mood` weight,
        // eye darts by the same weight, never-repeating twitches on brows/mouth always; angry flickers the eyes.
        double kw = filt(dt, 4);
        lookW += (A.look - lookW) * kw;
        moodW += (A.mood - moodW) * kw;
        double[] of = out.f;
        double mwt = moodW, tm = time;
        if (mwt > 1e-4) {
            for (int k = 0; k < MOOD_KEYS.length; k++) {
                double[] m = MOOD_FACE[k];
                double tgt = mw[0] * m[0] + mw[1] * m[1] + mw[2] * m[2];
                of[moodIdx[k]] += (tgt - of[moodIdx[k]]) * mwt;
            }
            of[GLANCE] += (dart * (1 - 0.8 * mw[2]) - of[GLANCE]) * mwt;
            of[GLOW] += mwt * mw[1] * (0.12 + 0.1 * StrictMath.sin(TAU * 1.7 * tm + rph[6]));
        }
        of[BROW_UP] += 0.06 * StrictMath.sin(TAU * 0.37 * tm + rph[0]) + 0.04 * StrictMath.sin(TAU * 0.83 * tm + rph[1]);
        of[FROWN] += 0.05 * StrictMath.sin(TAU * 0.29 * tm + rph[2]);
        of[OPEN] += 0.015 * (1 + StrictMath.sin(TAU * 0.53 * tm + rph[3]));
        of[WIDE] += 0.05 * StrictMath.sin(TAU * 0.41 * tm + rph[4]);
        of[SMIRK] += 0.06 * StrictMath.sin(TAU * 0.19 * tm + rph[5]) * (1 - mw[1] * mwt);

        // 3. Talking (voice line) overrides the animation's mouth: open = loudness, wide = brightness.
        talkW += ((talkActive ? 1 : 0) - talkW) * filt(dt, 10);
        double tw = talkW;
        if (tw > 0.001) {
            of[OPEN] += (talkLevel - of[OPEN]) * tw;
            of[WIDE] += ((talkShape - 0.5) * 1.4 - of[WIDE]) * tw;
            of[FROWN] = Math.max(of[FROWN], 0.45 * tw);
            of[SQUINT] = Math.max(of[SQUINT], 0.2 * tw);
        }

        // 4. Face filter, blinks (random, now and then a double blink).
        double k16 = filt(dt, 16);
        for (int k = 0; k < nf; k++) face[k] += (of[k] - face[k]) * k16;
        blinkWait -= dt;
        if (blinkWait <= 0 && blinkT < 0) {
            blinkT = 0;
            blinkWait = 2.5 + rng() * 3.5;
        }
        double blinkVal = 0;
        if (blinkT >= 0) {
            blinkT += dt;
            blinkVal = StrictMath.sin(clamp(blinkT / 0.18, 0, 1) * Math.PI);
            if (blinkT >= 0.18) {
                blinkT = -1;
                if (blinkWait > 1 && rng() < 0.2) blinkWait = 0.12;
            }
        }

        // 5. Head + eyes follow the look target.
        double fTgt = followOn && followHas ? A.follow : 0;
        fw += (fTgt - fw) * filt(dt, 8);
        double targetYaw = clamp(lookYaw, -yawLimit, yawLimit);
        double targetPitch = clamp(lookPitch, -0.08, 0.08);
        double k9 = filt(dt, 9);
        fyaw += (targetYaw - fyaw) * k9;
        fpitch += (targetPitch - fpitch) * k9;

        // 6. Pose the bones: rest + animation, head follow + look-around, then the face parts.
        for (int i = 0; i < nb * 3; i++) {
            rot[i] = d.restRot[i] + out.r[i];
            pos[i] = d.restPos[i] + out.p[i];
        }
        rot[HEAD * 3 + 1] += fyaw * fw;
        rot[HEAD * 3] += fpitch * fw;
        rot[HEAD * 3] -= 0.05 * talkLevel * talkW; // chin lifts on loud syllables
        double fr = 1 - 0.6 * fw, L = look * lookW * fr;
        double Pp = -0.25 * L * L + pitch * lookW * fr, Z = 0.06 * mw[2] * moodW;
        rot[HEAD * 3 + 1] += 0.75 * L;
        rot[HEAD * 3] += Pp;
        rot[HEAD * 3 + 2] += Z;
        pos[HEAD * 3 + 1] += 4.2 * (Math.sqrt(Pp * Pp + Z * Z + 4e-4) - 0.02);
        rot[CHEST * 3 + 1] += 0.25 * L;

        double pGlance = clamp(face[GLANCE] + fyaw * 0.35 * fw, -0.45, 0.45);
        glance = pGlance;
        pos[PUPIL_R * 3] = -2 + pGlance;
        pos[PUPIL_L * 3] = 2 + pGlance;

        double closeAmt = clamp(blinkVal + face[SQUINT] * 0.5, 0, 1);
        scl[LID_R * 3 + 1] = closeAmt;
        scl[LID_L * 3 + 1] = closeAmt;
        double lidZ = closeAmt > 0.05 ? 4.06 : 3.98; // lids rest just inside the head, step out only while closing
        pos[LID_R * 3 + 2] = lidZ;
        pos[LID_L * 3 + 2] = lidZ;

        double browY = 5.6 + face[BROW_UP] * 0.4 - face[FROWN] * 0.3;
        pos[BROW_R * 3 + 1] = browY;
        pos[BROW_L * 3 + 1] = browY;
        rot[BROW_R * 3 + 2] = -0.06 - face[FROWN] * 0.25;
        rot[BROW_L * 3 + 2] = 0.06 + face[FROWN] * 0.25;

        scl[MOUTH * 3 + 1] = 1 + face[OPEN] * 1.5;
        scl[MOUTH * 3] = 1 + face[WIDE] * 0.4 + face[GRIN] * 0.3;
        rot[MOUTH * 3 + 2] = 0.14 * face[SMIRK];
        pos[MOUTH * 3] = 0.25 * face[SMIRK];
        scl[TEETH * 3 + 1] = Math.max(0.001, face[GRIN]);
        pos[TEETH * 3 + 2] = -0.05 + 0.045 * clamp(face[GRIN] * 10, 0, 1);

        // 7. Red eye glow (anim + random pulse) and the heat-vision beams (thickness flickers).
        double glowV = clamp(Math.max(face[GLOW] + evGlow, face[LASER]), 0, 1);
        glowOpacity = Math.min(1, glowV * 1.3);
        coreOpacity = clamp(glowV * 1.6 - 0.45, 0, 1);
        haloOpacity = glowV * 0.35;
        double laserActive = face[LASER];
        laserOpacity = laserActive;
        laserCoreOpacity = laserActive * 0.95;
        beamW = laserActive > 0.01 ? (0.75 + 0.25 * StrictMath.sin(time * 45)) * Math.min(1, laserActive * 2) : 0.0001;

        // 8. Cape cloth: world matrices of this pose, one cloth frame (it turns the cape bones), then the final matrices.
        computeMatrices();
        double[] mv = move;
        if (mv == null) { animMove[0] = 0; animMove[1] = 0; animMove[2] = A.move * dt; mv = animMove; }
        cloth.step(world, CHEST, time, dt, mv, rot);
        computeMatrices();
    }

    /* ───────── Matrices ───────── */

    /** World matrices of every bone (three.js way, blocks) and the 3x4 drawing matrices. Parents come before children. */
    public void computeMatrices() {
        for (int b = 0; b < nb; b++) {
            HomelanderCloth.compose(tmpLocal, pos[b * 3] / 16, pos[b * 3 + 1] / 16, pos[b * 3 + 2] / 16,
                rot[b * 3], rot[b * 3 + 1], rot[b * 3 + 2], scl[b * 3], scl[b * 3 + 1], scl[b * 3 + 2]);
            int p = d.parent[b];
            if (p < 0) System.arraycopy(tmpLocal, 0, world[b], 0, 16);
            else HomelanderCloth.multiply(world[p], tmpLocal, world[b]);
            double[] w = world[b];
            int o = b * 12;
            mat[o] = w[0]; mat[o + 1] = w[4]; mat[o + 2] = w[8]; mat[o + 3] = w[12];
            mat[o + 4] = w[1]; mat[o + 5] = w[5]; mat[o + 6] = w[9]; mat[o + 7] = w[13];
            mat[o + 8] = w[2]; mat[o + 9] = w[6]; mat[o + 10] = w[10]; mat[o + 11] = w[14];
        }
    }

    /** A point in a bone's space (texels) -> world (blocks), through the model matrices and the entity transform. */
    public void toWorld(int bone, double x, double y, double z, double[] o) {
        int m = bone * 12;
        x /= 16; y /= 16; z /= 16;
        double mx = mat[m] * x + mat[m + 1] * y + mat[m + 2] * z + mat[m + 3];
        double my = mat[m + 4] * x + mat[m + 5] * y + mat[m + 6] * z + mat[m + 7];
        double mz = mat[m + 8] * x + mat[m + 9] * y + mat[m + 10] * z + mat[m + 11];
        double c = Math.cos(worldYaw), s = Math.sin(worldYaw);
        o[0] = c * mx - s * mz + worldX;
        o[1] = my + worldY;
        o[2] = s * mx + c * mz + worldZ;
    }

    /** A direction in a bone's space -> world direction (unit length if the bone has no scale). */
    public void dirToWorld(int bone, double x, double y, double z, double[] o) {
        int m = bone * 12;
        double mx = mat[m] * x + mat[m + 1] * y + mat[m + 2] * z;
        double my = mat[m + 4] * x + mat[m + 5] * y + mat[m + 6] * z;
        double mz = mat[m + 8] * x + mat[m + 9] * y + mat[m + 10] * z;
        double c = Math.cos(worldYaw), s = Math.sin(worldYaw);
        o[0] = c * mx - s * mz;
        o[1] = my;
        o[2] = s * mx + c * mz;
    }

    /** True if the last step() passed this event of the current animation. */
    public boolean firedEvent(String name) {
        for (int i = 0; i < firedCount; i++) if (fired[i].equals(name)) return true;
        return false;
    }

    /** Current time inside the animation loop (seconds). */
    public double loopTime() { return t % anim.T; }
}
