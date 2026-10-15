package com.custommobs.theking.anim;

/* ───────── The King's animation runtime: a 1:1 port of step() in mobs/the_king/thekingv3.html ─────────
   Same order, same numbers: crossfade, loop fade, arm clamps, talk, head-follow, face filter, blinks,
   lids/pupils/brows/mouth, head lift, springs, crown, ground check. Only the animation curves come
   baked (60 fps) instead of as code. The 1:1 test (src/test) replays the viewer's recording.
   Units: bone positions in texels, rotations in radians (order Z, then Y, then X), world tracking in blocks. */
public final class KingAnimator {
    private static final double TAU = Math.PI * 2;

    /* ───────── Pose buffer (one animation frame) ───────── */
    private static final class Pose {
        final double[] r, p;
        final double[] f = new double[6];
        double yawLimit = 0.5, follow = 1;

        Pose(int nb) { r = new double[nb * 3]; p = new double[nb * 3]; }

        void clear() { java.util.Arrays.fill(r, 0); java.util.Arrays.fill(p, 0); java.util.Arrays.fill(f, 0); }

        void copyTo(Pose d) {
            System.arraycopy(r, 0, d.r, 0, r.length);
            System.arraycopy(p, 0, d.p, 0, p.length);
            System.arraycopy(f, 0, d.f, 0, 6);
            d.yawLimit = yawLimit;
            d.follow = follow;
        }
    }

    /* Smoothed velocity / acceleration of a point (world, blocks). */
    private static class Motion {
        boolean init;
        final double[] prev = new double[3], vel = new double[3], velPrev = new double[3], acc = new double[3];
    }

    private static final class SpringState extends Motion {
        double ax, az, vx, vz;
    }

    private static final class CrownState extends Motion {
        double lift, liftV, tx, tz, vx, vz;
    }

    // Face channel indexes (KingData.FACE_KEYS order).
    private static final int BROW_UP = 0, FROWN = 1, OPEN = 2, WIDE = 3, GLANCE = 4, SQUINT = 5;

    public final KingData d;
    private final int nb;
    private final Pose snap, cur, out;

    /* ───────── State (same names as the viewer's sim) ───────── */
    public KingData.Anim anim;
    public double t, time, blend = 1;
    private final double[] face = new double[6];
    private double yawLimit = 0.5;
    public boolean talkActive;
    public double talkLevel, talkShape = 0.5;
    private double talkW;
    private int rngSeed;
    private double blinkWait = 1.2, blinkT = -1;
    private boolean blinkAgain;
    public boolean followOn = true, followHas;
    public double lookYaw, lookPitch;      // where the mob looks, relative to its body (radians; + yaw = his left, + pitch = down)
    private double fw = 1, fyaw, fpitch, feye;
    private final SpringState[] springs;
    private final CrownState crown = new CrownState();
    public double groundLift;
    /** Root motion still inside the root bone (texels): the renderer takes it out, the entity really moves instead. */
    public double rmZ, rmY;
    private double rmSnapZ, rmSnapY;

    /* Outer transform: the entity in the world (blocks, body yaw in radians, Minecraft yaw). 0 = viewer (no motion). */
    public double worldX, worldY, worldZ, worldYaw;

    /* ───────── Bones: local transform (texels, radians) and model-space matrices (blocks, 3x4 row-major) ───────── */
    public final double[] pos, rot, scl;
    public final double[] mat;
    private final double[] boxMat;     // each cube's own 3x4 matrix in its bone (blocks)
    private final double[] tmpW = new double[3], tmpV = new double[3], tmpM = new double[12], tmpLocal = new double[12]; // no allocation per frame

    private final int ROOT, HEAD, JAW, CROWN, LID_R, LID_L, PUPIL_R, PUPIL_L, BROW_R, BROW_L, MOUTH, ARM_R, ARM_L;

