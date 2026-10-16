/* Homelander viewer, part 'painters': Color palettes + texture painters.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Color Palettes ───────── */
const BASE = {
  // Skin: Rich dimensional tones matching The King (micro-shading, rosy cheeks & nose tip)
  skin: '#e5af95', skinLight: '#f4c7b0', skinBase: '#e5af95', skinWarm: '#d6977b',
  skinRosy: '#d98473', skinShadow: '#b8765c', lidSkin: '#cd8a6e',
  eyeWhite: '#f8fafc', eyePupil: '#1f5f9e', eyeIris: '#0284c7',
  browHair: '#8a6431', lipLine: '#a36456', lipSmirk: '#a8655a', teeth: '#f3efe4',
  // Blonde hair: natural golden blonde from the photos (less lemon-yellow)
  hairLight: '#f0d68a', hairGolden: '#dcb862', hairBase: '#c89f4a', hairWarm: '#a98236', hairDark: '#7d5c26', hairNape: '#5c4219',
  // Suit: royal blue with a fine scale texture (photos), not navy
  suitLight: '#3b62a6', suitBase: '#2c4f8f', suitDark: '#1d3767', suitDeep: '#142951',
  suitTexture: '#264682', suitPipingRed: '#c8343a', suitPipingDark: '#8f1f26',
  // Antique gold eagle armor (pauldrons & belt)
  goldLight: '#f3d47e', goldBright: '#dcae4a', goldBase: '#c08f32', goldWarm: '#9a6c22', goldDark: '#5e4013',
  // Copper scale bracers on the forearms
  bronzeLight: '#da8b52', bronzeBase: '#b9662f', bronzeWarm: '#95491f', bronzeDark: '#5f2b13',
  // Deep red leather gloves & boots
  crimsonLight: '#d2413a', crimsonBase: '#b3262a', crimsonWarm: '#931c22', crimsonDark: '#5f1216', crimsonSole: '#3d1612',
  // Cape: US flag (star yoke + equal red/white stripes, striped inside too)
  capeBlueDark: '#1b2f63', capeBlueBase: '#253f82', capeStarWhite: '#ffffff',
  capeRedBase: '#b8262c', capeRedDark: '#8e1b21',
  capeWhiteBase: '#f1f1ec', capeWhiteShade: '#d9d9d2',
  capeLining: '#f1f5f9'
};

const VARIANTS = [
  { id: 'classic', name: 'Classic (Show)', pal: {} },
  { id: 'damaged', name: 'Battle Damaged', pal: {
    suitLight: '#2d4677', suitBase: '#213a68', suitTexture: '#1b315a', suitDark: '#142647', suitDeep: '#0e1b34',
    skinLight: '#e4bcab', skinBase: '#d4a28f', skinWarm: '#c48974',
    skinRosy: '#bf7969', skinShadow: '#9c6654', skin: '#d4a28f', lidSkin: '#c48974',
    goldBase: '#a07428', goldWarm: '#7c5419', goldDark: '#4a3210',
    bronzeBase: '#96522a', bronzeWarm: '#743a1a',
    crimsonBase: '#962024', crimsonWarm: '#7a181d', crimsonDark: '#4c0e11',
    capeRedBase: '#962024', capeRedDark: '#6e161a', capeWhiteBase: '#c9c7bd', capeWhiteShade: '#b3b1a7'
  } },
  { id: 'comic', name: 'Comic Book', pal: {
    suitLight: '#4a86e0', suitBase: '#2f6bc9', suitDark: '#1f4f9e', suitTexture: '#3a78d6', suitDeep: '#173d7c',
    skinLight: '#ffe0cf', skinBase: '#ffceb4', skinWarm: '#f6b89c',
    skinRosy: '#f89e90', skinShadow: '#d88e76', skin: '#ffceb4', lidSkin: '#f6b89c',
    hairBase: '#fde047', hairGolden: '#facc15', hairLight: '#fef9c3',
    goldBright: '#fef08a', goldBase: '#facc15', goldWarm: '#eab308',
    crimsonLight: '#f87171', crimsonBase: '#ef4444', crimsonWarm: '#dc2626',
    capeBlueBase: '#2563eb', capeRedBase: '#ef4444'
  } }
];

