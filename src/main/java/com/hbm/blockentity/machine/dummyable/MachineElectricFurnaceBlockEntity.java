package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.MachineElectricFurnaceBlock;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.container.machine.dummyable.ElectricFurnaceMenu;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * CE {@code TileEntityMachineElectricFurnace} — 50 HE/t, 100 ticks.
 * {@code incrementPollution(SOOT, SOOT_PER_SECOND)} every 20t while processing Exact CE {@code :188-189}.
 * UpgradeManager / battery charge skipped. Block-swap Exact CE {@code :200-205}.
 */
public class MachineElectricFurnaceBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000L;
    public static final int CONSUMPTION = 50;
    public static final int MAX_PROGRESS = 100;

    public long power;
    public int progress;
    public int cooldown;

    public MachineElectricFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.electricFurnace");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 1 && smelt(stack).isPresent();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 2;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (cooldown > 0) cooldown--;
        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.values()) trySubscribe(level, worldPosition.relative(d), d);
        }

        if (power < CONSUMPTION) cooldown = 20;

        if (power >= CONSUMPTION && canProcess()) {
            progress++;
            power -= CONSUMPTION;
            // CE TileEntityMachineElectricFurnace.java:188-189
            if (level.getGameTime() % 20 == 0) {
                PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT,
                        PollutionHandler.SOOT_PER_SECOND);
            }
            if (progress >= MAX_PROGRESS) {
                progress = 0;
                process();
            }
        } else {
            progress = 0;
        }

        // Exact CE TileEntityMachineElectricFurnace.java:200-205
        boolean trigger = power < CONSUMPTION || !canProcess() || progress != 0;
        if (trigger) {
            MachineElectricFurnaceBlock.updateBlockState(progress > 0, level, worldPosition);
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean canProcess() {
        if (cooldown > 0) return false;
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(1));
        if (out.isEmpty()) return false;
        ItemStack dest = inventory.getStackInSlot(2);
        if (dest.isEmpty()) return true;
        ItemStack result = out.get();
        return ItemStack.isSameItemSameComponents(dest, result)
                && dest.getCount() + result.getCount() <= dest.getMaxStackSize();
    }

    private void process() {
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(1));
        if (out.isEmpty()) return;
        ItemStack result = out.get();
        ItemStack dest = inventory.getStackInSlot(2);
        if (dest.isEmpty()) inventory.setStackInSlot(2, result.copy());
        else dest.grow(result.getCount());
        inventory.extractItem(1, 1, false);
    }

    private Optional<ItemStack> smelt(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        Optional<RecipeHolder<SmeltingRecipe>> rec = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return rec.map(h -> h.value().assemble(new SingleRecipeInput(input), level.registryAccess()));
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putInt("cd", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        cooldown = tag.getInt("cd");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ElectricFurnaceMenu(id, inv, this);
    }
}