    public KingAnimator(KingData d, int seed) {
        this.d = d;
        nb = d.boneNames.length;
        snap = new Pose(nb);
        cur = new Pose(nb);
        out = new Pose(nb);
        pos = new double[nb * 3];
        rot = new double[nb * 3];
        scl = new double[nb * 3];
        mat = new double[nb * 12];
        rngSeed = seed;
        springs = new SpringState[d.springs.length];
        for (int i = 0; i < springs.length; i++) springs[i] = new SpringState();
        ROOT = d.bone("root"); HEAD = d.bone("head"); JAW = d.bone("chin_bandana"); CROWN = d.bone("golden_crown");
        LID_R = d.bone("lid_right"); LID_L = d.bone("lid_left"); PUPIL_R = d.bone("pupil_right"); PUPIL_L = d.bone("pupil_left");
        BROW_R = d.bone("brow_right"); BROW_L = d.bone("brow_left"); MOUTH = d.bone("mouth");
        ARM_R = d.bone("arm_right"); ARM_L = d.bone("arm_left");
        boxMat = new double[d.cubes.length * 12];
        for (int c = 0; c < d.cubes.length; c++) {
            KingData.Cube k = d.cubes[c];
            compose(boxMat, c * 12, k.boxCenter[0] / 16, k.boxCenter[1] / 16, k.boxCenter[2] / 16, k.boxRot[0], k.boxRot[1], k.boxRot[2], 1, 1, 1);
        }
        anim = d.anim("idle");
        System.arraycopy(d.restPos, 0, pos, 0, nb * 3);
        System.arraycopy(d.restRot, 0, rot, 0, nb * 3);
        java.util.Arrays.fill(scl, 1);
    }

    /* mulberry32, same bits as the viewer (blink timing). */
    private double rng() {
        rngSeed = rngSeed + 0x6D2B79F5;
        int x = (rngSeed ^ (rngSeed >>> 15)) * (1 | rngSeed);
        x = (x + ((x ^ (x >>> 7)) * (61 | x))) ^ x;
        return Integer.toUnsignedLong(x ^ (x >>> 14)) / 4294967296.0;
    }

    private static double filt(double dt, double rate) { return 1 - Math.exp(-dt * rate); }
    private static double clamp(double v, double a, double b) { return v < a ? a : v > b ? b : v; }
    private static double smooth01(double x) { x = clamp(x, 0, 1); return x * x * (3 - 2 * x); }

    /** Crossfade to another animation from whatever is on screen now (viewer: switchAnim). */
    public void switchAnim(String name) {
        KingData.Anim a = d.anim(name);
        if (a == anim) return;
        out.copyTo(snap);
        rmSnapZ = rmZ; rmSnapY = rmY;
        anim = a;
        t = 0;
        blend = 0;
    }

