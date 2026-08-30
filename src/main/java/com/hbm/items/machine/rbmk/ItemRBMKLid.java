package com.hbm.items.machine.rbmk;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Installs a removable lid on an RBMK column core, ported from CE's
 * {@code com.hbm.items.machine.ItemRBMKLid} (read in full - the {@code rbmk_lid}/{@code rbmk_lid_glass}
 * item pair). {@link RBMKBaseBlock}'s own javadoc explicitly names this exact interaction as
 * deferred ("toggled by a screwdriver interaction (not ported in this pass...)") and points at this
 * package's {@code RBMKItems#RBMK_LID}/{@code RBMK_LID_GLASS} fields as the two items this repurposed
 * meta range refers to - this class is that follow-up, replacing the generic {@link com.hbm.items.ItemBase}
 * those fields were registered with (see {@link RBMKItems}'s own registration call, updated by this
 * pass to construct this class instead).
 * <p>
 * Reuses {@link com.hbm.blocks.BlockDummyable#META}'s repurposed-rotation encoding directly (CE:
 * raw metadata read/write) - {@link RBMKBaseBlock#metaToLid} confirms the current lid state,
 * {@link RBMKBaseBlock#DIR_NORMAL_LID}/{@link RBMKBaseBlock#DIR_GLASS_LID} plus
 * {@link com.hbm.blocks.BlockDummyable#offset} give the new one. No NBT snapshot/restore dance is
 * needed the way CE's does (its {@code explodeOnBroken = false} guard exists only to suppress a
 * meltdown-on-break side effect while replacing the block, and {@link Level#setBlock} in this port
 * does not tear down and recreate the block entity the way CE's 1.12
 * {@code World#setBlockState}+{@code notifyBlockUpdate} pair implied for a decorative-metadata-only
 * change).
 */
public class ItemRBMKLid extends Item {

    private final boolean glass;

    public ItemRBMKLid(Properties properties, boolean glass) {
        super(properties);
        this.glass = glass;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();

        if (!(block instanceof RBMKBaseBlock dummy)) return InteractionResult.PASS;

        BlockPos core = dummy.findCore(level, pos);
        if (core == null) return InteractionResult.FAIL;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockState coreState = level.getBlockState(core);
        int currentMeta = coreState.getValue(BlockDummyable.META);
        Integer currentLid = RBMKBaseBlock.metaToLid(currentMeta);
        if (currentLid == null || currentLid != 0) return InteractionResult.FAIL;

        var lidDir = glass ? RBMKBaseBlock.DIR_GLASS_LID : RBMKBaseBlock.DIR_NORMAL_LID;
        int newMeta = lidDir.get3DDataValue() + BlockDummyable.offset;

        level.setBlock(core, coreState.setValue(BlockDummyable.META, newMeta), 3);
        level.playSound(null, core.getX() + 0.5, core.getY() + 0.5, core.getZ() + 0.5,
                glass ? SoundEvents.GLASS_PLACE : SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);

        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
