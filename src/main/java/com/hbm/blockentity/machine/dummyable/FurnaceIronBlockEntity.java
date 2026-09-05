package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.container.machine.dummyable.FurnaceIronMenu;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.modules.ModuleBurnTime;
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
 * {@code ModuleBurnTime} Exact CE {@code :46-52}/{@code :75}/{@code :185-186}.
 * Container-item leftover Exact CE {@code :80-83}.
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
    /** Exact CE {@code TileEntityFurnaceIron.java}:46-52. */
    public final ModuleBurnTime burnModule = new ModuleBurnTime()
            .setLigniteTimeMod(1.25)
            .setCoalTimeMod(1.25)
            .setCokeTimeMod(1.5)
            .setSolidTimeMod(2)
            .setRocketTimeMod(2)
            .setBalefireTimeMod(2);

    public FurnaceIronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceIron");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Exact CE TileEntityFurnaceIron.java:180-188 — hopper: in / fuel only
        if (slot == 0) return smeltResult(stack).isPresent();
        if (slot < 3) return burnModule.getBurnTime(stack) > 0;
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

        // Exact CE TileEntityFurnaceIron.java:65 / ItemMachineUpgrade.getSpeed :60-71
        processingTime = BASE_TIME - 15 * ItemMachineUpgrade.getSpeed(inventory.getStackInSlot(4));
        wasOn = false;

        if (burnTime <= 0) {
            for (int i = 1; i < 3; i++) {
                ItemStack fuel = inventory.getStackInSlot(i);
                if (fuel.isEmpty()) continue;
                int fuelTime = burnModule.getBurnTime(fuel);
                if (fuelTime > 0) {
                    maxBurnTime = burnTime = fuelTime;
                    // Exact CE :80-83 — shrink then leftover container item
                    ItemStack copy = fuel.copy();
                    ItemStack remainder = fuel.copy();
                    remainder.shrink(1);
                    if (remainder.isEmpty() && copy.hasCraftingRemainingItem()) {
                        inventory.setStackInSlot(i, copy.getCraftingRemainingItem());
                    } else {
                        inventory.setStackInSlot(i, remainder);
                    }
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
