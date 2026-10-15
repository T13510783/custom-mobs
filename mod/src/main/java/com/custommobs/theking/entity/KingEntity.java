package com.custommobs.theking.entity;

import com.custommobs.theking.KingConfig;
import com.custommobs.theking.anim.KingData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/* ───────── The King (server + shared logic) ─────────
   Hostile to players. Walks up, punches (Attack 2) when close, leaps into a dropkick (Attack) from 3 blocks up to his leap reach (~5).
   Dances 1 s after every kill (with music), says a voice line now and then (mouth synced on the client).
   Every action starts the same animation on all clients through an entity event; the dropkick's leap
   moves the real entity along the animation's own root-motion curves, so body and hitbox stay together. */
public class KingEntity extends Monster {
    /* ───────── Entity events (server -> clients) ───────── */
    public static final byte EV_KICK = 100, EV_PUNCH = 101, EV_DANCE = 102, EV_STOP = 103, EV_VOICE = 110; // EV_VOICE + line index

    // Dev runs only (gradlew runClient/runServer set -Dtheking.debug=true): actions go to the log, for testing.
    public static final boolean DEBUG = Boolean.getBoolean("theking.debug"); // dev runs only (also read by the client state)
    public static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(KingEntity.class, EntityDataSerializers.INT);

    public enum Action { NONE, KICK, PUNCH, DANCE }

    private Action action = Action.NONE;
    private int actionTicks;
    private LivingEntity actionTarget;
    private boolean hitDone, kickAimed;
    private double kickX, kickY, kickZ, kickScale = 1;
    private float kickYaw;
    private int danceDelay = -1, lastVoice = -1;
    int kickCooldown = 60, punchCooldown;

    /** Client-side animation state (KingClientState); never touched on the server. */
    public Object clientState;

