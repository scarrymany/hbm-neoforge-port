package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineChungusBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code MachineChungus}: Dummyable {3,0,0,3,2,2} offset 3.
 * TODO(CE: MachineChungus.java:40): TileEntityProxyCombo(false,true,true) on extras.
 * TODO(CE: RenderChungus.java:16): TESR. No GUI in CE.
 */
public class MachineChungusBlock extends BlockDummyable implements ILookOverlay {

    public MachineChungusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 3, 2, 2};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineChungusBlockEntity(DummyableProcessBlockEntities.MACHINE_CHUNGUS.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_CHUNGUS.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        if (!MultiblockHandlerXR.checkSpace(level, core, getDimensions(), placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{3, 0, 6, -1, 1, 1}, placedPos, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, new int[]{2, 0, 10, -7, 1, 1}, placedPos, dir)) return false;
        return level.getBlockState(placedPos.relative(dir).above(2)).canBeReplaced();
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{4, -4, 0, 3, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{3, 0, 6, -1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(level, core, new int[]{2, 0, 10, -7, 1, 1}, this, dir);

        BlockPos dummy = placedPos.relative(dir).above(2);
        level.setBlock(dummy, this.defaultBlockState().setValue(META, dir.get3DDataValue()), 3);
        makeExtra(level, dummy);
        makeExtra(level, placedPos.relative(dir, placementOffset - 10));
        Direction side = dir.getCounterClockWise();
        makeExtra(level, core.relative(side, 2));
        makeExtra(level, core.relative(side.getOpposite(), 2));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(core) instanceof MachineChungusBlockEntity entity)) {
            return InteractionResult.SUCCESS;
        }

        Direction dir = Direction.from3DDataValue(level.getBlockState(core).getValue(META) - offset);
        Direction turn = dir.getClockWise();
        int iX = core.getX() + dir.getStepX() + turn.getStepX() * 2;
        int iX2 = core.getX() + dir.getStepX() * 2 + turn.getStepX() * 2;
        int iZ = core.getZ() + dir.getStepZ() + turn.getStepZ() * 2;
        int iZ2 = core.getZ() + dir.getStepZ() * 2 + turn.getStepZ() * 2;

        if ((pos.getX() == iX || pos.getX() == iX2) && (pos.getZ() == iZ || pos.getZ() == iZ2)
                && pos.getY() < core.getY() + 2) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    HBMSoundHandler.chungus_lever.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
            if (!level.isClientSide) {
                if (!entity.operational) {
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            HBMSoundHandler.chungus_lever.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    entity.onLeverPull();
                } else {
                    player.displayClientMessage(Component.literal("Cannot change compressor setting while operational!")
                            .withStyle(ChatFormatting.RED), false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        BlockPos core = findCore(world, pos);
        if (core == null || !(world.getBlockEntity(core) instanceof MachineChungusBlockEntity chungus)) return;
        List<Component> text = new ArrayList<>();
        FluidType inputType = chungus.tanks[0].getTankType();
        if (inputType != Fluids.NONE) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(inputType.getLocalizedName())
                    .append(Component.literal(": " + chungus.tanks[0].getFill() + "/" + chungus.tanks[0].getMaxFill() + "mB")));
        }
        FluidType outputType = chungus.tanks[1].getTankType();
        if (outputType != Fluids.NONE) {
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(outputType.getLocalizedName())
                    .append(Component.literal(": " + chungus.tanks[1].getFill() + "/" + chungus.tanks[1].getMaxFill() + "mB")));
        }
        text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                .append(Component.literal(Library.getShortNumber(chungus.powerBuffer) + "HE").withStyle(ChatFormatting.RESET)));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
