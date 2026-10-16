/* Homelander viewer, part 'dummy': the test dummy that shows what each attack does to a victim.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Victims (shared with the mod) ─────────
   Where a victim stands for each attack (texels, Homelander's model space: +z = his front, feet at y 0).
   Grab attacks hold the victim by the neck at HAND_PT (a point in forearm_right's space, front of the fist):
   the victim's neck sits there and its body hangs 2 texels further away (half a body depth).
   THROW_VY = how fast the Throw flings the victim straight up (blocks per second; the mod uses it too). */
const HAND_PT = [0, -5.0, 0.6];
const DUMMY_SCALE = 0.9375;                 // player size: the 32-texel player model is drawn 0.9375 big
const DUMMY_NECK = 24 * DUMMY_SCALE;        // 22.5 texels: top of the body box
const THROW_VY = 24, GRAVITY_B = 32;        // blocks/s, blocks/s^2 (Minecraft: 0.08 blocks/tick^2)
const GRAB_LIFT = 0.15;                     // seconds: a grabbed victim is lifted to the hand over this time
const VICTIM = {
  attack: { spot: [0, 0, 15] },             // punched: knocked back
  special: { spot: [0, 0, 56] },            // 3.5 blocks: the beams come down onto its chest
  throw: { grab: true },                    // spot = under the hand at events.grab (computed below)
  clap: { spot: [0, 0, 12] },               // head between his palms (they reach z 11: the temples)
  throatLaser: { grab: true }
};

/* ───────── Dummy model ─────────
   A plain player-shaped mannequin (8x8x8 head, 8x12x4 body, 4x12x4 arms and legs), burlap colors.
   Bones: root (feet) > body (hips) > head (neck), arms (shoulders); legs (hips). Faces Homelander. */
const dummy = (() => {
  const T = TEXEL * DUMMY_SCALE;
  const mats = [0xc9b28a, 0x9b8460, 0x2b2620].map((c) => new THREE.MeshStandardMaterial({ color: c, roughness: 0.9, metalness: 0, transparent: true, opacity: 0 }));
  const [cloth, dark, eye] = mats;
  const group = (parent, x, y, z) => { const g = new THREE.Group(); g.position.set(x * T, y * T, z * T); g.rotation.order = 'ZYX'; parent.add(g); return g; };
  const cube = (parent, m, w, h, d, x, y, z) => {
    const mesh = new THREE.Mesh(new THREE.BoxGeometry(w * T, h * T, d * T), m);
    mesh.position.set(x * T, y * T, z * T); mesh.castShadow = true; mesh.receiveShadow = true; parent.add(mesh); return mesh;
  };
  const root = new THREE.Group(); root.rotation.order = 'YXZ'; root.visible = false; scene.add(root);
  const body = group(root, 0, 12, 0);
  cube(body, cloth, 8, 12, 4, 0, 6, 0);
  cube(body, dark, 8.2, 1, 4.2, 0, 1, 0);                       // belt line
  const head = group(body, 0, 12, 0);
  cube(head, cloth, 8, 8, 8, 0, 4, 0);
  cube(head, eye, 1, 1, 0.2, -2, 4, 4.05); cube(head, eye, 1, 1, 0.2, 2, 4, 4.05);   // button eyes
  cube(head, dark, 4, 0.6, 0.2, 0, 2, 4.05);                                         // stitched mouth
  const armR = group(body, -6, 12, 0), armL = group(body, 6, 12, 0);
  cube(armR, dark, 4, 12, 4, 0, -6, 0); cube(armL, dark, 4, 12, 4, 0, -6, 0);
  const legR = group(root, -2, 12, 0), legL = group(root, 2, 12, 0);
  cube(legR, dark, 4, 12, 4, 0, -6, 0); cube(legL, dark, 4, 12, 4, 0, -6, 0);
  // Hit puffs (clap crush, punch): a few small cubes, flying on fixed paths (no state)
  const puffMat = new THREE.MeshBasicMaterial({ color: 0x8e1b21, transparent: true, opacity: 0, depthWrite: false });
  const puffGeo = new THREE.BoxGeometry(1.2 * TEXEL, 1.2 * TEXEL, 1.2 * TEXEL);
  const puffs = Array.from({ length: 14 }, (_, i) => {
    const m = new THREE.Mesh(puffGeo, puffMat); m.visible = false; scene.add(m);
    const a = i * 2.39996, up = 0.3 + (i % 5) * 0.17;       // golden-angle spread
    return { m, v: [Math.cos(a) * 40, 30 + up * 60, Math.sin(a) * 40] };
  });
  return { root, body, head, armR, armL, legR, legL, mats, puffs, puffMat, opacity: 0, shown: null, on: true };
})();

