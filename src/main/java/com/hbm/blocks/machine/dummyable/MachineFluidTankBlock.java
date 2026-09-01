package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineFluidTankBlockEntity;
import com.hbm.blocks.BlockDummyable;
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
 * CE {@code MachineFluidTank} — Dummyable {2,0,1,1,2,2} offset 1 + 4 extras.
 * TODO(CE: MachineFluidTank.java:56-57): TileEntityProxyCombo(true,false,true) on extras.
 * TODO(CE: MachineFluidTank.java:173-197): onBlockExploded + EntityBombletZeta inferno.
 * TODO(CE: MachineFluidTank.java:201-205): IToolable torch repair.
 * TODO(CE: RenderFluidTank.java:1): TESR.
 */
public class MachineFluidTankBlock extends BlockDummyable {

    public MachineFluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineFluidTankBlockEntity(DummyableProcessBlockEntities.MACHINE_FLUIDTANK.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_FLUIDTANK.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof MachineFluidTankBlockEntity te && te.hasExploded) {
            return InteractionResult.FAIL;
        }
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() && stack.getItem() instanceof IItemFluidIdentifier ident) {
            if (!level.isClientSide) {
                BlockPos core = findCore(level, pos);
                if (core != null && level.getBlockEntity(core) instanceof MachineFluidTankBlockEntity te) {
                    if (te.hasExploded) return ItemInteractionResult.FAIL;
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
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos base = placedPos.relative(dir.getOpposite());
        makeExtra(level, base.offset(1, 0, 1));
        makeExtra(level, base.offset(1, 0, -1));
        makeExtra(level, base.offset(-1, 0, 1));
        makeExtra(level, base.offset(-1, 0, -1));
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof MachineFluidTankBlockEntity be) {
            return be.tank.getRedstoneComparatorPower();
        }
        return 0;
    }
}
