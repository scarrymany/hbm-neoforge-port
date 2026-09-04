package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.OreSlopperMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.OreSlopperRecipes;
import com.hbm.inventory.recipes.OreSlopperRecipes.OreSlopperRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineOreSlopper}: 100k HE, water→slop, bedrock_ore_base → BASE grades.
 * {@code tanks[0].setType(1)} Exact CE {@code :121}. Animation / entity shred skipped.
 */
public class MachineOreSlopperBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000;
    public static final int WATER_USED = 1_000;
    public static final long CONSUMPTION = 200;

    public final FluidTankNTM water;
    public final FluidTankNTM slop;
    public long power;
    public float progress;
    public boolean processing;
    private final double[] ores = new double[BedrockOreType.VALUES.length];

    public MachineOreSlopperBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11, true, true);
        this.water = new FluidTankNTM(Fluids.WATER, 16_000).withOwner(this);
        this.slop = new FluidTankNTM(Fluids.SLOP, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineOreSlopper");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 2) return OreSlopperRecipes.isInput(stack);
        if (slot == 9 || slot == 10) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 3 && slot <= 8;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);
        // CE TileEntityMachineOreSlopper.java:121
        this.water.setType(1, inventory);

        int speed = upgrade(UpgradeType.SPEED);
        int effect = upgrade(UpgradeType.EFFECT);
        long use = CONSUMPTION + (CONSUMPTION * speed) / 2 + CONSUMPTION * effect;

        processing = false;
        if (canSlop(use)) {
            power -= use;
            progress += 1F / (600 - speed * 150);
            processing = true;
            while (progress >= 1F && canSlop(use)) {
                progress -= 1F;
                ItemStack in = inventory.getStackInSlot(2);
                for (BedrockOreType type : BedrockOreType.VALUES) {
                    ores[type.ordinal()] += ItemBedrockOreBase.getOreAmount(in, type) * (1D + effect * 0.1);
                }
                inventory.extractItem(2, 1, false);
                water.setFill(water.getFill() - WATER_USED);
                slop.setFill(slop.getFill() + WATER_USED);
                setChanged();
            }
        } else {
            progress = 0F;
        }

        dumpOres();

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                if (water.getTankType() != Fluids.NONE) trySubscribe(water.getTankType(), level, pos);
            }
        }
        for (DirPos pos : getConPos()) {
            if (slop.getFill() > 0) tryProvide(slop, level, pos);
        }
        dataChanged();
        networkPackMK2(50);
    }

    private boolean canSlop(long use) {
        if (power < use) return false;
        if (!OreSlopperRecipes.isInput(inventory.getStackInSlot(2))) return false;
        if (water.getFill() < WATER_USED) return false;
        return slop.getFill() + WATER_USED <= slop.getMaxFill();
    }

    private void dumpOres() {
        OreSlopperRecipes.register();
        for (OreSlopperRecipe rec : OreSlopperRecipes.getAll()) {
            int idx = rec.type.ordinal();
            while (ores[idx] >= 1) {
                if (!insertOut(rec.output.copy())) break;
                ores[idx] -= 1;
            }
        }
    }

    private boolean insertOut(ItemStack out) {
        for (int i = 3; i <= 8; i++) {
            ItemStack dest = inventory.getStackInSlot(i);
            if (dest.isEmpty()) {
                inventory.setStackInSlot(i, out);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(dest, out) && dest.getCount() < dest.getMaxStackSize()) {
                dest.grow(1);
                return true;
            }
        }
        return false;
    }

    private int upgrade(UpgradeType type) {
        int level = 0;
        for (int s = 9; s <= 10; s++) {
            ItemStack st = inventory.getStackInSlot(s);
            if (st.getItem() instanceof ItemMachineUpgrade u && u.getType() == type) {
                level = Math.max(level, u.getTier());
            }
        }
        return Math.min(level, 3);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 3), dir),
                new DirPos(worldPosition.relative(dir, -3), dir.getOpposite()),
                new DirPos(worldPosition.relative(rot), rot),
                new DirPos(worldPosition.relative(rot.getOpposite()), rot.getOpposite()),
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
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(water);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(slop);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(water, slop);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putFloat("prog", progress);
        water.writeToNBT(tag, "t0");
        slop.writeToNBT(tag, "t1");
        for (int i = 0; i < ores.length; i++) tag.putDouble("ore" + i, ores[i]);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getFloat("prog");
        water.readFromNBT(tag, "t0");
        slop.readFromNBT(tag, "t1");
        for (int i = 0; i < ores.length; i++) ores[i] = tag.getDouble("ore" + i);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeFloat(progress);
        buf.writeBoolean(processing);
        water.serialize(buf);
        slop.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readFloat();
        processing = buf.readBoolean();
        water.deserialize(buf);
        slop.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new OreSlopperMenu(id, inv, this);
    }
}
