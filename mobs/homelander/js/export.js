/* Homelander viewer, part 'export': Blockbench .bbmodel exporter.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Blockbench .bbmodel Exporter ───────── */
function uuid() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
  });
}

const bbVec = (p) => [-p[0], p[1], p[2]];
const bbRot = (rx, ry, rz) => [-rx * 180 / Math.PI, -ry * 180 / Math.PI, rz * 180 / Math.PI];
const r4 = (v) => Math.round(v * 10000) / 10000;

function bakeAnimation(name) {
  const A = ANIMS[name];
  const fps = 24, frames = Math.round(A.T * fps);
  const S = makeSim(1);
  S.anim = name; S.follow.on = false;
  const tracks = {};
  boneList.forEach((g) => { tracks[g.name] = { rot: [], pos: [], scl: [] }; });

  for (let f = 0; f <= frames; f++) {
    S.t = (f / fps) % A.T;
    clearPose(S.cur);
    A.fn(S.t, S.cur);
    for (let i = 0; i < NB; i++) {
      const g = boneList[i], b = g.name;
      const j = i * 3;
      tracks[b].rot.push(bbRot(S.cur.r[j], S.cur.r[j + 1], S.cur.r[j + 2]));
      tracks[b].pos.push(bbVec([S.cur.p[j], S.cur.p[j + 1], S.cur.p[j + 2]]));
      tracks[b].scl.push([1, 1, 1]);
    }
  }

  const animators = {};
  const isRest = (list, def) => list.every((p) => Math.abs(p[0] - def) < 1e-4 && Math.abs(p[1] - def) < 1e-4 && Math.abs(p[2] - def) < 1e-4);
  boneList.forEach((g) => {
    const b = g.name, tr = tracks[b], keyframes = [];
    const add = (channel, list) => list.forEach((v, f) => keyframes.push({
      channel, data_points: [{ x: v[0], y: v[1], z: v[2] }], uuid: uuid(), time: r4(f / fps), color: -1, interpolation: 'catmullrom'
    }));
    if (!isRest(tr.rot, 0)) add('rotation', tr.rot);
    if (!isRest(tr.pos, 0)) add('position', tr.pos);
    if (keyframes.length) animators[g.userData.uuid] = { name: g.name, type: 'bone', keyframes };
  });

  return {
    uuid: uuid(), name: `animation.homelander.${name}`, loop: 'loop', override: false,
    length: A.T, snapping: 24, selected: name === 'idle', anim_time_update: '', blend_weight: '',
    start_delay: '', loop_delay: '', animators
  };
}

