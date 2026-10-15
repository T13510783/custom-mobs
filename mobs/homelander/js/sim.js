/* Homelander viewer, part 'sim': Springs, simulation state, step().
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Dynamic Physics Springs (Fluent Cape Ripple) ───────── */
// One spring per cape bone (u = old segment index 0..4.75). Angle values per bone are 1/CAPE_PER of
// the old per-4-texel values (4 bones now share each old bend; g = 2 / CAPE_PER = the old 10-bone
// numbers scaled). Lower bones are softer and also chase their parent's spring angle (`chain`):
// body motion ripples down the cape with a delay.
const SPRINGS = CAPE.map((bone, i) => {
  const u = i / CAPE_PER, g = 2 / CAPE_PER;
  return {
    bone, chain: i === 0 ? 0 : 0.55,
    k: 80 - 9 * u, d: 10 - 0.7 * u, accel: (0.125 + 0.025 * u) * g,
    x: [0.0, (0.075 + 0.012 * u) * g], z: [-0.0075 * g, 0.0075 * g],
    bx: (0.0075 + 0.0012 * u) * g, breeze: (0.004 + 0.0009 * u) * g, f: 1.1, lag: 0.09 * u // breeze wave travels down
  };
});

// Seeded random (mulberry32): same seed = same blinks / glow / hover timing (tests, shots)
function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6D2B79F5) >>> 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function makeSim(seed = 12345) {
  const rand = rng(seed);
  return {
    anim: 'idle', t: 0, time: 0, blend: 1, rand,
    snap: makePose(), cur: makePose(), out: makePose(),
    face: zeroFace(),
    yawLimit: 0.45,
    blink: { wait: 1.4, t: -1 },
    // Random Idle extras: faint red eye glow + a short Hover (first one after ~8 s)
    events: true,
    ev: { glowWait: 3 + rand() * 4, glowT: -1, glowDur: 1.5, glowAmp: 0.5, hoverWait: 8 + rand() * 3 },
    follow: { on: true, w: 1, yaw: 0, pitch: 0, eye: 0, mx: 0, my: 0, has: false, inject: null }, // inject {yaw, pitch}: look angles instead of the mouse (tests, mod)
    // Voice line playing: level = loudness 0..1, shape = brightness 0..1 (from the line's curve, see voice.js)
    talk: { active: false, level: 0, shape: 0.5 }, talkW: 0,
    onEvent: null,   // (name, anim) when an animation passes one of its `events` times (voice line, hit, grab ...)
    beamLen: null,   // () => beam length in texels to whatever the beam hits (test dummy); else LASER_LEN
    springs: SPRINGS.map((s) => ({ ...s, ax: 0, az: 0, vx: 0, vz: 0, prev: new THREE.Vector3(), vel: new THREE.Vector3(), velPrev: new THREE.Vector3(), acc: new THREE.Vector3(), init: false }))
  };
}

function switchAnim(S, name) {
  if (name === S.anim) return;
  copyPose(S.out, S.snap);
  S.anim = name; S.t = 0; S.blend = 0;
}

const vA = new THREE.Vector3(), vB = new THREE.Vector3(), vC = new THREE.Vector3();
function filt(dt, rate) { return 1 - Math.exp(-dt * rate); }

function trackMotion(st, world, dt) {
  if (!st.init) { st.prev.copy(world); st.vel.set(0, 0, 0); st.velPrev.set(0, 0, 0); st.acc.set(0, 0, 0); st.init = true; return; }
  vC.copy(world).sub(st.prev).divideScalar(dt);
  st.prev.copy(world);
  st.vel.lerp(vC, filt(dt, 25));
  vC.copy(st.vel).sub(st.velPrev).divideScalar(dt);
  st.velPrev.copy(st.vel);
  st.acc.lerp(vC, filt(dt, 18));
}

