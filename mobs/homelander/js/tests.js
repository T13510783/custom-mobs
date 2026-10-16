/* Homelander viewer, part 'tests': Self-tests and headless modes.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Self-Tests & Headless Testing ───────── */
function restPose() {
  boneList.forEach((g) => {
    g.rotation.copy(new THREE.Euler(g.userData.restRot.x, g.userData.restRot.y, g.userData.restRot.z, 'ZYX'));
    g.position.copy(g.userData.rest);
    g.scale.set(1, 1, 1);
  });
}

function clipTest() {
  const meshes = cubeList.map((c) => c.mesh);
  const inv = meshes.map(() => new THREE.Matrix4());
  const half = cubeList.map((c) => c.size.map((s) => (Math.abs(s) / 2 + c.inflate) * TEXEL));
  const pts = cubeList.map((c, i) => {
    const [hx, hy, hz] = half[i], list = [];
    for (let a = 0; a < 5; a++) for (let b = 0; b < 5; b++) {
      const u = -1 + a * 0.5, v = -1 + b * 0.5;
      list.push([hx, u * hy, v * hz], [-hx, u * hy, v * hz], [u * hx, hy, v * hz], [u * hx, -hy, v * hz], [u * hx, v * hy, hz], [u * hx, v * hy, -hz]);
    }
    return list.map((p) => new THREE.Vector3(...p));
  });
  const radius = half.map((h) => Math.hypot(h[0], h[1], h[2]) * 2.2);
  const centers = meshes.map(() => new THREE.Vector3());
  const EPS = 0.06, LAYER = 0.15;
  const wp = new THREE.Vector3(), lp = new THREE.Vector3();

  function depthInside(j, p) {
    const [hx, hy, hz] = half[j];
    const dx = hx - Math.abs(p.x), dy = hy - Math.abs(p.y), dz = hz - Math.abs(p.z);
    if (dx <= 0 || dy <= 0 || dz <= 0) return 0;
    return Math.min(dx, dy, dz);
  }

  function measure() {
    mobRoot.updateMatrixWorld(true);
    meshes.forEach((m, i) => { inv[i].copy(m.matrixWorld).invert(); centers[i].setFromMatrixPosition(m.matrixWorld); });
    const res = new Map();
    for (let i = 0; i < meshes.length; i++) for (let j = 0; j < meshes.length; j++) {
      if (i === j || cubeList[i].bone === cubeList[j].bone) continue;
      if (centers[i].distanceTo(centers[j]) > radius[i] + radius[j]) continue;
      let worst = 0;
      for (const p of pts[i]) {
        wp.copy(p).applyMatrix4(meshes[i].matrixWorld);
        lp.copy(wp).applyMatrix4(inv[j]);
        const d = depthInside(j, lp);
        if (d > worst) worst = d;
      }
      if (worst > 0) res.set(i + ':' + j, worst / TEXEL);
    }
    return res;
  }

  const JOINTS = [
    [/^(thigh_|calf_|boot_)/, /^torso_suit$/],
    [/^(calf_|boot_)/, /^thigh_/],
    [/^(lower_arm_|gauntlet_)/, /^(upper_arm_|pauldron_)/],
    [/^(upper_arm_|pauldron_)/, /^(torso_suit|head_cranium)$/],
    [/^head_cranium$/, /^torso_suit$/],
    [/^cape_seg_[1-4]$/, /^(torso_suit|head_cranium|pauldron_|upper_arm_)/],   // shoulders (top 4 texels)
    [/^cape_seg_[5-8]$/, /^upper_arm_/],
    ...CAPE.slice(1).map((n, i) => [new RegExp(`^${n}$`), new RegExp(`^${CAPE[i]}$`)]) // each bone with the one above
  ];
  const isJoint = (a, b) => JOINTS.some(([x, y]) => (x.test(a) && y.test(b)) || (x.test(b) && y.test(a)));

  restPose();
  const baseline = measure();
  const report = {
    cubes: cubeList.length, bones: NB, atlas: [ATLAS_W, ATLAS_H],
    clips: {}, joints: {}, ground: {},
    layeredAtRest: [...baseline].filter(([, d]) => d > EPS).map(([k, d]) => k.split(':').map((i) => cubeList[i].name).join(' / ') + ' ' + d.toFixed(2))
  };

  const boots = cubeList.filter((c) => /^boot_/.test(c.name));
  const corner = new THREE.Vector3();
  const mice = [[0, 0], [-0.95, 0], [0.95, 0], [0, 0.95], [0, -0.95]];

  for (const name of ALL_ANIMS) {
    let low = 99;
    for (const [, [mx, my]] of mice.entries()) {
      const S = makeSim(99);
      S.anim = name;
      S.events = false; // no random anim switches while testing one animation
      Object.assign(S.follow, { on: true, has: true, mx, my });
      const dt = 1 / 120;
      for (let t = 0; t < ANIMS[name].T; t += dt) step(S, dt);
      let since = 0;
      for (let t = 0; t < ANIMS[name].T * 2; t += dt) {
        step(S, dt);
        since += dt;
        if (since < 1 / 20) continue;
        since = 0;
        for (const [key, d] of measure()) {
          const base = baseline.get(key) || 0;
          const [i, j] = key.split(':').map(Number);
          const a = cubeList[i].name, b = cubeList[j].name;
          const joint = isJoint(a, b);
          if (!joint && d <= (base > EPS ? base + LAYER : EPS)) continue;
          if (joint && d <= base + EPS) continue;
          const bucket = joint ? report.joints : report.clips;
          const id = `${a} in ${b}`;
          if (!bucket[id] || bucket[id].depth < d) bucket[id] = { depth: +d.toFixed(3), rest: +base.toFixed(3), anim: name };
        }
        for (const c of boots) {
          const [hx, hy, hz] = half[cubeList.indexOf(c)];
          for (let k = 0; k < 8; k++) {
            corner.set(k & 1 ? hx : -hx, k & 2 ? hy : -hy, k & 4 ? hz : -hz).applyMatrix4(c.mesh.matrixWorld);
            low = Math.min(low, corner.y / TEXEL);
          }
        }
      }
    }
    report.ground[name] = +low.toFixed(3);
  }

  const bb = buildBBModel();
  const badUV = bb.elements.filter((e) => Object.values(e.faces).some((f) => f.uv.some((v, k) => v < 0 || v > (k % 2 ? ATLAS_H : ATLAS_W)))).map((e) => e.name);
  report.export = {
    elements: bb.elements.length, badUV,
    animations: bb.animations.map((a) => ({ name: a.name, bones: Object.keys(a.animators).length, keyframes: Object.values(a.animators).reduce((n, x) => n + x.keyframes.length, 0) })),
    bytes: JSON.stringify(bb).length
  };

  const out = document.createElement('pre');
  out.id = 'cliptest';
  out.textContent = JSON.stringify(report, null, 1);
  document.body.appendChild(out);
}

