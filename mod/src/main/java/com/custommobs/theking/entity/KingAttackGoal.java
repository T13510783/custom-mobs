package com.custommobs.theking.entity;

import com.custommobs.theking.KingConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/* ───────── Fighting: run up, punch when close, dropkick from 3 blocks up to his leap reach (~5) ───────── */
public class KingAttackGoal extends Goal {
    private final KingEntity king;
    private int repath;

    public KingAttackGoal(KingEntity king) {
        this.king = king;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity t = king.getTarget();
        return t != null && t.isAlive() && !king.isBusy();
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity t = king.getTarget();
        return t != null && t.isAlive() && king.isWithinRestriction(t.blockPosition());
    }

    @Override
    public void start() {
        king.setAggressive(true);
        repath = 0;
    }

    @Override
    public void stop() {
        king.setAggressive(false);
        king.setSprinting(false);
        king.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        LivingEntity t = king.getTarget();
        if (t == null || king.isBusy()) return;
        king.getLookControl().setLookAt(t, 30F, 30F);
        double dx = t.getX() - king.getX(), dz = t.getZ() - king.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz), dy = Math.abs(t.getY() - king.getY());
        boolean see = king.getSensing().hasLineOfSight(t);
        // Dropkick: on the ground, clear path, not too often (cooldown + chance per tick).
        if (KingConfig.DROPKICK.get() && king.kickCooldown <= 0 && see && king.onGround() && dist >= 3.0 && dist <= KingEntity.kickReach() - 0.25 && dy < 1.5
                && king.getRandom().nextInt(5) == 0 && pathClear(dx / dist, dz / dist, dist)) {
            king.startKick(t);
            return;
        }
        if (king.punchCooldown <= 0 && see && king.isWithinMeleeAttackRange(t)) {
            king.startPunch(t);
            return;
        }
        // Run (sprint: +30% speed, synced to the client, which plays Run) while far; walk the last few blocks.
        // Two limits so he does not flip between run and walk at one distance.
        if (dist > 4.5) king.setSprinting(true);
        else if (dist < 3.2) king.setSprinting(false);
        // Path all the way to the target (accuracy 0, like vanilla melee mobs). moveTo(entity) uses accuracy 1:
        // that path ends ~1.5 blocks away, just outside his punch reach (1.48), and he stood there doing nothing.
        if (--repath <= 0) {
            repath = 4 + king.getRandom().nextInt(7);
            Path path = king.getNavigation().createPath(t, 0);
            if (path != null) king.getNavigation().moveTo(path, 1.0);
        }
        // Path finished but still not in reach (target on the next block): take the last step straight at it.
        if (dist < 3 && king.getNavigation().isDone() && !king.isWithinMeleeAttackRange(t))
            king.getMoveControl().setWantedPosition(t.getX(), t.getY(), t.getZ(), 1.0);
    }

    /* Room for the leap: the body box, raised half a block, free at a few points on the way. */
    private boolean pathClear(double nx, double nz, double dist) {
        AABB box = king.getBoundingBox();
        for (double f = 0.25; f <= 0.76; f += 0.25) {
            double s = Mth.clamp(dist - 1.2, 0.5, 6) * f;
            if (!king.level().noCollision(king, box.move(nx * s, 0.5, nz * s))) return false;
        }
        return true;
    }
}
