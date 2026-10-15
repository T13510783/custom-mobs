/* Homelander viewer, part 'anims': Pose buffers, kinematics helpers, keyframes, animations.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Pose Buffers & Kinematics ───────── */
// Face channels: grin = teeth showing, smirk = one-sided mouth lift, glow = red eyes (heat vision warming up)
const FACE_KEYS = ['browUp', 'frown', 'open', 'wide', 'glance', 'squint', 'laser', 'grin', 'smirk', 'glow'];
const zeroFace = () => Object.fromEntries(FACE_KEYS.map((k) => [k, 0]));
function makePose() {
  return { r: new Float32Array(NB * 3), p: new Float32Array(NB * 3), f: zeroFace(), yawLimit: 0.45, follow: 1 };
}
function clearPose(Q) { Q.r.fill(0); Q.p.fill(0); for (const k of FACE_KEYS) Q.f[k] = 0; }
function copyPose(src, dst) { dst.r.set(src.r); dst.p.set(src.p); for (const k of FACE_KEYS) dst.f[k] = src.f[k]; dst.yawLimit = src.yawLimit; dst.follow = src.follow; }
function rot(Q, b, x, y, z) { if (B[b] !== undefined) { const i = B[b] * 3; Q.r[i] += x; Q.r[i + 1] += y; Q.r[i + 2] += z; } }
function pos(Q, b, x, y, z) { if (B[b] !== undefined) { const i = B[b] * 3; Q.p[i] += x; Q.p[i + 1] += y; Q.p[i + 2] += z; } }

const TAU = Math.PI * 2;
const clamp = (v, a, b) => (v < a ? a : v > b ? b : v);

function kf(t, keys) {
  for (let i = 1; i < keys.length; i++) {
    if (t <= keys[i][0]) {
      const a = keys[i - 1], b = keys[i];
      const s = (t - a[0]) / (b[0] - a[0]);
      return a[1] + (b[1] - a[1]) * (0.5 - 0.5 * Math.cos(Math.PI * s));
    }
  }
  return keys[keys.length - 1][1];
}

/* ───────── Legs + ground (2-bone legs, no ankle) ───────── */
// Boot geometry below the hip joint (texels): knee 5.6 down, sole 6.4 below the knee,
// heel 2.1 back and toe 2.9 forward of the shin line. The foot angle is hip + knee.
const THIGH = 5.6, SOLE = 6.4, HEEL = -2.1, TOE = 2.9, HIP_H = 12;

// Smooth min (no corner where the two values cross). Under-estimates by at most k*ln2.
function smin(a, b, k = 0.06) { return Math.min(a, b) - k * Math.log1p(Math.exp(-Math.abs(a - b) / k)); }

// Lowest boot point below the hip for hip angle h and knee angle k (Rx: y' = y cos - z sin)
function footLow(h, k) {
  const a = h + k, c = Math.cos(a), s = Math.sin(a);
  return -THIGH * Math.cos(h) + smin(-SOLE * c - HEEL * s, -SOLE * c - TOE * s);
}

// Pose both legs (hip forward = negative, knee bend = positive) and set the root height
// so the lower foot just touches the floor, plus `lift` (flying). Splay = legs a bit apart.
function legs(Q, hR, kR, hL, kL, lift = 0, splay = 0.03) {
  rot(Q, 'leg_right', hR, 0, -splay); rot(Q, 'knee_right', kR, 0, 0);
  rot(Q, 'leg_left', hL, 0, splay);   rot(Q, 'knee_left', kL, 0, 0);
  pos(Q, 'root', 0, -HIP_H - smin(footLow(hR, kR), footLow(hL, kL)) + 0.06 + lift, 0);
}

// Hands on hips (photo 1): elbows out, fists on the belt. w = 0..1 blend weight.
function handsOnHips(Q, side, w, breath = 0) {
  const sx = side === 'right' ? -1 : 1;
  rot(Q, `arm_${side}`, -0.1 * w, 0, sx * (0.34 + 0.02 * breath) * w);
  rot(Q, `forearm_${side}`, (-0.45 - 0.04 * breath) * w, 0, -sx * 0.08 * w);
}

// Hands behind the back (photo 3): upper arm swung back + elbow out, forearm folds
// inward (forearm z) so both fists rest behind the lower back. w = 0..1.
// Values from a clip search (hover t = 4): fists end ~3 texels beside the spine, ~6 behind the torso center
const HB = { ax: 0.8, az: 0.2, fx: 0.2, fz: 1.2 };
// w = upper arm swing, fold = forearm fold (fold lags behind w so the fists never pass through the back)
function handsBehind(Q, side, w, fold = w) {
  const sx = side === 'right' ? -1 : 1;
  rot(Q, `arm_${side}`, HB.ax * w, 0, sx * HB.az * w);
  rot(Q, `forearm_${side}`, HB.fx * fold, 0, -sx * HB.fz * fold);
}

