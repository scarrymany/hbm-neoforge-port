package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HeatBoilerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.items.machine.IItemFluidIdentifier;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityHeatBoiler} / {@code TileEntityHeatBoilerIndustrial} —
 * pull heat from {@link IHeatSource} below, {@link FT_Heatable} BOILER convert.
 * Explosion / Tom fire / audio skipped.
 * ROR: CE {@code TileEntityHeatBoiler.java:396-412} / industrial {@code :348-360}.
 */
public class HeatBoilerBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider, IRORValueProvider {

    public static final int MAX_HEAT = 12_800_000;
    public static final double DIFFUSION = 0.1D;

    public final FluidTankNTM water;
    public final FluidTankNTM steam;
    public final int maxHeat;
    public int heat;
    public boolean isOn;

    public static HeatBoilerBlockEntity small(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new HeatBoilerBlockEntity(type, pos, state, 16_000, 16_000 * 100, MAX_HEAT);
    }

    public static HeatBoilerBlockEntity industrial(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new HeatBoilerBlockEntity(type, pos, state, 64_000, 64_000 * 100, MAX_HEAT);
    }

    public HeatBoilerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 int waterCap, int steamCap, int maxHeat) {
        super(type, pos, state, 1, true, false);
        this.water = new FluidTankNTM(Fluids.WATER, waterCap).withOwner(this);
        this.steam = new FluidTankNTM(Fluids.STEAM, steamCap).withOwner(this);
        this.maxHeat = maxHeat;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heatBoiler");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(0);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            water.setTankType(ident.getType(level, worldPosition, id));
        }

        tryPullHeat();
        isOn = false;
        tryConvert();

        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                trySubscribe(water.getTankType(), level, worldPosition.relative(d, 2), d);
                if (steam.getFill() > 0) tryProvide(steam, level, worldPosition.relative(d, 2), d);
            }
            trySubscribe(water.getTankType(), level, worldPosition.above(4), Direction.UP);
            if (steam.getFill() > 0) tryProvide(steam, level, worldPosition.above(4), Direction.UP);
        }

        dataChanged();
        networkPackMK2(25);
    }

    private void tryPullHeat() {
        if (heat >= maxHeat) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * DIFFUSION);
                source.useUpHeat(diff);
                heat = Math.min(heat + diff, maxHeat);
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    private void tryConvert() {
        if (!water.getTankType().hasTrait(FT_Heatable.class)) return;
        FT_Heatable trait = water.getTankType().getTrait(FT_Heatable.class);
        if (trait.getEfficiency(FT_Heatable.HeatingType.BOILER) <= 0) return;
        FT_Heatable.HeatingStep entry = trait.getFirstStep();
        int heatReq = (int) Math.max(entry.heatReq / trait.getEfficiency(FT_Heatable.HeatingType.BOILER), 1);
        int inputOps = water.getFill() / entry.amountReq;
        int outputOps = (steam.getMaxFill() - steam.getFill()) / entry.amountProduced;
        int heatOps = heat / heatReq;
        int ops = Math.min(inputOps, Math.min(outputOps, heatOps));
        if (ops <= 0) return;
        water.setFill(water.getFill() - entry.amountReq * ops);
        steam.setFill(steam.getFill() + entry.amountProduced * ops);
        steam.setTankType(entry.typeProduced);
        heat -= heatReq * ops;
        isOn = true;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(water);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(water, steam);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        water.writeToNBT(tag, "water");
        steam.writeToNBT(tag, "steam");
        tag.putInt("heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        water.readFromNBT(tag, "water");
        steam.readFromNBT(tag, "steam");
        heat = tag.getInt("heat");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        water.serialize(buf);
        steam.serialize(buf);
        buf.writeInt(heat);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        water.deserialize(buf);
        steam.deserialize(buf);
        heat = buf.readInt();
        isOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeatBoilerMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE HeatBoiler :396-401 / Industrial :348-353
        return new String[]{
                PREFIX_VALUE + "input",
                PREFIX_VALUE + "output"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE tanks[0]/[1] → water/steam. Explosion skipped → never the hasExploded zero-path.
        if ((PREFIX_VALUE + "input").equals(name)) return "" + water.getFill();
        if ((PREFIX_VALUE + "output").equals(name)) return "" + steam.getFill();
        return null;
    }
}
