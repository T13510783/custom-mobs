package com.custommobs.homelander.anim;

/* ───────── Homelander's cape cloth: a 1:1 port of the Cloth section in mobs/homelander/homelander.html ─────────
   Verlet cloth of NK chains x NP points hanging from the chest; gravity, air drag, a random breeze, distance
   constraints (along, across, diagonal, bending, max distance from the top), collisions with his body boxes
   (shortest way out; with both hands behind his back the arms push it onto his back) and the floor; then every cape bone turns to follow its piece of cloth.
   Same order of operations as the viewer, StrictMath (fdlibm, like the browser's Math) and three.js-style
   matrices (Euler -> quaternion -> matrix), so the mod's cloth follows the viewer's.
   Units: texels, seconds, his model space (+x = his left, +y = up, +z = his front). */
public final class HomelanderCloth {
    private static final double TAU = Math.PI * 2, TEXEL = 1.0 / 16;

    final HomelanderData.Cloth cfg;
    public final int NK, NP, N;
    final int KMID;
    final double KGAP;
    boolean init;
    /** Top-edge width factor: eases to cfg.handsWidth while both hands are behind his back (viewer C.wf). */
    double wf = 1;
    /** Points now / one substep ago (texels, model space): index (k * NP + i) * 3. */
    public final double[] P, Q;
    final double[] A, col, R = new double[9], Rp = new double[9], chestM = new double[12];
    final double[] ph = new double[6];
    private final boolean[] handIn;
    private final double[] tmpM = new double[16], tmpW = new double[16];   // reused every frame (no garbage)

    /** rand = the animator's random numbers (6 breeze phases: same order as the viewer's makeCloth). */
    public HomelanderCloth(HomelanderData.Cloth cfg, java.util.function.DoubleSupplier rand) {
        this.cfg = cfg;
        NK = cfg.cols; N = cfg.n; NP = N + 1;
        KMID = (NK - 1) / 2;
        KGAP = cfg.width / (NK - 1);
        P = new double[NK * NP * 3];
        Q = new double[NK * NP * 3];
        A = new double[NK * 3];
        col = new double[cfg.colliders.length * 12];
        handIn = new boolean[cfg.colliders.length];
        for (int i = 0; i < 6; i++) ph[i] = rand.getAsDouble() * TAU;
    }

    /** Mod only (the viewer never turns): the body turned by dyaw (radians, Minecraft body yaw). Cloth that stays put in
     *  the world turns the other way in his model space; turning P and Q together keeps its world speed (inertia),
     *  so the cape swings out on a turn. Half of the turn only (the air drags the cloth along, like `move`), at most
     *  0.08 rad per frame: mobs snap their yaw much faster than a body can turn, and a full swing stuck out stiffly. */
    public void turn(double dyaw) {
        if (!init || dyaw == 0) return;
        dyaw = Math.max(-0.08, Math.min(0.08, dyaw * 0.5));
        double c = Math.cos(dyaw), s = Math.sin(dyaw);
        for (int j = 0; j < P.length; j += 3) {
            double x = P[j], z = P[j + 2];
            P[j] = c * x + s * z; P[j + 2] = -s * x + c * z;
            x = Q[j]; z = Q[j + 2];
            Q[j] = c * x + s * z; Q[j + 2] = -s * x + c * z;
        }
    }

    /* ───────── three.js math (Object3D.updateMatrix + matrixWorld), column-major 4x4 like Matrix4.elements ───────── */

