package com.hbm.items.tool;

import com.hbm.blockentity.IRadarCommandReceiver;
import com.hbm.blockentity.machine.dummyable.RadarScreenBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemRadarLinker} (38 lines, read in full) - concrete
 * {@link ItemCoordinateBase}: only lets the player save the coordinate of a block that resolves to
 * a radar-command target, and re-resolves that target to its multiblock core.
 * <p>
 * CE's {@code CompatExternal.getCoreFromPos(world, pos)} is replaced by this port's own confirmed
 * real {@link BlockDummyable#findCore(Level, BlockPos)} - the identical multiblock-core-lookup this
 * port's {@code ItemAnalysisTool} already uses, per {@code docs/phase3/scattered_military_items.md}'s
 * Key design decisions.
 * <p>
 * {@link #canGrabCoordinateHere} Exact CE {@code ItemRadarLinker.java:23-26}:
 * {@link IRadarCommandReceiver} or {@link RadarScreenBlockEntity} at dummyable core.
 */
public class ItemRadarLinker extends ItemCoordinateBase {

    public ItemRadarLinker(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canGrabCoordinateHere(Level level, BlockPos pos) {
        // CE ItemRadarLinker.java:23-26
        BlockEntity tile = level.getBlockEntity(getCoordinates(level, pos));
        return tile instanceof IRadarCommandReceiver || tile instanceof RadarScreenBlockEntity;
    }

    @Override
    public BlockPos getCoordinates(Level level, BlockPos pos) {
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof BlockDummyable dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core != null) return core;
        }
        return pos;
    }

    @Override
    public void onTargetSet(Level level, BlockPos pos, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
    }
}
