"""Homelander sound effects, made from code (no recordings, no licences needed).

Writes 44.1 kHz mono 16-bit .wav files; tools/sfx/make_homelander_sfx.ps1 turns them into
sounds/homelander/<name>.ogg (mod + browser) and .mp3 (browser backup).
  punch   - Attack: the super-punch lands (thump + smack)
  grab    - Throw / Clap / Throat laser: the hand closes on a neck or head
  whoosh  - Throw: the victim is flung up into the sky
  crush   - Clap: the head clap (thud + bone crunch + wet splat)
  laser   - Special / Throat laser: heat-vision beams (buzzing hum, 2 s, faded ends)
Same seed every run: the files come out byte-identical.
Usage: python tools/sfx/homelander_sfx.py <out_dir>
"""
import math
import random
import struct
import sys
import wave

RATE = 44100
TAU = math.tau


def svf(signal, fc, q=0.5):
    """State-variable band-pass filter. fc = cutoff in Hz (number or per-sample list)."""
    low = band = 0.0
    out = []
    for i, x in enumerate(signal):
        f = 2 * math.sin(math.pi * min((fc[i] if isinstance(fc, list) else fc), RATE / 6) / RATE)
        low += f * band
        high = x - low - q * band
        band += f * high
        out.append(band)
    return out


def lowpass(signal, fc):
    a = 1 - math.exp(-TAU * fc / RATE)
    y = 0.0
    out = []
    for x in signal:
        y += a * (x - y)
        out.append(y)
    return out


def noise(n, rnd):
    return [rnd.uniform(-1, 1) for _ in range(n)]


def normalize(s, peak=0.9):
    m = max(abs(v) for v in s) or 1
    return [v / m * peak for v in s]


def punch(rnd):
    n = int(0.35 * RATE)
    nz = noise(n, rnd)
    smack = svf(nz, 1800, 0.7)
    out, ph = [], 0.0
    for i in range(n):
        t = i / RATE
        ph += TAU * (45 + 35 * math.exp(-t * 25)) / RATE     # pitch drops: body thump
        out.append(0.9 * math.sin(ph) * math.exp(-t * 16) + 1.4 * smack[i] * math.exp(-t * 45) + 0.5 * nz[i] * math.exp(-t * 300))
    return normalize(out)


def grab(rnd):
    n = int(0.25 * RATE)
    rub = lowpass(noise(n, rnd), 900)
    out, ph = [], 0.0
    for i in range(n):
        t = i / RATE
        ph += TAU * 90 / RATE
        out.append(0.7 * math.sin(ph) * math.exp(-t * 28) + 2.2 * rub[i] * math.sin(math.pi * min(1, t / 0.25)) * math.exp(-t * 9))
    return normalize(out, 0.7)


def whoosh(rnd):
    n = int(0.6 * RATE)
    fc = [300 + 2200 * (i / n) ** 1.5 for i in range(n)]
    air = svf(noise(n, rnd), fc, 0.35)
    out = [air[i] * math.sin(math.pi * i / n) ** 2 for i in range(n)]
    return normalize(out, 0.8)


def crush(rnd):
    n = int(0.7 * RATE)
    nz = noise(n, rnd)
    wet = lowpass(nz, 700)
    out, ph = [], 0.0
    cracks = [(rnd.uniform(0, 0.18), rnd.uniform(0.4, 1.0)) for _ in range(14)]   # bone snaps: time, loudness
    for i in range(n):
        t = i / RATE
        ph += TAU * (40 + 30 * math.exp(-t * 20)) / RATE
        v = 1.0 * math.sin(ph) * math.exp(-t * 10) + 2.5 * wet[i] * math.exp(-t * 7)
        for (c, a) in cracks:
            d = t - c
            if 0 <= d < 0.012:
                v += a * nz[(i * 7) % n] * math.exp(-d * 400)
        out.append(v)
    return normalize(out)


def laser(rnd):
    n = int(2.0 * RATE)
    crackle = svf(noise(n, rnd), 3500, 0.6)
    out, p1, p2, p3 = [], 0.0, 0.0, 0.0
    for i in range(n):
        t = i / RATE
        vib = 1 + 0.02 * math.sin(TAU * 6 * t)
        p1 += TAU * 92 * vib / RATE
        p2 += TAU * 184.5 * vib / RATE
        p3 += TAU * (1150 + 180 * math.sin(TAU * 37 * t)) / RATE                     # FM buzz on top
        saw = ((p1 / TAU) % 1) * 2 - 1
        saw2 = ((p2 / TAU) % 1) * 2 - 1
        env = min(1, t / 0.08) * min(1, (2.0 - t) / 0.25)
        out.append(env * (0.45 * saw + 0.3 * saw2 + 0.25 * math.sin(p3) + 0.5 * crackle[i] * (0.6 + 0.4 * math.sin(TAU * 13 * t))))
    return normalize(lowpass(out, 6000), 0.75)


def write(path, s):
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, v)) * 32767)) for v in s))


if __name__ == '__main__':
    out_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    for name, fn in [('punch', punch), ('grab', grab), ('whoosh', whoosh), ('crush', crush), ('laser', laser)]:
        write(f'{out_dir}/{name}.wav', fn(random.Random(name)))
        print('wrote', name)
