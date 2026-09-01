package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineBigAssTankBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.items.machine.IItemFluidIdentifier;
import net.minecraft.ChatFormatting;
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

/**
 * CE {@code MachineBigAssTank} — Dummyable {5,0,4,4,4,4} offset 6 + XR extras.
 * Opens barrel GUI ({@code guiID_barrel}). ≠ {@code machine_bat9000}.
 * TODO(CE: MachineBigAssTank.java:44): TileEntityProxyCombo(false,false,true) on extras.
 * TODO(CE: RenderBigAssTank.java:1): TESR.
 */
public class MachineBigAssTankBlock extends BlockDummyable {

    public MachineBigAssTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{5, 0, 4, 4, 4, 4};
    }

    @Override
    public int getOffset() {
        return 6;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineBigAssTankBlockEntity(DummyableProcessBlockEntities.MACHINE_BIGASSTANK.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_BIGASSTANK.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof MachineBigAssTankBlockEntity te) {
                    var type = ident.getType(level, core, stack);
                    te.tank.setTankType(type);
                    te.setChanged();
                    player.displayClientMessage(Component.literal("Changed type to ")
                            .append(type.getLocalizedName())
                            .append(Component.literal("!"))
                            .withStyle(ChatFormatting.YELLOW), false);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        return MultiblockHandlerXR.checkSpace(level, core, getDimensions(), placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, 0, 5, -4, 2, 2}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, 0, -4, 5, 2, 2}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, 0, 2, 2, 5, -4}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{4, 0, 2, 2, -4, 5}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, 0, 6, -5, 0, 0}, placedPos, dir)
                && MultiblockHandlerXR.checkSpace(level, core, new int[]{3, 0, -5, 6, 0, 0}, placedPos, dir);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, 0, 5, -4, 2, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, 0, -4, 5, 2, 2}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, 0, 2, 2, 5, -4}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, 0, 2, 2, -4, 5}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, 0, 6, -5, 0, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, 0, -5, 6, 0, 0}, this, dir);
        makeExtra(level, core.relative(dir, 6));
        makeExtra(level, core.relative(dir, -6));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof MachineBigAssTankBlockEntity be) {
            return be.tank.getRedstoneComparatorPower();
        }
        return 0;
    }
}
