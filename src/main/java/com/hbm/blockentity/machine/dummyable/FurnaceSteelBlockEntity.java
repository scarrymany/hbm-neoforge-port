package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.container.machine.dummyable.FurnaceSteelMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * CE {@code TileEntityFurnaceSteel.java}:59-111 — 3-lane heat smelter, processTime 40_000,
 * maxHeat 100_000, diffusion 0.05. Ore bonus skipped.
 * {@code incrementPollution(SOOT, SOOT_PER_SECOND*2)} every 20t per smelting lane Exact CE {@code :80}.
 * Smoke particles stay skipped (VFX).
 */
public class FurnaceSteelBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int PROCESS_TIME = 40_000;
    public static final int MAX_HEAT = 100_000;
    public static final double DIFFUSION = 0.05D;

    public final int[] progress = new int[3];
    public int heat;
    public boolean wasOn;

    public FurnaceSteelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceSteel");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 3 && smeltResult(stack).isPresent();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        tryPullHeat();
        wasOn = false;
        int burn = Math.max(0, (heat - MAX_HEAT / 3) / 10);

        for (int i = 0; i < 3; i++) {
            if (!canSmelt(i)) {
                progress[i] = 0;
                continue;
            }
            progress[i] += burn;
            heat -= burn;
            wasOn = true;
            // CE TileEntityFurnaceSteel.java:80
            if (level.getGameTime() % 20 == 0) {
                PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT,
                        PollutionHandler.SOOT_PER_SECOND * 2);
            }
            if (progress[i] >= PROCESS_TIME) {
                Optional<ItemStack> result = smeltResult(inventory.getStackInSlot(i));
                if (result.isPresent()) {
                    ItemStack out = result.get();
                    ItemStack dest = inventory.getStackInSlot(i + 3);
                    if (dest.isEmpty()) inventory.setStackInSlot(i + 3, out.copy());
                    else dest.grow(out.getCount());
                    inventory.extractItem(i, 1, false);
                }
                progress[i] = 0;
                setChanged();
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean canSmelt(int lane) {
        if (heat <= MAX_HEAT / 3) return false;
        Optional<ItemStack> result = smeltResult(inventory.getStackInSlot(lane));
        if (result.isEmpty()) return false;
        ItemStack dest = inventory.getStackInSlot(lane + 3);
        if (dest.isEmpty()) return true;
        ItemStack out = result.get();
        if (!ItemStack.isSameItemSameComponents(dest, out)) return false;
        return dest.getCount() + out.getCount() <= dest.getMaxStackSize();
    }

    private void tryPullHeat() {
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int pulled = (int) (source.getHeatStored() * DIFFUSION);
            if (pulled > 0) {
                source.useUpHeat(pulled);
                heat = Math.min(MAX_HEAT, heat + pulled);
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
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
        tag.putIntArray("progress", progress);
        tag.putInt("heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] p = tag.getIntArray("progress");
        System.arraycopy(p, 0, progress, 0, Math.min(p.length, 3));
        heat = tag.getInt("heat");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeVarIntArray(progress);
        buf.writeInt(heat);
        buf.writeBoolean(wasOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        int[] p = buf.readVarIntArray();
        System.arraycopy(p, 0, progress, 0, Math.min(p.length, 3));
        heat = buf.readInt();
        wasOn = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FurnaceSteelMenu(id, inv, this);
    }
}