/* ───────── Simulation Step ───────── */
function step(S, dt) {
  S.time += dt;
  S.t += dt;

  // Random Idle extras (only Idle / Hover): start a red glow pulse; switch into Hover now
  // and then; Hover plays once, then crossfades back into Idle.
  if (S.events) {
    const ev = S.ev;
    if (S.anim === 'idle' || S.anim === 'hover') {
      ev.glowWait -= dt;
      if (ev.glowWait <= 0 && ev.glowT < 0) {
        ev.glowT = 0; ev.glowDur = 1.2 + S.rand() * 1.2; ev.glowAmp = 0.35 + S.rand() * 0.3;
        ev.glowWait = 6 + S.rand() * 9;
      }
    }
    if (S.anim === 'idle' && S.blend >= 1) {
      ev.hoverWait -= dt;
      if (ev.hoverWait <= 0) { switchAnim(S, 'hover'); ev.hoverWait = 16 + S.rand() * 14; }
    }
    if (S.anim === 'hover' && S.t >= ANIMS.hover.T) switchAnim(S, 'idle');
  }
  let evGlow = 0;
  if (S.ev.glowT >= 0) {
    S.ev.glowT += dt;
    const u = clamp(S.ev.glowT / S.ev.glowDur, 0, 1);
    evGlow = S.ev.glowAmp * Math.sin(Math.PI * u) ** 2; // smooth in and out
    if (u >= 1) S.ev.glowT = -1;
  }

  const A = ANIMS[S.anim] || ANIMS.idle;
  const loopT = S.t % A.T;

  // Animation events (voice line, grab, hit ...): tell the page when this step passed one.
  // t0 < 0 right after a loop wrap: then the end of the last loop counts too.
  if (S.onEvent && A.events) {
    const t0 = loopT - dt;
    for (const k in A.events) {
      const v = A.events[k];
      if ((v > t0 && v <= loopT) || (t0 < 0 && v > t0 + A.T)) S.onEvent(k, S.anim);
    }
  }

  clearPose(S.cur);
  A.fn(loopT, S.cur);

  // Smooth crossfade between animation transitions
  if (S.blend < 1) {
    S.blend = Math.min(1, S.blend + dt * 4.2);
    const w = S.blend * S.blend * (3 - 2 * S.blend);
    for (let i = 0; i < NB * 3; i++) {
      S.out.r[i] = S.snap.r[i] + (S.cur.r[i] - S.snap.r[i]) * w;
      S.out.p[i] = S.snap.p[i] + (S.cur.p[i] - S.snap.p[i]) * w;
    }
    for (const k of FACE_KEYS) S.out.f[k] = S.snap.f[k] + (S.cur.f[k] - S.snap.f[k]) * w;
    S.yawLimit = S.cur.yawLimit || 0.45;
  } else {
    copyPose(S.cur, S.out);
    S.yawLimit = A.yawLimit || 0.45;
  }

  // Talking (voice line) overrides the animation's mouth: open = loudness, wide = brightness
  // ("ee" wide, "o" narrow); a scream also frowns and squints a little. Fades in/out (talkW).
  const TK = S.talk;
  S.talkW += ((TK.active ? 1 : 0) - S.talkW) * filt(dt, 10);
  const tw = S.talkW, of = S.out.f;
  if (tw > 0.001) {
    of.open += (TK.level - of.open) * tw;
    of.wide += ((TK.shape - 0.5) * 1.4 - of.wide) * tw;
    of.frown = Math.max(of.frown, 0.45 * tw);
    of.squint = Math.max(of.squint, 0.2 * tw);
  }

  // Smooth facial expression easing
  for (const k of FACE_KEYS) {
    S.face[k] += (S.out.f[k] - S.face[k]) * filt(dt, 16);
  }

  // Natural eye blinking
  S.blink.wait -= dt;
  if (S.blink.wait <= 0 && S.blink.t < 0) {
    S.blink.t = 0;
    S.blink.wait = 2.5 + S.rand() * 3.5;
  }
  let blinkVal = 0;
  if (S.blink.t >= 0) {
    S.blink.t += dt;
    blinkVal = Math.sin(clamp(S.blink.t / 0.18, 0, 1) * Math.PI);
    if (S.blink.t >= 0.18) {
      S.blink.t = -1;
      if (S.blink.wait > 1 && S.rand() < 0.2) S.blink.wait = 0.12; // now and then a quick double blink
    }
  }

  // Mouse tracking head & eye follow
  const fTgt = (S.follow.on && S.follow.has) ? (A.follow !== undefined ? A.follow : 1) : 0;
  S.follow.w += (fTgt - S.follow.w) * filt(dt, 8);

  // Look target: the mouse in the viewer; `inject` = look angles given directly (mod, 1:1 test). + yaw = his left, + pitch = down.
  const inj = S.follow.inject;
  const targetYaw = clamp(inj ? inj.yaw : S.follow.mx * 0.55, -S.yawLimit, S.yawLimit);
  const targetPitch = clamp(inj ? inj.pitch : -S.follow.my * 0.15, -0.08, 0.08);
  S.follow.yaw += (targetYaw - S.follow.yaw) * filt(dt, 9);
  S.follow.pitch += (targetPitch - S.follow.pitch) * filt(dt, 9);

  // Apply Kinematic Pose to Bones
  for (let i = 0; i < NB; i++) {
    const g = boneList[i];
    const r0 = g.userData.restRot, p0 = g.userData.rest;
    const j = i * 3;
    g.rotation.set(r0.x + S.out.r[j], r0.y + S.out.r[j + 1], r0.z + S.out.r[j + 2]);
    g.position.set(p0.x + S.out.p[j] * TEXEL, p0.y + S.out.p[j + 1] * TEXEL, p0.z + S.out.p[j + 2] * TEXEL);
  }

  // Apply Head Follow
  if (bones.head) {
    bones.head.rotation.y += S.follow.yaw * S.follow.w;
    bones.head.rotation.x += S.follow.pitch * S.follow.w;
    bones.head.rotation.x -= 0.05 * S.talk.level * S.talkW; // chin lifts on loud syllables
  }

  // Pupils eye glance tracking
  const pGlance = clamp(S.face.glance + S.follow.yaw * 0.35 * S.follow.w, -0.45, 0.45);
  if (bones.pupil_right && bones.pupil_left) {
    bones.pupil_right.position.x = (-2 + pGlance) * TEXEL;
    bones.pupil_left.position.x = (2 + pGlance) * TEXEL;
  }

  // Sliding Eyelids
  const closeAmt = clamp(blinkVal + S.face.squint * 0.5, 0, 1);
  if (bones.lid_right && bones.lid_left) {
    bones.lid_right.scale.y = closeAmt;
    bones.lid_left.scale.y = closeAmt;
    bones.lid_right.position.z = (closeAmt > 0.05 ? 4.06 : 3.98) * TEXEL;
    bones.lid_left.position.z = (closeAmt > 0.05 ? 4.06 : 3.98) * TEXEL;
  }

  // Eyebrows
  if (bones.brow_right && bones.brow_left) {
    const browY = (5.6 + S.face.browUp * 0.4 - S.face.frown * 0.3) * TEXEL;
    bones.brow_right.position.y = browY;
    bones.brow_left.position.y = browY;
    bones.brow_right.rotation.z = -0.06 - S.face.frown * 0.25;
    bones.brow_left.rotation.z = 0.06 + S.face.frown * 0.25;
  }

  // Mouth: open / wider for the grin, smirk tilts it up on his left side and shifts it over;
  // teeth grow down from the lip line and step out of the head only while grinning
  if (bones.mouth) {
    bones.mouth.scale.y = 1 + S.face.open * 1.5;
    bones.mouth.scale.x = 1 + S.face.wide * 0.4 + S.face.grin * 0.3;
    bones.mouth.rotation.z = 0.14 * S.face.smirk;
    bones.mouth.position.x = 0.25 * S.face.smirk * TEXEL;
    bones.teeth.scale.y = Math.max(0.001, S.face.grin);
    bones.teeth.position.z = (-0.05 + 0.045 * clamp(S.face.grin * 10, 0, 1)) * TEXEL;
  }

  // Red eye glow: anim channel + random Idle pulse, dimmed while the lids are closed
  // (the lids slide over the glow planes, so a blink hides the red by itself)
  const glow = clamp(Math.max(S.face.glow + evGlow, S.face.laser), 0, 1);
  glowMat.opacity = Math.min(1, glow * 1.3);                 // red eye white
  glowCoreMat.opacity = clamp(glow * 1.6 - 0.45, 0, 1);      // hot core only when it really burns
  glowHaloMat.opacity = glow * 0.35;                         // faint bloom (more reads as a pink box)
  for (const e of eyeGlow) e.core.position.x = (e.x + pGlance) * TEXEL; // core sits on the pupil

  // Heat-vision beams: thickness flickers, length = the animation's `beam` (texels) or LASER_LEN
  const laserActive = S.face.laser;
  laserMat.opacity = laserActive;
  laserCoreMat.opacity = laserActive * 0.95;
  const beamW = laserActive > 0.01 ? (0.75 + 0.25 * Math.sin(S.time * 45)) * Math.min(1, laserActive * 2) : 0.0001;
  // (the mod does the same: A.beam, else the distance to the first thing the beam hits)
  const beamLen = Math.max(0.0001, A.beam || (S.beamLen && S.beamLen()) || LASER_LEN);
  for (const l of lasers) { l.beam.scale.set(beamW, beamW, beamLen); l.core.scale.set(beamW, beamW, beamLen); } // geometry runs along +z

  // Cape Physics Springs
  let parentSp = null;
  for (const sp of S.springs) {
    const b = bones[sp.bone];
    if (!b) continue;
    b.getWorldPosition(vA);
    trackMotion(sp, vA, dt);

    // target = own inertia + breeze (a wave running down the cape) + follow the bone above
    const breeze = Math.sin(TAU * sp.f * (S.time - sp.lag)) * sp.breeze;
    const chainX = parentSp ? sp.chain * (parentSp.ax - parentSp.bx) : 0;
    const chainZ = parentSp ? sp.chain * parentSp.az : 0;
    parentSp = sp;
    const targetX = clamp(-sp.acc.z * sp.accel * 0.03 + sp.bx + breeze + chainX, sp.x[0], sp.x[1]);
    const targetZ = clamp(sp.acc.x * sp.accel * 0.03 + chainZ, sp.z[0], sp.z[1]);

    sp.vx += (targetX - sp.ax) * sp.k * dt;
    sp.vx *= Math.max(0, 1 - sp.d * dt);
    sp.ax += sp.vx * dt;

    sp.vz += (targetZ - sp.az) * sp.k * dt;
    sp.vz *= Math.max(0, 1 - sp.d * dt);
    sp.az += sp.vz * dt;

    b.rotation.x += sp.ax;
    b.rotation.z += sp.az;
  }
}