/* ───────── Victim pose per attack (a function of the attack's time: no state) ─────────
   Fills DS: feet position (texels), lean back (rad), spin, arms up 0..1, leg kick, head squash 0..1, burn 0..1. */
const DS = { x: 0, y: 0, z: 0, lean: 0, spin: 0, arms: 0, cower: 0, kick: 0, squash: 0, burn: 0, faceBurn: 0, puff: -1, puffAt: [0, 0, 0] };
const smooth01 = (x) => { x = clamp(x, 0, 1); return x * x * (3 - 2 * x); };
const vHand = new THREE.Vector3();
const grabSpot = {};   // anim -> feet spot under the hand at events.grab

// Feet position of a victim held by the neck at world point h (texels): body hangs 2 texels further away.
function hangFrom(h, out) {
  const len = Math.hypot(h.x, h.z) || 1;
  out[0] = h.x + h.x / len * 2; out[1] = h.y - DUMMY_NECK; out[2] = h.z + h.z / len * 2;
  return out;
}
function handWorld(out) {
  vHand.set(HAND_PT[0] * TEXEL, HAND_PT[1] * TEXEL, HAND_PT[2] * TEXEL);
  bones.forearm_right.localToWorld(vHand);
  return out.set(vHand.x / TEXEL, vHand.y / TEXEL, vHand.z / TEXEL);
}
// Grab attacks: run each once (a throwaway sim, at load) to read the hand at the grab and at the release.
// spot = where the victim stands before the grab (under the hand); release = where it hangs when let go.
// (The page's own sim poses the bones again before the first frame is drawn.)
(function findGrabSpots() {
  const h = new THREE.Vector3();
  const handAt = (name, t) => {
    const S = makeSim(1); S.events = false; S.anim = name; S.follow.on = false;
    for (let k = 0; k < Math.round(t * 120); k++) step(S, 1 / 120);
    mobRoot.updateMatrixWorld(true);
    return hangFrom(handWorld(h), [0, 0, 0]);
  };
  for (const name of Object.keys(VICTIM)) {
    if (!VICTIM[name].grab) continue;
    const s = handAt(name, ANIMS[name].events.grab), r = handAt(name, ANIMS[name].events.release);
    VICTIM[name].spot = [s[0], 0, s[2]];
    VICTIM[name].release = [r[0], Math.max(0, r[1]), r[2]];
  }
})();
const hTmp = new THREE.Vector3(), aTmp = [0, 0, 0];   // reused every frame

