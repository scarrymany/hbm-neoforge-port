package com.hbm.blocks.generic;

import net.minecraft.world.level.block.TrapDoorBlock;

/**
 * Modded trapdoor, ported from CE's {@code BlockNTMTrapdoor}. CE's entire body beyond
 * construction ({@code updateLadderState}, the computed {@code LADDER} property, the ladder-shaped
 * open bounding box) exists to implement "an open trapdoor above a ladder is itself climbable" -
 * NeoForge already restores exactly that hook on vanilla {@link TrapDoorBlock} (its
 * {@code isLadder} override calling {@code Block#makesOpenTrapdoorAboveClimbable}, confirmed
 * against this toolchain's decompiled sources), and {@code makesOpenTrapdoorAboveClimbable}'s
 * default implementation already recognizes any {@code LadderBlock} subclass with a matching
 * facing - which {@link BlockNTMLadder} is. So this collapses to a thin subclass carrying CE's
 * always-hand-openable behavior via the same {@link BlockModDoor#HAND_OPENABLE_METAL()} set type;
 * CE's cosmetic {@code LADDER} blockstate property (used only to pick a ladder-styled model) is not
 * needed since this port does not do model baking here.
 */
public class BlockNTMTrapdoor extends TrapDoorBlock {

    public BlockNTMTrapdoor(Properties properties) {
        super(BlockModDoor.HAND_OPENABLE_METAL(), properties);
    }
}
