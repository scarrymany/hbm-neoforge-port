package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.dummyable.CondenserPoweredBlockEntity;
import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
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
import java.util.Locale;

/** CE {@code MachineCondenserPowered} — Dummyable {2,0,1,1,3,3} offset 1 + 6 extras. printHook Exact CE {@code :68-86}. */
public class MachineCondenserPoweredBlock extends BlockDummyable implements ILookOverlay {

    public MachineCondenserPoweredBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 3, 3};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new CondenserPoweredBlockEntity(DummyableProcessBlockEntities.MACHINE_CONDENSER_POWERED.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DummyableProcessBlockEntities.MACHINE_CONDENSER_POWERED.get() ? ITickableBE.ticker() : null;
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
        makeExtra(level, core.relative(rot, 3).above());
        makeExtra(level, core.relative(rot, -3).above());
        makeExtra(level, core.relative(dir).relative(rot).above());
        makeExtra(level, core.relative(dir).relative(rot, -1).above());
        makeExtra(level, core.relative(dir, -1).relative(rot).above());
        makeExtra(level, core.relative(dir, -1).relative(rot, -1).above());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // Exact CE MachineCondenserPowered.java:68-86 — HE short + green/red tanks %,d
        BlockPos core = findCore(world, pos);
        if (core == null) return;
        if (!(world.getBlockEntity(core) instanceof CondenserPoweredBlockEntity tower)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(BobMathUtil.getShortNumber(tower.power) + "HE / "
                + BobMathUtil.getShortNumber(tower.getMaxPower()) + "HE"));
        List<FluidTankNTM> tanks = tower.getAllTanks();
        for (int i = 0; i < tanks.size(); i++) {
            FluidTankNTM tank = tanks.get(i);
            text.add(Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.empty().withStyle(ChatFormatting.RESET)
                            .append(tank.getTankType().getLocalizedName())
                            .append(Component.literal(": "
                                    + String.format(Locale.US, "%,d", tank.getFill())
                                    + "/"
                                    + String.format(Locale.US, "%,d", tank.getMaxFill())
                                    + "mB"))));
        }
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