    /** One frame. dt = real seconds; rate = animation speed (1 = viewer speed; walk follows the ground speed). */
    public void step(double dt, double rate) {
        KingData.Anim A = anim;
        double dta = dt * rate;
        time += dt;
        // loopFade: an anim whose end differs from its start crossfades on the loop wrap.
        if (A.loopFade && t + dta >= A.T) { out.copyTo(snap); rmSnapZ = rmZ; rmSnapY = rmY; blend = 0; }
        t = (t + dta) % A.T;
        blend = Math.min(1, blend + dt / 0.35);

        // 1. Animation pose, crossfaded from the snapshot, added to the rest pose.
        cur.clear();
        cur.yawLimit = A.yawLimit;
        double fps = d.fps;
        for (int i = 0; i < nb * 3; i++) {
            cur.r[i] = KingData.Anim.sample(A.rot[i], t, fps);
            cur.p[i] = KingData.Anim.sample(A.pos[i], t, fps);
        }
        for (int k = 0; k < 6; k++) cur.f[k] = KingData.Anim.sample(A.face[k], t, fps);
        cur.follow = A.follow == null ? 1 : KingData.Anim.sample(A.follow, t, fps);
        double w = smooth01(blend);
        for (int i = 0; i < nb * 3; i++) {
            out.r[i] = snap.r[i] + (cur.r[i] - snap.r[i]) * w;
            out.p[i] = snap.p[i] + (cur.p[i] - snap.p[i]) * w;
        }
        for (int k = 0; k < 6; k++) out.f[k] = snap.f[k] + (cur.f[k] - snap.f[k]) * w;
        out.yawLimit = snap.yawLimit + (cur.yawLimit - snap.yawLimit) * w;
        out.follow = snap.follow + (cur.follow - snap.follow) * w;
        double curRmZ = A.rootMotion ? KingData.Anim.sample(A.rmZ, t, fps) : 0, curRmY = A.rootMotion ? KingData.Anim.sample(A.rmY, t, fps) : 0;
        rmZ = rmSnapZ + (curRmZ - rmSnapZ) * w;
        rmY = rmSnapY + (curRmY - rmSnapY) * w;

        for (int i = 0; i < nb * 3; i++) {
            rot[i] = d.restRot[i] + out.r[i];
            pos[i] = d.restPos[i] + out.p[i];
            scl[i] = 1;
        }
        // Arms only rotate outward on Z (right arm sits on -x, so outward is negative).
        rot[ARM_R * 3 + 2] = Math.min(d.restRot[ARM_R * 3 + 2], rot[ARM_R * 3 + 2]);
        rot[ARM_L * 3 + 2] = Math.max(d.restRot[ARM_L * 3 + 2], rot[ARM_L * 3 + 2]);

        // 2. Head + eyes follow the look target; chin lifts on loud syllables.
        rot[HEAD * 3] -= 0.06 * talkLevel * talkW;
        yawLimit += (out.yawLimit - yawLimit) * filt(dt, 4);
        fw += ((followOn && followHas ? out.follow : 0) - fw) * filt(dt, 5);
        double wantYaw = 0, wantPitch = 0, wantEye = 0;
        if (followOn && followHas) {
            wantYaw = clamp(lookYaw, -yawLimit, yawLimit);
            wantPitch = clamp(lookPitch, d.pitchUp, d.pitchDown);
            wantEye = clamp((lookYaw - wantYaw) * 2.2, -1, 1);
        }
        double k5 = filt(dt, 6);
        fyaw += (wantYaw - fyaw) * k5;
        fpitch += (wantPitch - fpitch) * k5;
        feye += (wantEye - feye) * filt(dt, 10);
        rot[HEAD * 3 + 1] = clamp(rot[HEAD * 3 + 1] + fyaw * fw, -yawLimit, yawLimit);
        rot[HEAD * 3] = clamp(rot[HEAD * 3] + fpitch * fw, d.pitchUp, d.pitchDown);

        // 3. Face: talking overrides the mouth, smoothing filter, random (double) blinks.
        talkW += ((talkActive ? 1 : 0) - talkW) * filt(dt, 10);
        double tw = talkW;
        double[] of = out.f;
        if (tw > 0.001) {
            of[OPEN] += (talkLevel - of[OPEN]) * tw;
            of[WIDE] += ((talkShape - 0.5) * 1.4 - of[WIDE]) * tw;
            of[FROWN] = Math.max(of[FROWN], 0.55 * tw);
            of[BROW_UP] += (-0.15 - of[BROW_UP]) * tw * 0.6;
            of[SQUINT] = Math.max(of[SQUINT], 0.25 * tw);
        }
        double kf16 = filt(dt, 16);
        for (int k = 0; k < 6; k++) face[k] += (of[k] - face[k]) * kf16;
        double blinkV = 0;
        if (blinkT < 0) {
            blinkWait -= dt;
            if (blinkWait <= 0) { blinkT = 0; blinkAgain = rng() < 0.18; }
        }
        if (blinkT >= 0) {
            blinkT += dt;
            if (blinkT >= 0.18) {
                blinkT = -1;
                blinkWait = blinkAgain ? 0.12 : 1.8 + rng() * 3.4;
                blinkAgain = false;
            } else blinkV = Math.sin(Math.PI * blinkT / 0.18);
        }
        double closed = clamp(Math.max(blinkV, face[SQUINT] * 0.5), 0.001, 1);
        scl[LID_R * 3 + 1] = closed;
        scl[LID_L * 3 + 1] = closed;
        double lidZ = closed > 0.002 ? 0.08 : 0; // lids step out of the head only while closing
        pos[LID_R * 3 + 2] = d.restPos[LID_R * 3 + 2] + lidZ;
        pos[LID_L * 3 + 2] = d.restPos[LID_L * 3 + 2] + lidZ;
        double glance = clamp(face[GLANCE] + feye * fw, -1, 1);
        pos[PUPIL_R * 3] = d.restPos[PUPIL_R * 3] + glance * 0.5;
        pos[PUPIL_L * 3] = d.restPos[PUPIL_L * 3] + glance * 0.5;
        double browY = clamp(face[BROW_UP] * 0.3 - face[FROWN] * 0.15 - blinkV * 0.08, -0.25, 0.25);
        pos[BROW_R * 3 + 1] = d.restPos[BROW_R * 3 + 1] + browY;
        pos[BROW_L * 3 + 1] = d.restPos[BROW_L * 3 + 1] + browY;
        rot[BROW_R * 3 + 2] = -face[FROWN] * 0.3;
        rot[BROW_L * 3 + 2] = face[FROWN] * 0.3;
        double wide = clamp(face[WIDE], -1, 1), open = clamp(face[OPEN], 0, 1);
        scl[MOUTH * 3] = 1 + wide * 0.3;
        scl[MOUTH * 3 + 1] = clamp(1 + open * 1.1 + Math.max(0, -wide) * 0.3, 1, 2);
        scl[MOUTH * 3 + 2] = 1;
        pos[MOUTH * 3 + 1] = d.restPos[MOUTH * 3 + 1] + open * 0.35;
        rot[JAW * 3] += open * 0.06;
        // The head rises on its neck so the bandana never sinks into the collar (pitch, turn, jaw drop).
        double hp = rot[HEAD * 3];
        pos[HEAD * 3 + 1] += Math.max(0, -hp) * 3 + Math.max(0, hp) * 4.6 + open * 0.06 * 4.2 + rot[HEAD * 3 + 1] * rot[HEAD * 3 + 1] * 1.4;

        // 4. Springs on loose parts (hair).
        computeMatrices();
        double[] wp = tmpW, vB = tmpV;
        for (int s = 0; s < springs.length; s++) {
            KingData.Spring sp = d.springs[s];
            SpringState st = springs[s];
            int g = sp.bone;
            worldPoint(g, wp);
            track(st, wp, dt);
            vB[0] = -st.acc[0] * sp.accel;
            vB[1] = -d.gravity - st.acc[1] * sp.accel;
            vB[2] = -st.acc[2] * sp.accel;
            toParentSpace(g, vB);
            double len = Math.sqrt(vB[0] * vB[0] + vB[1] * vB[1] + vB[2] * vB[2]);
            if (len > 0) { vB[0] /= len; vB[1] /= len; vB[2] /= len; }
            double tx = Math.atan2(-vB[2], -vB[1]) + sp.bx + sp.breeze * Math.sin(time * sp.f * TAU * 0.5);
            double tz = Math.atan2(vB[0], -vB[1]) + sp.bz + sp.breeze * 0.6 * Math.sin(time * sp.f * TAU * 0.37 + 1);
            st.vx += (sp.k * (tx - st.ax) - sp.d * st.vx) * dt;
            st.vz += (sp.k * (tz - st.az) - sp.d * st.vz) * dt;
            st.ax += st.vx * dt;
            st.az += st.vz * dt;
            if (st.ax < sp.x0 || st.ax > sp.x1) { st.ax = clamp(st.ax, sp.x0, sp.x1); st.vx = 0; }
            if (st.az < sp.z0 || st.az > sp.z1) { st.az = clamp(st.az, sp.z0, sp.z1); st.vz = 0; }
            rot[g * 3] = d.restRot[g * 3] + st.ax;
            rot[g * 3 + 2] = d.restRot[g * 3 + 2] + st.az;
        }

        // Crown: lifts a little when the head drops fast, tilts against side motion. Never sinks below rest.
        CrownState C = crown;
        worldPoint(CROWN, wp);
        track(C, wp, dt);
        vB[0] = C.acc[0]; vB[1] = C.acc[1]; vB[2] = C.acc[2];
        toParentSpace(CROWN, vB);
        double liftT = clamp(-vB[1] * 0.05, 0, 0.3) + 0.02 * (0.5 + 0.5 * Math.sin(time * 2.1));
        C.liftV += (220 * (liftT - C.lift) - 14 * C.liftV) * dt;
        C.lift = clamp(C.lift + C.liftV * dt, 0, 0.3);
        double tiltX = clamp(-vB[2] * 0.004, -0.01, 0.03), tiltZ = clamp(vB[0] * 0.004, -0.03, 0.03);
        C.vx += (160 * (tiltX - C.tx) - 10 * C.vx) * dt;
        C.vz += (160 * (tiltZ - C.tz) - 10 * C.vz) * dt;
        C.tx = clamp(C.tx + C.vx * dt, -0.01, 0.03);
        C.tz = clamp(C.tz + C.vz * dt, -0.03, 0.03);
        pos[CROWN * 3 + 1] = d.restPos[CROWN * 3 + 1] + C.lift;
        rot[CROWN * 3] = d.restRot[CROWN * 3] + C.tx;
        rot[CROWN * 3 + 2] = d.restRot[CROWN * 3 + 2] + C.tz;

        // 5. Nothing sinks into the ground: lift the whole body by the lowest corner of any box.
        computeMatrices();
        double low = 0;
        double[] m = tmpM;
        for (int c = 0; c < d.cubes.length; c++) {
            KingData.Cube k = d.cubes[c];
            mul(mat, k.bone * 12, boxMat, c * 12, m, 0);
            double hx = k.boxHalf[0] / 16, hy = k.boxHalf[1] / 16, hz = k.boxHalf[2] / 16;
            for (int q = 0; q < 8; q++) {
                double x = (q & 1) != 0 ? hx : -hx, y = (q & 2) != 0 ? hy : -hy, z = (q & 4) != 0 ? hz : -hz;
                double wy = m[4] * x + m[5] * y + m[6] * z + m[7];
                if (wy < low) low = wy;
            }
        }
        if (low < 0) pos[ROOT * 3 + 1] -= low * 16;
        groundLift = -low * 16;
    }

