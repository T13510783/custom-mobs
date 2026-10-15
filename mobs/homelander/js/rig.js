/* Homelander viewer, part 'rig': Materials, texture atlas, bones and cubes, color variants.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Materials & Texture Atlas ───────── */
const atlas = document.createElement('canvas');
const atlasCtx = atlas.getContext('2d');
const atlasTex = new THREE.CanvasTexture(atlas);
atlasTex.magFilter = atlasTex.minFilter = THREE.NearestFilter;
atlasTex.generateMipmaps = false;
atlasTex.colorSpace = THREE.SRGBColorSpace;
let ATLAS_W = 128, ATLAS_H = 128;

const matCache = {};
function mat(rough = 0.85, metal = 0.04, cut = false) {
  const key = rough + '|' + metal + '|' + cut;
  return matCache[key] || (matCache[key] = new THREE.MeshStandardMaterial({
    map: atlasTex, roughness: rough, metalness: metal,
    alphaTest: cut ? 0.5 : 0, side: cut ? THREE.DoubleSide : THREE.FrontSide
  }));
}

/* ───────── Model Rigging ───────── */
const mobRoot = new THREE.Group();
mobRoot.name = 'homelander';
scene.add(mobRoot);

const bones = {};
const boneList = [];
const cubeList = [];

function bone(name, parent, x, y, z, rx = 0, ry = 0, rz = 0) {
  const g = new THREE.Group();
  g.name = name;
  g.rotation.order = 'ZYX';
  g.position.set(x * TEXEL, y * TEXEL, z * TEXEL);
  g.rotation.set(rx, ry, rz);
  g.userData = { rest: g.position.clone(), restRot: new THREE.Vector3(rx, ry, rz) };
  (parent || mobRoot).add(g);
  bones[name] = g;
  boneList.push(g);
  return g;
}

function box(b, name, w, h, d, x, y, z, painter, opt = {}) {
  const inflate = opt.inflate || 0;
  const geo = new THREE.BoxGeometry((w + inflate * 2) * TEXEL, (h + inflate * 2) * TEXEL, (d + inflate * 2) * TEXEL);
  const nrm = geo.attributes.normal;
  for (let i = 0; i < 24; i++) {
    const f = i >> 2;
    nrm.setXYZ(i, f === 0 ? 1 : f === 1 ? -1 : 0, f === 2 ? 1 : f === 3 ? -1 : 0, f === 4 ? 1 : f === 5 ? -1 : 0);
  }
  const mesh = new THREE.Mesh(geo, mat(opt.rough, opt.metal, opt.cut));
  const rot = opt.rot || [0, 0, 0];
  mesh.position.set(x * TEXEL, y * TEXEL, z * TEXEL);
  mesh.rotation.order = 'ZYX';
  mesh.rotation.set(rot[0], rot[1], rot[2]);
  mesh.castShadow = opt.shadow !== false;
  mesh.receiveShadow = true;
  mesh.name = name;
  b.add(mesh);
  const c = { name, bone: b.name, size: [w, h, d], inflate, center: [x, y, z], rot, painter, mesh, shell: opt.shell || null, rects: [] };
  cubeList.push(c);
  return c;
}

/* ───────── Build Homelander Hierarchy ───────── */
// Ground root pivot
const root = bone('root', null, 0, 0, 0);

// Pelvis / Body at y = 12 texels
const body = bone('body', root, 0, 12, 0);

// Chest & Upper Torso (9x12x5, broad muscular superhero build)
const chest = bone('chest', body, 0, 0, 0);
box(chest, 'torso_suit', 9, 12, 5, 0, 6, 0, paintTorso);

// Head mounted DIRECTLY on chest at y = 12.0 (standard Minecraft humanoid, no neck box, no collar)
const head = bone('head', chest, 0, 12.0, 0);
box(head, 'head_cranium', 8, 8, 8, 0, 4.0, 0, paintHead);

// No nose block: flat player-style face like The King (nose is painted in paintHead)

// Extruded Blonde Hair: Authentic combed slickback outer shell, encases head top with zero clipping
box(head, 'hair_main', 8, 5, 8, 0, 5.7, 0, paintHair, { cut: true, inflate: 0.25, rough: 0.65 });

