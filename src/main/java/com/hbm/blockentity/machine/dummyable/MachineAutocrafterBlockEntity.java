package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.AutocrafterMenu;
import com.hbm.lib.Library;
import com.hbm.util.ItemStackUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityMachineAutocrafter} — 21 slots, 100 HE/craft.
 * Mode cycle is Exact CE {@code nextMode}: exact → wildcard → tag keys from
 * {@link ItemStackUtil#getOreDictNames} (1.21 tag ids, same helper already in-tree).
 */
public class MachineAutocrafterBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";
    public static final int CONSUMPTION = 100;
    public static final long MAX_POWER = CONSUMPTION * 100L;

    public final String[] modes = new String[9];
    public long power;
    public int recipeIndex;
    public int recipeCount;
    private final List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

    public MachineAutocrafterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 21, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.autocrafter");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 10 || slot > 18) return false;
        if (stack.getCount() > 1 && stack.hasCraftingRemainingItem()) return false;
        ItemStack filter = inventory.getStackInSlot(slot - 10);
        String mode = modes[slot - 10];
        if (filter.isEmpty() || mode == null || mode.isEmpty()) return false;
        return isValidForFilter(filter, mode, stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        if (slot == 19) return true;
        if (slot > 9 && slot < 19) {
            ItemStack filter = inventory.getStackInSlot(slot - 10);
            String mode = modes[slot - 10];
            if (mode == null || mode.isEmpty()) return true;
            return !isValidForFilter(filter, mode, stack);
        }
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        balanceInputs();
        power = Library.chargeTEFromItems(inventory, 20, power, MAX_POWER);
        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.values()) trySubscribe(level, worldPosition.relative(d), d);
        }

        if (!recipes.isEmpty() && power >= CONSUMPTION) {
            RecipeHolder<CraftingRecipe> holder = recipes.get(recipeIndex % recipes.size());
            CraftingInput grid = recipeGrid();
            if (holder.value().matches(grid, level)) {
                ItemStack result = holder.value().assemble(grid, level.registryAccess());
                if (!result.isEmpty() && insertOutput(result)) {
                    List<ItemStack> remaining = holder.value().getRemainingItems(grid);
                    for (int s = 0; s < 9; s++) {
                        int invSlot = 10 + s;
                        ItemStack in = inventory.getStackInSlot(invSlot);
                        if (!in.isEmpty()) inventory.extractItem(invSlot, 1, false);
                        ItemStack rem = remaining.get(s);
                        if (rem.isEmpty()) continue;
                        if (inventory.getStackInSlot(invSlot).isEmpty()) {
                            inventory.setStackInSlot(invSlot, rem.copy());
                        } else {
                            boolean placed = false;
                            for (int k = 10; k < 19; k++) {
                                if (inventory.getStackInSlot(k).isEmpty()) {
                                    inventory.setStackInSlot(k, rem.copy());
                                    placed = true;
                                    break;
                                }
                            }
                            if (!placed && level != null) {
                                level.addFreshEntity(new ItemEntity(level,
                                        worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                                        rem.copy()));
                            }
                        }
                    }
                    power -= CONSUMPTION;
                }
            }
        }

        dataChanged();
        networkPackMK2(15);
    }

    private boolean insertOutput(ItemStack result) {
        ItemStack dest = inventory.getStackInSlot(19);
        if (dest.isEmpty()) {
            inventory.setStackInSlot(19, result.copy());
            return true;
        }
        if (ItemStack.isSameItemSameComponents(dest, result)
                && dest.getCount() + result.getCount() <= dest.getMaxStackSize()) {
            dest.grow(result.getCount());
            return true;
        }
        return false;
    }

    public void initPattern(ItemStack stack, int i) {
        if (level != null && level.isClientSide) return;
        modes[i] = stack.isEmpty() ? null : MODE_EXACT;
    }

    public void nextMode(int i) {
        if (level != null && level.isClientSide) return;
        ItemStack stack = inventory.getStackInSlot(i);
        if (stack.isEmpty()) {
            modes[i] = null;
            return;
        }
        // Exact CE TileEntityMachineAutocrafter.java:90-130
        if (modes[i] == null) {
            modes[i] = MODE_EXACT;
        } else if (MODE_EXACT.equals(modes[i])) {
            modes[i] = MODE_WILDCARD;
        } else if (MODE_WILDCARD.equals(modes[i])) {
            List<String> names = ItemStackUtil.getOreDictNames(stack);
            modes[i] = names.isEmpty() ? MODE_EXACT : names.getFirst();
        } else {
            List<String> names = ItemStackUtil.getOreDictNames(stack);
            if (names.size() < 2 || modes[i].equals(names.getLast())) {
                modes[i] = MODE_EXACT;
            } else {
                for (int j = 0; j < names.size() - 1; j++) {
                    if (modes[i].equals(names.get(j))) {
                        modes[i] = names.get(j + 1);
                        setChanged();
                        return;
                    }
                }
            }
        }
        setChanged();
    }

    public void nextTemplate() {
        if (level != null && level.isClientSide) return;
        if (recipes.isEmpty()) {
            recipeIndex = 0;
            inventory.setStackInSlot(9, ItemStack.EMPTY);
            return;
        }
        recipeIndex = (recipeIndex + 1) % recipes.size();
        inventory.setStackInSlot(9, recipes.get(recipeIndex).value().assemble(templateGrid(), level.registryAccess()));
        setChanged();
    }

    public void updateTemplateGrid() {
        recipes.clear();
        if (level == null) {
            recipeCount = 0;
            recipeIndex = 0;
            inventory.setStackInSlot(9, ItemStack.EMPTY);
            return;
        }
        CraftingInput grid = templateGrid();
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            if (holder.value().matches(grid, level)) recipes.add(holder);
        }
        recipeCount = recipes.size();
        recipeIndex = 0;
        if (recipes.isEmpty()) inventory.setStackInSlot(9, ItemStack.EMPTY);
        else inventory.setStackInSlot(9, recipes.getFirst().value().assemble(grid, level.registryAccess()));
        setChanged();
    }

    public boolean isValidForFilter(ItemStack filter, String mode, ItemStack input) {
        // Exact CE TileEntityMachineAutocrafter.java:427-436
        if (MODE_EXACT.equals(mode)) return ItemStack.isSameItemSameComponents(input, filter);
        if (MODE_WILDCARD.equals(mode)) return input.getItem() == filter.getItem();
        return ItemStackUtil.getOreDictNames(input).contains(mode);
    }

    private CraftingInput templateGrid() {
        return gridFrom(0);
    }

    private CraftingInput recipeGrid() {
        return gridFrom(10);
    }

    private CraftingInput gridFrom(int start) {
        List<ItemStack> cells = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) cells.add(inventory.getStackInSlot(start + i));
        return CraftingInput.of(3, 3, cells);
    }

    private void balanceInputs() {
        for (int i = 10; i < 19; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            List<Integer> targets = new ArrayList<>();
            int total = stack.getCount();
            targets.add(i);
            for (int j = 10; j < 19; j++) {
                if (i == j) continue;
                ItemStack other = inventory.getStackInSlot(j);
                if (!other.isEmpty()) {
                    if (ItemStack.isSameItemSameComponents(other, stack)) {
                        targets.add(j);
                        total += other.getCount();
                    }
                } else {
                    ItemStack filter = inventory.getStackInSlot(j - 10);
                    String mode = modes[j - 10];
                    if (!filter.isEmpty() && mode != null && isValidForFilter(filter, mode, stack)) targets.add(j);
                }
            }
            if (targets.size() < 2) continue;
            int min = Integer.MAX_VALUE;
            int max = -1;
            for (int slot : targets) {
                int c = inventory.getStackInSlot(slot).getCount();
                if (c < min) min = c;
                if (c > max) max = c;
            }
            if (max - min <= 1) continue;
            int per = total / targets.size();
            int rem = total % targets.size();
            for (int slot : targets) {
                int amount = per + (rem > 0 ? 1 : 0);
                if (rem > 0) rem--;
                if (amount == 0) inventory.setStackInSlot(slot, ItemStack.EMPTY);
                else inventory.setStackInSlot(slot, stack.copyWithCount(amount));
            }
            return;
        }
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
        tag.putInt("rec", recipeIndex);
        for (int i = 0; i < 9; i++) {
            if (modes[i] != null) tag.putString("mode" + i, modes[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        recipeIndex = tag.getInt("rec");
        for (int i = 0; i < 9; i++) {
            modes[i] = tag.contains("mode" + i) ? tag.getString("mode" + i) : null;
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        for (int i = 0; i < 9; i++) {
            buf.writeBoolean(modes[i] != null);
            if (modes[i] != null) buf.writeUtf(modes[i]);
        }
        buf.writeInt(recipeCount);
        buf.writeInt(recipeIndex);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        for (int i = 0; i < 9; i++) {
            modes[i] = buf.readBoolean() ? buf.readUtf() : null;
        }
        recipeCount = buf.readInt();
        recipeIndex = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AutocrafterMenu(id, inv, this);
    }
}
