package com.custommobs.homelander.anim;

import java.util.Arrays;

/* ───────── Homelander's animation runtime: a 1:1 port of step() in mobs/homelander/homelander.html ─────────
   Same order, same numbers: random Idle extras (glow pulse, Hover), animation events, crossfade, talk,
   face filter, blinks, head-follow, pupils/lids/brows/mouth/teeth, eye glow + beam values, cape springs.
   Only the animation curves come baked (120 fps) instead of as code. The 1:1 test (src/test) replays the
   viewer's recording (#modtrace). Units: bone positions in texels, rotations in radians (order Z, then Y,
   then X), world tracking in blocks. Runs on the client (drawing) and on the server (hand, eyes, beams). */
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

    /* Smoothed velocity / acceleration of a point (world, blocks) + the spring's own angles. */
    private static final class SpringState {
        boolean init;
        final double[] prev = new double[3], vel = new double[3], velPrev = new double[3], acc = new double[3];
        double ax, az, vx, vz;
    }

    public final HomelanderData d;
    private final int nb, nf;
    private final Pose snap, cur, out;
    // Face channel indexes (rig.json faceKeys order)
    private final int BROW_UP, FROWN, OPEN, WIDE, GLANCE, SQUINT, LASER, GRIN, SMIRK, GLOW;
    private final int HEAD, PUPIL_R, PUPIL_L, LID_R, LID_L, BROW_R, BROW_L, MOUTH, TEETH;

    /* ───────── State (same names as the viewer's sim) ───────── */
    public HomelanderData.Anim anim;
    public double t, time, blend = 1;
    private final double[] face;
    private double yawLimit = 0.45;
    private int rngState;
    private double blinkWait = 1.4, blinkT = -1;
    /** Random Idle extras (viewer S.events): faint red eye glow now and then, and a Hover every 16-30 s. */
    public boolean eventsOn = true;
    private double glowWait, glowT = -1, glowDur = 1.5, glowAmp = 0.5, hoverWait;
    /** Head + eyes follow: on/has like the viewer; lookYaw/lookPitch = where to look (radians, + yaw = his left, + pitch = down). */
    public boolean followOn = true, followHas;
    public double lookYaw, lookPitch;
    private double fw = 1, fyaw, fpitch;
    /** Voice line: level = loudness 0..1, shape = brightness 0..1 (from the line's curve). */
    public boolean talkActive;
    public double talkLevel, talkShape = 0.5;
    private double talkW;
    private final SpringState[] springs;

    /** Events the last step() passed (names from the animation's `events`), e.g. "voice", "grab", "hit". */
    public final String[] fired = new String[8];
    public int firedCount;

    /* Outputs for the eye glow / beams (viewer: material opacities and beam scale). */
    public double glowOpacity, coreOpacity, haloOpacity, laserOpacity, laserCoreOpacity, beamW, glance;

    /* Outer transform: the entity in the world (blocks, body yaw in radians, Minecraft yaw). 0 = viewer (no motion). */
    public double worldX, worldY, worldZ, worldYaw;

    /* ───────── Bones: local transform (texels, radians) and model-space matrices (blocks, 3x4 row-major) ───────── */
    public final double[] pos, rot, scl;
    public final double[] mat;
    private final double[] tmpW = new double[3], tmpLocal = new double[12];

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
        mat = new double[nb * 12];
        springs = new SpringState[d.springs.length];
        for (int i = 0; i < springs.length; i++) springs[i] = new SpringState();
        BROW_UP = d.faceKey("browUp"); FROWN = d.faceKey("frown"); OPEN = d.faceKey("open"); WIDE = d.faceKey("wide");
        GLANCE = d.faceKey("glance"); SQUINT = d.faceKey("squint"); LASER = d.faceKey("laser"); GRIN = d.faceKey("grin");
        SMIRK = d.faceKey("smirk"); GLOW = d.faceKey("glow");
        HEAD = d.bone("head"); PUPIL_R = d.bone("pupil_right"); PUPIL_L = d.bone("pupil_left");
        LID_R = d.bone("lid_right"); LID_L = d.bone("lid_left"); BROW_R = d.bone("brow_right"); BROW_L = d.bone("brow_left");
        MOUTH = d.bone("mouth"); TEETH = d.bone("teeth");
        // makeSim(seed): the first two random numbers time the first glow pulse and the first Hover
        rngState = seed;
        glowWait = 3 + rng() * 4;
        hoverWait = 8 + rng() * 3;
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

    private static double filt(double dt, double rate) { return 1 - Math.exp(-dt * rate); }
    private static double clamp(double v, double a, double b) { return v < a ? a : v > b ? b : v; }

    /** Crossfade to another animation from whatever is on screen now (viewer: switchAnim). */
    public void switchAnim(String name) {
        HomelanderData.Anim a = d.anim(name);
        if (a == anim) return;
        out.copyTo(snap);
        anim = a;
        t = 0;
        blend = 0;
    }

    /** One frame. dt = real seconds; rate = animation speed (1 = viewer speed; walk follows the ground speed). */
    public void step(double dt, double rate) {
        double dta = dt * rate;
        time += dt;
        t += dta;
        firedCount = 0;

        // Random Idle extras (only Idle / Hover): start a red glow pulse; switch into Hover now
        // and then; Hover plays once, then crossfades back into Idle.
        if (eventsOn) {
            String n = anim.name;
            if (n.equals("idle") || n.equals("hover")) {
                glowWait -= dt;
                if (glowWait <= 0 && glowT < 0) {
                    glowT = 0; glowDur = 1.2 + rng() * 1.2; glowAmp = 0.35 + rng() * 0.3;
                    glowWait = 6 + rng() * 9;
                }
            }
            if (anim.name.equals("idle") && blend >= 1) {
                hoverWait -= dt;
                if (hoverWait <= 0) { switchAnim("hover"); hoverWait = 16 + rng() * 14; }
            }
            if (anim.name.equals("hover") && t >= anim.T) switchAnim("idle");
        }
        double evGlow = 0;
        if (glowT >= 0) {
            glowT += dt;
            double u = clamp(glowT / glowDur, 0, 1);
            double sn = Math.sin(Math.PI * u);
            evGlow = glowAmp * sn * sn; // smooth in and out
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

        // 2. Talking (voice line) overrides the animation's mouth: open = loudness, wide = brightness.
        talkW += ((talkActive ? 1 : 0) - talkW) * filt(dt, 10);
        double tw = talkW;
        double[] of = out.f;
        if (tw > 0.001) {
            of[OPEN] += (talkLevel - of[OPEN]) * tw;
            of[WIDE] += ((talkShape - 0.5) * 1.4 - of[WIDE]) * tw;
            of[FROWN] = Math.max(of[FROWN], 0.45 * tw);
            of[SQUINT] = Math.max(of[SQUINT], 0.2 * tw);
        }

        // 3. Face filter, blinks (random, now and then a double blink).
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
            blinkVal = Math.sin(clamp(blinkT / 0.18, 0, 1) * Math.PI);
            if (blinkT >= 0.18) {
                blinkT = -1;
                if (blinkWait > 1 && rng() < 0.2) blinkWait = 0.12;
            }
        }

        // 4. Head + eyes follow the look target.
        double fTgt = followOn && followHas ? A.follow : 0;
        fw += (fTgt - fw) * filt(dt, 8);
        double targetYaw = clamp(lookYaw, -yawLimit, yawLimit);
        double targetPitch = clamp(lookPitch, -0.08, 0.08);
        double k9 = filt(dt, 9);
        fyaw += (targetYaw - fyaw) * k9;
        fpitch += (targetPitch - fpitch) * k9;

        // 5. Pose the bones: rest + animation, then the face parts.
        for (int i = 0; i < nb * 3; i++) {
            rot[i] = d.restRot[i] + out.r[i];
            pos[i] = d.restPos[i] + out.p[i];
            scl[i] = 1;
        }
        rot[HEAD * 3 + 1] += fyaw * fw;
        rot[HEAD * 3] += fpitch * fw;
        rot[HEAD * 3] -= 0.05 * talkLevel * talkW; // chin lifts on loud syllables

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

        // 6. Red eye glow (anim + random Idle pulse) and the heat-vision beams (thickness flickers).
        double glowV = clamp(Math.max(face[GLOW] + evGlow, face[LASER]), 0, 1);
        glowOpacity = Math.min(1, glowV * 1.3);
        coreOpacity = clamp(glowV * 1.6 - 0.45, 0, 1);
        haloOpacity = glowV * 0.35;
        double laserActive = face[LASER];
        laserOpacity = laserActive;
        laserCoreOpacity = laserActive * 0.95;
        beamW = laserActive > 0.01 ? (0.75 + 0.25 * Math.sin(time * 45)) * Math.min(1, laserActive * 2) : 0.0001;

        // 7. Cape springs. Like three.js getWorldPosition(): each bone's world point uses the parent
        // as already bent by its own spring, so the matrices are updated bone by bone down the cape.
        computeMatrices();
        double[] wp = tmpW;
        SpringState parentSp = null;
        HomelanderData.Spring parentCfg = null;
        double c = Math.cos(worldYaw), s = Math.sin(worldYaw);
        for (int i = 0; i < springs.length; i++) {
            HomelanderData.Spring sp = d.springs[i];
            SpringState st = springs[i];
            int b = sp.bone, p = d.parent[b];
            // bone pivot in model space = parent matrix * local position (blocks); then into the world
            double lx = pos[b * 3] / 16, ly = pos[b * 3 + 1] / 16, lz = pos[b * 3 + 2] / 16;
            int o = p * 12;
            double mx = mat[o] * lx + mat[o + 1] * ly + mat[o + 2] * lz + mat[o + 3];
            double my = mat[o + 4] * lx + mat[o + 5] * ly + mat[o + 6] * lz + mat[o + 7];
            double mz = mat[o + 8] * lx + mat[o + 9] * ly + mat[o + 10] * lz + mat[o + 11];
            wp[0] = c * mx - s * mz + worldX;
            wp[1] = my + worldY;
            wp[2] = s * mx + c * mz + worldZ;
            track(st, wp, dt);
            // acceleration back into his own frame (the viewer's world = his model space)
            double accX = c * st.acc[0] + s * st.acc[2], accZ = -s * st.acc[0] + c * st.acc[2];

            double breeze = Math.sin(TAU * sp.f * (time - sp.lag)) * sp.breeze;
            double chainX = parentSp != null ? sp.chain * (parentSp.ax - parentCfg.bx) : 0;
            double chainZ = parentSp != null ? sp.chain * parentSp.az : 0;
            parentSp = st;
            parentCfg = sp;
            double targetX = clamp(-accZ * sp.accel * 0.03 + sp.bx + breeze + chainX, sp.x0, sp.x1);
            double targetZ = clamp(accX * sp.accel * 0.03 + chainZ, sp.z0, sp.z1);

            st.vx += (targetX - st.ax) * sp.k * dt;
            st.vx *= Math.max(0, 1 - sp.d * dt);
            st.ax += st.vx * dt;
            st.vz += (targetZ - st.az) * sp.k * dt;
            st.vz *= Math.max(0, 1 - sp.d * dt);
            st.az += st.vz * dt;

            rot[b * 3] += st.ax;
            rot[b * 3 + 2] += st.az;
            boneMatrix(b);
        }
    }

    /* ───────── Matrices ───────── */

    /** Model-space matrix of every bone (blocks), from pos/rot/scl. Parents come before children. */
    public void computeMatrices() {
        for (int b = 0; b < nb; b++) boneMatrix(b);
    }

    private void boneMatrix(int b) {
        double[] local = tmpLocal;
        compose(local, 0, pos[b * 3] / 16, pos[b * 3 + 1] / 16, pos[b * 3 + 2] / 16, rot[b * 3], rot[b * 3 + 1], rot[b * 3 + 2], scl[b * 3], scl[b * 3 + 1], scl[b * 3 + 2]);
        int p = d.parent[b];
        if (p < 0) System.arraycopy(local, 0, mat, b * 12, 12);
        else mul(mat, p * 12, local, 0, mat, b * 12);
    }

    /** T * Rz * Ry * Rx * S (Euler order ZYX, like the viewer). */
    public static void compose(double[] o, int at, double tx, double ty, double tz, double x, double y, double z, double sx, double sy, double sz) {
        double cx = Math.cos(x), sxn = Math.sin(x), cy = Math.cos(y), syn = Math.sin(y), cz = Math.cos(z), szn = Math.sin(z);
        o[at] = cz * cy * sx;        o[at + 1] = (cz * syn * sxn - szn * cx) * sy; o[at + 2] = (cz * syn * cx + szn * sxn) * sz; o[at + 3] = tx;
        o[at + 4] = szn * cy * sx;   o[at + 5] = (szn * syn * sxn + cz * cx) * sy; o[at + 6] = (szn * syn * cx - cz * sxn) * sz; o[at + 7] = ty;
        o[at + 8] = -syn * sx;       o[at + 9] = cy * sxn * sy;                    o[at + 10] = cy * cx * sz;                    o[at + 11] = tz;
    }

    /** o = a * b for 3x4 affine matrices (row-major). o may not overlap a or b. */
    public static void mul(double[] a, int ai, double[] b, int bi, double[] o, int oi) {
        for (int r = 0; r < 3; r++) {
            double a0 = a[ai + r * 4], a1 = a[ai + r * 4 + 1], a2 = a[ai + r * 4 + 2], a3 = a[ai + r * 4 + 3];
            o[oi + r * 4] = a0 * b[bi] + a1 * b[bi + 4] + a2 * b[bi + 8];
            o[oi + r * 4 + 1] = a0 * b[bi + 1] + a1 * b[bi + 5] + a2 * b[bi + 9];
            o[oi + r * 4 + 2] = a0 * b[bi + 2] + a1 * b[bi + 6] + a2 * b[bi + 10];
            o[oi + r * 4 + 3] = a0 * b[bi + 3] + a1 * b[bi + 7] + a2 * b[bi + 11] + a3;
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

    /* trackMotion(): smoothed velocity and acceleration of a world point. */
    private static void track(SpringState st, double[] w, double dt) {
        if (!st.init) {
            System.arraycopy(w, 0, st.prev, 0, 3);
            Arrays.fill(st.vel, 0); Arrays.fill(st.velPrev, 0); Arrays.fill(st.acc, 0);
            st.init = true;
            return;
        }
        double kv = filt(dt, 25), ka = filt(dt, 18);
        for (int i = 0; i < 3; i++) {
            double v = (w[i] - st.prev[i]) / dt;
            st.prev[i] = w[i];
            st.vel[i] += (v - st.vel[i]) * kv;
            double a = (st.vel[i] - st.velPrev[i]) / dt;
            st.velPrev[i] = st.vel[i];
            st.acc[i] += (a - st.acc[i]) * ka;
        }
    }
}
