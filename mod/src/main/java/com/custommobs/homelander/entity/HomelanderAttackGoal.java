package com.custommobs.homelander.entity;

import com.custommobs.config.SindicateConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/* ───────── Fighting: walk up, then pick an attack ─────────
   3-12 blocks + line of sight: now and then Heat vision.
   Close (the victim spots of the viewer are 0.75-1.1 blocks): a throat grab (Throw or Throat laser), the Smash,
   the Head clap, or a Punch, each with its own cooldown (set in the entity). Farther than 5 blocks he runs. */
public class HomelanderAttackGoal extends Goal {
    private final HomelanderEntity mob;
    private int repath;

    public HomelanderAttackGoal(HomelanderEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive() && !mob.isBusy();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive() && mob.isWithinRestriction(t.blockPosition());
    }

    @Override
    public void start() {
        mob.setAggressive(true);
        repath = 0;
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.setSprinting(false);
        mob.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null || mob.isBusy()) return;
        mob.getLookControl().setLookAt(t, 30F, 30F);
        double dx = t.getX() - mob.getX(), dz = t.getZ() - mob.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz), dy = Math.abs(t.getY() - mob.getY());
        boolean see = mob.getSensing().hasLineOfSight(t);
        var r = mob.getRandom();
        if (HomelanderEntity.DEBUG && mob.tickCount % 20 == 10)
            HomelanderEntity.LOG.info("[Homelander] goal: dist {} dy {} see {} tags {}", String.format("%.2f", dist), String.format("%.2f", dy), see, mob.getTags());
        // Dev test hook (theking.debug runs only): a datapack tag hl_punch / hl_laser / hl_throw / hl_clap / hl_burn
        // forces that attack as soon as the target is in its range.
        if (HomelanderEntity.DEBUG && forced(t, dist, dy, see)) return;
        // forced one not in range yet, or hl_hold (no random attacks during a scripted test)
        // (hl_fight / hl_player = a free fight: random attacks allowed)
        boolean waiting = HomelanderEntity.DEBUG && mob.getTags().stream().anyMatch(s -> s.startsWith("hl_") && !s.equals("hl_fight") && !s.equals("hl_player"));
        // Heat vision from a distance (needs a clear line from his eyes).
        if (!waiting && SindicateConfig.HL_HEAT_VISION.get() && mob.laserCooldown <= 0 && see && dist >= 3 && dist <= SindicateConfig.HL_HEAT_RANGE.get() && r.nextInt(15) == 0) {
            mob.laserCooldown = 140 + r.nextInt(60);
            mob.start(HomelanderEntity.Action.LASER, t, HomelanderEntity.EV_LASER);
            return;
        }
        // Close: the victim stands where the viewer's dummy stands (0.75-1.1 blocks from him).
        boolean close = see && dy < 1.2 && dist <= 1.5 + t.getBbWidth() / 2;
        if (close && !waiting) {
            boolean throwOk = SindicateConfig.HL_THROW.get(), burnOk = SindicateConfig.HL_BURN.get() && SindicateConfig.HL_HEAT_VISION.get();
            if (mob.grabCooldown <= 0 && (throwOk || burnOk) && mob.canGrab(t) && r.nextInt(3) == 0) {
                mob.grabCooldown = 160 + r.nextInt(80);
                mob.punchCooldown = Math.max(mob.punchCooldown, 20);
                boolean burn = burnOk && (!throwOk || r.nextBoolean());
                mob.start(burn ? HomelanderEntity.Action.BURN : HomelanderEntity.Action.THROW, t, burn ? HomelanderEntity.EV_BURN : HomelanderEntity.EV_THROW);
                return;
            }
            if (SindicateConfig.HL_SMASH.get() && mob.smashCooldown <= 0 && mob.canGrab(t) && r.nextInt(4) == 0) {
                mob.smashCooldown = 320 + r.nextInt(120);
                mob.punchCooldown = Math.max(mob.punchCooldown, 20);
                mob.start(HomelanderEntity.Action.SMASH, t, HomelanderEntity.EV_SMASH);
                return;
            }
            if (SindicateConfig.HL_CLAP.get() && mob.clapCooldown <= 0 && r.nextInt(4) == 0) {
                mob.clapCooldown = 300 + r.nextInt(120);
                mob.punchCooldown = Math.max(mob.punchCooldown, 20);
                mob.start(HomelanderEntity.Action.CLAP, t, HomelanderEntity.EV_CLAP);
                return;
            }
            if (mob.punchCooldown <= 0 && SindicateConfig.HL_PUNCH.get()) {
                mob.punchCooldown = 22 + r.nextInt(10);
                mob.start(HomelanderEntity.Action.PUNCH, t, HomelanderEntity.EV_PUNCH);
                return;
            }
        }
        // Far away: he RUNS (sprinting flag: the clients play Run), up to homelander.runSpeed blocks/s; close: walks in.
        boolean run = dist > 5;
        double speed = run ? runModifier() : 1.0;
        if (run != mob.isSprinting()) { mob.setSprinting(run); repath = 0; }
        // Path all the way to the target (accuracy 0): moveTo(entity) stops ~1.5 blocks away, outside his reach.
        if (--repath <= 0) {
            repath = 4 + r.nextInt(7);
            Path path = mob.getNavigation().createPath(t, 0);
            if (path != null) mob.getNavigation().moveTo(path, speed);
        }
        // Path done but not close enough (target on the next block): the last step straight at it.
        if (dist < 3 && mob.getNavigation().isDone() && !close)
            mob.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.0);
    }

    /** Speed modifier for running at homelander.runSpeed blocks/s. A mob's ground speed grows with the square of its
     *  speed (MoveControl feeds it in twice): blocks/s ~ RUN_K x (attribute x modifier)^2, RUN_K measured in game. */
    static final double RUN_K = 48;   // dev test 2026-10-16: at K 58.4 he ran 8.1-9.5 b/s for 11 wanted
    private double runModifier() {
        double attr = mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        return Math.sqrt(SindicateConfig.HL_RUN_SPEED.get() / RUN_K) / Math.max(0.05, attr);
    }

    private boolean forced(LivingEntity t, double dist, double dy, boolean see) {
        boolean close = see && dy < 1.2 && dist <= 1.5 + t.getBbWidth() / 2;
        for (HomelanderEntity.Action a : HomelanderEntity.Action.values()) {
            String tag = "hl_" + a.name().toLowerCase(java.util.Locale.ROOT);
            if (a == HomelanderEntity.Action.NONE || !mob.getTags().contains(tag)) continue;
            boolean ok = a == HomelanderEntity.Action.LASER ? see && dist >= 2.5 : close && (a == HomelanderEntity.Action.PUNCH
                    || a == HomelanderEntity.Action.CLAP || mob.canGrab(t));
            if (!ok) return false;
            mob.removeTag(tag);
            byte ev = switch (a) {
                case PUNCH -> HomelanderEntity.EV_PUNCH;
                case LASER -> HomelanderEntity.EV_LASER;
                case THROW -> HomelanderEntity.EV_THROW;
                case CLAP -> HomelanderEntity.EV_CLAP;
                case SMASH -> HomelanderEntity.EV_SMASH;
                default -> HomelanderEntity.EV_BURN;
            };
            mob.start(a, t, ev);
            return true;
        }
        return false;
    }
}