let P = { ...BASE };

/* ───────── Texture Painters ───────── */
function hash2D(x, y, seed = 1) {
  const n = Math.sin(x * 127.1 + y * 311.7 + seed * 74.7) * 43758.5453123;
  return n - Math.floor(n);
}
function px(ctx, x, y, col) { ctx.fillStyle = col; ctx.fillRect(x, y, 1, 1); }
const flat = (key) => (ctx, f, w, h) => { ctx.fillStyle = P[key]; ctx.fillRect(0, 0, w, h); };

// Head base (8x8x8): Rich dimensional skin tones with micro-shading, rosy cheeks & jawline depth (matching The King)
function paintHead(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const n = hash2D(x, y, 13);
      let col = n > 0.55 ? P.skinBase : P.skinWarm;

      if (face === 'front') {
        if (y <= 1) col = P.skinLight; // forehead light
        if (y === 4 && (x === 1 || x === 6)) col = P.skinRosy; // rosy cheekbones
        if (y === 5 && (x === 1 || x === 6)) col = P.skinShadow; // cheek shadows
        // Painted player-style nose (no block): lit right side (key light is on +x), shaded left
        if (y === 4 && x === 3) col = P.skinWarm;
        if (y === 4 && x === 4) col = P.skinLight;
        if (y === 5 && x >= 2 && x <= 5) col = (x === 3 || x === 4) ? P.skinWarm : P.skinLight; // soft shade under the nose, lit cheeks beside it
        if (y === 6 && (x === 2 || x === 5)) col = P.skinShadow; // mouth corners (the lip line itself is the mouth plane, like The King)
        if (y === 7) col = (x >= 2 && x <= 5) ? P.skinWarm : P.skinShadow; // strong square chin, dark jaw corners
      } else if (face === 'left' || face === 'right') {
        const fromFront = (face === 'right') ? x : (w - 1 - x);
        if (fromFront >= 4) {
          // Short tapered sides: combed strands, darker toward the nape
          const streak = hash2D(fromFront, 0, 7) > 0.5;
          col = y <= 5 ? (streak ? P.hairBase : P.hairWarm) : (y === 6 ? (streak ? P.hairWarm : P.hairDark) : P.hairDark);
        } else if (fromFront === 3 && y >= 2 && y <= 5) {
          col = P.hairGolden;
        } else if (fromFront === 2 && (y === 3 || y === 4)) {
          col = P.skinRosy;
        }
      } else if (face === 'top') {
        col = (x % 2 === 0) ? P.hairLight : P.hairGolden;
      } else if (face === 'back') {
        // Back of the head below the hair shell: same combed strands as the shell, tapering
        // into a short dark nape (photos: neat short back)
        const streak = hash2D(x, 0, 7) > 0.5;
        if (y <= 5) col = streak ? P.hairBase : P.hairWarm;
        else if (y === 6) col = streak ? P.hairWarm : P.hairDark;
        else col = streak ? P.hairDark : P.hairNape;
      } else if (face === 'bottom') {
        col = P.skinShadow;
      }
      px(ctx, x, y, col);
    }
  }
}