function victimPose(name, t, S) {
  Object.assign(DS, { lean: 0, spin: 0, arms: 0, cower: 0, kick: 0, squash: 0, burn: 0, faceBurn: 0, puff: -1 });
  const sp = VICTIM[name].spot, ev = ANIMS[name].events || {};
  [DS.x, DS.y, DS.z] = sp;
  if (name === 'attack') {
    const u = t - ev.hit;
    if (u > 0) {   // knocked back ~2.5 blocks with a short hop, lands on its back
      DS.z = sp[2] + 40 * (1 - Math.exp(-u * 5));
      DS.y = u < 0.5 ? 48 * u - 96 * u * u : 0;
      DS.lean = -Math.PI / 2 * smooth01(u / 0.45);
      DS.puff = u; DS.puffAt[0] = 0; DS.puffAt[1] = 24; DS.puffAt[2] = sp[2] - 2;
    }
  } else if (name === 'special') {
    DS.burn = kf(t, [[0, 0], [ev.laserOn, 0], [ev.laserOn + 0.4, 1], [ev.laserOff, 1], [ev.laserOff + 0.5, 0.6], [ANIMS.special.T, 0.6]]);
    DS.lean = -Math.PI / 2 * smooth01((t - ev.laserOff) / 0.5);
    DS.arms = 0.5 * smooth01((t - ev.laserOn) / 0.3);
  } else if (name === 'throw' || name === 'throatLaser') {
    if (t >= ev.grab && t < ev.release) {        // held by the neck: hangs from his hand, kicks, grabs the wrist
      hangFrom(handWorld(hTmp), aTmp); [DS.x, DS.y, DS.z] = aTmp;
      DS.y = Math.max(0, DS.y) * smooth01((t - ev.grab) / GRAB_LIFT); // yanked off the ground (the hand closes a bit above the neck)
      DS.arms = smooth01((t - ev.grab) / 0.2);
      DS.kick = smooth01((t - ev.grab) / 0.3) * Math.sin(TAU * 2.6 * t);
      if (name === 'throatLaser') DS.faceBurn = kf(t, [[0, 0], [ev.burnOn, 0], [ev.burnOn + 0.15, 1], [ev.burnOff, 1], [ev.burnOff + 0.3, 0.5], [ANIMS[name].T, 0.5]]);
    } else if (t >= ev.release) {
      const r = VICTIM[name].release, u = t - ev.release;
      [DS.x, DS.y, DS.z] = r;
      if (name === 'throw') {                     // flung straight up, spins, falls back down onto its back
        const land = (THROW_VY + Math.sqrt(THROW_VY * THROW_VY + 2 * GRAVITY_B * r[1] / 16)) / GRAVITY_B; // seconds in the air
        DS.y = Math.max(0, r[1] + 16 * (THROW_VY * u - 0.5 * GRAVITY_B * u * u));
        DS.spin = 2 * TAU * smooth01(u / land);    // exactly 2 rolls in the air: lands upright-ish, no snap
        DS.lean = -Math.PI / 2 * smooth01((u - land) / 0.15);
        DS.arms = 1 - smooth01(u / 0.3);
      } else {                                    // dropped: falls, crumples
        DS.y = Math.max(0, r[1] - 16 * 0.5 * GRAVITY_B * u * u);
        DS.lean = -Math.PI / 2 * smooth01((u - 0.2) / 0.35);
        DS.faceBurn = 0.5;
      }
    }
  } else if (name === 'clap') {
    DS.cower = kf(t, [[0, 0], [0.5, 0], [0.9, 1], [ev.grab - 0.3, 1], [ev.grab, 0], [4, 0]]); // leans away, arms out, while he screams
    DS.lean = -0.1 * DS.cower;
    const u = t - ev.hit;
    if (u > 0) {
      DS.squash = smooth01(u / 0.06);
      DS.lean = -Math.PI / 2 * smooth01((u - 0.12) / 0.4);
      DS.puff = u; DS.puffAt[0] = 0; DS.puffAt[1] = 26; DS.puffAt[2] = sp[2];
    }
  }
  return DS;
}

/* ───────── Per frame ─────────
   Shown only during attacks: fades in at the start and out at the end of each loop (so the reset
   at the loop wrap is never seen), and fades out before it moves to another attack's spot. */