    /** Matrix4.compose(position, Quaternion.setFromEuler(x, y, z, 'ZYX'), scale). */
    public static void compose(double[] te, double px, double py, double pz, double ex, double ey, double ez, double sx, double sy, double sz) {
        double c1 = StrictMath.cos(ex / 2), c2 = StrictMath.cos(ey / 2), c3 = StrictMath.cos(ez / 2);
        double s1 = StrictMath.sin(ex / 2), s2 = StrictMath.sin(ey / 2), s3 = StrictMath.sin(ez / 2);
        double x = s1 * c2 * c3 - c1 * s2 * s3;
        double y = c1 * s2 * c3 + s1 * c2 * s3;
        double z = c1 * c2 * s3 - s1 * s2 * c3;
        double w = c1 * c2 * c3 + s1 * s2 * s3;
        double x2 = x + x, y2 = y + y, z2 = z + z;
        double xx = x * x2, xy = x * y2, xz = x * z2;
        double yy = y * y2, yz = y * z2, zz = z * z2;
        double wx = w * x2, wy = w * y2, wz = w * z2;
        te[0] = (1 - (yy + zz)) * sx; te[1] = (xy + wz) * sx; te[2] = (xz - wy) * sx; te[3] = 0;
        te[4] = (xy - wz) * sy; te[5] = (1 - (xx + zz)) * sy; te[6] = (yz + wx) * sy; te[7] = 0;
        te[8] = (xz + wy) * sz; te[9] = (yz - wx) * sz; te[10] = (1 - (xx + yy)) * sz; te[11] = 0;
        te[12] = px; te[13] = py; te[14] = pz; te[15] = 1;
    }

    /** Matrix4.multiplyMatrices(a, b) into o (o may not be a or b). */
    public static void multiply(double[] ae, double[] be, double[] te) {
        double a11 = ae[0], a12 = ae[4], a13 = ae[8], a14 = ae[12];
        double a21 = ae[1], a22 = ae[5], a23 = ae[9], a24 = ae[13];
        double a31 = ae[2], a32 = ae[6], a33 = ae[10], a34 = ae[14];
        double a41 = ae[3], a42 = ae[7], a43 = ae[11], a44 = ae[15];
        double b11 = be[0], b12 = be[4], b13 = be[8], b14 = be[12];
        double b21 = be[1], b22 = be[5], b23 = be[9], b24 = be[13];
        double b31 = be[2], b32 = be[6], b33 = be[10], b34 = be[14];
        double b41 = be[3], b42 = be[7], b43 = be[11], b44 = be[15];
        te[0] = a11 * b11 + a12 * b21 + a13 * b31 + a14 * b41;
        te[4] = a11 * b12 + a12 * b22 + a13 * b32 + a14 * b42;
        te[8] = a11 * b13 + a12 * b23 + a13 * b33 + a14 * b43;
        te[12] = a11 * b14 + a12 * b24 + a13 * b34 + a14 * b44;
        te[1] = a21 * b11 + a22 * b21 + a23 * b31 + a24 * b41;
        te[5] = a21 * b12 + a22 * b22 + a23 * b32 + a24 * b42;
        te[9] = a21 * b13 + a22 * b23 + a23 * b33 + a24 * b43;
        te[13] = a21 * b14 + a22 * b24 + a23 * b34 + a24 * b44;
        te[2] = a31 * b11 + a32 * b21 + a33 * b31 + a34 * b41;
        te[6] = a31 * b12 + a32 * b22 + a33 * b32 + a34 * b42;
        te[10] = a31 * b13 + a32 * b23 + a33 * b33 + a34 * b43;
        te[14] = a31 * b14 + a32 * b24 + a33 * b34 + a34 * b44;
        te[3] = a41 * b11 + a42 * b21 + a43 * b31 + a44 * b41;
        te[7] = a41 * b12 + a42 * b22 + a43 * b32 + a44 * b42;
        te[11] = a41 * b13 + a42 * b23 + a43 * b33 + a44 * b43;
        te[15] = a41 * b14 + a42 * b24 + a43 * b34 + a44 * b44;
    }

    // Rotation columns + translation (texels) of a world matrix into o[at..at+11] (viewer mat34)
    private static void mat34(double[] e, double[] o, int at) {
        o[at] = e[0]; o[at + 1] = e[1]; o[at + 2] = e[2]; o[at + 3] = e[4]; o[at + 4] = e[5]; o[at + 5] = e[6];
        o[at + 6] = e[8]; o[at + 7] = e[9]; o[at + 8] = e[10]; o[at + 9] = e[12] / TEXEL; o[at + 10] = e[13] / TEXEL; o[at + 11] = e[14] / TEXEL;
    }