// Face Features: Menacing cold predator eyes, angled sinister brows, psychopathic closed-mouth smirk
const NO_SHADOW = { shadow: false };
box(head, 'eye_white_right', 2, 1, 0, -2, 4.5, 4.02, flat('eyeWhite'), NO_SHADOW);
box(head, 'eye_white_left', 2, 1, 0, 2, 4.5, 4.02, flat('eyeWhite'), NO_SHADOW);

const pupilR = bone('pupil_right', head, -2, 4.5, 4.04);
box(pupilR, 'pupil_right', 1, 1, 0, 0, 0, 0, flat('eyePupil'), NO_SHADOW);

const pupilL = bone('pupil_left', head, 2, 4.5, 4.04);
box(pupilL, 'pupil_left', 1, 1, 0, 0, 0, 0, flat('eyePupil'), NO_SHADOW);

const lidR = bone('lid_right', head, -2, 5, 3.98);
box(lidR, 'lid_right', 2, 1, 0, 0, -0.5, 0, flat('lidSkin'), NO_SHADOW);

const lidL = bone('lid_left', head, 2, 5, 3.98);
box(lidL, 'lid_left', 2, 1, 0, 0, -0.5, 0, flat('lidSkin'), NO_SHADOW);

// Eyebrows: nearly straight, low and confident (photos); frown tilts them inward
const browR = bone('brow_right', head, -2, 5.5, 4.08, 0, 0, -0.06);
box(browR, 'brow_right', 2, 1, 0, 0, 0, 0, flat('browHair'), NO_SHADOW);

const browL = bone('brow_left', head, 2, 5.5, 4.08, 0, 0, 0.06);
box(browL, 'brow_left', 2, 1, 0, 0, 0, 0, flat('browHair'), NO_SHADOW);

// Mouth: thin lip line (tilts for his one-sided smirk) + teeth plane under it that
// grows down from the lip line for the big PR grin (scale y 0 -> 1)
const mouth = bone('mouth', head, 0, 2.0, 4.03);
box(mouth, 'mouth', 2, 0.35, 0, 0, 0, 0, flat('lipSmirk'), NO_SHADOW);
const teeth = bone('teeth', mouth, 0, -0.1, -0.05); // rests just inside the head (hidden), steps out while grinning
box(teeth, 'teeth', 2, 0.8, 0, 0, -0.4, 0, flat('teeth'), NO_SHADOW);

// Red eyes (heat vision warming up): only the eyes glow, never the face (no light on the skin).
// Per eye: a red plane exactly on the 2x1 eye white + a hot 1x1 core that follows the pupil,
// both between the pupils (z 4.04) and the closing lids (z 4.06), so a blink hides them.
// A faint additive halo (3x2) adds a little bloom around the eye. Opacity = 'glow' / 'laser'.
// Mod: an emissive eye layer on the same pixels (vanilla "eyes" render type).
const glowMat = new THREE.MeshBasicMaterial({ color: 0xe0140a, transparent: true, opacity: 0, depthWrite: false });
const glowCoreMat = new THREE.MeshBasicMaterial({ color: 0xffb347, transparent: true, opacity: 0, depthWrite: false });
const glowHaloMat = new THREE.MeshBasicMaterial({ color: 0x701008, transparent: true, opacity: 0, blending: THREE.AdditiveBlending, depthWrite: false });
const eyeGlow = [-2, 2].map((x) => {
  const red = new THREE.Mesh(new THREE.PlaneGeometry(2 * TEXEL, 1 * TEXEL), glowMat);
  red.position.set(x * TEXEL, 4.5 * TEXEL, 4.05 * TEXEL);
  const core = new THREE.Mesh(new THREE.PlaneGeometry(1 * TEXEL, 1 * TEXEL), glowCoreMat);
  core.position.set(x * TEXEL, 4.5 * TEXEL, 4.052 * TEXEL);
  const halo = new THREE.Mesh(new THREE.PlaneGeometry(3 * TEXEL, 2 * TEXEL), glowHaloMat);
  halo.position.set(x * TEXEL, 4.5 * TEXEL, 4.1 * TEXEL);
  head.add(red, core, halo);
  return { red, core, halo, x };
});