// Blonde Hair Outer Shell (8x5x8 with 0.25 inflate, cut: true):
// Authentic pomaded slicked-back hairstyle with crisp left part, combed front-to-back crown,
// and neat tapered sides/nape ending cleanly above the neckline.
function paintHair(ctx, face, w, h) {
  ctx.clearRect(0, 0, w, h);
  if (face === 'bottom') return; // open underneath so neck/head fits

  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let filled = false;
      let col = P.hairGolden;

      if (face === 'top') {
        // Crown: Hair combed straight back from forehead (y=0) to back of head (y=7)
        filled = true;
        if (x === 2) {
          // Side parting line: honey-warm comb division
          col = P.hairWarm;
        } else if (x === 0 || x === 1) {
          // Right side of part: combed straight back
          col = (x === 1) ? P.hairLight : ((y <= 2) ? P.hairLight : P.hairGolden);
        } else {
          // Main combed-over volume to the left: parallel combed strands
          if (x === 3 || x === 5) {
            col = (y <= 4) ? P.hairLight : P.hairGolden; // bright platinum highlights
          } else if (x === 4 || x === 6) {
            col = (y >= 5) ? P.hairWarm : P.hairGolden; // warm golden lock
          } else {
            col = (y >= 4) ? P.hairWarm : P.hairGolden; // outer crest
          }
        }
      } else if (face === 'front') {
        // Front hairline: slicked back across top edge ONLY.
        // No hanging sideburn strips: face is completely clear and open.
        if (y === 0) {
          filled = true;
          if (x === 2) col = P.hairWarm; // soft honey parting groove
          else if (x === 3 || x === 5) col = P.hairLight;
          else if (x === 0 || x === 7) col = P.hairWarm;
          else col = P.hairGolden;
        }
      } else if (face === 'left' || face === 'right') {
        // Sides: Hair combed horizontally back tight against the head, clean temple/ears
        const fromFront = (face === 'right') ? x : (w - 1 - x);
        if (y === 0) {
          filled = true;
          col = (fromFront <= 2) ? P.hairLight : P.hairGolden;
        } else if (y === 1) {
          filled = true;
          col = (fromFront >= w - 3) ? P.hairWarm : P.hairGolden;
        } else if (y === 2) {
          if (fromFront >= 2) {
            filled = true;
            col = (fromFront >= w - 2) ? P.hairDark : P.hairWarm;
          }
        } else if (y === 3) {
          if (fromFront >= 3) {
            filled = true;
            col = (fromFront >= w - 2) ? P.hairDark : P.hairWarm;
          }
        } else if (y === 4) {
          if (fromFront >= 4) {
            filled = true;
            col = P.hairDark;
          }
        }
      } else if (face === 'back') {
        // Back: vertical combed strands (one tone per column), lighter on top,
        // flowing into the head's back face below (same column hash)
        filled = true;
        const streak = hash2D(x, 0, 7) > 0.5;
        if (y <= 1) col = streak ? P.hairLight : P.hairGolden;
        else if (y <= 3) col = streak ? P.hairGolden : P.hairBase;
        else col = streak ? P.hairBase : P.hairWarm;
      }

      if (filled) px(ctx, x, y, col);
    }
  }
}

// Suit fabric: fine scale texture from the photos. Mostly base blue, sparse darker scale
// edges and rare light glints (hash noise, so no harsh checkerboard).
function suitCol(x, y, seed = 3) {
  const n = hash2D(x, y, seed);
  return n < 0.28 ? P.suitTexture : n > 0.9 ? P.suitLight : P.suitBase;
}

