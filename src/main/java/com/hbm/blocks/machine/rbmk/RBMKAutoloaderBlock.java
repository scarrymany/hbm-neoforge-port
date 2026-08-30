package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Mechanical fuel-rod feeder, meant to be placed directly above a fuel rod channel - not an RBMK
 * grid column. Ported from CE's {@code RBMKLoader} block class (registry name kept as
 * {@code rbmk_autoloader} matching the tile entity/research report naming, not CE's block-class name).
 */
public class RBMKAutoloaderBlock extends BaseEntityBlock {

    public RBMKAutoloaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RBMKAutoloaderBlockEntity(RBMKBlockEntities.AUTOLOADER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RBMKBlockEntities.AUTOLOADER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MenuProvider menu && !player.isShiftKeyDown()) {
            player.openMenu(new SimpleMenuProvider(menu, menu.getDisplayName()), pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
