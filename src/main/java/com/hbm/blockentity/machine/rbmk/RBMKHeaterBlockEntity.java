package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.machine.rbmk.RBMKHeaterMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Heater column - the coolant-loop counterpart to {@link RBMKBoilerBlockEntity}: converts column
 * heat into hot coolant from a cold-coolant feed, instead of steam from water. Ported (simplified,
 * see {@link RBMKBoilerBlockEntity}'s javadoc for the same caveat) from CE's
 * {@code TileEntityRBMKHeater} (328 lines, signature-level survey).
 */
public class RBMKHeaterBlockEntity extends RBMKSlottedBlockEntity
        implements IFluidStandardTransceiverMK2, MenuProvider, IRORValueProvider {

    private static final double HEAT_PER_MB = 2D;

    public final FluidTankNTM feed;
    public final FluidTankNTM steam;

    public RBMKHeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
        feed = new FluidTankNTM(Fluids.COOLANT, 16_000).withOwner(this);
        steam = new FluidTankNTM(Fluids.COOLANT_HOT, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkHeater");
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide && heat > 100D) {
            int byHeat = (int) ((heat - 100D) / HEAT_PER_MB);
            int process = Math.min(byHeat, Math.min(feed.getFill(), steam.getMaxFill() - steam.getFill()));

            if (process > 0) {
                feed.setFill(feed.getFill() - process);
                steam.setFill(steam.getFill() + process);
                heat -= process * HEAT_PER_MB;
            }

            trySubscribe(feed.getTankType(), level, worldPosition.below(), Direction.UP);
            tryProvide(steam, level, worldPosition.above(), Direction.DOWN);
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
