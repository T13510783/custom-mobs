package com.custommobs.homelander.client;

import com.custommobs.config.SindicateConfig;
import com.custommobs.homelander.HomelanderMod;
import com.custommobs.homelander.anim.HomelanderAnimator;
import com.custommobs.homelander.anim.HomelanderData;
import com.custommobs.homelander.entity.HomelanderEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/* ───────── One Homelander on the client: which animation plays, sounds, lip sync, cape motion ─────────
   Base animation: Run when he sprints, Walk while moving (both sped up to his real ground speed, so the feet do not
   slide), otherwise Idle;
   from Idle the animator itself plays a random Hover or Idle 2 now and then (like the viewer).
   Attacks come from the server (entity events) and play once, then crossfade back.
   Sounds follow the animation's own events (viewer SFX_EVENTS + the Clap's "voice" line), so they hit on the frame. */
public final class HomelanderClientState {
    private static final double WALK_DESIGN_SPEED = 1.0; // blocks/s of one Walk loop at speed 1 (anim move = 16 texels/s)
    private static final double RUN_DESIGN_SPEED = 54 / 16.0; // Run loop at speed 1 (anim move = 54 texels/s)
    private static final double DEG = Math.PI / 180;

    // viewer SFX_EVENTS: animation -> event -> sound ("-laser" = stop the laser hum)
    private static final Map<String, Map<String, String>> SFX_EVENTS = Map.of(
            "attack", Map.of("hit", "punch"),
            "special", Map.of("laserOn", "laser", "laserOff", "-laser"),
            "throw", Map.of("grab", "grab", "release", "whoosh"),
            "clap", Map.of("swing", "whoosh", "hit", "slap"),
            "smash", Map.of("swing", "whoosh", "hit", "punch", "grab", "grab", "release", "whoosh"),
            "throatLaser", Map.of("grab", "grab", "burnOn", "laser", "burnOff", "-laser"));

    public final HomelanderAnimator a;
    private final HomelanderData d;
    private float lastAge = Float.NaN;
    private boolean oneShot;
    private double speed, prevX, prevY, prevZ, prevYaw;
    private boolean hasPrev;
    private final double[] move = new double[3];
    private int voiceLine = -1;
    private double voiceStart;
    private EntityBoundSoundInstance voiceSound;
    private final Map<String, EntityBoundSoundInstance> sfx = new HashMap<>();
    private final Map<String, String> sfxAnim = new HashMap<>();

    private HomelanderClientState(HomelanderEntity e) {
        d = HomelanderData.get();
        a = new HomelanderAnimator(d, e.getUUID().hashCode());
        a.followOn = true;
        a.followHas = true;
        a.move = move;
    }

    public static HomelanderClientState of(HomelanderEntity e) {
        if (e.clientState == null) {
            HomelanderClientState s = new HomelanderClientState(e);
            e.clientState = s;
            e.clientAnim = () -> s.a;
        }
        return (HomelanderClientState) e.clientState;
    }

    /* ───────── Server events: start an attack ───────── */
    public static void onEvent(HomelanderEntity e, byte id) {
        HomelanderClientState s = of(e);
        switch (id) {
            case HomelanderEntity.EV_PUNCH -> s.playOnce("attack");
            case HomelanderEntity.EV_LASER -> s.playOnce("special");
            case HomelanderEntity.EV_THROW -> s.playOnce("throw");
            case HomelanderEntity.EV_CLAP -> s.playOnce("clap");
            case HomelanderEntity.EV_BURN -> s.playOnce("throatLaser");
            case HomelanderEntity.EV_SMASH -> s.playOnce("smash");
            default -> s.oneShot = false;
        }
    }

    private void playOnce(String name) {
        a.switchAnim(name);
        oneShot = true;
    }

    /* ───────── Sounds ───────── */
    private SoundEvent sound(String name) {
        return switch (name) {
            case "punch" -> HomelanderMod.PUNCH.get();
            case "grab" -> HomelanderMod.GRAB.get();
            case "whoosh" -> HomelanderMod.WHOOSH.get();
            case "slap" -> HomelanderMod.SLAP.get();
            default -> HomelanderMod.LASER.get();
        };
    }

    private void playSfx(HomelanderEntity e, String name) {
        var sm = Minecraft.getInstance().getSoundManager();
        if (name.startsWith("-")) { stopSfx(name.substring(1)); return; }
        stopSfx(name);
        EntityBoundSoundInstance s = new EntityBoundSoundInstance(sound(name), SoundSource.HOSTILE, SindicateConfig.HL_SFX_VOLUME.get().floatValue(), 1F, e, e.getRandom().nextLong());
        sm.play(s);
        sfx.put(name, s);
        sfxAnim.put(name, a.anim.name);
    }

    private void stopSfx(String name) {
        EntityBoundSoundInstance s = sfx.remove(name);
        if (s != null) Minecraft.getInstance().getSoundManager().stop(s);
        sfxAnim.remove(name);
    }

    private void startVoice(HomelanderEntity e, int line, double now) {
        if (line < 0 || line >= d.voice.length || line >= HomelanderMod.VOICE.length) return;
        if (voiceSound != null) Minecraft.getInstance().getSoundManager().stop(voiceSound);
        voiceSound = new EntityBoundSoundInstance(HomelanderMod.VOICE[line].get(), SoundSource.HOSTILE, SindicateConfig.HL_VOICE_VOLUME.get().floatValue(), 1F, e, e.getRandom().nextLong());
        Minecraft.getInstance().getSoundManager().play(voiceSound);
        voiceLine = line;
        voiceStart = now;
    }

