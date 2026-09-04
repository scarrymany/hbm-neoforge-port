package com.hbm.blockentity.machine.dummyable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FireboxMenu;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.IConfigurableMachine;
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

import java.io.IOException;

/**
 * CE {@code TileEntityHeaterFirebox} / {@code TileEntityFireboxBase.java}:50-113 —
 * 2 fuel slots. Ashpit / pollution / door anim skipped.
 * {@link IConfigurableMachine} Exact CE {@code TileEntityHeaterFirebox.java:86-108} ({@code firebox}).
 */
public class HeaterFireboxBlockEntity extends MachineBaseBlockEntity
        implements IHeatSource, ITickableBE, MenuProvider {

    // CE TileEntityHeaterFirebox.java:24-41
    public static int baseHeat = 100;
    public static double timeMult = 1D;
    public static int maxHeatEnergy = 100_000;

    public static ModuleBurnTime burnModule = new ModuleBurnTime()
            .setLigniteTimeMod(1.25)
            .setCoalTimeMod(1.25)
            .setCokeTimeMod(1.25)
            .setSolidTimeMod(1.5)
            .setRocketTimeMod(1.5)
            .setBalefireTimeMod(0.5)
            .setLigniteHeatMod(2)
            .setCoalHeatMod(2)
            .setCokeHeatMod(2)
            .setSolidHeatMod(3)
            .setRocketHeatMod(5)
            .setBalefireHeatMod(15);

    public int maxBurnTime;
    public int burnTime;
    public int burnHeat;
    public int heatEnergy;
    public boolean wasOn;

    public HeaterFireboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.heaterFirebox");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 2 && getModule().getBurnTime(stack) > 0;
    }

    public ModuleBurnTime getModule() {
        return burnModule;
    }

    public int getBaseHeat() {
        return baseHeat;
    }

    public double getTimeMult() {
        return timeMult;
    }

    public int getMaxHeat() {
        return maxHeatEnergy;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        tickBurn();
        dataChanged();
        networkPackMK2(50);
    }

    protected void tickBurn() {
        // CE TileEntityFireboxBase.java:61-113
        wasOn = false;
        if (burnTime <= 0) {
            for (int i = 0; i < 2; i++) {
                ItemStack fuel = inventory.getStackInSlot(i);
                int base = getModule().getBurnTime(fuel);
                if (base > 0) {
                    maxBurnTime = burnTime = Math.max(1, (int) (base * getTimeMult()));
                    burnHeat = getModule().getBurnHeat(getBaseHeat(), fuel);
                    inventory.extractItem(i, 1, false);
                    wasOn = true;
                    setChanged();
                    break;
                }
            }
        } else {
            if (heatEnergy < getMaxHeat()) burnTime--;
            wasOn = true;
        }
        if (wasOn) {
            heatEnergy = Math.min(heatEnergy + burnHeat, getMaxHeat());
        } else {
            heatEnergy = Math.max(heatEnergy - Math.max(heatEnergy / 1000, 1), 0);
            burnHeat = 0;
        }
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("burn", burnTime);
        tag.putInt("maxBurn", maxBurnTime);
        tag.putInt("burnHeat", burnHeat);
        tag.putInt("heat", heatEnergy);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("burn");
        maxBurnTime = tag.getInt("maxBurn");
        burnHeat = tag.getInt("burnHeat");
        heatEnergy = tag.getInt("heat");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(maxBurnTime);
        buf.writeInt(burnTime);
        buf.writeInt(burnHeat);
        buf.writeInt(heatEnergy);
        buf.writeBoolean(wasOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        maxBurnTime = buf.readInt();
        burnTime = buf.readInt();
        burnHeat = buf.readInt();
        heatEnergy = buf.readInt();
        wasOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FireboxMenu(id, inv, this);
    }

    static void readFirebox(JsonObject obj) {
        // CE TileEntityHeaterFirebox.java:92-97 — write key is M:burnModule (rotaryfurnace :480)
        baseHeat = IConfigurableMachine.grab(obj, "I:baseHeat", baseHeat);
        timeMult = IConfigurableMachine.grab(obj, "D:burnTimeMult", timeMult);
        maxHeatEnergy = IConfigurableMachine.grab(obj, "I:heatCap", maxHeatEnergy);
        if (obj.has("M:burnModule")) {
            burnModule.readIfPresent(obj.get("M:burnModule").getAsJsonObject());
        }
    }

    static void writeFirebox(JsonWriter writer) throws IOException {
        // CE TileEntityHeaterFirebox.java:102-107
        writer.name("I:baseHeat").value(baseHeat);
        writer.name("D:burnTimeMult").value(timeMult);
        writer.name("I:heatCap").value(maxHeatEnergy);
        writer.name("M:burnModule").beginObject();
        burnModule.writeConfig(writer);
        writer.endObject();
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "firebox";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readFirebox(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeFirebox(writer);
        }
    }
}
