package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntityMachineMiniRTG} (read in full): a fuel-less, inventory-less,
 * GUI-less black-box generator that trickle-charges its own buffer every tick at a flat rate. CE
 * backs two distinct registry blocks ({@code machine_minirtg} / {@code machine_powerrtg}, the
 * "polonium RTG") off one TE class, branching on block identity; this port keeps that exact
 * decision (flagged as an open call in the research report, "either works") rather than splitting
 * into two classes, since the two variants share 100% of their logic and differ only in two
 * constants - the constructor flag below is that same branch made explicit instead of an
 * identity check against a {@code ModBlocks} field.
 */
public class MachineMiniRTGBlockEntity extends MachineBaseBlockEntity implements IEnergyProviderMK2, ITickableBE {

    private static final long MINI_RATE = 70L;
    private static final long MINI_MAX = 10_000L;
    private static final long POLONIUM_RATE = 2500L;
    private static final long POLONIUM_MAX = 50_000L;

    private final boolean polonium;
    private long power;

    public MachineMiniRTGBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean polonium) {
        super(type, pos, state, 0, false, true);
        this.polonium = polonium;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(polonium ? "container.machinePowerRtg" : "container.machineMiniRtg");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            this.tryProvide(level, target.getX(), target.getY(), target.getZ(), dir);
        }

        power = Math.min(getMaxPower(), power + (polonium ? POLONIUM_RATE : MINI_RATE));
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return polonium ? POLONIUM_MAX : MINI_MAX;
    }
}