// Cape pose helper: `fn(u, i)` gives the bend for the old 4-texel segment scale at cape
// position u (0 = shoulders, 4 = hem, continuous, so waves travel smoothly down 10 bones).
// Each 2-texel bone gets half of it, so the total curve matches the old 5-bone values.
function capePose(Q, fn) {
  for (let i = 0; i < CAPE_N; i++) {
    const u = i / 2;                         // joint of bone i sits 2i texels down = old segment index i/2
    const [x, z] = fn(u, i);
    rot(Q, CAPE[i], 0.5 * x, 0, 0.5 * z);
  }
}

/* ───────── Keyframe Sequences ───────── */
// Idle: surveys the city (8.0 s = 960 frames). Head yaw keys; eyes lead the head by 0.3 s.
const ID_LOOK  = [[0, 0], [1.2, 0], [2.0, 0.36], [3.2, 0.36], [4.4, -0.32], [5.4, -0.32], [6.4, 0], [8.0, 0]];
const ID_SMIRK = [[0, 0], [2.2, 0], [2.6, 1], [3.2, 1], [3.6, 0], [6.6, 0], [7.0, 0.7], [7.6, 0.7], [8.0, 0]];

// Hover (random from Idle, 9.0 s): lifts off, hands behind the back, looks around, lands
const HV_AIR    = [[0, 0], [1.4, 1], [7.4, 1], [9.0, 0]];
const HV_HANDS  = [[0, 0], [0.4, 0], [1.4, 1], [7.4, 1], [8.4, 0], [9.0, 0]];   // arms swing back
const HV_FOLD   = [[0, 0], [0.9, 0], [1.9, 1], [6.6, 1], [7.6, 0], [9.0, 0]];   // forearms fold behind (later in, earlier out)
const HV_LOOK   = [[0, 0], [1.8, 0], [2.6, -0.34], [3.8, -0.34], [4.8, 0.3], [5.8, 0.3], [6.6, 0], [9.0, 0]];
const HV_DOWN   = [[0, 0], [6.4, 0], [6.8, 1], [7.4, 1], [7.8, 0], [9.0, 0]];

// Idle 2: PR smile + peace sign (photo 2), then the smile drops into a cold stare (8.0 s)
const ID2_RAISE = [[0, 0], [0.7, 0], [1.5, 1], [3.6, 1], [4.3, 0], [8.0, 0]];
const ID2_WAVE  = [[0, 0], [1.5, 0], [1.8, 1], [3.3, 1], [3.6, 0], [8.0, 0]];
const ID2_GRIN  = [[0, 0], [0.9, 0], [1.3, 1], [4.5, 1], [4.75, 0], [8.0, 0]];
const ID2_STARE = [[0, 0], [4.5, 0], [4.75, 1], [6.6, 1], [7.3, 0], [8.0, 0]];
const ID2_RED   = [[0, 0], [5.2, 0], [5.6, 1], [6.0, 1], [6.5, 0], [8.0, 0]];
const ID2_SMIRK = [[0, 0], [6.8, 0], [7.2, 0.8], [7.7, 0.8], [8.0, 0]];

// Attack Sequence Curves: Anticipation -> Devastating Super-Punch -> Snap Recoil -> Recovery
const ATK_COIL   = [[0, 0], [0.35, 1], [0.55, 1], [0.65, 0], [1.8, 0]];
const ATK_PUNCH  = [[0, 0], [0.5, 0], [0.65, 1], [0.95, 1], [1.4, 0.2], [1.8, 0]];
const ATK_TWIST  = [[0, 0], [0.45, -0.35], [0.65, 0.65], [1.0, 0.5], [1.5, 0.1], [1.8, 0]];

// Special: Levitation Hover + Heat-Vision Laser Eyes Curves
const SPEC_LIFT  = [[0, 0], [0.6, 6], [1.2, 9.5], [2.8, 9.5], [3.4, 4], [4.0, 0]];
const SPEC_LASER = [[0, 0], [0.7, 0], [1.0, 1], [2.6, 1], [2.9, 0], [4.0, 0]];
const SPEC_SWEEP = [[0, 0], [1.0, 0], [1.5, -0.25], [2.1, 0.25], [2.6, 0], [4.0, 0]];

