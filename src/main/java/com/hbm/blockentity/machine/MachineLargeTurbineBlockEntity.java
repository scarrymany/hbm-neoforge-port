package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.machine.MachineLargeTurbineMenu;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineLargeTurbine} (block {@code MachineLargeTurbine},
 * regname {@code machine_large_turbine}, read in full): a multiblock turbine at 100%
 * {@link FT_Coolable} efficiency (no multiplier) with huge tanks (512 000 / 10 240 000 mB) and a
 * "burn at most 20% of the input buffer per tick" cap (CE's own comment: "amount of cycles by the
 * 'at least 20%' rule"), rather than the small turbine's fixed 6000-heat-equivalent cap.
 * <p>
 * Same scope trims as {@link MachineTurbineBlockEntity}: no fluid-identifier retyping, no
 * item-container fill/drain slots (both would need infrastructure this port doesn't have yet - see
 * that class's javadoc); only the battery-charging slot survives.
 */
public class MachineLargeTurbineBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000_000L;
    private static final int BATTERY_SLOT = 0;

    public final FluidTankNTM[] tanks;
    public float rotor;
    private long power;

    public MachineLargeTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, true);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, 512_000).withOwner(this),
                new FluidTankNTM(Fluids.SPENTSTEAM, 10_240_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineLargeTurbine");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return i == BATTERY_SLOT && Library.isBattery(stack);
    }

    private Direction coreDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    public DirPos[] getConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + rot.getStepX() * 2, y, z + rot.getStepZ() * 2, rot),
                new DirPos(x - rot.getStepX() * 2, y, z - rot.getStepZ() * 2, rot.getOpposite()),
                new DirPos(x + dir.getStepX() * 2, y, z + dir.getStepZ() * 2, dir)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        Direction dir = coreDirection();
        BlockPos powerTarget = worldPosition.relative(dir, -4);
        this.tryProvide(level, powerTarget.getX(), powerTarget.getY(), powerTarget.getZ(), dir.getOpposite());

        for (DirPos dirPos : getConPos()) {
            BlockPos p = dirPos.getPos();
            this.trySubscribe(tanks[0].getTankType(), level, p.getX(), p.getY(), p.getZ(), dirPos.getDir());
        }
        for (DirPos dirPos : getConPos()) {
            this.tryProvide(tanks[1], level, dirPos.getPos(), dirPos.getDir());
        }

        power = Library.chargeItemsFromTE(inventory, BATTERY_SLOT, power, MAX_POWER);

        FluidType in = tanks[0].getTankType();
        boolean valid = false;
        boolean shouldTurn = false;
        if (in.hasTrait(FT_Coolable.class)) {
            FT_Coolable trait = in.getTrait(FT_Coolable.class);
            double eff = trait.getEfficiency(FT_Coolable.CoolingType.TURBINE);
            if (eff > 0) {
                tanks[1].setTankType(trait.coolsTo);
                int inputOps = (int) Math.floor((double) tanks[0].getFill() / trait.amountReq);
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
                int cap = (int) Math.ceil((double) tanks[0].getFill() / trait.amountReq / 5D);
                int ops = Math.min(inputOps, Math.min(outputOps, cap));
                tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
                tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
                power += (long) (ops * trait.heatEnergy * eff);
                valid = true;
                shouldTurn = ops > 0;
            }
        }
        if (!valid) tanks[1].setTankType(Fluids.NONE);
        if (power > MAX_POWER) power = MAX_POWER;

        rotor = (rotor + (shouldTurn ? 6F : 0F)) % 360F;

        // CE: TileEntityMachineLargeTurbine's client-side fan-acceleration branch drives a continuous
        // AudioWrapper loop (HBMSoundHandler.turbofanOperate, 10-tick keepAlive) with volume/pitch
        // ramped by fanAcceleration while shouldTurn. No looped-block-audio bridge or client-side fan
        // easing ported yet (this class's rotor already moves in one server-side step, unlike CE's
        // split client/server model - see class javadoc's "Scope trims" note); substituted with a
        // periodic broadcast every 10 ticks while turning, at a fixed representative pitch/volume
        // rather than CE's live ramp.
        if (shouldTurn && level.getGameTime() % 10 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.turbofanOperate.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
        }

        dataChanged();
        networkPackMK2(50);
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
        return MAX_POWER;
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "steam");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineLargeTurbineMenu(containerId, playerInventory, this);
    }
}
