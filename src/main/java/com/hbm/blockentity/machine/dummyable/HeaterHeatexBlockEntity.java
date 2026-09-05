package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.dummyable.HeaterHeatexMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityHeaterHeatex.java}:89-157 — COOLANT_HOT → COOLANT via FT_Coolable HEATEXCHANGER.
 * {@code tanksNew[0].setType(0)} Exact CE {@code :85}. ROR: CE {@code :286-299}.
 */
public class HeaterHeatexBlockEntity extends MachineBaseBlockEntity
        implements IHeatSource, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IControlReceiver, IRORValueProvider {

    public final FluidTankNTM hot;
    public final FluidTankNTM cold;
    public int heatEnergy;
    public int amountToCool = 24_000;
    public int tickDelay = 1;

    public HeaterHeatexBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, false);
        this.hot = new FluidTankNTM(Fluids.COOLANT_HOT, 24_000).withOwner(this);
        this.cold = new FluidTankNTM(Fluids.COOLANT, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterHeatex");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityHeaterHeatex.java:85
        this.hot.setType(0, inventory);

        heatEnergy = (int) (heatEnergy * 0.999D);
        tryConvert();

        for (DirPos pos : getConPos()) {
            if (level.getGameTime() % 20 == 0) trySubscribe(hot.getTankType(), level, pos);
            if (cold.getFill() > 0) tryProvide(cold, level, pos);
        }

        dataChanged();
        networkPackMK2(25);
    }

    private void tryConvert() {
        if (level == null || tickDelay < 1 || level.getGameTime() % tickDelay != 0) return;
        FT_Coolable trait = hot.getTankType().getTrait(FT_Coolable.class);
        if (trait == null || trait.getEfficiency(CoolingType.HEATEXCHANGER) <= 0) return;
        if (cold.getTankType() != trait.coolsTo) cold.setTankType(trait.coolsTo);
        if (trait.amountReq <= 0 || trait.amountProduced <= 0) return;
        int inputOps = hot.getFill() / trait.amountReq;
        int outputOps = (cold.getMaxFill() - cold.getFill()) / trait.amountProduced;
        int ops = Math.min(Math.min(inputOps, outputOps), amountToCool);
        if (ops <= 0) return;
        hot.setFill(hot.getFill() - trait.amountReq * ops);
        cold.setFill(cold.getFill() + trait.amountProduced * ops);
        heatEnergy += (int) (trait.heatEnergy * ops * trait.getEfficiency(CoolingType.HEATEXCHANGER));
    }

    @Override
    public boolean hasPermission(Player player) {
        // Exact CE TileEntityHeaterHeatex.java:273-275
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) < 256.0D;
    }

    /** Exact CE {@code TileEntityHeaterHeatex.receiveControl} :278-282. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toCool")) {
            this.amountToCool = Mth.clamp(data.getInt("toCool"), 1, hot.getMaxFill());
        }
        if (data.contains("delay")) {
            this.tickDelay = Math.max(data.getInt("delay"), 1);
        }
        setChanged();
        dataChanged();
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 2).relative(rot), dir),
                new DirPos(worldPosition.relative(dir, 2).relative(rot.getOpposite()), dir),
                new DirPos(worldPosition.relative(dir.getOpposite(), 2).relative(rot), dir.getOpposite()),
                new DirPos(worldPosition.relative(dir.getOpposite(), 2).relative(rot.getOpposite()), dir.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void useUpHeat(int heat) {
        heatEnergy = Math.max(heatEnergy - Math.max(0, heat), 0);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(hot);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(cold);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(hot, cold);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", heatEnergy);
        tag.putInt("cool", amountToCool);
        tag.putInt("delay", tickDelay);
        hot.writeToNBT(tag, "h");
        cold.writeToNBT(tag, "c");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heatEnergy = tag.getInt("heat");
        amountToCool = Math.max(1, tag.getInt("cool"));
        tickDelay = Math.max(1, tag.getInt("delay"));
        hot.readFromNBT(tag, "h");
        cold.readFromNBT(tag, "c");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heatEnergy);
        buf.writeInt(amountToCool);
        buf.writeInt(tickDelay);
        hot.serialize(buf);
        cold.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heatEnergy = buf.readInt();
        amountToCool = buf.readInt();
        tickDelay = buf.readInt();
        hot.deserialize(buf);
        cold.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeaterHeatexMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :286-291
        return new String[]{
                PREFIX_VALUE + "hotfluid",
                PREFIX_VALUE + "coldfluid",
                PREFIX_VALUE + "heat"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :295-299
        if ((PREFIX_VALUE + "hotfluid").equals(name)) return "" + hot.getFill();
        if ((PREFIX_VALUE + "coldfluid").equals(name)) return "" + cold.getFill();
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heatEnergy;
        return null;
    }
}
