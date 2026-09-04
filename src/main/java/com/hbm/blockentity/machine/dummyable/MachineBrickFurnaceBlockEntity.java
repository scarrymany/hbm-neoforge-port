package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.BrickFurnaceMenu;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.machine.Phase11ProcessItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * CE {@code TileEntityFurnaceBrick} — fuel + vanilla smelt.
 * Exact CE ash {@code :91-98}/{@code :169-181} via {@code getAshFromFuel} hopper slot 3.
 * Block-swap stay skipped.
 */
public class MachineBrickFurnaceBlockEntity extends MachineBaseBlockEntity implements ITickableBE, MenuProvider {

    public static final int MAX_PROGRESS = 200;
    private static final int ASH_THRESHOLD = 2000;

    public int burnTime;
    public int maxBurnTime;
    public int progress;
    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;

    public MachineBrickFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceBrick");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return smelt(stack).isPresent();
        if (slot == 1) return getBurnTime(stack) > 0;
        return false;
    }

    /** Exact CE {@code TileEntityFurnaceBrick.java}:150-152 — output + ash hopper-out. */
    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 2;
    }

    /** Exact CE {@code :38}/{@code :145-147} — DOWN {2,3}, UP {0}, sides {1}. */
    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        if (side == Direction.DOWN) return new int[]{2, 3};
        if (side == Direction.UP) return new int[]{0};
        return new int[]{1};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (burnTime > 0) burnTime--;

        if (burnTime == 0 && canSmelt()) {
            int fuel = getBurnTime(inventory.getStackInSlot(1));
            if (fuel > 0) {
                maxBurnTime = burnTime = fuel;
                // Exact CE :89-98 — shrink then classify remaining stack, ash += burnTime, threshold 2000
                inventory.extractItem(1, 1, false);
                EnumAshType type = HeaterFireboxBlockEntity.getAshFromFuel(inventory.getStackInSlot(1));
                if (type == EnumAshType.WOOD) ashLevelWood += burnTime;
                if (type == EnumAshType.COAL) ashLevelCoal += burnTime;
                if (type == EnumAshType.MISC) ashLevelMisc += burnTime;
                if (processAsh(ashLevelWood, EnumAshType.WOOD, ASH_THRESHOLD)) ashLevelWood -= ASH_THRESHOLD;
                if (processAsh(ashLevelCoal, EnumAshType.COAL, ASH_THRESHOLD)) ashLevelCoal -= ASH_THRESHOLD;
                if (processAsh(ashLevelMisc, EnumAshType.MISC, ASH_THRESHOLD)) ashLevelMisc -= ASH_THRESHOLD;
            }
        }

        if (burnTime > 0 && canSmelt()) {
            progress += burnSpeed();
            if (progress >= MAX_PROGRESS) {
                progress = 0;
                process();
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(15);
    }

    /** Exact CE {@code TileEntityFurnaceBrick.java}:41-51/{@code :133-137}. */
    private int burnSpeed() {
        ItemStack in = inventory.getStackInSlot(0);
        if (in.is(Items.CLAY_BALL) || in.is(Phase11ProcessItems.BALL_FIRECLAY.get())
                || in.is(Blocks.NETHERRACK.asItem())) return 4;
        if (in.is(Blocks.COBBLESTONE.asItem()) || in.is(Blocks.SAND.asItem()) || in.is(ItemTags.LOGS)) return 2;
        return 1;
    }

    /** Exact CE {@code TileEntityFurnaceBrick.java}:169-181 — slot 3 hopper ash. */
    protected boolean processAsh(int level, EnumAshType type, int threshold) {
        if (level < threshold) return false;
        ItemStack dest = inventory.getStackInSlot(3);
        ItemStack ash = new ItemStack(BilletPowderItems.powderAsh(type).get());
        if (dest.isEmpty()) {
            inventory.setStackInSlot(3, ash);
            return true;
        }
        if (dest.is(ash.getItem()) && dest.getCount() < dest.getMaxStackSize()) {
            dest.grow(1);
            return true;
        }
        return false;
    }

    private boolean canSmelt() {
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(0));
        if (out.isEmpty()) return false;
        ItemStack dest = inventory.getStackInSlot(2);
        if (dest.isEmpty()) return true;
        ItemStack result = out.get();
        return ItemStack.isSameItemSameComponents(dest, result)
                && dest.getCount() + result.getCount() <= dest.getMaxStackSize();
    }

    private void process() {
        Optional<ItemStack> out = smelt(inventory.getStackInSlot(0));
        if (out.isEmpty()) return;
        ItemStack result = out.get();
        ItemStack dest = inventory.getStackInSlot(2);
        if (dest.isEmpty()) inventory.setStackInSlot(2, result.copy());
        else dest.grow(result.getCount());
        inventory.extractItem(0, 1, false);
    }

    private Optional<ItemStack> smelt(ItemStack input) {
        if (level == null || input.isEmpty()) return Optional.empty();
        Optional<RecipeHolder<SmeltingRecipe>> rec = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
        return rec.map(h -> h.value().assemble(new SingleRecipeInput(input), level.registryAccess()));
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTime = tag.getInt("burn");
        maxBurnTime = tag.getInt("maxBurn");
        progress = tag.getInt("progress");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(burnTime);
        buf.writeInt(maxBurnTime);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        burnTime = buf.readInt();
        maxBurnTime = buf.readInt();
        progress = buf.readInt();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BrickFurnaceMenu(id, inv, this);
    }
}
