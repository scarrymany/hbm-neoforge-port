package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.machine.rbmk.RBMKHeaterMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Heater column — CE {@code TileEntityRBMKHeater}.
 * {@code feed.setType(0)} Exact CE {@code :66}. {@code FT_Heatable} HEATEXCHANGER Exact CE {@code :68-95}.
 * Slot 0 Exact CE {@code ContainerRBMKHeater.java:24}.
 */
public class RBMKHeaterBlockEntity extends RBMKSlottedBlockEntity
        implements IFluidStandardTransceiverMK2, MenuProvider, IRORValueProvider {

    public final FluidTankNTM feed;
    public final FluidTankNTM steam;

    public RBMKHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
        feed = new FluidTankNTM(Fluids.COOLANT, 16_000).withOwner(this);
        steam = new FluidTankNTM(Fluids.COOLANT_HOT, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkHeater");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            // CE TileEntityRBMKHeater.java:66
            if (this.heat <= 50 || this.feed.getFill() <= 0) this.feed.setType(0, inventory);

            if (feed.getTankType().hasTrait(FT_Heatable.class)) {
                FT_Heatable trait = feed.getTankType().getTrait(FT_Heatable.class);
                HeatingStep step = trait.getFirstStep();
                steam.setTankType(step.typeProduced);
                double tempRange = this.heat - steam.getTankType().temperature;
                double eff = trait.getEfficiency(HeatingType.HEATEXCHANGER);

                if (tempRange > 0 && eff > 0) {
                    // CE :76 — 2000 TU/°C × HEATEXCHANGER eff
                    double tuPerDegree = 2_000D * eff;
                    int inputOps = feed.getFill() / step.amountReq;
                    int outputOps = (steam.getMaxFill() - steam.getFill()) / step.amountProduced;
                    int tempOps = (int) Math.floor((tempRange * tuPerDegree) / step.heatReq);
                    int ops = Math.min(inputOps, Math.min(outputOps, tempOps));

                    feed.setFill(feed.getFill() - step.amountReq * ops);
                    steam.setFill(steam.getFill() + step.amountProduced * ops);
                    this.heat -= (step.heatReq * ops / tuPerDegree) * eff;
                }

                if (eff <= 0) {
                    feed.setTankType(Fluids.NONE);
                    steam.setTankType(Fluids.NONE);
                }
            } else {
                feed.setTankType(Fluids.NONE);
                steam.setTankType(Fluids.NONE);
            }

            trySubscribe(feed.getTankType(), level, worldPosition.below(), Direction.UP);
            if (steam.getFill() > 0) tryProvide(steam, level, worldPosition.above(), Direction.DOWN);
        }

        super.updateEntity();
    }

    @Override
    public void onMelt(int reduce) {
        for (int i = 0; i < 2; i++) spawnDebris("BLANK");
        standardMelt(reduce);
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.HEATEX;
    }

    @Override
    public RBMKColumn getConsoleData() {
        RBMKColumn.HeaterColumn data = (RBMKColumn.HeaterColumn) super.getConsoleData();
        data.water = feed.getFill();
        data.maxWater = feed.getMaxFill();
        data.steam = steam.getFill();
        data.maxSteam = steam.getMaxFill();
        data.coldType = (short) feed.getTankType().getID();
        data.hotType = (short) steam.getTankType().getID();
        return data;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(feed, steam);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(feed);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        feed.writeToNBT(tag, "feed");
        steam.writeToNBT(tag, "steam");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        feed.readFromNBT(tag, "feed");
        steam.readFromNBT(tag, "steam");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        feed.serialize(buf);
        steam.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        feed.deserialize(buf);
        steam.deserialize(buf);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rbmkHeater");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RBMKHeaterMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE TileEntityRBMKHeater.java:315-319
        return new String[]{
                PREFIX_VALUE + "in",
                PREFIX_VALUE + "out"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :323-326
        if ((PREFIX_VALUE + "in").equals(name)) return "" + this.feed.getFill();
        if ((PREFIX_VALUE + "out").equals(name)) return "" + this.steam.getFill();
        return null;
    }
}
