package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.ArcFurnaceMenu;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.recipes.ArcFurnaceRecipes.ArcFurnaceRecipe;
import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.util.CrucibleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CE {@code TileEntityMachineArcFurnaceLarge}: 2.5M HE, 20-slot grid + 5 queue, liquid mode,
 * SPEED upgrade. Lid animation / pollution / particles skipped — process when electrodes+power.
 */
public class MachineArcFurnaceBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 2_500_000;
    public static final int MAX_LIQUID = MaterialShapes.BLOCK.q(128);

    public long power;
    public boolean liquidMode;
    public float progress;
    public boolean isProgressing;
    public int delay;
    public int upgrade;
    public final List<Mats.MaterialStack> liquids = new ArrayList<>();

    public MachineArcFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 30, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineArcFurnaceLarge");
    }

    public int getMaxInputSize() {
        return upgrade == 0 ? 1 : upgrade == 1 ? 4 : upgrade == 2 ? 8 : 16;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 3) return stack.getItem() instanceof ItemArcElectrode;
        if (slot == 3) return Library.isBattery(stack);
        if (slot == 4) return stack.getItem() instanceof ItemMachineUpgrade;
        if (slot > 4) {
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(stack, liquidMode);
            if (recipe == null) return false;
            return liquidMode ? recipe.fluidOutput != null : recipe.solidOutput != null;
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        if (slot < 3) return !(stack.getItem() instanceof ItemArcElectrode);
        if (slot > 4 && slot < 25) return ArcFurnaceRecipes.getOutput(stack, liquidMode) == null;
        if (slot >= 25) return ArcFurnaceRecipes.getOutput(stack, liquidMode) == null;
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2,
                5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
                25, 26, 27, 28, 29};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        upgrade = 0;
        ItemStack up = inventory.getStackInSlot(4);
        if (up.getItem() instanceof ItemMachineUpgrade u && u.getType() == UpgradeType.SPEED) {
            upgrade = Math.min(u.getTier(), 3);
        }

        power = Library.chargeTEFromItems(inventory, 3, power, MAX_POWER);
        isProgressing = false;
        loadIngredients();

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) trySubscribe(level, pos);
        }

        boolean ingredients = hasIngredients();
        boolean electrodes = hasElectrodes();
        int consumption = (int) (1_000 * Math.pow(5, upgrade));

        if (ingredients && electrodes && delay <= 0 && liquids.isEmpty() && power >= consumption) {
            int duration = 400 / (upgrade * 2 + 1);
            progress += 1F / duration;
            isProgressing = true;
            power -= consumption;
            if (progress >= 1F) {
                process();
                progress = 0;
                delay = (int) (120 / (upgrade * 0.5 + 1));
                setChanged();
            }
        } else {
            if (delay > 0) delay--;
            progress = 0;
        }

        if (!liquids.isEmpty()) {
            Direction dir = coreFacing();
            CrucibleUtil.pourFullStack(level,
                    worldPosition.getX() + 0.5 + dir.getStepX() * 2.875,
                    worldPosition.getY() + 1.25,
                    worldPosition.getZ() + 0.5 + dir.getStepZ() * 2.875,
                    6, true, liquids, MaterialShapes.INGOT.q(1));
            liquids.removeIf(o -> o.amount <= 0);
        }

        dataChanged();
        networkPackMK2(50);
    }

    public void loadIngredients() {
        for (int q = 25; q < 30; q++) {
            ItemStack queue = inventory.getStackInSlot(q);
            if (queue.isEmpty()) continue;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(queue, liquidMode);
            if (recipe == null) continue;
            int max = getMaxInputSize();
            if (!liquidMode && recipe.solidOutput != null) {
                max = Math.min(max, queue.getMaxStackSize() / Math.max(1, recipe.solidOutput.getCount()));
            }
            for (int i = 5; i < 25 && !queue.isEmpty(); i++) {
                ItemStack ing = inventory.getStackInSlot(i);
                if (ing.isEmpty()) {
                    int toMove = Math.min(max, queue.getCount());
                    ItemStack moved = queue.copy();
                    moved.setCount(toMove);
                    inventory.setStackInSlot(i, moved);
                    inventory.extractItem(q, toMove, false);
                    queue = inventory.getStackInSlot(q);
                    continue;
                }
                if (!ItemStack.isSameItemSameComponents(queue, ing)) continue;
                int toMove = Math.min(Math.min(ing.getMaxStackSize() - ing.getCount(), queue.getCount()), max - ing.getCount());
                if (toMove > 0) {
                    inventory.extractItem(q, toMove, false);
                    ing.grow(toMove);
                    queue = inventory.getStackInSlot(q);
                }
            }
        }
    }

    public void process() {
        for (int i = 5; i < 25; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(stack, liquidMode);
            if (recipe == null) continue;
            if (!liquidMode && recipe.solidOutput != null) {
                int amount = stack.getCount();
                ItemStack out = recipe.solidOutput.copy();
                out.setCount(out.getCount() * amount);
                inventory.setStackInSlot(i, out);
            }
            if (liquidMode && recipe.fluidOutput != null) {
                while (!inventory.getStackInSlot(i).isEmpty()) {
                    if (stackAmount(liquids) + stackAmount(recipe.fluidOutput) > MAX_LIQUID) break;
                    inventory.extractItem(i, 1, false);
                    for (Mats.MaterialStack mat : recipe.fluidOutput) addToStack(mat);
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            ItemStack el = inventory.getStackInSlot(i);
            if (el.getItem() instanceof ItemArcElectrode electrode && ItemArcElectrode.damage(el)) {
                String id = "arc_electrode_burnt_" + electrode.getType().name().toLowerCase(Locale.ROOT);
                Item burnt = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
                inventory.setStackInSlot(i, burnt == Items.AIR ? ItemStack.EMPTY : new ItemStack(burnt));
            }
        }
    }

    public boolean hasIngredients() {
        for (int i = 5; i < 25; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(stack, liquidMode);
            if (recipe == null) continue;
            if (liquidMode && recipe.fluidOutput != null) return true;
            if (!liquidMode && recipe.solidOutput != null) return true;
        }
        return false;
    }

    public boolean hasElectrodes() {
        for (int i = 0; i < 3; i++) {
            if (!(inventory.getStackInSlot(i).getItem() instanceof ItemArcElectrode)) return false;
        }
        return true;
    }

    public void toggleLiquid() {
        liquidMode = !liquidMode;
        setChanged();
    }

    public void addToStack(Mats.MaterialStack matStack) {
        for (Mats.MaterialStack mat : liquids) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }
        liquids.add(matStack.copy());
    }

    private static int stackAmount(List<Mats.MaterialStack> stacks) {
        int n = 0;
        for (Mats.MaterialStack s : stacks) n += s.amount;
        return n;
    }

    private static int stackAmount(Mats.MaterialStack[] stacks) {
        int n = 0;
        for (Mats.MaterialStack s : stacks) n += s.amount;
        return n;
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 2).relative(rot), dir),
                new DirPos(worldPosition.relative(dir, 2).relative(rot.getOpposite()), dir),
                new DirPos(worldPosition.relative(rot, 2).relative(dir), rot),
                new DirPos(worldPosition.relative(rot.getOpposite(), 2).relative(dir), rot.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
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
        tag.putBoolean("liquidMode", liquidMode);
        tag.putFloat("prog", progress);
        tag.putInt("delay", delay);
        ListTag list = new ListTag();
        for (Mats.MaterialStack s : liquids) {
            CompoundTag t = new CompoundTag();
            t.putInt("id", s.material.id);
            t.putInt("amt", s.amount);
            list.add(t);
        }
        tag.put("liquids", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        liquidMode = tag.getBoolean("liquidMode");
        progress = tag.getFloat("prog");
        delay = tag.getInt("delay");
        liquids.clear();
        ListTag list = tag.getList("liquids", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            NTMMaterial mat = Mats.matById.get(t.getInt("id"));
            if (mat != null) liquids.add(new Mats.MaterialStack(mat, t.getInt("amt")));
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(liquidMode);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
        buf.writeInt(upgrade);
        buf.writeInt(liquids.size());
        for (Mats.MaterialStack s : liquids) {
            buf.writeInt(s.material.id);
            buf.writeInt(s.amount);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        liquidMode = buf.readBoolean();
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
        upgrade = buf.readInt();
        liquids.clear();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            NTMMaterial mat = Mats.matById.get(buf.readInt());
            int amt = buf.readInt();
            if (mat != null) liquids.add(new Mats.MaterialStack(mat, amt));
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ArcFurnaceMenu(id, inv, this);
    }
}