// Throw (3.25 s): right hand grabs the neck, lifts, dips, flings the victim straight up,
// watches them fly, then looks down at where they land. Victim stands a bit to his right.
const TH_AX   = [[0, 0], [0.32, -1.6], [0.42, -1.55], [0.95, -1.9], [1.25, -1.3], [1.5, -3.0], [1.75, -2.95], [2.35, -2.6], [2.85, -0.1], [3.25, 0]];
// (no inward yaw: it swings the shoulder end of the upper arm into the chest and head.
//  Arm z: out = negative while the arm points down/forward, but POSITIVE once it is overhead)
const TH_AY   = [[0, 0], [0.32, 0.06], [1.25, 0.06], [1.5, 0], [3.25, 0]];
const TH_AZ   = [[0, 0], [0.32, -0.16], [1.25, -0.16], [1.5, 0.3], [2.35, 0.3], [2.85, 0], [3.25, 0]];
const TH_FX   = [[0, 0], [0.32, -0.15], [0.42, -0.35], [0.95, -0.3], [1.25, -0.7], [1.5, -0.05], [2.35, -0.1], [2.85, -0.15], [3.25, 0]];
const TH_STEP = [[0, 0], [0.32, 1], [2.45, 1], [2.95, 0], [3.25, 0]];
const TH_CROUCH = [[0, 0], [0.95, 0], [1.25, 1], [1.45, 0], [3.25, 0]];
const TH_EXT  = [[0, 0], [1.35, 0], [1.52, 1], [1.8, 0], [3.25, 0]];       // explosive push onto the toes
const TH_UP   = [[0, 0], [1.4, 0], [1.6, 1], [2.3, 1], [2.6, 0], [3.25, 0]]; // looks up after them
const TH_DOWN = [[0, 0], [2.4, 0], [2.7, 1], [3.0, 1], [3.25, 0]];          // ...and down where they land
const TH_HOLD = [[0, 0], [0.32, 0], [0.45, 1], [1.2, 1], [1.4, 0], [3.25, 0]]; // choking hold

// Clap (2.4 s), from the "I AM GOD" clip: arms fly out wide, chin up, screaming with red eyes,
// then both hands slam in from the sides onto the victim's head (palms at its sides), clenched-teeth sneer.
const CL_AX   = [[0, 0], [0.55, -1.75], [0.85, -1.8], [0.98, -1.9], [1.3, -1.9], [1.85, -0.35], [2.4, 0]];
// impact: hand ends at x = +-5.8 (palms just outside an 8-wide head), y ~26.5 (head height), z ~12
const CL_AY   = [[0, 0], [0.55, -1.35], [0.85, -1.45], [0.98, 0.02], [1.3, 0.02], [1.85, 0], [2.4, 0]];  // right arm; left = -
const CL_FX   = [[0, 0], [0.55, -0.45], [0.85, -0.5], [0.98, -0.1], [1.3, -0.1], [1.85, -0.3], [2.4, 0]];
const CL_FZ   = [[0, 0], [0.55, 0], [0.85, -0.1], [0.98, 0.1], [1.3, 0.1], [1.85, 0], [2.4, 0]];        // inward palm curl
const CL_WIDE = [[0, 0], [0.35, 0], [0.55, 1], [0.85, 1], [0.98, 0], [2.4, 0]];
const CL_SLAM = [[0, 0], [0.85, 0], [0.98, 1], [1.3, 1], [1.85, 0], [2.4, 0]];
const CL_HIT  = [[0, 0], [0.96, 0], [1.0, 1], [1.14, 0], [2.4, 0]];         // impact jolt
const CL_STANCE = [[0, 0], [0.5, 1], [1.9, 1], [2.4, 0]];

// Throat laser (3.4 s): grabs the throat and lifts, holds 1 s while his eyes heat up,
// then burns the victim's face point-blank, drops them and smirks.
const TL_AX   = [[0, 0], [0.3, -1.6], [0.45, -1.85], [2.75, -1.9], [3.0, -1.1], [3.4, 0]];
const TL_AY   = [[0, 0], [0.3, 0.06], [2.75, 0.06], [3.0, 0.03], [3.4, 0]];
const TL_FX   = [[0, 0], [0.3, -0.15], [0.45, -0.35], [2.75, -0.35], [3.0, -0.05], [3.4, 0]];
const TL_STEP = [[0, 0], [0.3, 1], [2.9, 1], [3.4, 0]];
const TL_HOLD = [[0, 0], [0.45, 1], [2.75, 1], [3.1, 0], [3.4, 0]];
const TL_GLOW = [[0, 0], [0.5, 0], [1.4, 1], [2.75, 1], [3.2, 0], [3.4, 0]];  // heats up during the 1 s hold
const TL_LASER = [[0, 0], [1.45, 0], [1.6, 1], [2.6, 1], [2.75, 0], [3.4, 0]];
const TL_SMIRK = [[0, 0], [0.5, 0], [0.8, 1], [1.3, 1], [1.5, 0], [2.95, 0], [3.2, 0.8], [3.4, 0]];