function updateDummy(S, dt) {
  const D = dummy, want = D.on && VICTIM[S.anim] ? S.anim : null;
  const A = want && ANIMS[want], t = want ? S.t % A.T : 0;
  const loopFade = want ? smooth01(t / 0.25) * smooth01((A.T - t) / 0.25) : 0;
  const target = want && want === D.shown ? loopFade : 0;
  D.opacity += (target - D.opacity) * filt(dt, 12);
  if (D.opacity < 0.02 && want !== D.shown) D.shown = want;
  D.root.visible = D.opacity > 0.01 && !!D.shown;
  for (const m of D.mats) m.opacity = D.opacity;
  for (const p of D.puffs) p.m.visible = false;
  if (!D.root.visible) return;
  mobRoot.updateMatrixWorld(true);              // the hand's world matrix of this frame's pose
  const v = victimPose(D.shown, S.t % ANIMS[D.shown].T, S);
  D.root.position.set(v.x * TEXEL, v.y * TEXEL, v.z * TEXEL);
  D.root.rotation.set(v.lean, Math.atan2(-v.x, -v.z), v.spin);   // faces him, leans back, rolls
  D.armR.rotation.set(-2.7 * v.arms, 0, 0.25 * v.arms - 0.5 * v.cower); D.armL.rotation.set(-2.7 * v.arms, 0, -0.25 * v.arms + 0.5 * v.cower);
  D.legR.rotation.x = 0.35 * v.kick; D.legL.rotation.x = -0.35 * v.kick;
  D.head.scale.set(1 - 0.75 * v.squash, 1 + 0.15 * v.squash, 1 - 0.2 * v.squash);
  D.head.rotation.x = -0.35 * v.arms;           // head tilts back while held up by the neck
  const burn = Math.max(v.burn, 0);
  for (const m of D.mats) m.emissive.setRGB(0.9 * burn, 0.25 * burn, 0.05 * burn);
  if (v.faceBurn > 0) D.mats[0].emissive.setRGB(0.9 * Math.max(burn, v.faceBurn * 0.6), 0.25 * v.faceBurn * 0.6, 0.05);
  // Puffs: burst from the hit point, fall, fade
  if (v.puff >= 0 && v.puff < 0.7) {
    D.puffMat.opacity = D.opacity * (1 - v.puff / 0.7);
    for (const p of D.puffs) {
      p.m.visible = true;
      p.m.position.set((v.puffAt[0] + p.v[0] * v.puff) * TEXEL, (v.puffAt[1] + p.v[1] * v.puff - 256 * v.puff * v.puff) * TEXEL, (v.puffAt[2] + p.v[2] * v.puff) * TEXEL);
    }
  }
}

// Special: the beams stop where they meet the dummy (its box at the spot), like the mod's ray hit.
const rayO = new THREE.Vector3(), rayD = new THREE.Vector3();
function dummyBeamLen() {
  const D = dummy;
  if (!D.root.visible || D.shown !== 'special') return 0;
  bones.head.updateWorldMatrix(true, false);    // called inside step(): this frame's head
  rayO.set(2 * TEXEL, 4.5 * TEXEL, 4.1 * TEXEL); bones.head.localToWorld(rayO);
  rayD.set(0, 0, 1).transformDirection(bones.head.matrixWorld);
  const sp = VICTIM.special.spot, lo = [sp[0] - 4, 0, sp[2] - 2].map((v) => v * TEXEL), hi = [sp[0] + 4, 30, sp[2] + 2].map((v) => v * TEXEL);
  let t0 = 0, t1 = 1e9;
  const o = [rayO.x, rayO.y, rayO.z], d = [rayD.x, rayD.y, rayD.z];
  for (let k = 0; k < 3; k++) {   // slab test
    if (Math.abs(d[k]) < 1e-9) { if (o[k] < lo[k] || o[k] > hi[k]) return 0; continue; }
    let a = (lo[k] - o[k]) / d[k], b = (hi[k] - o[k]) / d[k];
    if (a > b) [a, b] = [b, a];
    t0 = Math.max(t0, a); t1 = Math.min(t1, b);
    if (t0 > t1) return 0;
  }
  return t0 / TEXEL;
}
