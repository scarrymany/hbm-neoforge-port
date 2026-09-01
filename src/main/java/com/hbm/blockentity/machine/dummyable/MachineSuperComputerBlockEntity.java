package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.dummyable.SuperComputerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SuperComputerRecipes;
import com.hbm.inventory.recipes.SuperComputerRecipes.ChanceOut;
import com.hbm.inventory.recipes.SuperComputerRecipes.SuperComputerRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
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
 * CE {@code TileEntityMachineSuperComputer} — 8 slots, 2×4k tanks (grow to recipe×2), 100k HE.
 * Recipe picker GUI ({@code IControlReceiver} / {@code ModuleMachineSuperComputer}) is not ported —
 * auto-match like ChemPlant so the CE table actually runs.
 * TODO(CE: com.hbm.tileentity.machine.TileEntityMachineSuperComputer.java:186-194):
 * recipe dropdown setRecipe(selection) — blocked by ModuleMachineBase. Do not invent.
 */
public class MachineSuperComputerBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000;

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    public long power;
    public long maxPower = MAX_POWER;
    public int progress;
    public boolean didProcess;

    public MachineSuperComputerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8, true, true);
        this.input = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
        this.output = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSuperComputer");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return true;
        if (slot >= 5) return false;
        return slot >= 2 && slot <= 4;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 5 && slot <= 7;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{2, 3, 4, 5, 6, 7};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        SuperComputerRecipes.register();

        SuperComputerRecipe recipe = findRecipe();
        if (recipe != null) {
            this.maxPower = Math.max(Math.max(power, recipe.power * 100), MAX_POWER);
            resizeTanks(recipe);
        } else {
            this.maxPower = Math.max(power, MAX_POWER);
        }

        power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

        ItemStack id = inventory.getStackInSlot(1);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            input.setTankType(ident.getType(level, worldPosition, id));
        }

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                trySubscribe(input.getTankType(), level, pos);
            }
        }
        for (DirPos pos : getConPos()) {
            if (output.getFill() > 0) tryProvide(output, level, pos);
        }

        didProcess = false;
        if (recipe != null && power >= recipe.power && hasOutputSpace(recipe)) {
            power -= recipe.power;
            progress++;
            if (progress >= recipe.duration) {
                process(recipe);
                progress = 0;
                didProcess = true;
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(25);
    }

    private SuperComputerRecipe findRecipe() {
        SuperComputerRecipe best = null;
        for (SuperComputerRecipe recipe : SuperComputerRecipes.RECIPES) {
            if (!matchesItems(recipe) || !matchesFluid(recipe)) continue;
            if (best == null || recipe.inputItems.length > best.inputItems.length) best = recipe;
        }
        return best;
    }

    private boolean matchesItems(SuperComputerRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            boolean found = false;
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(2 + i), false)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private boolean matchesFluid(SuperComputerRecipe recipe) {
        if (recipe.inputFluid == null) return true;
        return input.getTankType() == recipe.inputFluid.type && input.getFill() >= recipe.inputFluid.fill;
    }

    private boolean hasOutputSpace(SuperComputerRecipe recipe) {
        ItemStack sample = recipe.outputChoices.length == 0 ? ItemStack.EMPTY : recipe.outputChoices[0].stack();
        if (!sample.isEmpty() && !inventory.insertItem(5, sample, true).isEmpty()) return false;
        if (recipe.outputFluid != null) {
            if (output.getTankType() != Fluids.NONE && output.getTankType() != recipe.outputFluid.type) return false;
            if (output.getFill() + recipe.outputFluid.fill > output.getMaxFill()) return false;
        }
        return true;
    }

    private void process(SuperComputerRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(2 + i), false)) {
                    inventory.extractItem(2 + i, key.count(), false);
                    used[i] = true;
                    break;
                }
            }
        }
        if (recipe.inputFluid != null) {
            input.setFill(input.getFill() - recipe.inputFluid.fill);
        }
        ItemStack out = roll(recipe);
        if (!out.isEmpty()) inventory.insertItem(5, out.copy(), false);
        if (recipe.outputFluid != null) {
            output.setTankType(recipe.outputFluid.type);
            output.setFill(output.getFill() + recipe.outputFluid.fill);
        }
    }

    private ItemStack roll(SuperComputerRecipe recipe) {
        if (recipe.outputChoices.length == 0) return ItemStack.EMPTY;
        int total = 0;
        for (ChanceOut c : recipe.outputChoices) total += c.weight();
        int pick = level.random.nextInt(Math.max(1, total));
        for (ChanceOut c : recipe.outputChoices) {
            pick -= c.weight();
            if (pick < 0) return c.stack();
        }
        return recipe.outputChoices[0].stack();
    }

    private void resizeTanks(SuperComputerRecipe recipe) {
        if (recipe.inputFluid != null) {
            input.changeTankSize(Math.max(Math.max(input.getFill(), recipe.inputFluid.fill * 2), 4_000));
            if (input.getFill() == 0) input.setTankType(recipe.inputFluid.type);
        }
        if (recipe.outputFluid != null) {
            output.changeTankSize(Math.max(Math.max(output.getFill(), recipe.outputFluid.fill * 2), 4_000));
        }
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 9), dir),
                new DirPos(worldPosition.relative(dir, 7).relative(rot, 2), rot),
                new DirPos(worldPosition.relative(dir, 7).relative(rot, -2), rot.getOpposite()),
                new DirPos(worldPosition.relative(dir, 5).relative(rot, 2), rot),
                new DirPos(worldPosition.relative(dir, 5).relative(rot, -2), rot.getOpposite()),
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
        return maxPower;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(output);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, output);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
        tag.putInt("progress", progress);
        input.writeToNBT(tag, "in");
        output.writeToNBT(tag, "out");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        maxPower = tag.getLong("maxPower");
        if (maxPower <= 0) maxPower = MAX_POWER;
        progress = tag.getInt("progress");
        input.readFromNBT(tag, "in");
        output.readFromNBT(tag, "out");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeInt(progress);
        buf.writeBoolean(didProcess);
        input.serialize(buf);
        output.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        progress = buf.readInt();
        didProcess = buf.readBoolean();
        input.deserialize(buf);
        output.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new SuperComputerMenu(id, inv, this);
    }
}