/* ───────── Animations: Idle, Idle 2, Walk + asked-for Attack, Special (+ hidden Hover) ───────── */
const ANIMS = {
  // 1 · Idle: "surveying his city". Hands on hips, chest out, slow weight shift from leg to
  // leg (the free knee softens), looks left and right with smug pauses and a one-sided smirk.
  // Random extras live in step(): faint red eye glow, and now and then a short Hover.
  idle: {
    T: 8.0, yawLimit: 0.35,
    fn(t, Q) {
      const T = 8.0;
      const br = Math.sin(TAU * 2 * t / T), inB = (br + 1) * 0.5; // 2 breaths per loop
      const shift = Math.sin(TAU * t / T);                         // weight: + = on his right leg
      const look = kf(t, ID_LOOK);

      // Legs: weight-bearing leg straight, the other knee softens; hips follow the knee a bit
      const kR = 0.03 + 0.09 * (1 - shift) * 0.5, kL = 0.03 + 0.09 * (1 + shift) * 0.5;
      legs(Q, -0.4 * kR, kR, -0.4 * kL, kL);
      pos(Q, 'body', -0.3 * shift, 0, 0);

      // Chest out, breathing, shoulders follow the gaze a little
      rot(Q, 'chest', -0.04 - 0.03 * inB, 0.25 * look, 0.015 * shift);
      pos(Q, 'chest', 0, 0.08 * inB, 0);

      handsOnHips(Q, 'right', 1, inB);
      handsOnHips(Q, 'left', 1, inB);

      // Head: turns with the look keys, chin lifts while holding a look (look^2: smooth)
      rot(Q, 'head', -0.03 - 0.25 * look * look - 0.015 * br, 0.75 * look, -0.02 * shift);
      pos(Q, 'head', 0, 1.5 * look * look, 0);

      // Face: eyes lead the head, smirk at the holds, brows lift when he looks around
      Q.f.glance = clamp(1.2 * kf(t + 0.3, ID_LOOK), -0.45, 0.45);
      Q.f.smirk = kf(t, ID_SMIRK);
      Q.f.browUp = 0.08 + 1.4 * look * look + 0.04 * br;
      Q.f.frown = 0.12 + 0.2 * Math.max(0, -look) ** 2 / 0.1;    // judging frown to his left side
      Q.f.squint = 0.15;
      Q.f.wide = 0.15;

      capePose(Q, (u) => {
        const phase = TAU * 2 * t / T - u * 0.45;
        return [0.015 + u * 0.008 + (0.012 + u * 0.006) * Math.sin(phase), 0.003 * Math.cos(phase)];
      });
    }
  },

  // Hidden · Hover: plays by itself from Idle now and then (step()). Floats ~3.5 texels up,
  // legs hang loose (toes down), hands behind the back, cape blown back over the hands,
  // slow look around and a look down at the ground, then lands back into Idle.
  hover: {
    T: 9.0, yawLimit: 0.12, // arms swung back raise the pauldron fronts: keep head turns small
    fn(t, Q) {
      const T = 9.0;
      const air = kf(t, HV_AIR), hands = kf(t, HV_HANDS), fold = kf(t, HV_FOLD);
      const look = kf(t, HV_LOOK), down = kf(t, HV_DOWN);
      const bob = 0.35 * Math.sin(TAU * 3 * t / T) * air;        // 3 slow bobs per hover
      const br = Math.sin(TAU * 2 * t / T), inB = (br + 1) * 0.5;

      // Legs: idle stance on the ground -> hanging (knees soft, right one a bit more)
      legs(Q, -0.03 - 0.05 * air, 0.06 + 0.2 * air, -0.03 - 0.08 * air, 0.06 + 0.12 * air, 3.5 * air + bob);

      rot(Q, 'chest', -0.05 - 0.02 * inB, 0.2 * look, 0);
      pos(Q, 'chest', 0, 0.06 * inB, 0);

      // Arms: hands on hips -> hands behind the back
      handsOnHips(Q, 'right', 1 - hands, inB);
      handsOnHips(Q, 'left', 1 - hands, inB);
      handsBehind(Q, 'right', hands, fold);
      handsBehind(Q, 'left', hands, fold);

      // Head: look around, then a cold look down at the ground below him
      rot(Q, 'head', -0.03 - 0.2 * look * look + 0.1 * down, 0.6 * look, 0);
      pos(Q, 'head', 0, 1.5 * look * look + 0.3 * hands, 0); // chin up / neck long: head clears the raised pauldrons

      Q.f.glance = clamp(1.2 * kf(t + 0.3, HV_LOOK), -0.45, 0.45);
      Q.f.smirk = 0.6 * air;
      Q.f.browUp = 0.1 + 0.15 * air - 0.1 * down;
      Q.f.frown = 0.12 + 0.25 * down;
      Q.f.squint = 0.15 + 0.15 * down;
      Q.f.wide = 0.15;

      // Cape: pushed back over the hands behind the back (seg 1-3 out, 4-5 hang down again)
      // straight back-slant (~0.85 rad) over the fists for 12 texels, then the tail hangs down
      // (per bone, before capePose halves it: the bends sit on the old 4-texel joints)
      const flare = [1.64, 0, 0, 0, 0, 0, -1.0, 0, -0.6, 0];
      capePose(Q, (u, i) => {
        const phase = TAU * 3 * t / T - u * 0.5;
        return [0.015 + u * 0.008 + flare[i] * hands + (0.01 + u * 0.006) * Math.sin(phase), 0.003 * Math.cos(phase)];
      });
    }
  },

  // 2 · Idle 2: the PR showman. Big fake grin + peace sign (photo 2) and a little wave, then
  // the arm comes down and the smile drops off his face in a quarter second: cold dead stare,
  // a flicker of red in the eyes, until the smug smirk comes back. Left hand stays on the hip.
  idle2: {
    T: 8.0, yawLimit: 0.3,
    fn(t, Q) {
      const T = 8.0;
      const raise = kf(t, ID2_RAISE), grin = kf(t, ID2_GRIN), stare = kf(t, ID2_STARE);
      const wave = kf(t, ID2_WAVE) * Math.sin(TAU * 2.25 * t);  // ~2 waves per second while held
      const br = Math.sin(TAU * 2 * t / T), inB = (br + 1) * 0.5;

      // Legs: weight on his left leg, right knee relaxed (contrapposto)
      legs(Q, -0.05, 0.13, 0.0, 0.03);
      pos(Q, 'body', 0.25, 0, 0);

      // Chest lifts on the breath and leans back a little while posing
      rot(Q, 'chest', -0.02 * inB - 0.04 * raise, 0.05 * raise, -0.02);
      pos(Q, 'chest', 0, 0.06 * inB, 0);

      // Right arm: hand-on-hip -> raised out to the side, twisted -1.5 so the elbow bend
      // points up (peace sign); the wave rocks the raised arm side to side
      rot(Q, 'arm_right', -0.1 - 0.1 * raise, -1.5 * raise, -0.34 - 0.91 * raise + 0.07 * wave);
      rot(Q, 'forearm_right', -0.45 - 0.87 * raise, 0, 0.08 * (1 - raise));
      handsOnHips(Q, 'left', 1, inB);

      // Head: tilts toward the raised hand; in the stare it drops a little and goes still
      rot(Q, 'head', -0.03 - 0.01 * inB + 0.06 * stare, 0.05 * Math.sin(TAU * t / T) * (1 - stare), 0.06 * raise + 0.03 * stare);
      pos(Q, 'head', 0, 0.3 * raise, 0);

      // Face: grin (teeth) + raised brows -> flat brows, open eyes locked on you, red flicker
      Q.f.grin = grin;
      Q.f.wide = 0.15 + 0.5 * grin;
      Q.f.browUp = 0.08 + 0.45 * grin - 0.05 * stare;
      Q.f.frown = 0.12 * (1 - grin) + 0.15 * stare;
      Q.f.squint = 0.12 + 0.18 * grin - 0.12 * stare;
      Q.f.glance = 0.15 * Math.sin(TAU * 2 * t / T) * (1 - stare);
      Q.f.glow = 0.3 * kf(t, ID2_RED);
      Q.f.smirk = kf(t, ID2_SMIRK);

      // Cape: slower, wider sway than Idle (own rhythm)
      capePose(Q, (u) => {
        const phase = TAU * 2 * t / T - u * 0.6;
        return [0.02 + u * 0.01 + (0.01 + u * 0.007) * Math.sin(phase), 0.004 * Math.sin(TAU * t / T - u * 0.3)];
      });
    }
  },

  // 3 · Walk: upright, chest-out, unhurried stride (1 gait cycle = 2 steps in 1.25 s = 150 frames).
  // 2-bone legs: heel strikes with toes up, toe-off with heel up (foot angle = hip + knee),
  // knee folds ~1 rad in the swing for foot clearance; elbows bend more on the forward swing.
  walk: {
    T: 1.25, yawLimit: 0.20,
    fn(t, Q) {
      const p = t / 1.25, c = Math.cos(TAU * p), s = Math.sin(TAU * p);
      // Per leg: hip from -0.42 (forward, heel strike) to +0.42 (back, toe-off); knee: small
      // flex in stance, big fold in the swing (sin^2 halves: smooth at every joint of the cycle)
      const leg = (ph) => {
        const sp = Math.sin(TAU * ph), sw = sp * sp, swing = sp < 0 ? 1 : 0;
        return [-0.42 * Math.cos(TAU * ph) - 0.1 * sw * swing, (swing ? 1.0 : 0.07) * sw];
      };
      const [hR, kR] = leg(p), [hL, kL] = leg(p + 0.5);
      legs(Q, hR, kR, hL, kL);

      // Weight moves over the stance foot; pelvis turns with the forward leg, chest counters
      pos(Q, 'body', -0.3 * s, 0, 0);
      rot(Q, 'body', 0, 0.06 * c, 0);
      rot(Q, 'chest', 0.03, -0.1 * c, 0.02 * s);

      // Arms opposite to the legs, elbows bend more on the forward swing, slight flare
      rot(Q, 'arm_right', 0.38 * c, 0, -0.12);
      rot(Q, 'arm_left', -0.38 * c, 0, 0.12);
      rot(Q, 'forearm_right', -0.22 - 0.3 * (1 - c) * 0.5, 0, 0);
      rot(Q, 'forearm_left', -0.22 - 0.3 * (1 + c) * 0.5, 0, 0);

      // Head steady (counter-turns the chest), small nod on every step
      rot(Q, 'head', -0.03 + 0.015 * Math.cos(2 * TAU * p), 0.04 * c, 0);

      capePose(Q, (u) => {
        const wave = Math.sin(2 * TAU * p - u * 0.48);
        return [0.04 + u * 0.014 + (0.012 + u * 0.008) * wave, (0.004 + u * 0.002) * Math.sin(TAU * p - u * 0.3)];
      });

      Q.f.smirk = 0.35;
      Q.f.browUp = 0.08;
      Q.f.frown = 0.15;
      Q.f.squint = 0.15;
      Q.f.wide = 0.15;
      Q.f.glance = 0.12 * Math.sin(TAU * p * 0.5) ** 2 - 0.06;
    }
  },

  // 4 · Attack: wind-up and explosive right super-punch, lunging onto the left leg
  attack: {
    T: 1.8, yawLimit: 0.05, follow: 0,
    fn(t, Q) {
      const coil = kf(t, ATK_COIL);
      const punch = kf(t, ATK_PUNCH);
      const twist = kf(t, ATK_TWIST);

      // Lunge: left leg steps forward (flat foot: hip + knee = 0), right leg pushes back on its toes
      legs(Q, 0.12 * coil + 0.3 * punch, 0.1 * coil + 0.15 * punch, -0.1 * coil - 0.38 * punch, 0.25 * coil + 0.38 * punch);

      // Torso wind-up and explosive forward torque
      rot(Q, 'chest', 0.08 * punch, twist * 0.6, 0);
      pos(Q, 'chest', 0, -0.15 * punch, 0.3 * punch);

      // Right Arm: chambers back during wind-up with outward shoulder clearance, snaps forward
      rot(Q, 'arm_right', 0.22 * coil - 1.45 * punch, 0, -0.32 - 0.15 * coil);
      rot(Q, 'forearm_right', -0.85 * coil - 0.25 * punch, 0, 0);

      // Left Arm: guards chest and counter-balances
      rot(Q, 'arm_left', -0.2 * coil + 0.4 * punch, 0, 0.35);
      rot(Q, 'forearm_left', -0.6 * coil - 0.3 * punch, 0, 0);

      // Head locked onto the victim
      rot(Q, 'head', 0.04 * punch, 0, 0);

      // Cape: the bend runs down the cape a little after the punch (lag grows with u)
      capePose(Q, (u) => {
        const lagged = kf(clamp(t - 0.04 * u, 0, 1.8), ATK_PUNCH);
        return [0.035 + u * 0.015 + (0.04 + u * 0.02) * lagged, -(0.008 + u * 0.004) * twist];
      });

      // Violent grimace: bared teeth, low brows, eyes flash red on the hit
      Q.f.frown = 0.95 * (coil + punch);
      Q.f.squint = 0.65 * punch;
      Q.f.open = 0.3 * punch;
      Q.f.grin = 0.6 * punch;
      Q.f.glow = Math.min(1, 0.45 * coil + 0.75 * punch); // eyes flare red in the wind-up, peak on the hit
    }
  },

  // 5 · Special: Levitation Hover & Heat-Vision Laser Eyes!
  special: {
    T: 4.0, yawLimit: 0.2, follow: 0.15,
    fn(t, Q) {
      const lift = kf(t, SPEC_LIFT);
      const laser = kf(t, SPEC_LASER);
      const sweep = kf(t, SPEC_SWEEP);

      // Hover legs: relaxed hanging pose once airborne (toes point down with the shin)
      const hoverAir = clamp((lift - 0.4) / 3.0, 0, 1);
      legs(Q, 0.06 * hoverAir, 0.06 + 0.2 * hoverAir, 0.06 * hoverAir, 0.06 + 0.14 * hoverAir, lift);

      // Supreme power stance: arms drawn back and clenched at sides
      rot(Q, 'arm_right', 0.18 * Math.min(1, lift / 4), 0, -0.32);
      rot(Q, 'forearm_right', -0.25 * Math.min(1, lift / 4), 0, 0.1);
      rot(Q, 'arm_left', 0.18 * Math.min(1, lift / 4), 0, 0.32);
      rot(Q, 'forearm_left', -0.25 * Math.min(1, lift / 4), 0, -0.1);

      // Head tilted downward with terrifying laser gaze sweeping across the target
      rot(Q, 'head', 0.12 * laser, sweep, 0);

      // Cape flutters in the updraft: a wave that runs from the shoulders to the hem
      const air = Math.min(1, lift / 4);
      capePose(Q, (u) => {
        const segFlap = Math.sin(TAU * 4.5 * (t - 0.035 * u));
        return [(0.05 + u * 0.025) * air + (0.02 + u * 0.012) * segFlap * (0.3 + 0.7 * air), (0.006 + u * 0.004) * Math.sin(TAU * 2.25 * (t - 0.05 * u))];
      });

      Q.f.laser = laser;
      Q.f.glow = clamp(lift / 6, 0, 1) ** 2;   // eyes heat up while he rises (full red just before the beams)
      Q.f.frown = 1.0 * laser;
      Q.f.open = 0.25 * laser;
      Q.f.grin = 0.5 * laser;
      Q.f.squint = 0.45 * laser;
    }
  },

  // 6 · Throw: grabs the victim by the neck (right hand), lifts them off the ground, dips
  // and flings them straight up, watches them fly and looks down where they will land.
  throw: {
    T: 3.25, yawLimit: 0.05, follow: 0,
    fn(t, Q) {
      const step = kf(t, TH_STEP), cr = kf(t, TH_CROUCH), ext = kf(t, TH_EXT);
      const up = kf(t, TH_UP), down = kf(t, TH_DOWN), hold = kf(t, TH_HOLD);

      // Legs: left foot steps in, dips with the throw, pushes up onto the toes on the release
      legs(Q, 0.22 * step - 0.2 * cr + 0.1 * ext, 0.08 * step + 0.55 * cr, -0.3 * step - 0.2 * cr, 0.3 * step + 0.55 * cr, 0.7 * ext);

      // Chest: right shoulder turns forward to reach, leans into the dip, arches back on the throw
      // (chest twist 0.42 while holding = the right hand sits on his center line, where the victim is)
      rot(Q, 'chest', 0.04 * step + 0.14 * cr - 0.16 * up, 0.42 * hold + 0.12 * cr, 0);
      pos(Q, 'chest', 0, 0, 0.2 * cr);

      // Right arm: reach -> neck -> lift -> dip -> fling overhead (out to the side of the head)
      rot(Q, 'arm_right', kf(t, TH_AX), kf(t, TH_AY), kf(t, TH_AZ));
      rot(Q, 'forearm_right', kf(t, TH_FX), 0, 0);
      // Left arm: fist low and back for balance, swings forward on the throw
      rot(Q, 'arm_left', 0.25 * step - 0.35 * ext, 0, 0.2 * step);
      rot(Q, 'forearm_left', -0.5 * step, 0, -0.05);

      // Head: locked on the victim, then up after them (neck long: clears the shoulders), then down
      // (head counter-turns to keep his eyes on the victim and looks up into their face; the
      //  neck lift keeps the turned head's front corner above the raised upper arm)
      rot(Q, 'head', -0.22 * hold - 0.38 * up + 0.14 * down, -0.3 * hold, -0.03 * hold);
      pos(Q, 'head', 0, 1.0 * hold + 1.3 * up + 0.7 * down + 0.8 * cr, 0);

      // Cape: whips back with the throw, the bend runs down a moment later
      capePose(Q, (u) => {
        const lagE = kf(clamp(t - 0.05 * u, 0, 3.25), TH_EXT);
        return [0.03 + u * 0.012 + (0.05 + u * 0.03) * lagE + 0.04 * step, 0.004 * Math.sin(TAU * (t / 3.25) * 2 - u * 0.4)];
      });

      // Face: snarl while choking, faint red eyes; shout on the release; smirk watching; cruel grin down
      Q.f.frown = 0.7 * hold + 0.5 * ext + 0.2 * down;
      Q.f.grin = 0.5 * hold + 0.7 * down;
      Q.f.open = 0.8 * ext;
      Q.f.wide = 0.15 + 0.3 * ext;
      Q.f.smirk = 0.8 * up;
      Q.f.browUp = 0.4 * up;
      Q.f.squint = 0.3 * hold + 0.2 * down;
      Q.f.glow = 0.35 * hold + 0.5 * ext;
      Q.f.glance = 0;
    }
  },

  // 7 · Clap: "I AM GOD" (reference clip): arms fly out wide, chin up, screaming with red eyes,
  // then both palms slam in from the sides onto the victim's head. Clenched-teeth sneer after.
  clap: {
    T: 2.4, yawLimit: 0.05, follow: 0,
    fn(t, Q) {
      const wide = kf(t, CL_WIDE), slam = kf(t, CL_SLAM), hit = kf(t, CL_HIT), st = kf(t, CL_STANCE);
      const ax = kf(t, CL_AX), ay = kf(t, CL_AY), fx = kf(t, CL_FX), fz = kf(t, CL_FZ);

      // Legs: wide planted stance, knees sink into the slam
      // (lift 0.13*st: the wide splay tips the boots' outer edges below the floor solve)
      legs(Q, -0.05 * st - 0.15 * slam, 0.06 * st + 0.3 * slam, -0.05 * st - 0.15 * slam, 0.06 * st + 0.3 * slam, 0.13 * st, 0.03 + 0.09 * st);

      // Chest: puffs up and arches back while screaming, snaps forward into the slam (+ jolt)
      rot(Q, 'chest', -0.12 * wide + 0.12 * slam + 0.04 * hit, 0, 0);
      pos(Q, 'chest', 0, 0.25 * wide, 0.3 * slam - 0.15 * hit);

      // Arms (mirrored): out wide and a bit up -> swing in level -> palms on the head's sides
      // (early in the rise the arms also roll out a little: keeps the shoulder ends off the chest)
      const out = 0.2 * kf(t, [[0, 0], [0.25, 1], [0.55, 0], [2.4, 0]]);
      rot(Q, 'arm_right', ax, ay, -out);
      rot(Q, 'arm_left', ax, -ay, out);
      rot(Q, 'forearm_right', fx, 0, fz);
      rot(Q, 'forearm_left', fx, 0, -fz);

      // Head: chin up for the scream, down onto the victim for the slam, shakes on the impact
      rot(Q, 'head', -0.2 * wide + 0.06 * slam + 0.05 * hit, 0, 0.03 * Math.sin(TAU * 9 * t) * hit);
      pos(Q, 'head', 0, 1.0 * wide + 0.3 * slam, 0);

      // Cape: billows back with the spreading arms, snaps forward on the slam
      capePose(Q, (u) => {
        const w = kf(clamp(t - 0.05 * u, 0, 2.4), CL_WIDE), s = kf(clamp(t - 0.05 * u, 0, 2.4), CL_SLAM);
        return [0.03 + u * 0.012 + (0.04 + u * 0.02) * w - (0.01 + 0.006 * u) * s, 0.004 * Math.sin(TAU * t / 2.4 - u * 0.4)];
      });

      // Face: scream (wide open mouth, red eyes) -> clenched teeth, deep frown, then a smirk
      Q.f.open = 1.0 * wide;
      Q.f.wide = 0.2 + 0.5 * wide;
      Q.f.browUp = 0.5 * wide;
      Q.f.frown = 0.5 * wide + 1.0 * slam;
      Q.f.grin = 0.9 * slam;
      Q.f.squint = 0.1 + 0.4 * slam;
      Q.f.glow = 0.75 * wide + 0.4 * slam;
      Q.f.smirk = 0.6 * kf(t, [[0, 0], [1.6, 0], [2.0, 1], [2.4, 0]]);
    }
  },

  // 8 · Throat laser: grabs the throat, lifts the victim, holds them 1 s while the eyes heat
  // up, then burns their face point-blank (short beams), drops them and smirks.
  throatLaser: {
    T: 3.4, yawLimit: 0.05, follow: 0, beam: 6, // beams end at the victim's face (probe: head center height, ~6 texels)
    fn(t, Q) {
      const step = kf(t, TL_STEP), hold = kf(t, TL_HOLD), laser = kf(t, TL_LASER);

      // Legs: steps in with the left foot, sets the right back to carry the weight
      legs(Q, 0.2 * step, 0.08 * step, -0.3 * step, 0.25 * step);

      // Chest: right shoulder twists forward (hand on his center line = victim in front of him),
      // leans back a little under the lifted weight
      rot(Q, 'chest', -0.05 * hold, 0.42 * hold, 0);

      rot(Q, 'arm_right', kf(t, TL_AX), kf(t, TL_AY), -0.12 * step);
      rot(Q, 'forearm_right', kf(t, TL_FX), 0, 0);
      // Left arm: clenched fist by his side
      rot(Q, 'arm_left', 0.08 * step, 0, 0.16 * step);
      rot(Q, 'forearm_left', -0.35 * hold, 0, -0.06 * hold);

      // Head: looks up into the victim's face, curious tilt, buzzes while the beams fire
      const buzz = 0.012 * Math.sin(TAU * 15 * t) * laser;
      rot(Q, 'head', -0.3 * hold + buzz, -0.42 * hold, 0.08 * hold * (1 - laser) + buzz); // counter-turn: face to face
      pos(Q, 'head', 0, 1.0 * hold, 0);

      capePose(Q, (u) => {
        const phase = TAU * 2 * t / 3.4 - u * 0.5;
        return [0.03 + u * 0.012 + (0.01 + u * 0.006) * Math.sin(phase) + 0.03 * step, 0.003 * Math.cos(phase)];
      });

      // Face: smug smirk while he squeezes, brows sink as the eyes heat up, snarl while burning
      Q.f.smirk = kf(t, TL_SMIRK);
      Q.f.glow = kf(t, TL_GLOW);
      Q.f.laser = laser;
      Q.f.frown = 0.35 * hold + 0.4 * kf(t, TL_GLOW) + 0.3 * laser;
      Q.f.grin = 0.85 * laser;
      Q.f.open = 0.15 * laser;
      Q.f.squint = 0.2 * hold + 0.3 * laser;
      Q.f.browUp = 0.15 * hold * (1 - laser);
      Q.f.glance = 0;
    }
  }
};

const ANIM_ORDER = ['idle', 'idle2', 'walk', 'attack', 'special', 'throw', 'clap', 'throatLaser'];   // dock buttons / keys 1-8
const ALL_ANIMS = [...ANIM_ORDER, 'hover'];                           // + hidden ones (tests, export)