    /* ───────── One frame ─────────
       world = every bone's three.js world matrix this frame (blocks), chest = the chest bone, time = sim time after
       this frame, move = how far his body moved through the air this frame (texels, model space).
       rot = the animator's bone rotations: the cape bones get their new angles. */
    public void step(double[][] world, int chest, double time, double dt, double[] move, double[] rot) {
        final double[] P = this.P, Q = this.Q;
        final double seg = cfg.seg;
        mat34(world[chest], chestM, 0);
        // body boxes this frame: bone world x (center, rotation)
        for (int c = 0; c < cfg.colliders.length; c++) {
            HomelanderData.Collider cd = cfg.colliders[c];
            compose(tmpM, cd.c[0] * TEXEL, cd.c[1] * TEXEL, cd.c[2] * TEXEL, cd.r[0], cd.r[1], cd.r[2], 1, 1, 1);
            multiply(world[cd.bone], tmpM, tmpW);
            mat34(tmpW, col, c * 12);
        }
        boolean hands = handsBlock();
        // width: hands behind the back -> the top edge eases in to handsWidth (it fits between the arms, under the hands)
        wf += ((hands ? cfg.handsWidth : 1) - wf) * (1 - StrictMath.exp(-dt * 6));
        final double gap = KGAP * wf, diag = Math.sqrt(gap * gap + seg * seg);
        // anchors: along the top edge of the cape, in chest space
        for (int k = 0; k < NK; k++) {
            double x = cfg.top[0] + (k - KMID) * gap, y = cfg.top[1], z = cfg.top[2];
            double[] m = chestM;
            A[k * 3] = m[0] * x + m[3] * y + m[6] * z + m[9];
            A[k * 3 + 1] = m[1] * x + m[4] * y + m[7] * z + m[10];
            A[k * 3 + 2] = m[2] * x + m[5] * y + m[8] * z + m[11];
        }
        if (!init) {                                       // first frame: hang straight down
            for (int k = 0; k < NK; k++) for (int i = 0; i <= N; i++) {
                int j = (k * NP + i) * 3;
                P[j] = Q[j] = A[k * 3]; P[j + 1] = Q[j + 1] = A[k * 3 + 1] - i * seg; P[j + 2] = Q[j + 2] = A[k * 3 + 2];
            }
            init = true;
        }

        int n = Math.max(1, (int) Math.ceil(dt / cfg.sub - 1e-9));
        double h = dt / n;
        double damp = StrictMath.exp(-cfg.drag * h);
        double mx = move[0] / n, my = move[1] / n, mz = move[2] / n;
        for (int s = 0; s < n; s++) {
            double tt = time - dt + (s + 1) * h;
            // breeze: slow gusts, mostly from the front (blows the cape back), a bit sideways and up/down
            double W = cfg.wind * (0.55 + 0.45 * StrictMath.sin(TAU * 0.13 * tt + ph[0]));
            double wx = W * 0.35 * StrictMath.sin(TAU * 0.23 * tt + ph[1]), wy = W * 0.15 * StrictMath.sin(TAU * 0.41 * tt + ph[2]);
            double wz = -W * (0.6 + 0.4 * StrictMath.sin(TAU * 0.17 * tt + ph[3]));
            // integrate (verlet): inertia (damped by the air), air pushing on a moving body, gravity, breeze
            for (int k = 0; k < NK; k++) for (int i = 1; i <= N; i++) {
                int j = (k * NP + i) * 3;
                double ripple = 0.7 + 0.3 * StrictMath.sin(TAU * 1.3 * tt - i * 0.25 + ph[4] + k * 0.2);
                double px = P[j], py = P[j + 1], pz = P[j + 2];
                P[j] = px + (px - Q[j]) * damp - mx * (1 - damp) + wx * ripple * h * h;
                P[j + 1] = py + (py - Q[j + 1]) * damp - my * (1 - damp) + (wy * ripple - cfg.gravity) * h * h;
                P[j + 2] = pz + (pz - Q[j + 2]) * damp - mz * (1 - damp) + wz * ripple * h * h;
                Q[j] = px; Q[j + 1] = py; Q[j + 2] = pz;
            }
            for (int k = 0; k < NK; k++) { int j = k * NP * 3; P[j] = Q[j] = A[k * 3]; P[j + 1] = Q[j + 1] = A[k * 3 + 1]; P[j + 2] = Q[j + 2] = A[k * 3 + 2]; }
            for (int it = 0; it < cfg.iters; it++) {
                for (int k = 0; k < NK; k++) for (int i = 0; i < N; i++) link((k * NP + i) * 3, (k * NP + i + 1) * 3, seg, i == 0);
                // across (neighbour chains) and diagonally both ways
                for (int k = 0; k < NK - 1; k++) for (int i = 1; i <= N; i++) {
                    int a = (k * NP + i) * 3, b = ((k + 1) * NP + i) * 3;
                    link(a, b, gap, false);
                    link(a - 3, b, diag, i == 1); link(b - 3, a, diag, i == 1);
                }
                // bending across: every second chain + edge to edge keep each row nearly straight
                for (int k = 0; k < NK - 2; k++) for (int i = 1; i <= N; i++) link((k * NP + i) * 3, ((k + 2) * NP + i) * 3, 2 * gap, false);
                for (int i = 1; i <= N; i++) link(i * 3, ((NK - 1) * NP + i) * 3, (NK - 1) * gap, false);
                // never further from the top than the cloth length above it
                for (int k = 0; k < NK; k++) for (int i = 1; i <= N; i++) {
                    int j = (k * NP + i) * 3;
                    double dx = P[j] - A[k * 3], dy = P[j + 1] - A[k * 3 + 1], dz = P[j + 2] - A[k * 3 + 2];
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz), L = i * seg;
                    if (d > L) { double f = L / d; P[j] = A[k * 3] + dx * f; P[j + 1] = A[k * 3 + 1] + dy * f; P[j + 2] = A[k * 3 + 2] + dz * f; }
                }
                collide(hands);
            }
        }
        bones(rot);
    }

    // Keep two points `len` apart (each moves half; a pinned first point does not move).
    private void link(int a, int b, double len, boolean pinA) {
        double dx = P[b] - P[a], dy = P[b + 1] - P[a + 1], dz = P[b + 2] - P[a + 2];
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 1e-9) return;
        double f = (d - len) / d;
        if (pinA) { P[b] -= dx * f; P[b + 1] -= dy * f; P[b + 2] -= dz * f; return; }
        P[a] += dx * f * 0.5; P[a + 1] += dy * f * 0.5; P[a + 2] += dz * f * 0.5;
        P[b] -= dx * f * 0.5; P[b + 1] -= dy * f * 0.5; P[b + 2] -= dz * f * 0.5;
    }

    // Hands behind the back (viewer handsBlock): marks the forearm/fist boxes behind the cape's plane; true only when
    // BOTH fists sit behind the back near the spine (also while they swing in), so the arms slide over the cape.
    private boolean handsBlock() {
        double[] M = col;
        double mg = cfg.margin;
        boolean inR = false, inL = false;
        for (int c = 0; c < cfg.colliders.length; c++) {
            handIn[c] = false;
            HomelanderData.Collider cd = cfg.colliders[c];
            if (!cd.hand) continue;
            int o = c * 12;
            double h0 = cd.h[0], h1 = cd.h[1], h2 = cd.h[2];
            double lo2 = 1e9;
            for (int k = 0; k < 8; k++) {                  // corners -> chest space z (how far back)
                double a = (k & 1) != 0 ? h0 : -h0, b = (k & 2) != 0 ? h1 : -h1, d = (k & 4) != 0 ? h2 : -h2;
                double wx = M[o + 9] + M[o] * a + M[o + 3] * b + M[o + 6] * d - chestM[9];
                double wy = M[o + 10] + M[o + 1] * a + M[o + 4] * b + M[o + 7] * d - chestM[10];
                double wz = M[o + 11] + M[o + 2] * a + M[o + 5] * b + M[o + 8] * d - chestM[11];
                lo2 = Math.min(lo2, chestM[6] * wx + chestM[7] * wy + chestM[8] * wz);
            }
            if (lo2 >= cfg.top[2] - mg) continue;          // not behind the cape's plane
            handIn[c] = true;
            // hands ON the back: the fist's center behind the cape, near the spine, between hips and shoulders
            if (!cd.name.startsWith("gauntlet_")) continue;
            double wx = M[o + 9] - chestM[9], wy = M[o + 10] - chestM[10], wz = M[o + 11] - chestM[11];
            double x = chestM[0] * wx + chestM[1] * wy + chestM[2] * wz, y = chestM[3] * wx + chestM[4] * wy + chestM[5] * wz, z = chestM[6] * wx + chestM[7] * wy + chestM[8] * wz;
            if (z > cfg.top[2] || z < -10 || Math.abs(x) > 11 || y < -2 || y > 12) continue;
            if (cd.name.endsWith("_right")) inR = true; else inL = true;
        }
        if (!(inR && inL)) { java.util.Arrays.fill(handIn, false); return false; }   // only with BOTH hands behind the back
        return true;
    }

    // Push every point out of every body box (shortest way out) and above the floor; the old position moves along.
    // Hands behind the back: forearm/fist boxes push toward his back (chest +z), upper arms toward his spine, those
    // first, the plain boxes (torso) last: the cape lies ON his back, the hands rest on top of it.
    private void collide(boolean hands) {
        double[] M = col;
        double mg = cfg.margin;
        for (int j = 0; j < P.length; j += 3) {
            if (j % (NP * 3) == 0) continue;               // pinned top points
            double px = P[j], py = P[j + 1], pz = P[j + 2];
            for (int pass = hands ? 0 : 1; pass < 2; pass++) for (int c = 0; c < cfg.colliders.length; c++) {
                HomelanderData.Collider cd = cfg.colliders[c];
                boolean arm = hands && (handIn[c] || cd.upper);
                if ((pass == 0) != arm) continue;
                int o = c * 12;
                double dx = px - M[o + 9], dy = py - M[o + 10], dz = pz - M[o + 11];
                double lx = M[o] * dx + M[o + 1] * dy + M[o + 2] * dz;
                double ly = M[o + 3] * dx + M[o + 4] * dy + M[o + 5] * dz;
                double lz = M[o + 6] * dx + M[o + 7] * dy + M[o + 8] * dz;
                double hx = cd.h[0] + mg, hy = cd.h[1] + mg, hz = cd.h[2] + mg;
                double ex = hx - Math.abs(lx), ey = hy - Math.abs(ly), ez = hz - Math.abs(lz);
                if (ex <= 0 || ey <= 0 || ez <= 0) continue;
                if (arm) {
                    // forearm/fist: toward his back (chest +z); upper arm: toward his spine. That direction in this
                    // box's axes, then walk along it to the box's surface.
                    double wx = chestM[6], wy = chestM[7], wz = chestM[8];
                    if (!handIn[c]) {
                        double sx = chestM[0] * (px - chestM[9]) + chestM[1] * (py - chestM[10]) + chestM[2] * (pz - chestM[11]) < 0 ? 1 : -1;
                        wx = chestM[0] * sx; wy = chestM[1] * sx; wz = chestM[2] * sx;
                    }
                    double ux = M[o] * wx + M[o + 1] * wy + M[o + 2] * wz;
                    double uy = M[o + 3] * wx + M[o + 4] * wy + M[o + 5] * wz;
                    double uz = M[o + 6] * wx + M[o + 7] * wy + M[o + 8] * wz;
                    double s = 1e9;
                    if (ux > 1e-9) s = Math.min(s, (hx - lx) / ux); else if (ux < -1e-9) s = Math.min(s, (-hx - lx) / ux);
                    if (uy > 1e-9) s = Math.min(s, (hy - ly) / uy); else if (uy < -1e-9) s = Math.min(s, (-hy - ly) / uy);
                    if (uz > 1e-9) s = Math.min(s, (hz - lz) / uz); else if (uz < -1e-9) s = Math.min(s, (-hz - lz) / uz);
                    lx += ux * s; ly += uy * s; lz += uz * s;
                } else if (ex <= ey && ex <= ez) lx = lx < 0 ? -hx : hx; else if (ey <= ez) ly = ly < 0 ? -hy : hy; else lz = lz < 0 ? -hz : hz;
                px = M[o + 9] + M[o] * lx + M[o + 3] * ly + M[o + 6] * lz;
                py = M[o + 10] + M[o + 1] * lx + M[o + 4] * ly + M[o + 7] * lz;
                pz = M[o + 11] + M[o + 2] * lx + M[o + 5] * ly + M[o + 8] * lz;
            }
            if (py < mg) py = mg;
            Q[j] += px - P[j]; Q[j + 1] += py - P[j + 1]; Q[j + 2] += pz - P[j + 2];
            P[j] = px; P[j + 1] = py; P[j + 2] = pz;
        }
    }

    // Each cape bone follows its piece of the middle chain (x axis from the edge chains): local = Rp^T R, Euler ZYX.
    private void bones(double[] rot) {
        for (int a = 0; a < 9; a++) Rp[a] = chestM[a];
        for (int i = 0; i < N; i++) {
            int m0 = (KMID * NP + i) * 3, m1 = m0 + 3, l0 = i * 3, r0 = ((NK - 1) * NP + i) * 3;
            double yx = P[m0] - P[m1], yy = P[m0 + 1] - P[m1 + 1], yz = P[m0 + 2] - P[m1 + 2];
            double d = Math.sqrt(yx * yx + yy * yy + yz * yz);
            if (d == 0) d = 1;
            yx /= d; yy /= d; yz /= d;
            double xx = P[r0] + P[r0 + 3] - P[l0] - P[l0 + 3], xy = P[r0 + 1] + P[r0 + 4] - P[l0 + 1] - P[l0 + 4], xz = P[r0 + 2] + P[r0 + 5] - P[l0 + 2] - P[l0 + 5];
            double dot = xx * yx + xy * yy + xz * yz;
            xx -= yx * dot; xy -= yy * dot; xz -= yz * dot;
            d = Math.sqrt(xx * xx + xy * xy + xz * xz);
            if (d == 0) d = 1;
            xx /= d; xy /= d; xz /= d;
            double zx = xy * yz - xz * yy, zy = xz * yx - xx * yz, zz = xx * yy - xy * yx;
            R[0] = xx; R[1] = xy; R[2] = xz; R[3] = yx; R[4] = yy; R[5] = yz; R[6] = zx; R[7] = zy; R[8] = zz;
            double m11 = m(0, 0), m21 = m(1, 0), m31 = m(2, 0), m32 = m(2, 1), m33 = m(2, 2), m12 = m(0, 1), m22 = m(1, 1);
            double ry = StrictMath.asin(-Math.max(-1, Math.min(1, m31)));
            double rx, rz;
            if (Math.abs(m31) < 0.9999999) { rx = StrictMath.atan2(m32, m33); rz = StrictMath.atan2(m21, m11); } else { rx = 0; rz = StrictMath.atan2(-m12, m22); }
            int b = cfg.bones[i];
            rot[b * 3] = rx; rot[b * 3 + 1] = ry; rot[b * 3 + 2] = rz;
            System.arraycopy(R, 0, Rp, 0, 9);
        }
    }

    // m_rc = parent column r . world column c
    private double m(int r, int c) { return Rp[r * 3] * R[c * 3] + Rp[r * 3 + 1] * R[c * 3 + 1] + Rp[r * 3 + 2] * R[c * 3 + 2]; }
}
