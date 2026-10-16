/* Shared viewer UI for every mob page (mobs/<mob>/<mob>.html).
   Load it as a classic script BEFORE the page's own script (works on file:// pages, no modules):
     <link rel="stylesheet" href="../_shared/viewer.css">
     <script src="../_shared/viewer.js"></script>
   What it gives every page:
     - the expression menu next to the camera-reset button (top right): Auto / Happy / Sad / Angry / Afraid
     - MobViewer.face(dt): the forced expression as smoothed, generic face channels the page maps onto its own face
   The page keeps its own model, face rig and animations; it only reads MobViewer.face(dt) once per frame. */

/* ───────── Expressions ─────────
   Generic face channels (every mob maps them onto whatever face parts it has):
     brow  -1..1  brows down (angry) .. up (surprised)
     tilt  -1..1  inner brow ends down (angry V) .. up (sad / afraid)
     smile -1..1  mouth corners down .. up (grin)
     open   0..1  mouth open
     eyes  -1..1  eyes squeezed / narrowed .. wide open
     look  -1..1  gaze / head down .. up
     roll  -1..1  head tilted to its right .. left
     shake  0..1  trembling (afraid) / shaking with rage (angry)
   Mobs without a face (masks, helmets) show the expression with the head: MobViewer.head (pitch, roll in rad). */