function buildBBModel() {
  const abs = new Map();
  boneList.forEach((g) => {
    g.userData.uuid = uuid();
    const p = g.parent.isGroup && abs.has(g.parent) ? abs.get(g.parent) : [0, 0, 0];
    const r = g.userData.rest;
    abs.set(g, [p[0] + r.x / TEXEL, p[1] + r.y / TEXEL, p[2] + r.z / TEXEL]);
  });

  const elements = [];
  const groupOf = {};
  for (const c of cubeList) {
    const id = uuid();
    const bp = abs.get(bones[c.bone]);
    const ctr = bbVec([bp[0] + c.center[0], bp[1] + c.center[1], bp[2] + c.center[2]]);
    const [w, h, d] = c.size;
    const R = (f) => { const r = c.rects[f]; return [r.x, r.y, r.x + r.w, r.y + r.h]; };
    const Rf = (f) => { const r = c.rects[f]; return [r.x + r.w, r.y + r.h, r.x, r.y]; };
    const rotated = c.rot.some((v) => v !== 0);

    elements.push({
      name: c.name, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: [r4(ctr[0] - w / 2), r4(ctr[1] - h / 2), r4(ctr[2] - d / 2)],
      to: [r4(ctr[0] + w / 2), r4(ctr[1] + h / 2), r4(ctr[2] + d / 2)],
      autouv: 0, color: 0, inflate: c.inflate,
      origin: rotated ? ctr : bbVec(bp),
      ...(rotated ? { rotation: bbRot(c.rot[0], c.rot[1], c.rot[2]) } : {}),
      faces: {
        north: { uv: R(4), texture: 0 }, south: { uv: R(5), texture: 0 },
        east: { uv: R(1), texture: 0 }, west: { uv: R(0), texture: 0 },
        up: { uv: Rf(2), texture: 0 }, down: { uv: Rf(3), texture: 0 }
      },
      type: 'cube', uuid: id
    });
    (groupOf[c.bone] = groupOf[c.bone] || []).push(id);
  }

  const outlinerNode = (g) => ({
    name: g.name, origin: bbVec(abs.get(g)), rotation: bbRot(g.userData.restRot.x, g.userData.restRot.y, g.userData.restRot.z),
    color: 0, uuid: g.userData.uuid, export: true, mirror_uv: false, isOpen: true, locked: false, visibility: true, autouv: 0,
    children: [...(groupOf[g.name] || []), ...g.children.filter((ch) => ch.isGroup).map(outlinerNode)]
  });

  const animations = ALL_ANIMS.map(bakeAnimation);

  return {
    meta: { format_version: '4.5', model_format: 'free', box_uv: false },
    name: 'homelander', model_identifier: 'homelander',
    visible_box: [2, 2.5, 1], variable_placeholders: '', variable_placeholder_buttons: [],
    resolution: { width: ATLAS_W, height: ATLAS_H },
    elements, outliner: [outlinerNode(root)],
    textures: [{
      path: '', name: 'homelander.png', folder: 'entity', namespace: '', id: '0', particle: false,
      render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', frame_order: '',
      frame_interpolate: false, visible: true, internal: true, saved: false, uuid: uuid(),
      source: atlas.toDataURL('image/png')
    }],
    animations
  };
}

/* ───────── Mod export (#modexport) ─────────
   Everything the NeoForge mod needs to show Homelander 1:1, read by tools/export_mod_assets.ps1:
   bones (rest pose), cubes as finished quads (exact UVs of this atlas), springs, the eye glow / laser
   geometry, victim spots, the voice line's lip-sync curve, textures per color variant and every
   animation's raw pose (before crossfade, follow, face filter, springs) baked at 120 fps.
   Units: texels, radians. Axes like here: +x = his left, +y = up, +z = his front. UV v goes down (Minecraft). */