function dumpFrame() {
  controls.update();
  renderer.render(scene, camera);
  const out = document.createElement('pre');
  out.id = 'shot';
  out.textContent = canvas.toDataURL('image/png');
  document.body.appendChild(out);
}

function shot(anim, variant, secs) {
  setVariant(variant);
  sim.anim = anim;
  sim.events = false;
  Object.assign(sim.follow, { has: true, mx: 0.25, my: 0.15 });
  if (anim === 'special') {
    camera.position.set(2.8, 2.2, 3.4);
    controls.target.set(0, 1.45, 0);
  }
  for (let t = 0; t < secs; t += 1 / 60) step(sim, 1 / 60);
  dumpFrame();
}

window.setVariant = setVariant;
window.selectAnim = selectAnim;
window.homelanderDebug = { cubeList, bones, boneList, atlas, get sim() { return sim; }, ANIMS, camera, controls, dumpFrame, setVariant, selectAnim, buildBBModel, rigView, toggleBones, makeSim, step, HB, VICTIM, dummy, voice };
// Headless JSON modes (tools/export_mod_assets.ps1, .bbmodel save): the result goes into <pre id="...">.
const dumpJson = (id, obj) => { const pre = document.createElement('pre'); pre.id = id; pre.textContent = JSON.stringify(obj); document.body.appendChild(pre); };
if (location.hash === '#cliptest') clipTest();
else if (location.hash === '#modexport') dumpJson('modexport', modExport());
else if (location.hash === '#modtrace') dumpJson('modtrace', modTrace());
else if (location.hash === '#bbmodel') dumpJson('bbmodel', buildBBModel());
else if (location.hash === '#rest') { restPose(); dumpFrame(); }
else if (location.hash === '#back') { restPose(); camera.position.set(0, 1.7, -3.6); controls.target.set(0, 1.05, 0); dumpFrame(); }
else if (location.hash.startsWith('#shot')) { const [, a, v, s] = location.hash.split('-'); shot(a, +v, +s); }
else requestAnimationFrame(frame);