// Heat-vision lasers: each beam starts at the front of its eye (z 4.1) and points forward.
// Geometry is 1 texel long from its origin, so scale.y = beam length in texels (per animation).
const laserMat = new THREE.MeshBasicMaterial({ color: 0xff1e1e, transparent: true, opacity: 0, depthWrite: false });
const laserCoreMat = new THREE.MeshBasicMaterial({ color: 0xfff1e0, transparent: true, opacity: 0, depthWrite: false });
const beamGeo = new THREE.CylinderGeometry(0.7 * TEXEL, 0.7 * TEXEL, TEXEL, 8).translate(0, TEXEL / 2, 0).rotateX(Math.PI / 2);
const coreGeo = new THREE.CylinderGeometry(0.28 * TEXEL, 0.28 * TEXEL, TEXEL, 6).translate(0, TEXEL / 2, 0).rotateX(Math.PI / 2);
const lasers = [-2, 2].map((x) => {
  const beam = new THREE.Mesh(beamGeo, laserMat), core = new THREE.Mesh(coreGeo, laserCoreMat);
  for (const m of [beam, core]) { m.position.set(x * TEXEL, 4.5 * TEXEL, 4.1 * TEXEL); head.add(m); }
  return { beam, core };
});
const LASER_LEN = 56; // default beam length in texels (3.5 blocks); anims may set `beam`

// American flag cape: 10 bones of 2 texels each (was 5 x 4) so it bends in a smooth curve.
// Same look as before: each segment paints its rows of the old 4-texel pattern
// (rows 0-3 star yoke, 4-7 V transition, 8-19 stripes), with the same rest bend per 4 texels.
const CAPE_N = 10, CAPE_SEG = 2;
const capeSub = document.createElement('canvas');            // reused: old 4-row segment is painted here
capeSub.width = capeSub.height = 32;
const capeSubCtx = capeSub.getContext('2d');
function capePainter(y0) {
  return (ctx, face, w, h) => {
    const old = y0 < 4 ? paintCapeMantle : y0 < 8 ? paintCapeTrans : paintCapeStripes;
    const tall = face !== 'top' && face !== 'bottom';         // side faces carry the 4 rows; top/bottom don't
    capeSubCtx.clearRect(0, 0, 32, 32);
    old(capeSubCtx, face, w, tall ? 4 : h);
    ctx.drawImage(capeSub, 0, tall ? y0 % 4 : 0, w, h, 0, 0, w, h); // copy our 2 rows
  };
}
const CAPE = [];
let capeParent = chest;
for (let i = 0; i < CAPE_N; i++) {
  const name = `cape_seg_${i + 1}`;
  // rest bend: 0.04 at the shoulders, then 0.02 per 4 texels -> 0.01 per 2 texels
  const b = i === 0 ? bone(name, chest, 0, 11.6, -2.65, 0.04, 0, 0) : bone(name, capeParent, 0, -CAPE_SEG, 0, i % 2 === 0 ? 0.02 : 0, 0, 0);
  box(b, name, i < 2 ? 10.0 : 9.6, CAPE_SEG, 0.4, 0, -CAPE_SEG / 2, 0, capePainter(i * CAPE_SEG), { rough: 0.65 });
  CAPE.push(name);
  capeParent = b;
}

// Arms with Golden Eagle Pauldrons and Gauntlet Bracers (muscular proportions)
function buildArm(side, sx) {
  const s = bone(`arm_${side}`, chest, sx * 6.8, 11.2, 0, 0, 0, sx * 0.08);
  box(s, `upper_arm_${side}`, 4.0, 5.6, 4.0, 0, -2.8, 0, paintUpperArm);

  // Sculpted Golden Eagle Pauldron on top of shoulder (outer armor shell below head height, zero clipping)
  box(s, `pauldron_${side}`, 4.4, 1.4, 4.4, sx * 0.2, -0.10, 0, paintPauldrons, { metal: 0.45, rough: 0.4 });
  box(s, `pauldron_wing_${side}`, 1.4, 1.4, 3.6, sx * 2.2, -0.10, 0, paintPauldrons, { metal: 0.45, rough: 0.4 });

  const e = bone(`forearm_${side}`, s, 0, -5.8, 0, -0.08, 0, 0);
  box(e, `lower_arm_${side}`, 3.8, 5.6, 3.8, 0, -2.8, 0, paintLowerArm);

  // Copper scale bracer (photos): from 1.1 below the elbow to 4.7, so the red glove
  // shows below it (lower arm rows 4-5) and blue sleeve above it
  box(e, `gauntlet_${side}`, 4.2, 3.6, 4.2, 0, -2.9, 0, paintBracer, { inflate: 0.05 });
}
// 2 bones per arm (rig rule): arm_<side> = upper arm (shoulder), forearm_<side> = forearm + glove (elbow)
buildArm('right', -1);
buildArm('left', 1);

