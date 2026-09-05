package com.hbm.blocks.machine.chem;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.chem.ChemIsotopeBlockEntities;
import com.hbm.blockentity.machine.chem.CyclotronBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code MachineCyclotron} (regname {@code machine_cyclotron}) - particle-accelerator transmutation.
 * Plug insert Exact CE {@code MachineCyclotron.java:47-57}. TESR stay skipped.
 */
public class CyclotronBlock extends BlockDummyable {

    public CyclotronBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 2, 2, 2, 2};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new CyclotronBlockEntity(ChemIsotopeBlockEntities.CYCLOTRON.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ChemIsotopeBlockEntities.CYCLOTRON.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlockPos core = findCore(level, pos);
        if (core == null) return ItemInteractionResult.FAIL;
        if (!(level.getBlockEntity(core) instanceof CyclotronBlockEntity cyc)) return ItemInteractionResult.FAIL;

        // Exact CE MachineCyclotron.java:47-57
        if (!stack.isEmpty()) {
            for (int i = 0; i < 4; i++) {
                Item plug = CyclotronBlockEntity.getItemForPlug(i);
                if (plug != null && stack.getItem() == plug && !cyc.getPlug(i)) {
                    stack.shrink(1);
                    cyc.setPlug(i);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        InteractionResult result = standardOpenBehavior(level, pos, player);
        return result == InteractionResult.FAIL ? ItemInteractionResult.FAIL : ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
