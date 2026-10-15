/* Homelander viewer, part 'ui': Rig overlay, UI controls, render loop.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Rig overlay ("Show bones" button, key B) ───────── */
// Same block in every mob. A dot on every bone pivot (yellow) and a line to its parent
// (cyan), drawn on top of the model. Buffers are made once; update() only rewrites them.
const rigView = (() => {
  const list = boneList.filter((b) => b.name !== 'root');
  const linePos = new Float32Array(list.length * 6), dotPos = new Float32Array(list.length * 3);
  const lineGeo = new THREE.BufferGeometry(), dotGeo = new THREE.BufferGeometry();
  lineGeo.setAttribute('position', new THREE.BufferAttribute(linePos, 3));
  dotGeo.setAttribute('position', new THREE.BufferAttribute(dotPos, 3));
  const lines = new THREE.LineSegments(lineGeo, new THREE.LineBasicMaterial({ color: 0x67e8f9, depthTest: false, transparent: true, opacity: 0.9 }));
  const dots = new THREE.Points(dotGeo, new THREE.PointsMaterial({ color: 0xfacc15, size: 6, sizeAttenuation: false, depthTest: false, transparent: true }));
  for (const o of [lines, dots]) { o.renderOrder = 999; o.frustumCulled = false; o.visible = false; scene.add(o); }
  const a = new THREE.Vector3(), b = new THREE.Vector3();
  return {
    on: false,
    set(on) { this.on = on; lines.visible = dots.visible = on; if (on) this.update(); },
    update() {
      mobRoot.updateMatrixWorld(true);
      list.forEach((g, i) => {
        g.getWorldPosition(a);
        (g.parent.isGroup && g.parent !== mobRoot ? g.parent : g).getWorldPosition(b);
        const j = i * 6, k = i * 3; // plain writes: no new arrays per frame
        linePos[j] = b.x; linePos[j + 1] = b.y; linePos[j + 2] = b.z;
        linePos[j + 3] = a.x; linePos[j + 4] = a.y; linePos[j + 5] = a.z;
        dotPos[k] = a.x; dotPos[k + 1] = a.y; dotPos[k + 2] = a.z;
      });
      lineGeo.attributes.position.needsUpdate = true;
      dotGeo.attributes.position.needsUpdate = true;
    }
  };
})();

/* ───────── UI Controls ───────── */
let sim = makeSim(999);
// The page's sim: Clap shouts its voice line at events.voice; the beams stop at the test dummy.
function hookSim(S) {
  S.onEvent = (name, anim) => { if (name === 'voice') sayLine(S, 'i_am_god', anim); };
  S.beamLen = dummyBeamLen;
  return S;
}
hookSim(sim);

const dotsEl = document.getElementById('dots');
VARIANTS.forEach((v, i) => {
  const d = document.createElement('button');
  d.className = 'dot';
  d.title = `${v.name} colors`;
  d.style.background = (v.pal.suitBase || BASE.suitBase);
  d.addEventListener('click', () => setVariant(i));
  dotsEl.appendChild(d);
});

const animBtns = document.querySelectorAll('.anim-btn');
const markAnim = (name) => animBtns.forEach((b) => b.classList.toggle('active', b.dataset.anim === name));

function selectAnim(name) {
  switchAnim(sim, name);
  markAnim(name);
}
animBtns.forEach((b) => b.addEventListener('click', () => selectAnim(b.dataset.anim)));

const followBtn = document.getElementById('followBtn');
followBtn.addEventListener('click', () => {
  sim.follow.on = !sim.follow.on;
  followBtn.classList.toggle('on', sim.follow.on);
  followBtn.setAttribute('aria-pressed', String(sim.follow.on));
});

const bonesBtn = document.getElementById('bonesBtn');
function toggleBones() {
  rigView.set(!rigView.on);
  bonesBtn.classList.toggle('on', rigView.on);
  bonesBtn.setAttribute('aria-pressed', String(rigView.on));
}
bonesBtn.addEventListener('click', toggleBones);