    public KingEntity(EntityType<? extends KingEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.2)   // ~2.3 blocks/s when chasing: his walk animation speeds up to match
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ARMOR, 2);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new KingBusyGoal(this));
        goalSelector.addGoal(2, new KingAttackGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /* ───────── Spawning ───────── */
    /** Natural spawns: any light (he is a daytime guy), rare, never another King within 48 blocks. */
    public static boolean checkKingSpawnRules(EntityType<KingEntity> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            if (!KingConfig.NATURAL_SPAWNS.get() || random.nextInt(3) != 0) return false;
            if (!level.getEntitiesOfClass(KingEntity.class, new AABB(pos).inflate(48)).isEmpty()) return false;
        }
        return Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    @SuppressWarnings("deprecation") // NeoForge marks it "override-only": overriding is the intended use, only calling it is deprecated
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        setVariant(random.nextInt(KingData.get().variants.length));
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    /* ───────── Color variant (synced, saved) ───────── */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    public int getVariant() { return entityData.get(VARIANT); }

    public void setVariant(int v) { entityData.set(VARIANT, v); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
    }

    /* ───────── Actions ───────── */
    public boolean isBusy() { return action != Action.NONE; }

    public Action getAction() { return action; }

    private void start(Action a, LivingEntity target, byte event) {
        action = a;
        actionTicks = 0;
        actionTarget = target;
        hitDone = false;
        kickAimed = false;
        setSprinting(false); // actions are not runs
        getNavigation().stop();
        level().broadcastEntityEvent(this, event);
        if (DEBUG) LOG.info("[TheKing] {} start, target {}", a, target == null ? "-" : target.getName().getString());
    }

    private void end() {
        if (action == Action.KICK) setNoGravity(false);
        action = Action.NONE;
        actionTarget = null;
    }

    private static final double PUNCH_EXTRA_REACH = 0.5;

    /** Longest leap = the animation's travel x this (more looks floaty). */
    private static final double KICK_MAX_SCALE = 2.2;

    /** Farthest target (center, blocks) a dropkick can reach: hips travel at the hit + feet 0.85 ahead + target half width. */
    public static double kickReach() {
        if (kickReach < 0) {
            KingData d = KingData.get();
            KingData.Anim a = d.anim("attack");
            kickReach = KingData.Anim.sample(a.rmZ, a.event("hit", 1), d.fps) / 16 * KICK_MAX_SCALE + 0.85 + 0.3;
        }
        return kickReach;
    }
    private static double kickReach = -1;

    /** Dropkick: face the target and size the leap so the feet meet it at the hit moment. */
    public void startKick(LivingEntity target) {
        KingData.Anim a = KingData.get().anim("attack");
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        kickYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        setYRot(kickYaw);
        yBodyRot = kickYaw;
        yHeadRot = kickYaw;
        double hipsAtHit = KingData.Anim.sample(a.rmZ, a.event("hit", 1), KingData.get().fps) / 16; // blocks the hips travel until the hit
        double want = Math.sqrt(dx * dx + dz * dz) - 0.85 - target.getBbWidth() / 2; // feet reach ~0.85 block past the hips
        kickScale = Mth.clamp(want / hipsAtHit, 0.5, KICK_MAX_SCALE);
        kickX = getX(); kickY = getY(); kickZ = getZ();
        setNoGravity(true);
        kickCooldown = 140 + random.nextInt(60);
        start(Action.KICK, target, EV_KICK);
    }

    public void startPunch(LivingEntity target) {
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        setYRot(yaw);
        yBodyRot = yaw;
        punchCooldown = 26 + random.nextInt(8);
        start(Action.PUNCH, target, EV_PUNCH);
    }

    private void startDance() {
        start(Action.DANCE, null, EV_DANCE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) serverTick();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (DEBUG && target != getTarget())
            LOG.info("[TheKing] target {} -> {}", getTarget() == null ? "-" : getTarget().getName().getString(), target == null ? "-" : target.getName().getString());
        super.setTarget(target);
    }

    private void serverTick() {
        // Dev: once a second while fighting, his state (to find why a fight stalls).
        if (DEBUG && tickCount % 20 == 0 && getTarget() != null) {
            LivingEntity tg = getTarget();
            LOG.info("[TheKing] state: target {} dist {} see {} melee {} action {} navDone {} punchCd {} kickCd {}",
                    tg.getName().getString(), String.format("%.2f", distanceTo(tg)), getSensing().hasLineOfSight(tg),
                    isWithinMeleeAttackRange(tg), action, getNavigation().isDone(), punchCooldown, kickCooldown);
        }
        if (kickCooldown > 0) kickCooldown--;
        if (punchCooldown > 0) punchCooldown--;
        if (danceDelay > 0 && --danceDelay == 0) {
            if (action == Action.NONE && isAlive()) startDance();
            else danceDelay = 1; // still busy: dance right after
        }
        if (action == Action.NONE) return;
        actionTicks++;
        KingData data = KingData.get();
        double t = actionTicks / 20.0;
        switch (action) {
            case KICK -> {
                KingData.Anim a = data.anim("attack");
                // Follow the animation's root motion: travel (forward, scaled to the target distance) + jump arc.
                double land = a.event("land", 1.5);
                double z = KingData.Anim.sample(a.rmZ, Math.min(t, a.T), data.fps) / 16 * kickScale;
                double y = t < land ? KingData.Anim.sample(a.rmY, t, data.fps) / 16 : 0;
                float yr = kickYaw * Mth.DEG_TO_RAD;
                double tx = kickX - Mth.sin(yr) * z, tz = kickZ + Mth.cos(yr) * z;
                Vec3 move = new Vec3(tx - getX(), t < land ? kickY + y - getY() : 0, tz - getZ());
                move(MoverType.SELF, move);
                setDeltaMovement(Vec3.ZERO);
                fallDistance = 0;
                setYRot(kickYaw);
                yBodyRot = kickYaw;
                yHeadRot = kickYaw;
                if (t >= land) setNoGravity(false);
                // Takeoff: aim again at where the target will be at the hit (it may have stepped away during the crouch).
                if (!kickAimed && t >= a.event("takeoff", 0.45)) {
                    kickAimed = true;
                    reaimKick(a, z / kickScale, t);
                }
                // Hit window (hit .. hit + 0.15 s), not one tick: a target that moves a little is still hit.
                double hit = a.event("hit", 1);
                if (!hitDone && t >= hit && t <= hit + 0.15) hitDone = kickHit(yr);
                if (DEBUG && !hitDone && t >= hit + 0.15 && t < hit + 0.2 && actionTarget != null)
                    LOG.info("[TheKing] dropkick missed: dist {} dy {} scale {}", String.format("%.2f", distanceTo(actionTarget)),
                            String.format("%.2f", actionTarget.getY() - getY()), String.format("%.2f", kickScale));
                if (t >= a.T) end();
            }
            case PUNCH -> {
                KingData.Anim a = data.anim("attack2");
                LivingEntity tg = actionTarget;
                if (tg != null && tg.isAlive()) getLookControl().setLookAt(tg, 30F, 30F);
                if (!hitDone && t >= a.event("hit", 0.42)) {
                    hitDone = true;
                    // Reach + 0.5 block at the hit: in the animation he steps in and stretches his arm, and in the
                    // 0.42 s wind-up the target often slides just out of the normal reach (punches whiffed).
                    if (tg != null && tg.isAlive() && getAttackBoundingBox().inflate(PUNCH_EXTRA_REACH, 0, PUNCH_EXTRA_REACH).intersects(tg.getHitbox())
                            && getSensing().hasLineOfSight(tg)) {
                        doHurtTarget(tg);
                        if (DEBUG) LOG.info("[TheKing] punch hit {}", tg.getName().getString());
                    } else if (DEBUG && tg != null)
                        LOG.info("[TheKing] punch MISS {}: dist {} melee {} see {}", tg.getName().getString(),
                                String.format("%.2f", distanceTo(tg)), isWithinMeleeAttackRange(tg), getSensing().hasLineOfSight(tg));
                }
                if (t >= a.T) end();
            }
            case DANCE -> {
                if (t >= data.anim("dance").T) end();
            }
            default -> {}
        }
    }

    /** Re-aim at takeoff: turn (max 35°) and re-size the leap toward the target's predicted spot at the hit.
     *  rmNow = blocks travelled so far (unscaled); the start point moves so the King does not jump sideways. */
    private void reaimKick(KingData.Anim a, double rmNow, double t) {
        LivingEntity tg = actionTarget;
        if (tg == null || !tg.isAlive()) return;
        double lead = Math.max(0, a.event("hit", 1) - t) * 20; // ticks until the hit
        Vec3 v = tg.getDeltaMovement();
        double dx = tg.getX() + v.x * lead - getX(), dz = tg.getZ() + v.z * lead - getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
        kickYaw += Mth.clamp(Mth.wrapDegrees(want - kickYaw), -35F, 35F);
        double hipsAtHit = KingData.Anim.sample(a.rmZ, a.event("hit", 1), KingData.get().fps) / 16;
        double left = Math.max(0.01, hipsAtHit - rmNow); // unscaled travel still to come
        double dist = Math.sqrt(dx * dx + dz * dz) - 0.85 - tg.getBbWidth() / 2;
        kickScale = Mth.clamp(dist / left, 0.5, KICK_MAX_SCALE); // remaining travel = left * kickScale = dist
        if (DEBUG) LOG.info("[TheKing] takeoff re-aim: still to go {} blocks, travel left {}, scale {}",
                String.format("%.2f", dist), String.format("%.2f", left), String.format("%.2f", kickScale));
        float yr = kickYaw * Mth.DEG_TO_RAD;
        double done = rmNow * kickScale;
        kickX = getX() + Mth.sin(yr) * done;
        kickZ = getZ() - Mth.cos(yr) * done;
    }

    /** The feet land ~0.85 block in front of the hips at ~1.3 blocks high: hit whoever is there (the target first). */
    private boolean kickHit(float yawRad) {
        Vec3 feet = position().add(-Mth.sin(yawRad) * 0.85, 1.3, Mth.cos(yawRad) * 0.85);
        AABB reach = new AABB(feet, feet).inflate(0.9, 1.0, 0.9);
        LivingEntity tg = actionTarget;
        if (tg == null || !tg.isAlive() || !tg.getBoundingBox().intersects(reach)) return false;
        float damage = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * KingConfig.DROPKICK_DAMAGE.get());
        DamageSource src = damageSources().mobAttack(this);
        if (DEBUG) LOG.info("[TheKing] dropkick hit {}", tg.getName().getString());
        if (tg.hurt(src, damage)) {
            tg.knockback(1.6, Mth.sin(yawRad), -Mth.cos(yawRad));
            tg.setDeltaMovement(tg.getDeltaMovement().add(0, 0.3, 0));
            if (level() instanceof ServerLevel sl) net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(sl, tg, src);
            setLastHurtMob(tg);
        }
        return true; // contact made (even if blocked): one kick hits once
    }

    /* ───────── Kill -> dance 1 second later ───────── */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        if (KingConfig.DANCE_ON_KILL.get()) danceDelay = 20;
        if (DEBUG) LOG.info("[TheKing] killed {}, dance in 1 s", victim.getName().getString());
        return super.killedEntity(level, victim);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        // Hit while dancing: he stops and fights back.
        if (hurt && action == Action.DANCE && source.getEntity() instanceof LivingEntity) {
            end();
            level().broadcastEntityEvent(this, EV_STOP);
        }
        return hurt;
    }

    /* ───────── Voice lines (now and then; the client plays them with lip sync) ───────── */
    @Override
    public int getAmbientSoundInterval() { return 320; }

    @Override
    public void playAmbientSound() {
        if (level().isClientSide || action != Action.NONE || !KingConfig.VOICE_LINES.get()) return;
        int n = KingData.get().voice.length;
        int i = random.nextInt(n);
        if (i == lastVoice) i = (i + 1) % n; // never the same line twice in a row
        lastVoice = i;
        level().broadcastEntityEvent(this, (byte) (EV_VOICE + i));
        if (DEBUG) LOG.info("[TheKing] voice line {}", KingData.get().voice[i].id);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public int getMaxHeadYRot() { return 40; } // the body turns along sooner; the model's own head limit does the rest

    /* ───────── Client side: start the matching animation / sound ───────── */
    @Override
    public void handleEntityEvent(byte id) {
        if (level().isClientSide && (id >= EV_KICK && id <= EV_STOP || id >= EV_VOICE && id < EV_VOICE + 16)) {
            com.custommobs.theking.client.KingClientState.onEvent(this, id);
            return;
        }
        super.handleEntityEvent(id);
    }
}
