package com.hbm.items.tool;

import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Force-triggers an RBMK meltdown, ported from CE's {@code com.hbm.items.tool.ItemDyatlov} (read in
 * full - CE's own name for the button that pushed the real Chernobyl RBMK past its design limits).
 * Dispatches straight to {@link RBMKBaseBlockEntity#meltdown()} - a real, live method on this port's
 * RBMK column base class whose own javadoc names this exact use case ("convenience for a column TE
 * that wants to trigger a meltdown directly...matching CE's {@code TileEntityRBMKBase.meltdown()}
 * being callable from e.g. a debug tool"), which itself dispatches into the full BFS/heat-diffusion/
 * debris-conversion state machine ({@code runMeltdown}) the parallel {@code rbmk_core_logic} package
 * ported this same wave. No stubbing needed - both the column lookup ({@link RBMKBaseBlock}/
 * {@code findCore}) and the meltdown target are real, shipped content as of this pass.
 */
public class ItemDyatlov extends Item {

    public ItemDyatlov(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.PASS;

        Block block = level.getBlockState(context.getClickedPos()).getBlock();
        if (!(block instanceof RBMKBaseBlock dummy)) return InteractionResult.PASS;

        BlockPos core = dummy.findCore(level, context.getClickedPos());
        if (core == null) return InteractionResult.PASS;

        if (level.getBlockEntity(core) instanceof RBMKBaseBlockEntity rbmk) {
            rbmk.meltdown();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