// 2 bones per leg (rig rule): leg_<side> = thigh (hip), knee_<side> = shin + boot (knee).
// No ankle: the boot tilts with the shin (foot angle = hip + knee).
function buildLeg(side, sx) {
  const hp = bone(`leg_${side}`, body, sx * 2.2, 0, 0);
  box(hp, `thigh_${side}`, 4.2, 5.8, 4.2, 0, -2.8, 0, paintThigh);

  const k = bone(`knee_${side}`, hp, 0, -5.6, 0);
  box(k, `calf_${side}`, 4.0, 5.0, 4.0, 0, -2.3, 0, paintCalf);
  // Boot: sole at knee - 6.4 = floor at rest, toe 2.9 forward, heel 2.1 back (used by footLow())
  box(k, `boot_${side}`, 4.2, 2.2, 5.0, 0, -5.3, 0.4, paintBootFoot, { rough: 0.8 }); // matte: at 0.4 the blue rim light glared on the toe top
}
buildLeg('right', -1);
buildLeg('left', 1);

const B = {};
boneList.forEach((b, i) => { B[b.name] = i; });
const NB = boneList.length;

/* ───────── Texture Atlas Packing ───────── */
const FACES = ['right', 'left', 'top', 'bottom', 'front', 'back'];
const faceSize = (c, f) => {
  const [w, h, d] = c.size, r = (v) => Math.max(1, Math.round(v));
  return f < 2 ? [r(d), r(h)] : f < 4 ? [r(w), r(d)] : [r(w), r(h)];
};

(function packAtlas() {
  const items = [];
  for (const c of cubeList) for (let f = 0; f < 6; f++) { const [w, h] = faceSize(c, f); items.push({ c, f, w, h }); }
  items.sort((a, b) => b.h - a.h || b.w - a.w);
  let x = 1, y = 1, shelf = 0;
  for (const it of items) {
    if (x + it.w + 1 > ATLAS_W) { x = 1; y += shelf + 2; shelf = 0; }
    it.c.rects[it.f] = { x, y, w: it.w, h: it.h };
    x += it.w + 2;
    shelf = Math.max(shelf, it.h);
  }
  while (y + shelf + 1 > ATLAS_H) ATLAS_H *= 2;
  atlas.width = ATLAS_W; atlas.height = ATLAS_H;
})();

// Map face UVs to packed atlas rects
for (const c of cubeList) {
  const uv = c.mesh.geometry.attributes.uv;
  for (let f = 0; f < 6; f++) {
    const r = c.rects[f];
    for (let k = 0; k < 4; k++) {
      const i = f * 4 + k, u = uv.getX(i), v = uv.getY(i);
      uv.setXY(i, (r.x + u * r.w) / ATLAS_W, 1 - (r.y + (1 - v) * r.h) / ATLAS_H);
    }
  }
  uv.needsUpdate = true;
}

const scratch = document.createElement('canvas');
scratch.width = scratch.height = 32;
const sctx = scratch.getContext('2d');
const GUTTER = [[-1, 0], [1, 0], [0, -1], [0, 1]];

function paintAtlas() {
  atlasCtx.imageSmoothingEnabled = false;
  atlasCtx.clearRect(0, 0, ATLAS_W, ATLAS_H);
  for (const c of cubeList) {
    for (let f = 0; f < 6; f++) {
      const r = c.rects[f];
      sctx.clearRect(0, 0, 32, 32);
      c.painter(sctx, FACES[f], r.w, r.h);
      for (const [dx, dy] of GUTTER) atlasCtx.drawImage(scratch, 0, 0, r.w, r.h, r.x + dx, r.y + dy, r.w, r.h);
      atlasCtx.clearRect(r.x, r.y, r.w, r.h);
      atlasCtx.drawImage(scratch, 0, 0, r.w, r.h, r.x, r.y, r.w, r.h);
    }
  }
  atlasTex.needsUpdate = true;
}

function setVariant(i) {
  P = { ...BASE, ...VARIANTS[i].pal };
  paintAtlas();
  document.querySelectorAll('.dot').forEach((d, k) => d.classList.toggle('active', k === i));
}
