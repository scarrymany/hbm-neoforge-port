package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.dummyable.DiFurnaceMenu;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT.BlastFurnaceRecipe;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityDiFurnace} — fuel + 2-in alloy via {@link BlastFurnaceRecipesNT}.
 * Smoke / pollution / extension / side-config / block-swap skipped.
 */
public class MachineDiFurnaceBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int MAX_FUEL = 12_800;
    public static final int PROCESS_TICKS = 400;

    public int fuel;
    public int progress;

    public MachineDiFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.diFurnace");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 3) return false;
        if (slot == 2) return getItemPower(stack) > 0;
        return BlastFurnaceRecipesNT.INSTANCE.isIngredient(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        if (side == Direction.DOWN) return new int[]{3};
        if (side == Direction.UP) return new int[]{0, 1};
        return new int[]{2};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        int power = getItemPower(inventory.getStackInSlot(2));
        if (power > 0 && fuel <= MAX_FUEL - power) {
            fuel += power;
            inventory.extractItem(2, 1, false);
        }

        if (canProcess()) {
            fuel--;
            progress++;
            if (progress >= PROCESS_TICKS) {
                progress = 0;
                process();
            }
            if (fuel < 0) fuel = 0;
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(15);
    }

    private boolean canProcess() {
        if (fuel <= 0) return false;
        ItemStack a = inventory.getStackInSlot(0);
        ItemStack b = inventory.getStackInSlot(1);
        if (a.isEmpty() || b.isEmpty()) return false;
        BlastFurnaceRecipe recipe = BlastFurnaceRecipesNT.INSTANCE.getRecipe(a, b);
        if (recipe == null || recipe.inputs.length < 2) return false;
        if (!hasQuantities(recipe)) return false;
        return canOutput(recipe);
    }

    private boolean hasQuantities(BlastFurnaceRecipe recipe) {
        AStack in0 = recipe.inputs[0];
        AStack in1 = recipe.inputs[1];
        ItemStack s0 = inventory.getStackInSlot(0);
        ItemStack s1 = inventory.getStackInSlot(1);
        if (in0.matchesRecipe(s0, false) && in1.matchesRecipe(s1, false)) return true;
        return in0.matchesRecipe(s1, false) && in1.matchesRecipe(s0, false);
    }

    private boolean canOutput(BlastFurnaceRecipe recipe) {
        for (int i = 0; i < recipe.outputs.length; i++) {
            ItemStack out = recipe.outputs[i];
            if (out.isEmpty()) continue;
            ItemStack dest = inventory.getStackInSlot(3);
            if (dest.isEmpty()) return true;
            if (!ItemStack.isSameItem(dest, out)) return false;
            return dest.getCount() + out.getCount() <= dest.getMaxStackSize();
        }
        return true;
    }

    private void process() {
        BlastFurnaceRecipe recipe = BlastFurnaceRecipesNT.INSTANCE.getRecipe(inventory.getStackInSlot(0), inventory.getStackInSlot(1));
        if (recipe == null || recipe.inputs.length < 2) return;
        AStack in0 = recipe.inputs[0];
        AStack in1 = recipe.inputs[1];
        if (in0.matchesRecipe(inventory.getStackInSlot(0), false) && in1.matchesRecipe(inventory.getStackInSlot(1), false)) {
            inventory.extractItem(0, in0.count(), false);
            inventory.extractItem(1, in1.count(), false);
        } else {
            inventory.extractItem(0, in1.count(), false);
            inventory.extractItem(1, in0.count(), false);
        }
        for (ItemStack out : recipe.outputs) {
            if (out.isEmpty()) continue;
            ItemStack dest = inventory.getStackInSlot(3);
            if (dest.isEmpty()) inventory.setStackInSlot(3, out.copy());
            else dest.grow(out.getCount());
            break;
        }
    }

    /** CE {@code BlastFurnaceRecipes.getItemPower} — explicit table + vanilla burn/8 fallback. */
    public static int getItemPower(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        Item item = stack.getItem();
        if (item == Items.COAL) return 200;
        if (item == Items.CHARCOAL) return 150;
        if (item == Items.COAL_BLOCK) return 2000;
        if (item == Items.LAVA_BUCKET) return 12_800;
        if (item == Items.BLAZE_ROD) return 1000;
        if (item == Items.BLAZE_POWDER) return 300;
        if (item == hbmItem("briquette")) return 200;
        if (item == hbmItem("lignite")) return 150;
        if (item == hbmItem("solid_fuel")) return 400;
        if (item == hbmItem("solid_fuel_presto")) return 800;
        if (item == hbmItem("solid_fuel_presto_triplet")) return 2400;
        if (item == hbmItem("coke")) return 400;
        if (item == hbmItem("coke_coal")) return 400;
        if (item == hbmItem("coke_lignite")) return 400;
        if (item == hbmItem("coke_petroleum")) return 400;
        int burn = stack.getBurnTime(RecipeType.SMELTING);
        return burn > 0 ? Math.max(1, burn / 8) : 0;
    }

    private static Item hbmItem(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("powerTime", fuel);
        tag.putInt("cookTime", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("powerTime");
        progress = tag.getInt("cookTime");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(fuel);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        fuel = buf.readInt();
        progress = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DiFurnaceMenu(id, inv, this);
    }
}