    /* ───────── Every rendered frame ───────── */
    public void update(HomelanderEntity e, float ageInTicks, float netHeadYaw, float headPitch) {
        double dt = Float.isNaN(lastAge) ? 1 / 60.0 : (ageInTicks - lastAge) / 20.0;
        if (dt < 1e-4) return;                 // drawn twice in one frame: pose is already up to date
        dt = Math.min(dt, 0.1);
        lastAge = ageInTicks;
        float partial = ageInTicks - e.tickCount;

        // Where he is (blocks) and which way his body faces, this frame.
        double x = Mth.lerp(partial, e.xo, e.getX()), y = Mth.lerp(partial, e.yo, e.getY()), z = Mth.lerp(partial, e.zo, e.getZ());
        double yaw = Mth.rotLerp(partial, e.yBodyRotO, e.yBodyRot) * DEG;
        // Ground speed (blocks/s, smoothed) + how far he moved through the air (texels, his model space: the cape trails).
        move[0] = move[1] = move[2] = 0;
        if (hasPrev) {
            double dx = x - prevX, dy = y - prevY, dz = z - prevZ;
            double v = Math.sqrt(dx * dx + dz * dz) / dt;
            if (v < 20) {                      // faster = a teleport, not walking
                speed += (v - speed) * (1 - Math.exp(-dt * 8));
                double c = Math.cos(yaw), s = Math.sin(yaw);
                move[0] = (c * dx + s * dz) * 16;
                move[1] = dy * 16;
                move[2] = (-s * dx + c * dz) * 16;
            }
            a.cloth.turn(Mth.wrapDegrees((yaw - prevYaw) / DEG) * DEG); // a turn swings the cape out
        }
        prevX = x; prevY = y; prevZ = z; prevYaw = yaw; hasPrev = true;

        // Which animation.
        double rate = 1;
        if (oneShot && (a.t + dt >= a.anim.T - 0.05 || !e.isAlive())) oneShot = false;
        if (!oneShot) {
            boolean moving = speed > 0.35 && !e.isVehicle();
            // Run: the server sets the sprinting flag when he chases from far away (synced vanilla flag)
            boolean running = moving && e.isSprinting() && speed > 2.0;
            String now = a.anim.name;
            boolean calm = now.equals("idle") || now.equals("idle2") || now.equals("hover");
            String want = running ? "run" : moving ? "walk" : calm ? now : "idle";
            if (!want.equals(now)) {
                if (HomelanderEntity.DEBUG) HomelanderEntity.LOG.info("[Homelander] client anim {} -> {} (speed {} sprint {})", now, want, String.format("%.2f", speed), e.isSprinting());
                a.switchAnim(want);
            }
            if (want.equals("walk")) rate = Mth.clamp(speed / WALK_DESIGN_SPEED, 0.5, 2.5);
            if (want.equals("run")) rate = Mth.clamp(speed / RUN_DESIGN_SPEED, 0.6, 3.5);
        }
        // Walk while attacking (Punch / Heat vision, server walkAt): legs walk under the attack's upper body
        String an = a.anim.name;
        boolean walkable = oneShot && (an.equals("attack") || an.equals("special"));
        a.walkAtk = walkable ? Mth.clamp((speed - 0.25) / 0.5, 0, 1) : 0;
        a.walkRate = Mth.clamp(speed / WALK_DESIGN_SPEED, 0.5, 2.5);

        // Head + eyes follow where he looks (viewer: the mouse). Minecraft head yaw + = his right, so flip it.
        a.lookYaw = -netHeadYaw * DEG;
        a.lookPitch = headPitch * DEG;

        // Lip sync from the voice line's loudness curve (30 values per second).
        double now = ageInTicks / 20.0;
        a.talkActive = false;
        if (voiceLine >= 0) {
            HomelanderData.VoiceLine l = d.voice[voiceLine];
            int i = (int) Math.floor((now - voiceStart) * 30);
            if (i >= 0 && i < l.env.length()) {
                a.talkActive = true;
                a.talkLevel = Math.min(1, Character.digit(l.env.charAt(i), 36) / 35.0 * 1.1);
                a.talkShape = Character.digit(l.zcr.charAt(i), 10) / 9.0;
            } else if (i >= l.env.length()) voiceLine = -1;
        }

        a.worldX = x; a.worldY = y; a.worldZ = z;
        a.worldYaw = yaw;
        a.step(dt, rate);
        HomelanderVictims.track(e, a, partial);   // the Clap / Smash victim falls, lies, is dragged (any mob)

        // Sounds on the animation's own events; a sound of an animation that stopped stops too (viewer updateSfx).
        Map<String, String> ev = SFX_EVENTS.get(a.anim.name);
        for (int i = 0; i < a.firedCount; i++) {
            String name = a.fired[i];
            if (name.equals("voice")) startVoice(e, 0, now);
            String fx = ev == null ? null : ev.get(name);
            if (fx != null) playSfx(e, fx);
        }
        sfxAnim.entrySet().removeIf(en -> {
            if (en.getValue().equals(a.anim.name)) return false;
            EntityBoundSoundInstance s = sfx.remove(en.getKey());
            if (s != null) Minecraft.getInstance().getSoundManager().stop(s);
            return true;
        });
    }
}
