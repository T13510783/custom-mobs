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
