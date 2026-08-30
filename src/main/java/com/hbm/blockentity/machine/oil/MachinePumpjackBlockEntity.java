package com.hbm.blockentity.machine.oil;

import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntityMachinePumpjack} (239 lines, read in full). Adds
 * {@link Direction}-dependent connector points (facing-aware, unlike the derrick's fixed cardinal
 * set) and a purely cosmetic client-side rotating-rod animation ({@link #rot}/{@link #prevRot}/
 * {@link #speed}, synced via {@link #serialize}/{@link #deserialize} exactly like CE's own
 * {@code ByteBuf} payload - the speed value itself, not a raw angle, so each client free-runs its own
 * interpolation between packets).
 */
public class MachinePumpjackBlockEntity extends OilDrillBaseBlockEntity {

    private static final long MAX_POWER = 250_000L;
    private static final int POWER_REQ = 200;
    private static final int DELAY = 25;
    private static final int OIL_PER_DEPOSIT = 750;
    private static final int GAS_PER_DEPOSIT_MIN = 50;
    private static final int GAS_PER_DEPOSIT_MAX = 250;
    private static final double DRAIN_CHANCE = 0.025D;

    /** Client-side-only cosmetic rotation state - never persisted, only ever set from {@link #deserialize}. */
    public float rot = 0;
    public float prevRot = 0;
    public float speed = 0;

    public MachinePumpjackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pumpjack");
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public int getPowerReq() {
        return POWER_REQ;
    }

    @Override
    public int getDelay() {
        return DELAY;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && level.isClientSide) {
            this.prevRot = rot;

            if (this.indicator == 0) {
                this.rot += speed;
            }

            if (this.rot >= 360) {
                this.prevRot -= 360;
                this.rot -= 360;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeFloat(this.indicator == 0 ? (5F + (2F * this.speedLevel)) + (this.overLevel - 1F) * 10 : 0F);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.speed = buf.readFloat();
    }

    @Override
    public void onSuck(BlockPos pos) {
        if (level == null) return;
        if (level.getBlockState(pos).getBlock() != oreOil()) return;

        getOilTank().setTankType(Fluids.OIL);
        getGasTank().setTankType(Fluids.GAS);

        getOilTank().setFill(getOilTank().getFill() + OIL_PER_DEPOSIT);
        getGasTank().setFill(getGasTank().getFill() + GAS_PER_DEPOSIT_MIN
                + level.getRandom().nextInt(GAS_PER_DEPOSIT_MAX - GAS_PER_DEPOSIT_MIN + 1));

        if (level.getRandom().nextDouble() < DRAIN_CHANCE) {
            level.setBlock(pos, oreOilEmpty().defaultBlockState(), 3);
        }
    }

    /**
     * Facing-dependent connector points, ported from CE's {@code getConPos} ({@code
     * ForgeDirection.getRotation(DOWN)} -&gt; {@link Direction#getClockWise(Direction.Axis)}, matching
     * this port's own {@code MachineCombustionEngineBlockEntity#getConPos} precedent for the identical
     * CE idiom).
     */
    @Override
    public DirPos[] getConPos() {
        BlockState state = getBlockState();
        Direction dir = state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
        Direction rot = dir.getClockWise(Direction.Axis.Y);

        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + rot.getStepX() * 2 + dir.getStepX() * 2, y, z + rot.getStepZ() * 2 + dir.getStepZ() * 2, dir),
                new DirPos(x + rot.getStepX() * 2 + dir.getStepX() * 2, y, z + rot.getStepZ() * 4 - dir.getStepZ() * 2, dir.getOpposite()),
                new DirPos(x + rot.getStepX() * 4 - dir.getStepX() * 2, y, z + rot.getStepZ() * 4 + dir.getStepZ() * 2, dir),
                new DirPos(x + rot.getStepX() * 4 - dir.getStepX() * 2, y, z + rot.getStepZ() * 2 - dir.getStepZ() * 2, dir.getOpposite())
        };
    }
}
