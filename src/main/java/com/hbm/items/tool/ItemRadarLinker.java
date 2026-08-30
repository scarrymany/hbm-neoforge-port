package com.hbm.items.tool;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

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
 * <b>Deferred</b>: CE checks the resolved core against {@code IRadarCommandReceiver}/
 * {@code TileEntityMachineRadarScreen} - confirmed absent from this port (no radar-screen multiblock
 * has landed in any Phase 2/3 package this wave; see the report's Deferred scope). Until one exists,
 * this item has nothing to link to and {@link #canGrabCoordinateHere} always returns {@code false} -
 * the item still registers and behaves exactly like CE's own item would against a world with no
 * radar screens placed (a "no valid target" result, not a crash or fake success).
 */
public class ItemRadarLinker extends ItemCoordinateBase {

    public ItemRadarLinker(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canGrabCoordinateHere(Level level, BlockPos pos) {
        // TODO(IRadarCommandReceiver/TileEntityMachineRadarScreen, unowned per the research report's
        // Deferred scope): CE resolves the clicked block to its multiblock core (via findCore, see
        // getCoordinates below) and accepts it when that core's block entity implements either
        // interface. Neither interface exists anywhere in this port yet - no radar-screen multiblock
        // has landed - so this always rejects until that package ships. Not a stub of fake behavior:
        // this is CE's own real "no valid target" outcome when no radar screen is placed.
        return false;
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