    /* ───────── Matrices ───────── */

    /** Model-space matrix of every bone (blocks), from pos/rot/scl. Parents come before children. */
    public void computeMatrices() {
        double[] local = tmpLocal;
        for (int b = 0; b < nb; b++) {
            compose(local, 0, pos[b * 3] / 16, pos[b * 3 + 1] / 16, pos[b * 3 + 2] / 16, rot[b * 3], rot[b * 3 + 1], rot[b * 3 + 2], scl[b * 3], scl[b * 3 + 1], scl[b * 3 + 2]);
            int p = d.parent[b];
            if (p < 0) System.arraycopy(local, 0, mat, b * 12, 12);
            else mul(mat, p * 12, local, 0, mat, b * 12);
        }
    }

    /** T * Rz * Ry * Rx * S (Euler order ZYX, like the viewer and Minecraft). */
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

    /* World position of a bone's pivot (blocks): the entity's transform on top of model space. */
    private void worldPoint(int b, double[] o) {
        double x = mat[b * 12 + 3], y = mat[b * 12 + 7], z = mat[b * 12 + 11];
        // Model -> world: turn by -yaw around Y (Minecraft yaw 0 = facing +Z, model front = +Z).
        double c = Math.cos(worldYaw), s = Math.sin(worldYaw);
        o[0] = c * x - s * z + worldX;
        o[1] = y + worldY;
        o[2] = s * x + c * z + worldZ;
    }

    /* A world vector into a bone's parent space (inverse of the parent's world rotation; parents of springs have scale 1). */
    private void toParentSpace(int b, double[] v) {
        double c = Math.cos(worldYaw), s = Math.sin(worldYaw);
        double x = c * v[0] + s * v[2], y = v[1], z = -s * v[0] + c * v[2]; // undo the entity turn
        int p = d.parent[b] * 12;
        v[0] = mat[p] * x + mat[p + 4] * y + mat[p + 8] * z;  // transpose of the parent's rotation
        v[1] = mat[p + 1] * x + mat[p + 5] * y + mat[p + 9] * z;
        v[2] = mat[p + 2] * x + mat[p + 6] * y + mat[p + 10] * z;
    }

    /* trackMotion(): smoothed velocity and acceleration of a world point. */
    private static void track(Motion st, double[] w, double dt) {
        if (!st.init) {
            System.arraycopy(w, 0, st.prev, 0, 3);
            java.util.Arrays.fill(st.vel, 0); java.util.Arrays.fill(st.velPrev, 0); java.util.Arrays.fill(st.acc, 0);
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
