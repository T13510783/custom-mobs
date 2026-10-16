package com.custommobs.homelander.entity;

import com.custommobs.config.SindicateConfig;
import com.custommobs.homelander.anim.HomelanderAnimator;
import com.custommobs.homelander.anim.HomelanderData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/* ───────── Homelander (server + shared logic) ─────────
   Hostile to players, villagers and (by default) every other mob. Five attacks, each one animation of the viewer:
     Punch (attack)              close: one heavy punch, blood, knockback
     Heat vision (special)       3-12 blocks: laser beams from his eyes, burns and sets on fire
     Throw (throw)               grabs the victim by the throat, lifts it, flings it high into the air
     Head clap (clap)            shouts "I am god" and smacks both ears: the victim grabs its ears and falls over screaming
     Throat laser (throatLaser)  grabs by the throat and burns the victim's face with heat vision, then drops it
   Every attack starts the same animation on all clients through an entity event. The server runs its own copy
   of the animator (no cape) so a held victim hangs from his real hand and the beams start at his real eyes.
   A held victim rides him (vanilla passenger): positionRider() puts its neck on his fist every
   tick, on both sides. The victim of a clap is synced (VICTIM): clients lay it down (HomelanderVictims).
   Chasing far: he runs (sprinting flag = the client plays Run), up to homelander.runSpeed blocks/s.
   Punch and heat vision: he keeps walking at the target (homelander.attackWhileWalking; the client layers the Walk). */
public class HomelanderEntity extends Monster {
    /* ───────── Entity events (server -> clients) ───────── */
    public static final byte EV_PUNCH = 100, EV_LASER = 101, EV_THROW = 102, EV_CLAP = 103, EV_BURN = 104, EV_STOP = 105;