// Torso Suit (9x12x5): royal blue scale suit with sculpted chest + abs (photos).
// Front: plain blue neckline (no collar, user request), one red piping line across the upper chest
// (y 1, out to his left shoulder) that turns straight down on his right chest
// (viewer's left, x 2) to the belt, like the show suit (photos).
function paintTorso(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const weave = hash2D(x, y, 9) < 0.5;
      let col = suitCol(x, y);

      if (face === 'front') {
        const isLeftPec = (x >= 2 && x <= 3);
        const isRightPec = (x >= 5 && x <= 7);
        const pipe = (y === 1 && x >= 2 && x <= 7) || (x === 2 && y >= 1 && y <= 9);

        if (pipe) {
          col = y >= 7 ? P.suitPipingDark : P.suitPipingRed; // red piping, darker low on the flank
        } else if (y === 0) {
          // Clavicle line under the head
          col = (x === 0 || x === 8) ? P.suitDark : P.suitLight;
        } else if (y === 1 || y === 2) {
          // Upper Pectorals: prominent muscular chest crests with highlight sheen
          if (x === 4) col = P.suitDark; // center sternum groove
          else if (x === 0 || x === 8) col = P.suitDark; // flank contour
          else if (isLeftPec || isRightPec) col = weave ? P.suitLight : P.suitBase;
        } else if (y === 3) {
          // Mid Pectorals: full chest volume
          if (x === 4) col = P.suitDark;
          else if (x === 0 || x === 8) col = P.suitDark;
          else if (isLeftPec || isRightPec) col = weave ? P.suitBase : P.suitTexture;
        } else if (y === 4) {
          // Lower Pectoral Shelf: curved anatomical crease defining lower pectoral border
          if (x === 4) col = P.suitDeep; // epigastric fossa
          else if (isLeftPec || isRightPec) col = P.suitDark; // under-pec shadow crease
          else col = P.suitDark;
        } else if (y >= 5 && y <= 9) {
          // Six-pack Abdominals & Obliques:
          // Linea alba center groove at x=4, lateral ab borders at x=2 and x=6
          if (x === 4) {
            col = P.suitDark; // vertical center line
          } else if (x === 0 || x === 8) {
            col = P.suitDark; // athletic flank taper
          } else if (y === 6 || y === 8) {
            // Horizontal inscriptions dividing upper, mid, and lower ab pads
            if (x >= 2 && x <= 6) col = P.suitDark;
            else col = weave ? P.suitBase : P.suitTexture;
          } else if (x === 2 || x === 6) {
            col = P.suitDark; // lateral ab boundary grooves
          } else if (x === 3 || x === 5) {
            // Abdominal muscle pads (upper: y=5, middle: y=7, lower: y=9)
            col = (y === 5 || y === 7) ? (weave ? P.suitBase : P.suitTexture) : (weave ? P.suitBase : P.suitDark);
          } else {
            // Obliques (x=1, x=7)
            col = weave ? P.suitBase : P.suitTexture;
          }
        } else if (y >= 10) {
          // Built-in Golden Belt with Red Center Square Buckle (Ref 1)
          if (x >= 3 && x <= 5) {
            // Red square in the middle of the belt
            col = (y === 10) ? (x === 4 ? P.crimsonLight : P.crimsonBase) : (x === 4 ? P.crimsonBase : P.crimsonWarm);
          } else {
            // Gold belt strap & buckle rim
            col = (y === 10) ? P.goldBright : P.goldWarm;
          }
        }
      } else if (face === 'back') {
        if (y === 0) col = P.suitDark; // shoulder line
        else if (y >= 10) col = (y === 10) ? P.goldBright : P.goldWarm; // built-in gold belt strap
        else if (x === 4) col = P.suitDeep; // spine groove
      } else if (face === 'left' || face === 'right') {
        if (y === 0) col = P.suitDark;
        else if (y >= 10) col = (y === 10) ? P.goldBright : P.goldWarm; // built-in gold belt strap
      } else if (face === 'bottom') {
        col = P.suitDark;
      }
      px(ctx, x, y, col);
    }
  }
}

// Golden Eagle Pauldrons (Shoulder pads): sculpted gold eagles perched on shoulders
function paintPauldrons(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = P.goldBase;
      if (face === 'top') {
        // Eagle feathers: staggered light/dark rows, lit front edge
        col = y === 0 ? P.goldLight : ((x + y) % 2 === 0 ? P.goldBright : P.goldBase);
        if (y === h - 1) col = P.goldWarm;
      } else if (face === 'bottom') {
        col = P.goldDark;
      } else {
        if (y === 0) col = P.goldBright;
        else if (y === h - 1) col = P.goldDark;
        else col = (x % 2 === 0) ? P.goldBase : P.goldWarm;
      }
      px(ctx, x, y, col);
    }
  }
}

// Red leather (gloves, boots): soft hash shading instead of a checkerboard
const leather = (x, y, seed) => hash2D(x, y, seed) < 0.35 ? P.crimsonWarm : P.crimsonBase;

// Upper Arm: plain blue scale sleeve (photos: no band at the elbow)
function paintUpperArm(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      px(ctx, x, y, face === 'bottom' ? P.suitDark : suitCol(x, y, 21));
    }
  }
}

// Lower arm (6 rows of ~0.93 texel): blue sleeve at the elbow, hidden middle under the
// bracer, red glove for the bottom 2 rows (the hand, below the bracer).
function paintLowerArm(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = suitCol(x, y, 23);
      if (face === 'bottom') col = P.crimsonDark;          // palm / fist underside
      else if (face !== 'top' && y >= h - 2) col = leather(x, y, 25);
      px(ctx, x, y, col);
    }
  }
}

