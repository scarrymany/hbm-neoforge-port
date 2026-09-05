package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadiolysisMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.util.Tuple.Pair;
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
 * CE {@code TileEntityMachineRadiolysis}: RTG heat ×10 HE, crack 100 mB when heat&gt;100.
 * {@code tanks[0].setType(10, 11)} Exact CE {@code :118}. {@code sterilize} Exact CE {@code :211-238}
 * ({@code ntmContagion} via {@code CUSTOM_DATA}). Pancake food-exception skipped (item not registered).
 */
public class MachineRadiolysisBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;

    public final FluidTankNTM input;
    public final FluidTankNTM out1;
    public final FluidTankNTM out2;
    public long power;
    public int heat;

    public MachineRadiolysisBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 15, true, true);
        this.input = new FluidTankNTM(Fluids.NONE, 2_000).withOwner(this);
        this.out1 = new FluidTankNTM(Fluids.NONE, 2_000).withOwner(this);
        this.out2 = new FluidTankNTM(Fluids.NONE, 2_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radiolysis");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 10) return stack.getItem() instanceof ItemRTGPellet;
        if (slot == 10) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 12) return true;
        if (slot == 14) return Library.isBattery(stack);
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot < 10 || slot == 13;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 13};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeItemsFromTE(inventory, 14, power, MAX_POWER);
        int newHeat = 0;
        for (int i = 0; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!(stack.getItem() instanceof ItemRTGPellet)) continue;
            newHeat += ItemRTGPellet.getScaledPower(stack);
            inventory.setStackInSlot(i, ItemRTGPellet.handleDecay(stack));
        }
        heat = newHeat;
        power = Math.min(MAX_POWER, power + heat * 10L);

        // CE TileEntityMachineRadiolysis.java:118
        this.input.setType(10, 11, inventory);
        setupTanks();

        if (heat > 100) {
            int crackTime = (int) Math.max(-0.1 * (heat - 100) + 30, 5);
            if (level.getGameTime() % crackTime == 0) crack();
            // Exact CE TileEntityMachineRadiolysis.java:127-128
            if (heat >= 200 && level.getGameTime() % 100 == 0) {
                sterilize();
            }
        }

        for (DirPos pos : getConPos()) {
            tryProvide(level, pos.getPos(), pos.getDir());
            trySubscribe(input.getTankType(), level, pos);
            if (out1.getFill() > 0) tryProvide(out1, level, pos);
            if (out2.getFill() > 0) tryProvide(out2, level, pos);
        }
        dataChanged();
        networkPackMK2(25);
    }

    private void setupTanks() {
        Pair<FluidStack, FluidStack> rec = RadiolysisRecipes.getRadiolysis(input.getTankType());
        if (rec != null) {
            out1.setTankType(rec.getKey().type);
            out2.setTankType(rec.getValue().type);
        } else {
            input.setTankType(Fluids.NONE);
            out1.setTankType(Fluids.NONE);
            out2.setTankType(Fluids.NONE);
        }
    }

    private void crack() {
        Pair<FluidStack, FluidStack> rec = RadiolysisRecipes.getRadiolysis(input.getTankType());
        if (rec == null) return;
        int left = rec.getKey().fill;
        int right = rec.getValue().fill;
        if (input.getFill() < 100) return;
        if (out1.getFill() + left > out1.getMaxFill()) return;
        if (out2.getFill() + right > out2.getMaxFill()) return;
        input.setFill(input.getFill() - 100);
        out1.setFill(out1.getFill() + left);
        out2.setFill(out2.getFill() + right);
    }

    /** Exact CE {@code TileEntityMachineRadiolysis#sterilize} {@code :211-238}. */
    private void sterilize() {
        ItemStack in = inventory.getStackInSlot(12);
        if (in.isEmpty()) {
            return;
        }
        // CE destroys 1 ItemFood unless pancake — pancake is not registered here.
        if (in.getFoodProperties(null) != null) {
            inventory.extractItem(12, 1, false);
        }
        in = inventory.getStackInSlot(12);
        if (in.isEmpty() || !EntityEffectHandler.hasNtmContagion(in)) {
            return;
        }
        ItemStack output = in.copy();
        EntityEffectHandler.setNtmContagion(output, false);
        output.setCount(1);
        if (inventory.insertItem(13, output, true).isEmpty()) {
            inventory.extractItem(12, output.getCount(), false);
            inventory.insertItem(13, output, false);
        }
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ(), Direction.EAST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ(), Direction.WEST),
                new DirPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
        };
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
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(out1, out2);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, out1, out2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("heat", heat);
        input.writeToNBT(tag, "input");
        out1.writeToNBT(tag, "output1");
        out2.writeToNBT(tag, "output2");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        heat = tag.getInt("heat");
        input.readFromNBT(tag, "input");
        out1.readFromNBT(tag, "output1");
        out2.readFromNBT(tag, "output2");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(heat);
        input.serialize(buf);
        out1.serialize(buf);
        out2.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        heat = buf.readInt();
        input.deserialize(buf);
        out1.deserialize(buf);
        out2.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadiolysisMenu(id, inv, this);
    }
}