const BAKE_FPS = 120;
function modExport() {
  const r6 = (n) => Math.round(n * 1e6) / 1e6 || 0;
  restPose();
  mobRoot.updateMatrixWorld(true);
  const bonesOut = boneList.map((g) => ({
    name: g.name, parent: g.parent.isGroup && B[g.parent.name] !== undefined ? B[g.parent.name] : -1,
    pos: [g.userData.rest.x, g.userData.rest.y, g.userData.rest.z].map((v) => r6(v / TEXEL)),
    rot: [g.userData.restRot.x, g.userData.restRot.y, g.userData.restRot.z].map(r6)
  }));
  const v3 = new THREE.Vector3(), n3 = new THREE.Vector3(), nm = new THREE.Matrix3();
  const cubesOut = cubeList.map((c) => {
    const m = c.mesh;
    m.updateMatrix();
    nm.getNormalMatrix(m.matrix);
    const geo = m.geometry, Pa = geo.attributes.position, U = geo.attributes.uv, N = geo.attributes.normal, I = geo.index.array;
    const quads = [];
    for (let f = 0; f < 6; f++) {
      const ids = [I[f * 6], I[f * 6 + 1], I[f * 6 + 4], I[f * 6 + 2]]; // BoxGeometry quad a, b, c, d (counter-clockwise from outside)
      const vs = ids.map((i) => v3.fromBufferAttribute(Pa, i).applyMatrix4(m.matrix).divideScalar(TEXEL).toArray().map(r6));
      const e1 = new THREE.Vector3(...vs[1]).sub(new THREE.Vector3(...vs[0])), e2 = new THREE.Vector3(...vs[3]).sub(new THREE.Vector3(...vs[0]));
      if (e1.cross(e2).length() < 1e-6) continue; // the zero-width sides of a face plane
      n3.fromBufferAttribute(N, ids[0]).applyMatrix3(nm).normalize();
      quads.push({ v: vs, uv: ids.map((i) => [r6(U.getX(i)), r6(1 - U.getY(i))]), n: n3.toArray().map(r6) });
    }
    return { name: c.name, bone: B[c.bone], twoSided: m.material.alphaTest > 0, quads };
  });
  // Animations: the raw anim pose sampled at 120 fps, T included. Channels that never move are left out.
  const anims = {}, chunks = [];
  let offset = 0;
  for (const name of ALL_ANIMS) {
    const A = ANIMS[name], frames = Math.round(A.T * BAKE_FPS) + 1;
    // The mod samples frame f at f / BAKE_FPS: a length that is not a whole number of frames loses its last bit.
    if (Math.abs(A.T * BAKE_FPS - Math.round(A.T * BAKE_FPS)) > 1e-6) throw new Error(`${name}: T ${A.T} s is not a whole number of 1/${BAKE_FPS} s frames`);
    const Q = makePose(), cols = new Map();
    const put = (key, f, v) => { if (!cols.has(key)) cols.set(key, new Float64Array(frames)); cols.get(key)[f] = v; };
    for (let f = 0; f < frames; f++) {
      const t = Math.min(f / BAKE_FPS, A.T);
      clearPose(Q);
      A.fn(t, Q);
      for (let b = 0; b < NB; b++) for (let k = 0; k < 3; k++) {
        put(`r:${b}:${k}`, f, Q.r[b * 3 + k]);
        put(`p:${b}:${k}`, f, Q.p[b * 3 + k]);
      }
      for (const k of FACE_KEYS) put(`f:${k}`, f, Q.f[k]);
    }
    const channels = [];
    for (const [key, arr] of cols) {
      let lo = Infinity, hi = -Infinity;
      for (const v of arr) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
      if (!key.startsWith('f:') && hi - lo <= 1e-7 && Math.abs(lo) <= 1e-7) continue;
      // 16-bit steps between lo and hi, stored as differences from the previous frame (small numbers pack well).
      const q = new Int16Array(frames), span = hi - lo;
      let prev = 0;
      for (let f = 0; f < frames; f++) {
        const level = span > 0 ? Math.round((arr[f] - lo) / span * 65535) - 32768 : -32768;
        q[f] = level - prev; prev = level;
      }
      chunks.push(q);
      channels.push({ key, lo, hi, at: offset }); // exact lo/hi: the 16-bit levels map back without drift
      offset += frames;
    }
    anims[name] = { T: A.T, frames, yawLimit: A.yawLimit || 0.45, follow: A.follow !== undefined ? A.follow : 1, beam: A.beam || 0, events: A.events || {}, channels };
  }
  const all = new Int16Array(offset);
  let at = 0;
  for (const q of chunks) { all.set(q, at); at += q.length; }
  const bytes = new Uint8Array(all.length * 2), dv = new DataView(bytes.buffer);
  all.forEach((v, i) => dv.setInt16(i * 2, v, false)); // big-endian (Java DataInputStream)
  let bin = '';
  for (let i = 0; i < bytes.length; i += 0x8000) bin += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  const textures = {};
  const keep = variantIndex;
  VARIANTS.forEach((v, i) => { setVariant(i); textures[v.id] = atlas.toDataURL('image/png'); });
  setVariant(keep);
  const col = (m) => '#' + m.color.getHexString();
  return {
    rig: {
      format: 1, fps: BAKE_FPS, atlas: [ATLAS_W, ATLAS_H], variants: VARIANTS.map((v) => v.id), animOrder: ALL_ANIMS,
      faceKeys: FACE_KEYS, bones: bonesOut, cubes: cubesOut,
      springs: SPRINGS.map((s) => ({ ...s, bone: B[s.bone] })),
      // Eye glow planes and heat-vision beams (drawn by the mod in the head bone, like eyeGlow / lasers here)
      fx: {
        eyesX: [-2, 2], eyeY: 4.5, glowZ: [4.05, 4.052, 4.1], glowSize: [[2, 1], [1, 1], [3, 2]],
        glowColors: [col(glowMat), col(glowCoreMat), col(glowHaloMat)], beamZ: 4.1, beamR: [0.7, 0.28], beamSides: [8, 6],
        beamColors: [col(laserMat), col(laserCoreMat)], laserLen: LASER_LEN
      },
      victim: { hand: HAND_PT, neck: DUMMY_NECK, scale: DUMMY_SCALE, throwVy: THROW_VY, grabLift: GRAB_LIFT, spots: VICTIM },
      voice: VOICE_LINES.map((v) => ({ id: v.id, env: v.env, zcr: v.zcr })),
      anims
    },
    bin: btoa(bin), textures
  };
}

