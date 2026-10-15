package com.custommobs.theking.entity;

import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/* ───────── While he kicks, punches or dances: no strolling, jumping or looking around ─────────
   Holds the MOVE/LOOK/JUMP flags so lower goals pause; the action itself runs in KingEntity.serverTick(). */
public class KingBusyGoal extends Goal {
    private final KingEntity king;

    public KingBusyGoal(KingEntity king) {
        this.king = king;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() { return king.isBusy(); }

    @Override
    public void start() { king.getNavigation().stop(); }

    @Override
    public void tick() { king.getNavigation().stop(); }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }
}
