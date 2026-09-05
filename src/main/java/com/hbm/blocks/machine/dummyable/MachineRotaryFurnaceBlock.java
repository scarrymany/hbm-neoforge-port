package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.MachineRotaryFurnaceBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
 * CE {@code MachineRotaryFurnace} — Dummyable {4,0,1,1,2,2} offset 1 + extras.
 * printHook Exact CE {@code :73-104} ({@code hitCheck} steam/fluids/fuel).
 */
public class MachineRotaryFurnaceBlock extends BlockDummyable implements ILookOverlay {

    public MachineRotaryFurnaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new MachineRotaryFurnaceBlockEntity(DummyableProcessBlockEntities.MACHINE_ROTARY_FURNACE.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_ROTARY_FURNACE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction rot = dir.getClockWise();
        for (int i = -2; i <= 2; i++) {
            makeExtra(level, core.relative(dir.getOpposite()).relative(rot, i));
        }
        makeExtra(level, core.relative(dir).relative(rot, 2));
        makeExtra(level, core.relative(rot).above(4));
        makeExtra(level, core.relative(dir).relative(rot));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineRotaryFurnace.java:73-104
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof MachineRotaryFurnaceBlockEntity furnace)) return;

        Direction dir = Direction.from3DDataValue(world.getBlockState(core).getValue(META) - offset);

        List<Component> text = new ArrayList<>();

        // steam
        if (hitCheck(dir, core.getX(), core.getY(), core.getZ(), -1, -1, 0, pos.getX(), pos.getY(), pos.getZ())
                || hitCheck(dir, core.getX(), core.getY(), core.getZ(), -1, -2, 0, pos.getX(), pos.getY(), pos.getZ())) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(furnace.steam.getTankType().getLocalizedName())));
            text.add(Component.literal("<- ").withStyle(ChatFormatting.RED)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(furnace.spent.getTankType().getLocalizedName())));
        }

        // fluids
        if (hitCheck(dir, core.getX(), core.getY(), core.getZ(), 1, 2, 0, pos.getX(), pos.getY(), pos.getZ())
                || hitCheck(dir, core.getX(), core.getY(), core.getZ(), -1, 2, 0, pos.getX(), pos.getY(), pos.getZ())) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(furnace.process.getTankType().getLocalizedName())));
        }

        if (hitCheck(dir, core.getX(), core.getY(), core.getZ(), 1, 1, 0, pos.getX(), pos.getY(), pos.getZ())) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("Fuel").withStyle(ChatFormatting.RESET)));
        }

        if (!text.isEmpty()) {
            ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
        }
    }

    /** Exact CE {@code MachineRotaryFurnace.hitCheck} ({@code :106-115}). {@code getRotation(DOWN)} → {@code getClockWise}. */
    protected boolean hitCheck(Direction dir, int coreX, int coreY, int coreZ, int exDir, int exRot, int exY,
                               int hitX, int hitY, int hitZ) {
        Direction turn = dir.getClockWise();
        int iX = coreX + dir.getStepX() * exDir + turn.getStepX() * exRot;
        int iY = coreY + exY;
        int iZ = coreZ + dir.getStepZ() * exDir + turn.getStepZ() * exRot;
        return iX == hitX && iZ == hitZ && iY == hitY;
    }
}