/* Reference recording for the mod's Java test (#modtrace): a fixed scene (seed 7, 1/120 s steps) with every
   animation, look angles, the Clap's voice line (mouth from its curve) and the random Idle extras (glow, Hover).
   Every 1/30 s: each bone's rotation, position (texels) and scale, plus the eye/laser effect values. */
function modTrace() {
  const S = makeSim(7), dt = 1 / 120, out = [], fx = [];
  const plan = [[0, 'idle'], [3, 'idle2'], [6, 'walk'], [8, 'attack'], [10, 'special'], [14.2, 'throw'], [17.6, 'clap'], [21.4, 'throatLaser'], [25, 'idle']];
  const end = 36; // seconds recorded (the Java test reads it); the last Idle runs into a Hover
  S.follow.on = true; S.follow.has = true; S.follow.inject = { yaw: 0, pitch: 0 };
  const line = VOICE_LINES[0];
  let talkStart = -1;
  S.onEvent = (name) => { if (name === 'voice') talkStart = S.time; };
  let p = 0, next = 0;
  for (let i = 0; i <= end * 120; i++) {
    const t = i * dt;
    while (p < plan.length && t >= plan[p][0] - 1e-9) { switchAnim(S, plan[p][1]); p++; }
    S.follow.inject.yaw = 0.6 * Math.sin(0.7 * t); S.follow.inject.pitch = 0.1 * Math.sin(0.5 * t);
    // the page's updateVoice(): mouth from the curve on the line's own clock
    const k = talkStart >= 0 ? Math.floor((S.time - talkStart) * 30) : -1;
    if (k >= line.env.length) talkStart = -1;
    S.talk.active = k >= 0 && k < line.env.length;
    if (S.talk.active) { S.talk.level = Math.min(1, parseInt(line.env[k], 36) / 35 * 1.1); S.talk.shape = +line.zcr[k] / 9; }
    step(S, dt);
    if (t >= next - 1e-9) {
      next += 1 / 30;
      const r5 = (v) => Math.round(v * 1e5) / 1e5;
      out.push(boneList.map((g) => [g.rotation.x, g.rotation.y, g.rotation.z, g.position.x / TEXEL, g.position.y / TEXEL, g.position.z / TEXEL, g.scale.x, g.scale.y, g.scale.z].map(r5)));
      fx.push([glowMat.opacity, glowCoreMat.opacity, glowHaloMat.opacity, laserMat.opacity, lasers[0].beam.scale.x, eyeGlow[0].core.position.x / TEXEL + 2].map(r5));
    }
  }
  return { dt, every: 1 / 30, end, plan, frames: out, fx };
}