package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FunnelMenu;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * CE {@code TileEntityMachineFunnel} — 9→1 / 4→1 compress via vanilla crafting.
 */
public class MachineFunnelBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int MODE_AUTO = 0;
    public static final int MODE_3x3 = 1;
    public static final int MODE_2x2 = 2;

    public int mode;
    private final Map<Item, ItemStack> from9 = new HashMap<>();
    private final Map<Item, ItemStack> from4 = new HashMap<>();

    public MachineFunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 18, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_funnel");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot > 8) return false;
        if (!inventory.getStackInSlot(slot).isEmpty()) return true;
        return !getFrom9(stack).isEmpty() || !getFrom4(stack).isEmpty();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 9;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        if (side == Direction.DOWN) return new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17};
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (int i = 0; i < 9; i++) {
            ItemStack in = inventory.getStackInSlot(i);
            if (in.isEmpty()) continue;
            int count = 9;
            ItemStack compressed = (mode == MODE_2x2 || in.getCount() < 9) ? ItemStack.EMPTY : getFrom9(in);
            if (compressed.isEmpty()) {
                compressed = (mode == MODE_3x3 || in.getCount() < 4) ? ItemStack.EMPTY : getFrom4(in);
                count = 4;
            }
            if (compressed.isEmpty() || in.getCount() < count) continue;
            ItemStack dest = inventory.getStackInSlot(i + 9);
            if (dest.isEmpty()) {
                inventory.setStackInSlot(i + 9, compressed.copy());
                inventory.extractItem(i, count, false);
            } else if (ItemStack.isSameItemSameComponents(dest, compressed)
                    && dest.getCount() + compressed.getCount() <= dest.getMaxStackSize()) {
                dest.grow(compressed.getCount());
                inventory.extractItem(i, count, false);
            }
        }

        dataChanged();
        networkPackMK2(15);
    }

    public void cycleMode() {
        mode = (mode + 1) % 3;
        setChanged();
    }

    private ItemStack getFrom4(ItemStack ingredient) {
        Item key = ingredient.getItem();
        if (from4.containsKey(key)) return from4.get(key);
        if (level == null) return ItemStack.EMPTY;
        List<ItemStack> cells = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) cells.add(ingredient.copyWithCount(1));
        ItemStack match = match(CraftingInput.of(2, 2, cells));
        from4.put(key, match);
        return match;
    }

    private ItemStack getFrom9(ItemStack ingredient) {
        Item key = ingredient.getItem();
        if (from9.containsKey(key)) return from9.get(key);
        if (level == null) return ItemStack.EMPTY;
        List<ItemStack> cells = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) cells.add(ingredient.copyWithCount(1));
        ItemStack match = match(CraftingInput.of(3, 3, cells));
        from9.put(key, match);
        return match;
    }

    private ItemStack match(CraftingInput input) {
        Optional<RecipeHolder<CraftingRecipe>> rec = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        return rec.map(h -> h.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("mode", mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = tag.getInt("mode");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(mode);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FunnelMenu(id, inv, this);
    }
}