// Voice line on/off + volume (key V = shout it now)
const voiceBtn = document.getElementById('voiceBtn'), voiceVol = document.getElementById('voiceVol');
voiceBtn.addEventListener('click', () => {
  voice.on = !voice.on;
  if (!voice.on) stopLine();
  voiceBtn.classList.toggle('on', voice.on);
  voiceBtn.setAttribute('aria-pressed', String(voice.on));
  voiceVol.disabled = !voice.on;
});
voiceVol.value = String(Math.round(voice.vol * 100));
voiceVol.title = `Voice line volume: ${voiceVol.value}%`;
voiceVol.addEventListener('input', () => {
  setVoiceVol(+voiceVol.value / 100);
  voiceVol.title = `Voice line volume: ${voiceVol.value}%`;
  try { localStorage.setItem('homelander.voiceVol', String(voice.vol)); } catch (e) { /* storage blocked: fine */ }
});

// Test dummy on/off (key D): the victim of the attacks
const dummyBtn = document.getElementById('dummyBtn');
function toggleDummy() {
  dummy.on = !dummy.on;
  dummyBtn.classList.toggle('on', dummy.on);
  dummyBtn.setAttribute('aria-pressed', String(dummy.on));
}
dummyBtn.addEventListener('click', toggleDummy);

let resetting = false;
document.getElementById('resetBtn').addEventListener('click', () => { resetting = true; });


window.addEventListener('keydown', (e) => {
  if (e.target.tagName === 'INPUT' && e.target.type !== 'range') return; // the volume slider keeps the keys
  const i = ['1', '2', '3', '4', '5', '6', '7', '8'].indexOf(e.key);
  if (i >= 0) selectAnim(ANIM_ORDER[i]);
  if (e.key === 'r' || e.key === 'R') resetting = true;
  if (e.key === 'b' || e.key === 'B') toggleBones();
  if (e.key === 'd' || e.key === 'D') toggleDummy();
  if (e.key === 'v' || e.key === 'V') { voice.unlocked = true; sayLine(sim, 'i_am_god'); }
});

window.addEventListener('pointermove', (e) => {
  sim.follow.mx = (e.clientX / window.innerWidth) * 2 - 1;
  sim.follow.my = -(e.clientY / window.innerHeight) * 2 + 1;
  sim.follow.has = true;
});

window.addEventListener('resize', () => {
  camera.aspect = window.innerWidth / window.innerHeight;
  camera.updateProjectionMatrix();
  renderer.setSize(window.innerWidth, window.innerHeight);
});

/* ───────── 60/120 FPS Locked Render Loop ───────── */
let targetHz = 60, measuring = true, lastNow = 0, lastRender = 0;
const intervals = [];

function frame(now) {
  requestAnimationFrame(frame);
  if (measuring) {
    if (lastNow) intervals.push(now - lastNow);
    lastNow = now;
    if (intervals.length >= 30) {
      const sorted = intervals.slice().sort((a, b) => a - b);
      targetHz = 1000 / sorted[15] >= 110 ? 120 : 60;
      measuring = false;
    }
  } else {
    const interval = 1000 / targetHz;
    if (now < lastRender + interval - 1) return;
  }
  const dt = lastRender ? Math.min((now - lastRender) / 1000, 0.1) : 1 / 60;
  lastRender = measuring ? now : Math.max(lastRender + 1000 / targetHz, now - 1000 / targetHz);

  updateVoice(sim);
  step(sim, dt);
  updateDummy(sim, dt);
  if (rigView.on) rigView.update();

  if (resetting) {
    const k = filt(dt, 8);
    camera.position.lerp(CAM_POS, k);
    controls.target.lerp(CAM_TARGET, k);
    if (camera.position.distanceTo(CAM_POS) < 0.005 && controls.target.distanceTo(CAM_TARGET) < 0.005) resetting = false;
  }
  controls.dampingFactor = filt(dt, 4.5);
  controls.update();
  renderer.render(scene, camera);
}

setVariant(0);
