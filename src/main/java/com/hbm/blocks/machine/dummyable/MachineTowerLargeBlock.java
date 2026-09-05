package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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

/** CE {@code MachineTowerLarge} — Dummyable {12,0,4,4,4,4} offset 4. Fluid condenser. fillSpace extras Exact CE {@code :48-61}. printHook Exact CE {@code :64-83}. */
public class MachineTowerLargeBlock extends BlockDummyable implements ILookOverlay {

    public MachineTowerLargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{12, 0, 4, 4, 4, 4};
    }

    @Override
    public int getOffset() {
        return 4;
    }

    /**
     * Exact CE {@code MachineTowerLarge.fillSpace} extras ({@code MachineTowerLarge.java:48-61}).
     * After {@code super.fillSpace}: add {@code dir * o} (core), then for CE {@code ForgeDirection}
     * ids {@code 2..6} (N/S/W/E + {@code UNKNOWN}): extras at {@code core + dr2*4} and
     * {@code ± rot*3} on that face. {@code rot = dr2.getRotation(UP)} = clockwise around Y.
     * {@code UNKNOWN} is {@code (0,0,0)} so those three extras land on the core. No ProxyCombo TE.
     */
    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        BlockPos core = placedPos.relative(dir, placementOffset);
        Direction[] horiz = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction dr2 : horiz) {
            Direction rot = dr2.getClockWise();
            BlockPos face = core.relative(dr2, 4);
            makeExtra(level, face);
            makeExtra(level, face.relative(rot, 3));
            makeExtra(level, face.relative(rot.getOpposite(), 3));
        }
        // CE i=6 UNKNOWN — three extras at core
        makeExtra(level, core);
        makeExtra(level, core);
        makeExtra(level, core);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? CondenserBlockEntity.towerLarge(DummyableProcessBlockEntities.MACHINE_TOWER_LARGE.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_TOWER_LARGE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineTowerLarge.java:64-83 — green input / red output, fill/max no %,d
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof CondenserBlockEntity tower)) return;

        List<Component> text = new ArrayList<>();
        List<FluidTankNTM> tanks = tower.getAllTanks();
        for (int i = 0; i < tanks.size(); i++) {
            FluidTankNTM tank = tanks.get(i);
            text.add(Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(tank.getTankType().getLocalizedName())
                            .append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB"))));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
