/* Homelander viewer, part 'voice': the "I AM GOD" voice line and its lip sync.
   Classic script (loaded in order by homelander.html; THREE is global). */
/* ───────── Voice line ─────────
   sounds/homelander/i_am_god: .ogg (also used by the mod) and .mp3 (browsers without Ogg Vorbis).
   It plays when the Clap passes its events.voice time (0.3 s), or on key V (any animation).
   The mouth follows a loudness curve made beforehand with tools/lipsync.html (30 values per second):
   live audio analysis gives zeros on file:// pages. env: loudness 0-35 (base 36 digit), zcr: brightness 0-9.
   The curve runs on its own clock, so the mouth moves the same with the sound muted or still locked
   (browsers allow sound only after the first click or key) - and the same as in the mod. */
const VOICE_LINES = [
  { id: 'i_am_god', src: '../../sounds/homelander/i_am_god',
    env: '6iuyzzvsssmdbfmtutsstsstkb9aa635ivywuttvzzzyutqkjha5321100000000',
    zcr: '5232222451135442233433100369855552333333222233244545555555555555' }
];
// vol: 0.6 by default; the dock slider changes it (saved in this browser).
const voice = { on: true, unlocked: false, line: null, start: 0, anim: null, audio: null, vol: 0.6 };
try { const s = localStorage.getItem('homelander.voiceVol'); if (s !== null && isFinite(+s)) voice.vol = clamp(+s, 0, 1); } catch (e) { /* storage blocked: keep the default */ }
const AUDIO_EXT = new Audio().canPlayType('audio/ogg; codecs="vorbis"') ? '.ogg' : '.mp3';
VOICE_LINES.forEach((v) => { v.audio = new Audio(v.src + AUDIO_EXT); v.audio.preload = 'auto'; v.audio.volume = voice.vol; });

// Start a line now: S = the sim whose mouth it drives; anim = stop it when this animation ends ('' = never).
function sayLine(S, id = 'i_am_god', anim = '') {
  const v = VOICE_LINES.find((l) => l.id === id);
  if (!v) return;
  stopLine();
  voice.line = v; voice.start = S.time; voice.anim = anim;
  if (voice.on && voice.unlocked) { v.audio.currentTime = 0; v.audio.play().catch(() => {}); }
}
function stopLine() {
  if (voice.line) voice.line.audio.pause();
  voice.line = null;
}
// Every frame, before step(): feed the mouth from the line's curve at the line's own clock.
function updateVoice(S) {
  const v = voice.line, T = S.talk;
  if (v && voice.anim && S.anim !== voice.anim) stopLine();   // the attack was cut off: so is the shout
  if (!voice.line) { T.active = false; return; }
  const i = Math.floor((S.time - voice.start) * 30);
  if (i >= v.env.length) { stopLine(); T.active = false; return; }
  T.active = true;
  T.level = Math.min(1, parseInt(v.env[Math.max(0, i)], 36) / 35 * 1.1);
  T.shape = +v.zcr[Math.max(0, i)] / 9;
}
function setVoiceVol(x) {
  voice.vol = clamp(x, 0, 1);
  VOICE_LINES.forEach((l) => { l.audio.volume = voice.vol; });
}
const unlockVoice = () => { voice.unlocked = true; };
window.addEventListener('pointerdown', unlockVoice);
window.addEventListener('keydown', unlockVoice);
