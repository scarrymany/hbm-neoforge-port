package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FireboxMenu;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityHeaterFirebox} / {@code TileEntityFireboxBase.java}:50-113 —
 * 2 fuel slots, baseHeat 100, maxHeat 100_000. Ashpit / pollution / door anim skipped.
 * ModuleBurnTime fuel-class mods → vanilla burn time.
 */
public class HeaterFireboxBlockEntity extends MachineBaseBlockEntity
        implements IHeatSource, ITickableBE, MenuProvider {

    public static final int BASE_HEAT = 100;
    public static final int MAX_HEAT = 100_000;

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
        return slot < 2 && getBurnTime(stack) > 0;
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
        tickBurn(BASE_HEAT, 1D, MAX_HEAT);
        dataChanged();
        networkPackMK2(50);
    }

    protected void tickBurn(int baseHeat, double timeMult, int maxHeat) {
        wasOn = false;
        if (burnTime <= 0) {
            for (int i = 0; i < 2; i++) {
                ItemStack fuel = inventory.getStackInSlot(i);
                int base = getBurnTime(fuel);
                if (base > 0) {
                    maxBurnTime = burnTime = Math.max(1, (int) (base * timeMult));
                    burnHeat = baseHeat;
                    inventory.extractItem(i, 1, false);
                    wasOn = true;
                    setChanged();
                    break;
                }
            }
        } else {
            if (heatEnergy < maxHeat) burnTime--;
            wasOn = true;
        }
        if (wasOn) {
            heatEnergy = Math.min(heatEnergy + burnHeat, maxHeat);
        } else {
            heatEnergy = Math.max(heatEnergy - Math.max(heatEnergy / 1000, 1), 0);
            burnHeat = 0;
        }
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return stack.getBurnTime(RecipeType.SMELTING);
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
}