// Bracer (gauntlet box, 3 rows): copper scales with a dark top rim and a red glove cuff
// flaring over the bottom edge (photos)
function paintBracer(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = (x + y) % 2 === 0 ? P.bronzeBase : P.bronzeWarm; // tiny scales
      if (face === 'top' || face === 'bottom') col = P.bronzeDark;
      else if (y === 0) col = x % 2 === 0 ? P.bronzeDark : P.bronzeWarm; // ridged top rim
      else if (y === 1) col = x % 2 === 0 ? P.bronzeLight : P.bronzeBase; // lit scale row
      else if (y === h - 1) col = P.crimsonBase;                           // glove cuff
      px(ctx, x, y, col);
    }
  }
}

// Thighs (blue scale suit, darker knee fold)
function paintThigh(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = suitCol(x, y, 31);
      if (y === h - 1 && face !== 'top') col = P.suitDark; // knee fold shadow
      px(ctx, x, y, col);
    }
  }
}

// Calf = knee-high red boot shaft: antique gold top rim, round gold eagle medallion on the shin
function paintCalf(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = leather(x, y, 33);
      if (y === 0) {
        col = P.goldBase; // plain antique gold boot top rim
      } else if (face === 'front' && y >= 1 && y <= 2 && (x === 1 || x === 2)) {
        // 2x2 medallion: lit top-left, shaded bottom-right
        col = y === 1 ? (x === 1 ? P.goldLight : P.goldBright) : (x === 1 ? P.goldBase : P.goldWarm);
      } else if (y === 1 && (face === 'left' || face === 'right')) {
        col = P.crimsonDark; // shadow under the rim on the sides
      }
      px(ctx, x, y, col);
    }
  }
}

// Boot Foot: red leather boot with a dark sole and lit toe
function paintBootFoot(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = leather(x, y, 37);
      if (y === h - 1 || face === 'bottom') {
        col = P.crimsonSole;
      } else if (face === 'top' || (face === 'front' && y === 0)) {
        col = P.crimsonLight;
      }
      px(ctx, x, y, col);
    }
  }
}

// Cape stripes (photos): equal 1-texel red/white stripes, mirrored around the center so
// both edges are white. Same pattern outside and inside (the lining is striped too).
function capeStripe(x, y, w, seed) {
  const k = Math.min(x, w - 1 - x), shade = hash2D(x, y, seed) < 0.2;
  return k % 2 === 0 ? (shade ? P.capeWhiteShade : P.capeWhiteBase) : (shade ? P.capeRedDark : P.capeRedBase);
}
// Star field: staggered white stars on blue
const capeStar = (x, y) => (x + 2 * y) % 4 === 1;
const capeBlue = (x, y) => hash2D(x, y, 41) < 0.3 ? P.capeBlueDark : P.capeBlueBase;

// Cape segment 1: blue star yoke over the shoulders (outside), plain blue inside
function paintCapeMantle(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = P.capeBlueDark;
      if (face === 'back') col = capeStar(x, y) ? P.capeStarWhite : capeBlue(x, y);
      else if (face === 'front') col = capeBlue(x, y);
      px(ctx, x, y, col);
    }
  }
}

// Cape segment 2: the yoke ends in a V (2 rows), then stripes start
function paintCapeTrans(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = P.capeWhiteShade; // side edges = white edge stripe
      if (face === 'back') {
        const k = Math.min(x, w - 1 - x);            // distance from the nearest edge
        const yoke = (y === 0 && k >= 2) || (y === 1 && k >= 4);
        col = yoke ? (capeStar(x, y + 4) ? P.capeStarWhite : capeBlue(x, y + 4)) : capeStripe(x, y, w, 43);
      } else if (face === 'front') {
        col = capeStripe(x, y, w, 44);
      } else if (face === 'top') {
        col = P.capeBlueDark;
      }
      px(ctx, x, y, col);
    }
  }
}

// Cape segments 3-5: red/white stripes outside and inside
function paintCapeStripes(ctx, face, w, h) {
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      let col = P.capeWhiteShade;
      if (face === 'back' || face === 'front') col = capeStripe(x, y, w, face === 'back' ? 45 : 46);
      else if (face === 'bottom' || face === 'top') col = capeStripe(x, 0, w, 47);
      px(ctx, x, y, col);
    }
  }
}
