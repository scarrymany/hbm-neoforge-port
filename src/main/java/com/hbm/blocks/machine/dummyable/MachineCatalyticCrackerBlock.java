package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineCatalyticCrackerBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.items.machine.IItemFluidIdentifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** CE {@code MachineCatalyticCracker} — Dummyable {0,0,3,3,2,3} offset 3. Real tank GUI (CE was overlay). */
public class MachineCatalyticCrackerBlock extends BlockDummyable {

    public MachineCatalyticCrackerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 3, 3, 2, 3};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineCatalyticCrackerBlockEntity(DummyableProcessBlockEntities.MACHINE_CATALYTIC_CRACKER.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_CATALYTIC_CRACKER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof MachineCatalyticCrackerBlockEntity cracker) {
                    cracker.oil.setTankType(ident.getType(level, core, stack));
                    cracker.setChanged();
                    player.displayClientMessage(Component.literal("Changed type to " + cracker.oil.getTankType().getLocalizedName().getString()), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        return super.checkRequirement(level, placedPos, dir, placementOffset)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, -1, 3, -1, 1, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{13, 0, 0, 3, 2, 1}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{14, -13, -1, 2, 1, 0}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, -1, 2, 3, -1, 3}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, -1, 3, -1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{13, 0, 0, 3, 2, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{14, -13, -1, 2, 1, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, -1, 2, 3, -1, 3}, this, dir);
        Direction rot = dir.getClockWise();
        makeExtra(level, core.relative(dir, 3).relative(rot));
        makeExtra(level, core.relative(dir, 3).relative(rot.getOpposite(), 2));
        makeExtra(level, core.relative(dir.getOpposite(), 3).relative(rot));
        makeExtra(level, core.relative(dir.getOpposite(), 3).relative(rot.getOpposite(), 2));
        makeExtra(level, core.relative(dir, 2).relative(rot, 2));
        makeExtra(level, core.relative(dir, 2).relative(rot.getOpposite(), 3));
        makeExtra(level, core.relative(dir.getOpposite(), 2).relative(rot, 2));
        makeExtra(level, core.relative(dir.getOpposite(), 2).relative(rot.getOpposite(), 3));
    }
}