window.MobViewer = (() => {
  const EXPRESSIONS = {
    auto:   null,
    happy:  { brow: 0.35, tilt: 0.15, smile: 1.0, open: 0.25, eyes: -0.35, look: 0.3, roll: 0.5, shake: 0 },
    sad:    { brow: 0.05, tilt: 1.0, smile: -1.0, open: 0.05, eyes: -0.35, look: -1.0, roll: 0.6, shake: 0 },
    angry:  { brow: -0.8, tilt: -1.0, smile: -0.5, open: 0.2, eyes: -0.45, look: -0.4, roll: 0, shake: 0.3 },
    afraid: { brow: 1.0, tilt: 0.75, smile: -0.4, open: 0.65, eyes: 1.0, look: 0.4, roll: -0.3, shake: 1.0 },
  };
  const LABELS = { auto: 'Auto', happy: 'Happy', sad: 'Sad', angry: 'Angry', afraid: 'Afraid' };
  const KEYS = ['brow', 'tilt', 'smile', 'open', 'eyes', 'look', 'roll', 'shake'];

  const api = {
    expression: 'auto',                       // current menu choice
    EXPRESSIONS, KEYS,
    // Smoothed state: w = how much the forced face is applied (0 = animation's own face, 1 = forced),
    // the channels ease to the picked expression (same per-frame filter as the faces: never snaps).
    state: { w: 0, brow: 0, tilt: 0, smile: 0, open: 0, eyes: 0, look: 0, roll: 0, shake: 0, time: 0 },
    // Head offsets for the forced expression (rad, already weighted by w): add them to the head bone after the
    // animation pose. pitch + = chin down (three.js x). Small: stays inside each rig's head limits.
    head: { pitch: 0, roll: 0 },
    listeners: [],
    onChange(fn) { this.listeners.push(fn); },
    set(name) {
      if (!(name in EXPRESSIONS)) name = 'auto';
      this.expression = name;
      sync();
      for (const fn of this.listeners) fn(name);
    },
    // Call once per frame with delta time (seconds); returns the smoothed state.
    face(dt) {
      const s = this.state, target = EXPRESSIONS[this.expression];
      const k = 1 - Math.exp(-Math.min(dt, 0.1) * 10);
      s.w += ((target ? 1 : 0) - s.w) * k;
      if (target) for (const c of KEYS) s[c] += (target[c] - s[c]) * k;
      // Tremble: two fast sines (never a hard step), scaled by shake
      s.time += dt;
      const tr = s.shake * (0.6 * Math.sin(s.time * 47) + 0.4 * Math.sin(s.time * 31 + 1.3));
      this.head.pitch = s.w * (-0.22 * s.look + 0.015 * tr);
      this.head.roll = s.w * (0.12 * s.roll + 0.012 * tr);
      if (this.overlay) stepOverlay(this.overlay, this.expression, dt);
      return s;
    },
    // Painted faces (pixels in the head texture, no face bones): an 8x8 pixel overlay just in front of the
    // face shows the expression (moved brow + mouth pixels, the old ones painted over with skin); it fades in
    // and out, never pops. o = { center: [x, y, z] of the face front in head units, size: face width in head
    // units, skin, brow, lip, dark: colors, browRow: row of the brows (0 = top), mouthRow: row of the mouth,
    // lit: false for an unlit page }. Updated by face(dt).
    // Other face layouts: o.res = [w, h] pixels, o.size = [w, h] head units, o.erase + o.patterns = own pixel lists
    // ([col, row, color], row 0 = top) per expression (happy, sad, angry, afraid).
    faceOverlay(THREE, head, o) {
      this.overlay = buildOverlay(THREE, head, o);
      return this.overlay;
    },
    // Blend helper: v = the animation's own value, forced = the value for this expression.
    mix(v, forced) { return v + (forced - v) * this.state.w; },
  };

  /* ───────── Face overlay (painted faces) ───────── */
  function buildOverlay(THREE, head, o) {
    const b = o.browRow ?? 3, m = o.mouthRow ?? 6;
    const S = o.skin, B = o.brow, L = o.lip, D = o.dark || '#2a1410';
    // Every expression first paints the brow rows (b - 1, b) at the brow columns with skin, then draws its own
    // brows: the painted brows under it never show twice. Later pixels in a list paint over earlier ones.
    const erase = o.erase || [1, 2, 5, 6].flatMap((x) => [[x, b - 1, S], [x, b, S]]);
    const [W, H] = o.res || [8, 8];
    const pix = (list) => {                  // list of [col, row, color]: only these pixels are drawn
      list = [...erase, ...list];
      const c = document.createElement('canvas'); c.width = W; c.height = H;
      const g = c.getContext('2d');
      for (const [x, y, col] of list) if (y >= 0 && y < H) { g.fillStyle = col; g.fillRect(x, y, 1, 1); }
      const tex = new THREE.CanvasTexture(c);
      tex.magFilter = THREE.NearestFilter; tex.minFilter = THREE.NearestFilter; tex.generateMipmaps = false;
      if (o.srgb !== false) { if ('colorSpace' in tex && THREE.SRGBColorSpace) tex.colorSpace = THREE.SRGBColorSpace; else if (THREE.sRGBEncoding) tex.encoding = THREE.sRGBEncoding; }
      return tex;
    };
    const browsUp = [[1, b - 1, B], [2, b - 1, B], [5, b - 1, B], [6, b - 1, B], [1, b, S], [2, b, S], [5, b, S], [6, b, S]];
    const tex = o.patterns ? Object.fromEntries(Object.entries(o.patterns).map(([k, v]) => [k, pix(v)])) : {
      // happy: brows up a row, mouth corners up (smile)
      happy: pix([...browsUp, [1, m - 1, L], [6, m - 1, L], [1, m, S], [2, m, L], [3, m, L], [4, m, L], [5, m, L], [6, m, S]]),
      // sad: inner brow ends up, mouth corners down
      sad: pix([[2, b - 1, B], [5, b - 1, B], [2, b, S], [5, b, S], [1, b, B], [6, b, B],
                [1, m, S], [2, m, L], [3, m, L], [4, m, L], [5, m, L], [6, m, S], [1, m + 1, L], [6, m + 1, L]]),
      // angry: outer brow ends up, inner ends low and close together (V), tight dark mouth line
      angry: pix([[1, b - 1, B], [6, b - 1, B], [1, b, S], [6, b, S], [2, b, B], [3, b, B], [4, b, B], [5, b, B],
                  [1, m, S], [2, m, D], [3, m, D], [4, m, D], [5, m, D], [6, m, S]]),
      // afraid: brows high, small open mouth
      afraid: pix([...browsUp, [1, m, S], [2, m, S], [3, m, D], [4, m, D], [5, m, S], [6, m, S], [3, m + 1, D], [4, m + 1, D]]),
    };
    const opt = { map: tex.happy, transparent: true, opacity: 0, depthWrite: false, alphaTest: 0.01 };
    const mat = o.lit === false ? new THREE.MeshBasicMaterial(opt) : new THREE.MeshStandardMaterial({ ...opt, roughness: 0.9, metalness: 0 });
    const size = o.size ?? 8, [sw, sh] = Array.isArray(size) ? size : [size, size];
    const mesh = new THREE.Mesh(new THREE.PlaneGeometry(sw, sh), mat);
    mesh.position.set(o.center[0], o.center[1], o.center[2]);
    mesh.visible = false; mesh.renderOrder = 2;
    head.add(mesh);
    return { tex, mat, mesh, op: 0 };
  }
  // Fade the overlay to the picked expression; a new one waits until the old one has faded out.
  function stepOverlay(ov, name, dt) {
    const tex = ov.tex[name] || null;
    const target = tex && ov.mat.map === tex ? 1 : 0;
    ov.op += (target - ov.op) * (1 - Math.exp(-Math.min(dt, 0.1) * 10));
    if (ov.op < 0.02 && tex && ov.mat.map !== tex) { ov.mat.map = tex; ov.mat.needsUpdate = true; }
    ov.mat.opacity = ov.op;
    ov.mesh.visible = ov.op > 0.01;
  }

  /* ───────── Menu ─────────
     Sits just left of the page's camera-reset button, whatever layout the page has (the button is
     found by id, class or its title). A reset button inside a bar/row: the menu goes right before it in
     that row. A reset button floating on its own (fixed / absolute): the menu floats just left of it. */
  let select = null;
  const findReset = () =>
    document.querySelector('#resetBtn, #reset-camera, #resetCameraBtn, #btn-reset-cam, button.reset, [data-reset-camera]') ||
    [...document.querySelectorAll('button')].find((b) => /reset/i.test(`${b.id} ${b.title} ${b.getAttribute('aria-label') || ''}`));

  let floating = false;
  function place() {
    const reset = findReset();
    if (!select || !reset) return;
    if (!select.parentNode) {                // first time: pick inline or floating
      const pos = getComputedStyle(reset).position;
      floating = pos === 'fixed' || pos === 'absolute';
      if (floating) document.body.appendChild(select);
      else { select.classList.add('mv-inline'); reset.parentNode.insertBefore(select, reset); }
    }
    if (!floating) return;
    const r = reset.getBoundingClientRect();
    if (!r.width) return;                     // hidden (e.g. thumbnail render): keep the last spot
    select.style.top = `${Math.round(r.top + (r.height - select.offsetHeight) / 2)}px`;
    select.style.right = `${Math.round(window.innerWidth - r.left + 8)}px`;
  }

  // Icons (24x24 line drawings, currentColor): a small face per expression, a magic wand for Auto
  const FACE = '<circle cx="12" cy="12" r="9"/>';
  const ICONS = {
    auto: '<path d="M5 19l9-9"/><path d="M15 4v2M19 8h2M18 5l1.5-1.5M14 10l2 2"/><path d="M8 4l.6 1.4L10 6l-1.4.6L8 8l-.6-1.4L6 6l1.4-.6z"/>',
    happy: FACE + '<path d="M8 14c1 1.6 2.4 2.4 4 2.4s3-.8 4-2.4"/><path d="M9 9.5h.01M15 9.5h.01"/>',
    sad: FACE + '<path d="M8.5 16.5c.9-1.2 2.1-1.8 3.5-1.8s2.6.6 3.5 1.8"/><path d="M8 9l2 1M16 9l-2 1"/>',
    angry: FACE + '<path d="M8 8.5l3 1.5M16 8.5l-3 1.5"/><path d="M8.5 16h7"/>',
    afraid: FACE + '<circle cx="9" cy="10" r="1.2"/><circle cx="15" cy="10" r="1.2"/><ellipse cx="12" cy="15.6" rx="1.8" ry="2.2"/>',
  };
  const icon = (k) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[k]}</svg>`;
  let btn = null, menu = null, items = [];

  // Show the current choice on the button and in the list
  function sync() {
    if (!btn) return;
    const k = api.expression;
    btn.querySelector('.mv-expr-icon').innerHTML = icon(k);
    btn.querySelector('.mv-expr-label').textContent = LABELS[k];
    btn.classList.toggle('on', k !== 'auto');
    for (const it of items) { const on = it.dataset.expr === k; it.classList.toggle('active', on); it.setAttribute('aria-selected', String(on)); }
  }
  function open(show) {
    select.classList.toggle('open', show);
    btn.setAttribute('aria-expanded', String(show));
    if (show) {
      const r = select.getBoundingClientRect();
      select.classList.toggle('up', r.top > window.innerHeight / 2);   // near the bottom (in a dock): open upward
      (items.find((it) => it.dataset.expr === api.expression) || items[0]).focus();
    }
  }

  function build() {
    if (document.getElementById('expr')) return;
    // Pill button (icon + word + chevron) with a small list of the expressions under it
    select = document.createElement('div');
    select.id = 'expr';
    select.className = 'mv-expr';
    select.innerHTML = '<button type="button" class="mv-expr-btn" aria-haspopup="listbox" aria-expanded="false" title="Force an expression (Auto = the animations drive the face)">' +
      '<span class="mv-expr-icon"></span><span class="mv-expr-label"></span>' +
      '<svg class="mv-expr-chev" viewBox="0 0 10 6" aria-hidden="true"><path d="M1 1l4 4 4-4" fill="none" stroke="currentColor" stroke-width="1.5"/></svg></button>' +
      '<div class="mv-expr-menu" role="listbox" aria-label="Expression"></div>';
    btn = select.querySelector('.mv-expr-btn');
    menu = select.querySelector('.mv-expr-menu');
    for (const k of Object.keys(EXPRESSIONS)) {
      const it = document.createElement('button');
      it.type = 'button'; it.className = 'mv-expr-item'; it.dataset.expr = k; it.setAttribute('role', 'option');
      it.innerHTML = `${icon(k)}<span>${LABELS[k]}</span>`;
      it.addEventListener('click', () => { api.set(k); open(false); btn.focus(); });
      menu.appendChild(it); items.push(it);
    }
    btn.addEventListener('click', () => open(!select.classList.contains('open')));
    // Keys inside the menu never reach the page (number keys, R, B ...); arrows move, Esc closes
    select.addEventListener('keydown', (e) => {
      e.stopPropagation();
      if (e.key === 'Escape') { open(false); btn.focus(); }
      else if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
        e.preventDefault();
        if (!select.classList.contains('open')) { open(true); return; }
        const i = items.indexOf(document.activeElement), n = items.length;
        items[(i + (e.key === 'ArrowDown' ? 1 : n - 1) + n) % n].focus();
      }
    });
    document.addEventListener('pointerdown', (e) => { if (!select.contains(e.target)) open(false); });
    sync();
    place();
    window.addEventListener('resize', place);
    // Pages build or move their buttons after load (fonts, tailwind): follow the reset button for a while
    let n = 0; const id = setInterval(() => { place(); if (++n > 20) clearInterval(id); }, 250);
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', build);
  else build();
  return api;
})();
