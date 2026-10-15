package com.custommobs.theking.client;

import com.custommobs.theking.KingConfig;
import com.custommobs.theking.TheKingMod;
import com.custommobs.theking.anim.KingAnimator;
import com.custommobs.theking.anim.KingData;
import com.custommobs.theking.entity.KingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/* ───────── One King on the client: which animation plays, sounds, lip sync ─────────
   Base animation: Walk while moving, Run while sprinting (both sped up to match his real ground speed, so the feet do not slide),
   otherwise Idle with now and then one loop of Idle 2. One-shots (Attack, Attack 2, Dance) come from the server
   and play once, then crossfade back. The animator itself is the 1:1 port of the HTML viewer. */
public final class KingClientState {
    private static final double WALK_DESIGN_SPEED = 1.0; // blocks/s of one Walk loop at speed 1 (16 texels/s stride)
    // Run: the planted foot slides 7.4 texels in 0.36 x 0.5625 s (viewer RUN) = 36.5 texels/s = 2.28 blocks/s.
    private static final double RUN_DESIGN_SPEED = 2.28;
    private static final double DEG = Math.PI / 180;

    public final KingAnimator a;
    private final KingData d;
    private float lastAge = Float.NaN;
    private boolean oneShot;
    private double lastT, speed, prevX, prevZ;
    private boolean hasPrev;
    private int voiceLine = -1;
    private double voiceStart;
    private EntityBoundSoundInstance voiceSound, musicSound;

    private KingClientState(KingEntity e) {
        d = KingData.get();
        a = new KingAnimator(d, e.getUUID().hashCode());
        a.followOn = true;
        a.followHas = true;
    }

    public static KingClientState of(KingEntity e) {
        if (e.clientState == null) e.clientState = new KingClientState(e);
        return (KingClientState) e.clientState;
    }

    /* ───────── Server events ───────── */
    public static void onEvent(KingEntity e, byte id) {
        KingClientState s = of(e);
        switch (id) {
            case KingEntity.EV_KICK -> s.playOnce("attack");
            case KingEntity.EV_PUNCH -> s.playOnce("attack2");
            case KingEntity.EV_DANCE -> { s.playOnce("dance"); s.startMusic(e); }
            case KingEntity.EV_STOP -> { s.oneShot = false; s.stopMusic(); }
            default -> s.startVoice(e, id - KingEntity.EV_VOICE);
        }
    }

    private void playOnce(String name) {
        a.switchAnim(name);
        oneShot = true;
    }

    private void startVoice(KingEntity e, int line) {
        if (line < 0 || line >= d.voice.length || line >= TheKingMod.VOICE.length) return;
        if (voiceSound != null) Minecraft.getInstance().getSoundManager().stop(voiceSound);
        voiceSound = new EntityBoundSoundInstance(TheKingMod.VOICE[line].get(), SoundSource.HOSTILE,
                KingConfig.VOICE_VOLUME.get().floatValue(), 1F, e, e.getRandom().nextLong());
        Minecraft.getInstance().getSoundManager().play(voiceSound);
        voiceLine = line;
        voiceStart = (e.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) / 20.0;
    }

    private void startMusic(KingEntity e) {
        stopMusic();
        float vol = KingConfig.MUSIC_VOLUME.get().floatValue();
        if (vol <= 0) return;
        musicSound = new EntityBoundSoundInstance(TheKingMod.DANCE_MUSIC.get(), SoundSource.HOSTILE, vol, 1F, e, e.getRandom().nextLong());
        Minecraft.getInstance().getSoundManager().play(musicSound);
    }

    private void stopMusic() {
        if (musicSound != null) Minecraft.getInstance().getSoundManager().stop(musicSound);
        musicSound = null;
    }

    /* ───────── Every rendered frame ───────── */
    public void update(KingEntity e, float ageInTicks, float netHeadYaw, float headPitch) {
        double dt = Float.isNaN(lastAge) ? 1 / 60.0 : (ageInTicks - lastAge) / 20.0;
        if (dt < 1e-4) return;                 // drawn twice in one frame: pose is already up to date
        dt = Math.min(dt, 0.1);
        lastAge = ageInTicks;
        float partial = ageInTicks - e.tickCount;

        // Ground speed (blocks/s), smoothed.
        double x = Mth.lerp(partial, e.xo, e.getX()), y = Mth.lerp(partial, e.yo, e.getY()), z = Mth.lerp(partial, e.zo, e.getZ());
        if (hasPrev) {
            double v = Math.sqrt((x - prevX) * (x - prevX) + (z - prevZ) * (z - prevZ)) / dt;
            if (v < 20) speed += (v - speed) * (1 - Math.exp(-dt * 8)); // faster = a teleport, not running
        }
        prevX = x; prevZ = z; hasPrev = true;

        // Which animation.
        double rate = 1;
        if (oneShot) {
            double lead = a.anim.name.equals("dance") ? 0.35 : 0.05; // the dance fades back while he stands up
            if (a.t + dt >= a.anim.T - lead || !e.isAlive()) {
                oneShot = false;
                if (a.anim.name.equals("dance")) stopMusic();
            }
        }
        if (!oneShot) {
            boolean moving = speed > 0.35 && !e.isBusy();
            boolean running = moving && e.isSprinting(); // the server sprints him while he chases (flag is synced)
            String want = running ? "run" : moving ? "walk" : a.anim.name.equals("idle2") ? "idle2" : "idle";
            String now = a.anim.name;
            if (!moving && now.equals("idle") && a.t < lastT && e.getRandom().nextInt(5) == 0) want = "idle2"; // after an Idle loop: sometimes Idle 2
            if (!moving && now.equals("idle2") && a.t < lastT) want = "idle";                                   // Idle 2 plays one loop
            if (!want.equals(now)) {
                if (KingEntity.DEBUG && (want.equals("run") || now.equals("run")))
                    KingEntity.LOG.info("[TheKing] client: {} -> {} at {} blocks/s", now, want, String.format("%.2f", speed));
                a.switchAnim(want);
            }
            if (want.equals("walk")) rate = Mth.clamp(speed / WALK_DESIGN_SPEED, 0.5, 2.2);
            if (want.equals("run")) rate = Mth.clamp(speed / RUN_DESIGN_SPEED, 0.6, 1.8);
        }
        lastT = a.t;

        // Head + eyes follow where he looks (viewer: the mouse). Minecraft head yaw + = his right, so flip it.
        a.lookYaw = -netHeadYaw * DEG;
        a.lookPitch = headPitch * DEG;

        // Lip sync from the voice line's loudness curve (30 values per second).
        a.talkActive = false;
        if (voiceLine >= 0) {
            KingData.VoiceLine l = d.voice[voiceLine];
            int i = (int) Math.floor((ageInTicks / 20.0 - voiceStart) * 30);
            if (i >= 0 && i < l.env.length()) {
                a.talkActive = true;
                a.talkLevel = Math.min(1, Character.digit(l.env.charAt(i), 36) / 35.0 * 1.1);
                a.talkShape = Character.digit(l.zcr.charAt(i), 10) / 9.0;
            } else if (i >= l.env.length()) voiceLine = -1;
        }

        // World transform (springs feel his real movement).
        a.worldX = x; a.worldY = y; a.worldZ = z;
        a.worldYaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot) * DEG;
        a.step(dt, rate);
    }
}
