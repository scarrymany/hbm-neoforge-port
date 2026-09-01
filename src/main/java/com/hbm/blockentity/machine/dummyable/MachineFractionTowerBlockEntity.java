package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.FractionTowerMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineFractionTower}: 100 mB/10t, stackable +3Y.
 */
public class MachineFractionTowerBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public final FluidTankNTM input;
    public final FluidTankNTM left;
    public final FluidTankNTM right;

    public MachineFractionTowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, false);
        this.input = new FluidTankNTM(Fluids.HEAVYOIL, 4_000).withOwner(this);
        this.left = new FluidTankNTM(Fluids.BITUMEN, 4_000).withOwner(this);
        this.right = new FluidTankNTM(Fluids.SMEAR, 4_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_fraction_tower");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(0);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            BlockEntity above = level.getBlockEntity(worldPosition.below(3));
            if (!(above instanceof MachineFractionTowerBlockEntity)) {
                input.setTankType(ident.getType(level, worldPosition, id));
            }
        }

        BlockEntity stackTe = level.getBlockEntity(worldPosition.above(3));
        if (stackTe instanceof MachineFractionTowerBlockEntity frac) {
            frac.input.setTankType(input.getTankType());
            frac.left.setTankType(left.getTankType());
            frac.right.setTankType(right.getTankType());
            int oil = Math.min(input.getFill(), frac.input.getMaxFill() - frac.input.getFill());
            int l = Math.min(frac.left.getFill(), left.getMaxFill() - left.getFill());
            int r = Math.min(frac.right.getFill(), right.getMaxFill() - right.getFill());
            input.setFill(input.getFill() - oil);
            left.setFill(left.getFill() + l);
            right.setFill(right.getFill() + r);
            frac.input.setFill(frac.input.getFill() + oil);
            frac.left.setFill(frac.left.getFill() - l);
            frac.right.setFill(frac.right.getFill() - r);
        }

        setupTanks();
        if (level.getGameTime() % 20 == 0) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                trySubscribe(input.getTankType(), level, worldPosition.relative(d, 2), d);
                if (left.getFill() > 0) tryProvide(left, level, worldPosition.relative(d, 2), d);
                if (right.getFill() > 0) tryProvide(right, level, worldPosition.relative(d, 2), d);
            }
        }
        if (level.getGameTime() % 10 == 0) fractionate();

        dataChanged();
        networkPackMK2(50);
    }

    private void setupTanks() {
        Pair<FluidStack, FluidStack> rec = FractionRecipes.getFractions(input.getTankType());
        if (rec != null) {
            left.setTankType(rec.getKey().type);
            right.setTankType(rec.getValue().type);
        }
    }

    private void fractionate() {
        Pair<FluidStack, FluidStack> rec = FractionRecipes.getFractions(input.getTankType());
        if (rec == null) return;
        int l = rec.getKey().fill;
        int r = rec.getValue().fill;
        if (input.getFill() >= 100 && left.getFill() + l <= left.getMaxFill() && right.getFill() + r <= right.getMaxFill()) {
            input.setFill(input.getFill() - 100);
            left.setFill(left.getFill() + l);
            right.setFill(right.getFill() + r);
        }
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(left, right);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, left, right);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        input.writeToNBT(tag, "in");
        left.writeToNBT(tag, "l");
        right.writeToNBT(tag, "r");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input.readFromNBT(tag, "in");
        left.readFromNBT(tag, "l");
        right.readFromNBT(tag, "r");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        input.serialize(buf);
        left.serialize(buf);
        right.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        input.deserialize(buf);
        left.deserialize(buf);
        right.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FractionTowerMenu(id, inv, this);
    }
}