    // Dev runs only (gradlew runClient sets -Dtheking.debug=true): actions go to the log, for testing.
    public static final boolean DEBUG = com.custommobs.Sindicate.DEBUG;
    public static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(HomelanderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LASER_TARGET = SynchedEntityData.defineId(HomelanderEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VICTIM = SynchedEntityData.defineId(HomelanderEntity.class, EntityDataSerializers.INT);

    public enum Action {
        NONE(""), PUNCH("attack"), LASER("special"), THROW("throw"), CLAP("clap"), BURN("throatLaser");
        public final String anim;
        Action(String anim) { this.anim = anim; }
    }

    private Action action = Action.NONE;
    private int actionTicks;
    private LivingEntity actionTarget;
    private boolean hitDone, victimHit;
    private float actionYaw;
    int punchCooldown = 10, laserCooldown = 60, grabCooldown = 40, clapCooldown = 60;   // ticks until the first of each

    // Held victim (throw / throat laser): where it was when grabbed, and when (anim seconds), for the lift to the hand.
    private double grabX, grabY, grabZ, grabAt = -1;
    private double dbgX, dbgZ;   // dev log: position one second ago
    private double pinX = Double.NaN, pinZ;   // where a lying victim (Clap) is held (NaN = not yet)
    private @Nullable Player flingPlayer;     // a player flung last tick: its fling is sent once more (see serverTick)
    private Vec3 flingVel = Vec3.ZERO;

    /** The server's animator (body only); the client has its own in clientState. */
    private HomelanderAnimator serverAnim;
    /** Client-side animation state (HomelanderClientState); never touched on the server. */
    public Object clientState;
    /** Client: returns the client animator (set by the client state, so this class stays free of client classes). */
    public java.util.function.Supplier<HomelanderAnimator> clientAnim;

    private static final double DEG = Math.PI / 180, TEXEL = 1 / 16.0;

    public HomelanderEntity(EntityType<? extends HomelanderEntity> type, Level level) {
        super(type, level);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 75)      // config homelander.maxHealth / armor / armorToughness override these per world
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.26)  // ~3 blocks/s when chasing: his walk animation speeds up to match
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 20)           // a full diamond set
                .add(Attributes.ARMOR_TOUGHNESS, 8)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HomelanderBusyGoal(this));
        goalSelector.addGoal(2, new HomelanderAttackGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        // Neutral by default: he only fights whoever hits him (homelander.neutral). Off = he also hunts (huntPlayers / huntVillagers / huntMobs).
        targetSelector.addGoal(1, new HurtByTargetGoal(this, HomelanderEntity.class));
        // Dev test hook (theking.debug runs only): a datapack hl_* tag (not hl_hold) = fight the nearest mob, hl_player = the player
        if (DEBUG) targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 1, false, false,
                e -> !(e instanceof HomelanderEntity) && (e instanceof Player ? getTags().contains("hl_player")
                        : getTags().stream().anyMatch(s -> s.startsWith("hl_") && !s.equals("hl_hold") && !s.equals("hl_player")))));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, e -> !SindicateConfig.HL_NEUTRAL.get() && SindicateConfig.HL_HUNT_PLAYERS.get()));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> !SindicateConfig.HL_NEUTRAL.get() && (e instanceof AbstractVillager || e instanceof IronGolem) && SindicateConfig.HL_HUNT_VILLAGERS.get()));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 20, true, false,
                e -> !SindicateConfig.HL_NEUTRAL.get() && SindicateConfig.HL_HUNT_MOBS.get() && !(e instanceof HomelanderEntity) && !(e instanceof AbstractVillager) && !(e instanceof IronGolem)));
    }

    /* ───────── Spawning ───────── */
    /** Natural spawns: any light, very rare, never another Homelander within 64 blocks. */
    public static boolean checkHomelanderSpawnRules(EntityType<HomelanderEntity> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            if (!SindicateConfig.HL_NATURAL_SPAWNS.get() || random.nextDouble() >= SindicateConfig.HL_SPAWN_CHANCE.get()) return false;
            if (!level.getEntitiesOfClass(HomelanderEntity.class, new AABB(pos).inflate(64)).isEmpty()) return false;
        }
        return Monster.checkAnyLightMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    @SuppressWarnings("deprecation") // NeoForge marks it "override-only": overriding is the intended use
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
        setVariant(random.nextInt(HomelanderData.get().variants.length));
        applyConfig();
        setHealth(getMaxHealth());
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    /* ───────── Synced data: color variant (saved), heat-vision target ───────── */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(LASER_TARGET, -1);
        builder.define(VICTIM, -1);
    }

    /** Entity id of the victim of his clap (clients lay it down), or -1. */
    public int getVictimId() { return entityData.get(VICTIM); }

    public int getVariant() { return entityData.get(VARIANT); }

    public void setVariant(int v) { entityData.set(VARIANT, v); }

    /** The entity his heat vision aims at (client: the beams end on it), or null. */
    @Nullable
    public Entity getLaserTarget() {
        int id = entityData.get(LASER_TARGET);
        return id < 0 ? null : level().getEntity(id);
    }

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

    /** A victim he can lift by the throat (or by the feet): not too big, not riding anything, not a boss. */
    public boolean canGrab(LivingEntity e) {
        return e.isAlive() && (SindicateConfig.HL_GRAB_PLAYERS.get() || !(e instanceof Player)) && !(e instanceof HomelanderEntity) && !(e instanceof EnderDragon) && !(e instanceof WitherBoss)
                && e.getBbHeight() <= 2.3F && e.getBbWidth() <= 1.2F && !e.isPassenger() && !e.isVehicle()
                && !(e instanceof Player p && p.isSpectator());   // creative players too: they see every attack (no damage)
    }

    public boolean isHolding(Entity e) {
        return (action == Action.THROW || action == Action.BURN) && e != null && hasPassenger(e);
    }

    private boolean lettingGo;   // true while he releases a victim himself (the dismount block below must not stop that)

    /** The victim may not climb off (EntityMountEvent, HomelanderMod) - except when he lets go himself. */
    public boolean keepsHold(Entity e) { return !lettingGo && isHolding(e); }

    /** He lets go of a held victim (release / end of an attack). */
    private void letGo(Entity e) {
        lettingGo = true;
        try { e.stopRiding(); } finally { lettingGo = false; }
    }

    void start(Action a, LivingEntity target, byte event) {
        action = a;
        actionTicks = 0;
        actionTarget = target;
        hitDone = false;
        victimHit = false;
        grabAt = -1;
        pinX = Double.NaN;
        faceTarget(target, 360F);
        actionYaw = getYRot();
        getNavigation().stop();
        // also drop the chase's last MoveControl step (setWantedPosition): it kept him walking into the victim,
        // shoving it out of reach. Only Punch / Heat vision walk on (walkAt).
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0);
        setZza(0); setXxa(0); setSpeed(0);
        setSprinting(false);
        anim().switchAnim(a.anim);
        if (a == Action.LASER) entityData.set(LASER_TARGET, target.getId());
        entityData.set(VICTIM, a == Action.CLAP ? target.getId() : -1);
        level().broadcastEntityEvent(this, event);
        if (DEBUG) LOG.info("[Homelander] {} start, target {} at {} blocks", a, target.getName().getString(), String.format("%.2f", distanceTo(target)));
    }

    private void end() {
        if (action == Action.NONE) return;
        for (Entity p : java.util.List.copyOf(getPassengers())) letGo(p);
        entityData.set(LASER_TARGET, -1);
        entityData.set(VICTIM, -1);
        setSprinting(false);
        action = Action.NONE;
        actionTarget = null;
        anim().switchAnim("idle");
    }

    /** Turn the body (and head) toward the target, at most maxDeg per call. */
    private void faceTarget(@Nullable LivingEntity tg, float maxDeg) {
        if (tg == null) return;
        float want = (float) (Mth.atan2(tg.getZ() - getZ(), tg.getX() - getX()) * Mth.RAD_TO_DEG) - 90F;
        float yaw = getYRot() + Mth.clamp(Mth.wrapDegrees(want - getYRot()), -maxDeg, maxDeg);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    private void lockYaw() {
        setYRot(actionYaw);
        yBodyRot = actionYaw;
        yHeadRot = actionYaw;
    }

    /** The server's animator: stepped every tick, so hands and eyes are where the clients draw them. */
    private HomelanderAnimator anim() {
        if (serverAnim == null) {
            serverAnim = new HomelanderAnimator(HomelanderData.get(), getUUID().hashCode());
            serverAnim.clothOn = false;   // the cape is only looks
            serverAnim.eventsOn = false;  // no random Hover / Idle 2 on the server (actions crossfade from any pose)
            serverAnim.followOn = false;
        }
        return serverAnim;
    }

    /** The animator that poses him on this side (client: the drawn one; server: its own). */
    private HomelanderAnimator poseAnim() {
        if (level().isClientSide && clientAnim != null) return clientAnim.get();
        return anim();
    }

    private double damage(double times) {
        return getAttributeValue(Attributes.ATTACK_DAMAGE) * times * SindicateConfig.HL_DAMAGE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) serverTick();
    }

    /* ───────── Settings that change his stats (server config, live: checked every 2 s) ───────── */
    private void applyConfig() {
        setBase(Attributes.MAX_HEALTH, SindicateConfig.HL_HEALTH.get());
        setBase(Attributes.ARMOR, SindicateConfig.HL_ARMOR.get());
        setBase(Attributes.ARMOR_TOUGHNESS, SindicateConfig.HL_TOUGHNESS.get());
        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    private void setBase(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a, double v) {
        var inst = getAttribute(a);
        if (inst != null && inst.getBaseValue() != v) inst.setBaseValue(v);
    }

    /* ───────── Boss bar while he fights (homelander.bossBar) ───────── */
    private final ServerBossEvent bossBar = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bossBar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bossBar.removePlayer(p);
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        bossBar.setName(getDisplayName());
    }

    private void serverTick() {
        if (tickCount % 40 == 1) applyConfig();
        bossBar.setVisible(SindicateConfig.HL_BOSS_BAR.get() && getTarget() != null && isAlive());
        bossBar.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0F, 1F));
        if (punchCooldown > 0) punchCooldown--;
        if (laserCooldown > 0) laserCooldown--;
        if (grabCooldown > 0) grabCooldown--;
        if (clapCooldown > 0) clapCooldown--;
        HomelanderAnimator a = anim();
        a.worldX = getX(); a.worldY = getY(); a.worldZ = getZ();
        a.worldYaw = yBodyRot * DEG;
        a.step(0.05, 1);
        if (DEBUG && tickCount % 20 == 0) {
            // ground speed over the last second (blocks/s): calibrates the run (HomelanderAttackGoal.RUN_K)
            double sp = Math.sqrt((getX() - dbgX) * (getX() - dbgX) + (getZ() - dbgZ) * (getZ() - dbgZ));
            dbgX = getX(); dbgZ = getZ();
            if (getTarget() != null)
                LOG.info("[Homelander] state: target {} dist {} action {} speed {} sprint {} riders {} cds p{} l{} g{} c{}", getTarget().getName().getString(),
                        String.format("%.2f", distanceTo(getTarget())), action, String.format("%.2f", sp), isSprinting(), getPassengers().size(),
                        punchCooldown, laserCooldown, grabCooldown, clapCooldown);
        }
        // a flung player: send the fling again once its client has handled the dismount teleport (the first one gets lost)
        if (flingPlayer != null) {
            if (flingPlayer.isAlive() && !flingPlayer.isPassenger()) {
                flingPlayer.setDeltaMovement(flingVel);
                flingPlayer.hurtMarked = true;
            }
            flingPlayer = null;
        }
        if (action == Action.NONE) return;
        actionTicks++;
        HomelanderData.Anim A = HomelanderData.get().anim(action.anim);
        double t = actionTicks / 20.0;
        LivingEntity tg = actionTarget;
        boolean alive = tg != null && tg.isAlive() && !tg.isRemoved();
        switch (action) {
            case PUNCH -> {
                if (alive && t < A.event("hit", 0.65)) faceTarget(tg, 20F);
                if (alive && t < A.event("hit", 0.65)) walkAt(tg, 1.0 + tg.getBbWidth() / 2, 1.0);   // closes in while winding up
                if (!hitDone && t >= A.event("hit", 0.65)) {
                    hitDone = true;
                    // The fist reaches ~1.1 blocks at the hit; + a little extra (the target slides away during the wind-up).
                    if (alive && distanceTo(tg) < 2.0 + tg.getBbWidth() / 2 && getSensing().hasLineOfSight(tg)) {
                        hurtTarget(tg, damage(1.0), 1.1, 0.25);
                        blood(tg.getX(), tg.getY() + tg.getBbHeight() * 0.65, tg.getZ(), 10);
                    }
                }
            }
            case LASER -> {
                if (alive) faceTarget(tg, 6F); // the beams sweep after a target that walks away
                if (alive) walkAt(tg, 4.0, 0.55); // slow, menacing walk toward the target while burning it
                if (t >= A.event("laserOn", 0.85) && t <= A.event("laserOff", 2.75)) laserTick(tg, alive);
            }
            case THROW -> {
                if (grabAt < 0 && t < A.event("grab", 0.42) && alive) faceTarget(tg, 30F); else lockYaw();
                if (grabAt < 0 && !hitDone && t >= A.event("grab", 0.42)) grab(tg, alive, t);
                if (isHolding(tg) && t >= A.event("release", 1.52)) {
                    // Flung straight up from the hand, like the viewer's dummy (THROW_VY blocks/s; it lands on its own).
                    letGo(tg);
                    double[] h = hangPoint(tg);
                    tg.teleportTo(h[0], h[1], h[2]);
                    tg.setDeltaMovement(0, HomelanderData.get().throwVy / 20.0 * SindicateConfig.HL_THROW_POWER.get(), 0);
                    if (tg instanceof Player p) { flingPlayer = p; flingVel = tg.getDeltaMovement(); }
                    tg.hurtMarked = true;  // players: send the new speed
                    tg.fallDistance = 0;
                    if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, h[0], h[1], h[2], 12, 0.2, 0.1, 0.2, 0.05);
                }
            }
            case CLAP -> {
                double hitT = A.event("hit", 1.2);
                if (alive && t < hitT) {
                    faceTarget(tg, 20F);
                    // Frozen by his stare while he shouts (otherwise every victim walks away before the smack).
                    if (actionTicks % 10 == 1) tg.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 5, false, false));
                }
                // After the smack it falls over and lies there screaming (clients lay it down): held still, hurt sounds.
                if (hitDone && alive && victimHit) {
                    double u = t - hitT;
                    if (actionTicks % 10 == 1) tg.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 6, false, false));
                    if (u > 0.3 && u < 2.4 && actionTicks % 14 == 0) scream(tg);
                    // staggers back first (knockback), then lies where it fell until it gets up (clients: hit+0.95 .. hit+3.4)
                    if (u >= 0.9 && u < 3.4) {
                        if (pinX != pinX) { pinX = tg.getX(); pinZ = tg.getZ(); }   // NaN = not pinned yet
                        pinDown(tg);
                    }
                }
                if (!hitDone && t >= hitT) {
                    hitDone = true;
                    victimHit = alive && distanceTo(tg) < 1.9 + tg.getBbWidth() / 2;
                    if (victimHit) {
                        hurtTarget(tg, damage(1.5), 0.15, 0); // staggers back (viewer: 7 texels, ~0.4 block), then the effects
                        tg.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                        tg.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
                        tg.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                        // blood from both ears + a small shockwave at the head
                        double hy = tg.getY() + tg.getBbHeight() * 0.85;
                        float yr = getYRot() * Mth.DEG_TO_RAD;
                        double sx = Mth.cos(yr) * tg.getBbWidth() * 0.6, sz = Mth.sin(yr) * tg.getBbWidth() * 0.6;
                        blood(tg.getX() + sx, hy, tg.getZ() + sz, 8);
                        blood(tg.getX() - sx, hy, tg.getZ() - sz, 8);
                        if (level() instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.EXPLOSION, tg.getX(), hy, tg.getZ(), 1, 0, 0, 0, 0);
                            sl.sendParticles(ParticleTypes.CRIT, tg.getX(), hy, tg.getZ(), 16, 0.3, 0.2, 0.3, 0.4);
                        }
                    }
                }
            }
            case BURN -> {
                if (grabAt < 0 && t < A.event("grab", 0.45) && alive) faceTarget(tg, 30F); else lockYaw();
                if (grabAt < 0 && !hitDone && t >= A.event("grab", 0.45)) grab(tg, alive, t);
                if (isHolding(tg) && t >= A.event("burnOn", 1.6) && t <= A.event("burnOff", 2.6)) {
                    if (actionTicks % 5 == 0) {
                        tg.invulnerableTime = 0;
                        hurtNoKnock(tg, damage(0.4));
                        tg.igniteForSeconds(4);
                    }
                    if (level() instanceof ServerLevel sl) {
                        double fy = tg.getY() + tg.getBbHeight() * 0.85;
                        sl.sendParticles(ParticleTypes.FLAME, tg.getX(), fy, tg.getZ(), 2, 0.12, 0.1, 0.12, 0.01);
                        sl.sendParticles(ParticleTypes.SMOKE, tg.getX(), fy + 0.2, tg.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                    }
                }
                if (isHolding(tg) && t >= A.event("release", 2.85)) {
                    letGo(tg);
                    double[] h = hangPoint(tg);
                    tg.teleportTo(h[0], h[1], h[2]);
                    float yr = getYRot() * Mth.DEG_TO_RAD;
                    tg.setDeltaMovement(-Mth.sin(yr) * 0.25, 0.05, Mth.cos(yr) * 0.25); // dropped, a little shove away
                    tg.hurtMarked = true;
                }
            }
            default -> {}
        }
        if (t >= A.T || (!alive && action != Action.LASER && t > 0.2 && !isVehicle())) end();
    }

    /** A lying victim stays where it fell: its own AI (a panicking villager, a fleeing mob) may not walk it away. */
    private void pinDown(LivingEntity tg) {
        if (tg instanceof Mob m) {
            m.getNavigation().stop();
            m.getMoveControl().setWantedPosition(m.getX(), m.getY(), m.getZ(), 0);
            m.setZza(0); m.setXxa(0); m.setSpeed(0); m.setJumping(false);
            if (pinX == pinX) m.setPos(pinX, m.getY(), pinZ);
        }
        Vec3 v = tg.getDeltaMovement();
        tg.setDeltaMovement(0, Math.min(v.y, 0), 0);
        if (tg instanceof Player) tg.hurtMarked = true;
    }

    /** A screaming victim: a small hurt (vanilla hurt sound + red flash) without knockback. */
    private void scream(LivingEntity tg) {
        tg.invulnerableTime = 0;
        hurtNoKnock(tg, 0.5);
    }

    /** Attack while walking (homelander.attackWhileWalking): keep stepping at the target until `stop` blocks from it. */
    private void walkAt(LivingEntity tg, double stop, double speed) {
        if (!SindicateConfig.HL_ATTACK_WHILE_WALKING.get() || distanceTo(tg) <= stop) return;
        getMoveControl().setWantedPosition(tg.getX(), tg.getY(), tg.getZ(), speed);
    }

    /** Throat grab: the victim rides him from now on (positionRider hangs it on his fist). */
    private void grab(LivingEntity tg, boolean alive, double t) {
        hitDone = true;
        if (!alive || distanceTo(tg) > 2.6 || !canGrab(tg) || !getSensing().hasLineOfSight(tg)) {
            if (DEBUG) LOG.info("[Homelander] grab missed: dist {}", tg == null ? "-" : String.format("%.2f", distanceTo(tg)));
            return;
        }
        grabX = tg.getX(); grabY = tg.getY(); grabZ = tg.getZ(); grabAt = t;
        tg.startRiding(this, true);
        tg.invulnerableTime = 0;
        hurtNoKnock(tg, damage(0.25));
        if (DEBUG) LOG.info("[Homelander] grabbed {}", tg.getName().getString());
    }

    /** Heat vision, one tick: from his eyes to the target (or the first block in the way). */
    private void laserTick(@Nullable LivingEntity tg, boolean alive) {
        if (!(level() instanceof ServerLevel sl)) return;
        HomelanderAnimator a = anim();
        HomelanderData d = a.d;
        int head = d.bone("head");
        double[] e = new double[3];
        a.toWorld(head, 0, d.eyeY, d.beamZ, e); // between the eyes
        Vec3 from = new Vec3(e[0], e[1], e[2]);
        Vec3 to;
        if (alive) to = new Vec3(tg.getX(), tg.getY() + tg.getBbHeight() * 0.6, tg.getZ());
        else {
            double[] dir = new double[3];
            a.dirToWorld(head, 0, 0, 1, dir);
            to = from.add(dir[0] * 12, dir[1] * 12, dir[2] * 12);
        }
        BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        boolean reaches = alive && hit.getType() == HitResult.Type.MISS;
        if (reaches) {
            if (actionTicks % 10 == 0) {
                tg.invulnerableTime = 0;
                hurtNoKnock(tg, damage(0.45));
            }
            tg.igniteForSeconds(5);
        } else if (hit.getType() == HitResult.Type.BLOCK && actionTicks % 20 == 0 && SindicateConfig.HL_LASER_FIRE.get()
                && sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            BlockPos fire = hit.getBlockPos().relative(hit.getDirection());
            if (level().getBlockState(fire).isAir() && hit.getDirection() != Direction.DOWN)
                level().setBlockAndUpdate(fire, BaseFireBlock.getState(level(), fire));
        }
        // impact: sparks, smoke and a little lava spit where the beams end
        sl.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 3, 0.08, 0.08, 0.08, 0.02);
        sl.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.01);
        if (actionTicks % 4 == 0) sl.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 1, 0.05, 0.05, 0.05, 0);
    }

    /** Damage without vanilla's own knockback (hurt() pushes 0.4 away from the attacker): burns and grabs keep it in place. */
    private boolean hurtNoKnock(LivingEntity tg, double amount) {
        Vec3 v = tg.getDeltaMovement();
        boolean hurt = tg.hurt(damageSources().mobAttack(this), (float) amount);
        tg.setDeltaMovement(v);
        tg.hurtMarked = true;
        return hurt;
    }

    /** Hurt + knockback away from him (strength, instead of vanilla's), + a little lift. */
    private void hurtTarget(LivingEntity tg, double amount, double knock, double lift) {
        DamageSource src = damageSources().mobAttack(this);
        if (hurtNoKnock(tg, amount)) {
            float yr = getYRot() * Mth.DEG_TO_RAD;
            tg.knockback(knock, Mth.sin(yr), -Mth.cos(yr));
            tg.setDeltaMovement(tg.getDeltaMovement().add(0, lift, 0));
            if (level() instanceof ServerLevel sl) net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(sl, tg, src);
            setLastHurtMob(tg);
        }
    }

    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.56F, 0.07F, 0.09F), 1.2F);

    private void blood(double x, double y, double z, int n) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(BLOOD, x, y, z, n, 0.12, 0.12, 0.12, 0.05);
    }

    /* ───────── Held victim: hangs by the neck from his fist (viewer hangFrom) ───────── */

    /** Feet position (blocks) of a victim held at his fist this tick, lifted from where it stood over GRAB_LIFT. */
    double[] hangPoint(Entity v) {
        HomelanderAnimator a = poseAnim();
        HomelanderData d = a.d;
        double[] h = new double[3];
        a.worldX = getX(); a.worldY = getY(); a.worldZ = getZ();
        a.worldYaw = yBodyRot * DEG;
        a.toWorld(d.bone("forearm_right"), d.hand[0], d.hand[1], d.hand[2], h);
        // out from his body (horizontal), so the victim's throat touches the fist and its body never passes through his
        double ox = h[0] - getX(), oz = h[2] - getZ(), len = Math.sqrt(ox * ox + oz * oz);
        if (len < 1e-4) { ox = -Mth.sin(yBodyRot * Mth.DEG_TO_RAD); oz = Mth.cos(yBodyRot * Mth.DEG_TO_RAD); len = 1; }
        double out = (2.4 + v.getBbWidth() * 16 / 5) * TEXEL;   // fist half width + room + body half depth (viewer HANG_OUT)
        double neck = v.getBbHeight() * 0.75;                   // top of the body (viewer DUMMY_NECK: 22.5 of 30 texels)
        h[0] += ox / len * out;
        h[1] += d.hangUp * TEXEL - neck;
        h[2] += oz / len * out;
        // lift from where it stood to the hand (viewer GRAB_LIFT, smooth)
        double u = grabAt >= 0 ? Mth.clamp((a.t - grabAt) / d.grabLift, 0, 1) : 1;  // a.t = seconds into the attack
        if (grabAt >= 0 && u < 1) {
            double w = u * u * (3 - 2 * u);
            h[0] = grabX + (h[0] - grabX) * w; h[1] = grabY + (h[1] - grabY) * w; h[2] = grabZ + (h[2] - grabZ) * w;
        }
        return h;
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (level().isClientSide) {
            // the client's lift starts here too: where the victim stood, at the client's animation time
            grabX = passenger.getX(); grabY = passenger.getY(); grabZ = passenger.getZ();
            HomelanderAnimator a = poseAnim();
            grabAt = a.t;
            String n = a.anim.name;
            action = n.equals("throw") ? Action.THROW : Action.BURN;
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (level().isClientSide) { grabAt = -1; action = Action.NONE; }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        if (!hasPassenger(passenger)) return;
        double[] h = hangPoint(passenger);
        move.accept(passenger, h[0], h[1], h[2]);
        passenger.fallDistance = 0;
        // the victim faces him (players turn their own head)
        if (passenger instanceof LivingEntity le && !(passenger instanceof Player)) {
            float yaw = yBodyRot + 180F;
            le.setYRot(yaw);
            le.yBodyRot = yaw;
            le.yHeadRot = yaw;
        }
    }

    @Override
    public boolean shouldRiderSit() { return false; } // held by the throat: legs dangle

    /** A held mob never steers him (vanilla: a Mob passenger takes over the vehicle's movement and switches off his AI goals). */
    @Override
    public @Nullable LivingEntity getControllingPassenger() { return null; }

    @Override
    protected boolean canAddPassenger(Entity passenger) { return getPassengers().isEmpty(); }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) { return false; } // he flies

    @Override
    public boolean removeWhenFarAway(double distance) { return !SindicateConfig.HL_PERSISTENT.get(); }

    /* ───────── Sounds ───────── */
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public int getMaxHeadYRot() { return 40; }

    /* ───────── Client side: start the matching animation ───────── */
    @Override
    public void handleEntityEvent(byte id) {
        if (level().isClientSide && id >= EV_PUNCH && id <= EV_STOP) {
            com.custommobs.homelander.client.HomelanderClientState.onEvent(this, id);
            return;
        }
        super.handleEntityEvent(id);
    }
}
