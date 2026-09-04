package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.container.machine.dummyable.FurnaceIronMenu;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
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
 * CE {@code TileEntityFurnaceIron.java}:61-116 — coal-fuel vanilla smelt, baseTime 160,
 * SPEED upgrade slot scan.
 * {@code incrementPollution(SOOT, SOOT_PER_SECOND)} every 20t while smelting Exact CE {@code :116}.
 * Smoke particles stay skipped (VFX).
 */
public class FurnaceIronBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int BASE_TIME = 160;

    public int maxBurnTime;
    public int burnTime;
    public int progress;
    public int processingTime = BASE_TIME;
    public boolean wasOn;

    public FurnaceIronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceIron");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return smeltResult(stack).isPresent();
        if (slot == 1 || slot == 2) return getBurnTime(stack) > 0;
        if (slot == 4) return stack.getItem() instanceof ItemMachineUpgrade u && u.getType() == UpgradeType.SPEED;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        processingTime = Math.max(20, BASE_TIME - 15 * speedTier());
        wasOn = false;

        if (burnTime <= 0) {
            for (int i = 1; i < 3; i++) {
                ItemStack fuel = inventory.getStackInSlot(i);
                int fuelTime = getBurnTime(fuel);
                if (fuelTime > 0) {
                    maxBurnTime = burnTime = fuelTime;
                    inventory.extractItem(i, 1, false);
                    setChanged();
                    break;
                }
            }
        }

        if (canSmelt()) {
            wasOn = true;
            progress++;
            burnTime--;
            if (progress >= processingTime) {
                Optional<ItemStack> result = smeltResult(inventory.getStackInSlot(0));
                if (result.isPresent()) {
                    ItemStack out = result.get();
                    ItemStack dest = inventory.getStackInSlot(3);
                    if (dest.isEmpty()) inventory.setStackInSlot(3, out.copy());
                    else dest.grow(out.getCount());
                    inventory.extractItem(0, 1, false);
                }
                progress = 0;
                setChanged();
            }
            // CE TileEntityFurnaceIron.java:116
            if (level.getGameTime() % 20 == 0) {
                PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT,
                        PollutionHandler.SOOT_PER_SECOND);
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(50);
    }

    public boolean canSmelt() {
        if (burnTime <= 0) return false;
        Optional<ItemStack> result = smeltResult(inventory.getStackInSlot(0));
        if (result.isEmpty()) return false;
        ItemStack dest = inventory.getStackInSlot(3);
        if (dest.isEmpty()) return true;
        ItemStack out = result.get();
        if (!ItemStack.isSameItemSameComponents(dest, out)) return false;
        return dest.getCount() + out.getCount() <= dest.getMaxStackSize();
    }

    private Optional<ItemStack> smeltResult(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        Optional<RecipeHolder<SmeltingRecipe>> rec = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return rec.map(h -> h.value().assemble(new SingleRecipeInput(input), level.registryAccess()));
    }

    private int speedTier() {
        ItemStack up = inventory.getStackInSlot(4);
        if (up.getItem() instanceof ItemMachineUpgrade u && u.getType() == UpgradeType.SPEED) {
            return u.getTier();
        }
        return 0;
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("burn", burnTime);
        tag.putInt("maxBurn", maxBurnTime);
        tag.putInt("progress", progress);
        tag.putInt("proc", processingTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("burn");
        maxBurnTime = tag.getInt("maxBurn");
        progress = tag.getInt("progress");
        processingTime = tag.getInt("proc");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(maxBurnTime);
        buf.writeInt(burnTime);
        buf.writeInt(progress);
        buf.writeInt(processingTime);
        buf.writeBoolean(wasOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        maxBurnTime = buf.readInt();
        burnTime = buf.readInt();
        progress = buf.readInt();
        processingTime = buf.readInt();
        wasOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FurnaceIronMenu(id, inv, this);
    }
}
