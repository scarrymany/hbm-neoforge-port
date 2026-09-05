package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.dummyable.BlastFurnaceMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT.BlastFurnaceRecipe;
import com.hbm.lib.DirPos;
import com.hbm.modules.ModuleBurnTime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
 * CE {@code TileEntityMachineBlastFurnace}: coal-fuel + airblast speed, 2-in 2-out.
 * {@code FT_Polluting.pollute(SPILL, spill)} on flue overflow Exact CE {@code :113}.
 * Audio / particles stay skipped.
 */
public class MachineBlastFurnaceBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int FUEL_COAL = 200 * 8;
    public static final int FUEL_RATE = 200 * 4;
    public static final int MAX_FUEL = FUEL_COAL * 24;
    public static final int FLUE_GAS = 8;

    public final FluidTankNTM airblast;
    public final FluidTankNTM flue;
    public boolean isProgressing;
    public float progress;
    public float speed;
    public int fuel;
    /** Exact CE {@code TileEntityMachineBlastFurnace.java:60-61}. */
    public final ModuleBurnTime burnModule = new ModuleBurnTime().setWoodHeatMod(0D);

    public MachineBlastFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, false);
        this.airblast = new FluidTankNTM(Fluids.AIRBLAST, 4_000).withOwner(this);
        this.flue = new FluidTankNTM(Fluids.FLUE, 1_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.blastFurnace");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return getBurnTime(stack) > 0;
        if (slot == 1 || slot == 2) return BlastFurnaceRecipesNT.INSTANCE.isIngredient(stack);
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 3;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (DirPos pos : getConPos()) {
            trySubscribe(airblast.getTankType(), level, pos);
            if (flue.getFill() > 0) tryProvide(flue, level, pos);
        }

        if (!inventory.getStackInSlot(0).isEmpty()) {
            int capacity = MAX_FUEL - fuel;
            int burnValue = getBurnTime(inventory.getStackInSlot(0));
            if (burnValue > 0 && burnValue <= capacity) {
                fuel += burnValue;
                inventory.extractItem(0, 1, false);
            }
        }

        speed = 0F;
        BlastFurnaceRecipe recipe = BlastFurnaceRecipesNT.INSTANCE.getRecipe(inventory.getStackInSlot(1), inventory.getStackInSlot(2));
        if (recipe != null && fuel >= FUEL_RATE && hasQuantities(recipe) && canOutput(recipe)) {
            speed = Mth.clamp(0.5F + airblast.getFill() * 8F / airblast.getMaxFill(), 0.5F, 5F);
            isProgressing = true;
            progress += speed / recipe.duration;
            if (progress >= 1F) {
                process(recipe);
                progress = 0F;
                fuel -= FUEL_RATE;
                flue.setFill((int) (flue.getFill() + FLUE_GAS * (recipe.duration / Math.max(0.5F, speed))));
                if (flue.getFill() > flue.getMaxFill()) {
                    int spill = flue.getFill() - flue.getMaxFill();
                    // CE TileEntityMachineBlastFurnace.java:112-114
                    flue.getTankType().onFluidRelease(level, worldPosition.above(7), flue, spill);
                    FT_Polluting.pollute(level, worldPosition, flue.getTankType(),
                            FluidTrait.FluidReleaseType.SPILL, spill);
                    flue.setFill(flue.getMaxFill());
                }
            }
        } else {
            isProgressing = false;
            progress = 0F;
        }

        if (airblast.getFill() > 0) airblast.setFill((int) (airblast.getFill() * 0.95));
        dataChanged();
        networkPackMK2(100);
    }

    public int getBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        // CE TileEntityMachineBlastFurnace.java:215-217
        return burnModule.getBurnHeat(burnModule.getBurnTime(stack, 0D), stack, 0D);
    }

    public boolean hasQuantities(BlastFurnaceRecipe recipe) {
        if (recipe.inputs.length == 1) {
            return recipe.inputs[0].matchesRecipe(inventory.getStackInSlot(1), false)
                    || recipe.inputs[0].matchesRecipe(inventory.getStackInSlot(2), false);
        }
        AStack a = recipe.inputs[0];
        AStack b = recipe.inputs[1];
        return (a.matchesRecipe(inventory.getStackInSlot(1), false) && b.matchesRecipe(inventory.getStackInSlot(2), false))
                || (a.matchesRecipe(inventory.getStackInSlot(2), false) && b.matchesRecipe(inventory.getStackInSlot(1), false));
    }

    public boolean canOutput(BlastFurnaceRecipe recipe) {
        for (int i = 0; i < recipe.outputs.length; i++) {
            ItemStack slot = inventory.getStackInSlot(3 + i);
            if (slot.isEmpty()) continue;
            ItemStack out = recipe.outputs[i];
            if (!ItemStack.isSameItem(out, slot)) return false;
            if (out.getCount() + slot.getCount() > slot.getMaxStackSize()) return false;
        }
        return true;
    }

    public void process(BlastFurnaceRecipe recipe) {
        for (int i = 0; i < recipe.outputs.length; i++) {
            ItemStack out = recipe.outputs[i].copy();
            if (out.isEmpty()) continue;
            ItemStack slot = inventory.getStackInSlot(3 + i);
            if (slot.isEmpty()) inventory.setStackInSlot(3 + i, out);
            else slot.grow(out.getCount());
        }
        if (recipe.inputs.length == 1) {
            if (recipe.inputs[0].matchesRecipe(inventory.getStackInSlot(1), false)) {
                inventory.extractItem(1, recipe.inputs[0].count(), false);
            } else {
                inventory.extractItem(2, recipe.inputs[0].count(), false);
            }
        } else if (recipe.inputs[0].matchesRecipe(inventory.getStackInSlot(1), false)
                && recipe.inputs[1].matchesRecipe(inventory.getStackInSlot(2), false)) {
            inventory.extractItem(1, recipe.inputs[0].count(), false);
            inventory.extractItem(2, recipe.inputs[1].count(), false);
        } else {
            inventory.extractItem(2, recipe.inputs[0].count(), false);
            inventory.extractItem(1, recipe.inputs[1].count(), false);
        }
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ(), Direction.EAST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ(), Direction.WEST),
                new DirPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.relative(dir, 2).above(3), dir),
                new DirPos(worldPosition.relative(dir, 2).above(5), dir),
                new DirPos(worldPosition.above(7), Direction.UP)
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    public int getProgressScaled(int i) {
        return (int) (progress * i);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(airblast);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(flue);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(airblast, flue);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("progress", progress);
        tag.putInt("fuel", fuel);
        airblast.writeToNBT(tag, "t0");
        flue.writeToNBT(tag, "t1");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getFloat("progress");
        fuel = tag.getInt("fuel");
        airblast.readFromNBT(tag, "t0");
        flue.readFromNBT(tag, "t1");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
        buf.writeFloat(speed);
        buf.writeInt(fuel);
        airblast.serialize(buf);
        flue.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
        speed = buf.readFloat();
        fuel = buf.readInt();
        airblast.deserialize(buf);
        flue.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new BlastFurnaceMenu(id, inv, this);
    }
}
