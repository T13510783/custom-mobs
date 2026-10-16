package com.custommobs.homelander.entity;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/* ───────── While he attacks: no strolling, jumping or looking around ─────────
   Holds the MOVE/LOOK/JUMP flags so lower goals pause; the attack itself runs in HomelanderEntity.serverTick(). */
public class HomelanderBusyGoal extends Goal {
    private final HomelanderEntity mob;

    public HomelanderBusyGoal(HomelanderEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() { return mob.isBusy(); }

    @Override
    public void start() { mob.getNavigation().stop(); }

    @Override
    public void tick() { mob.getNavigation().stop(); }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }
}
