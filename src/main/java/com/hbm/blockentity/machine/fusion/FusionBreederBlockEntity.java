package com.hbm.blockentity.machine.fusion;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.fusion.FusionBreederMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.weapon.WeaponMeleeItems;
import com.hbm.lib.DirPos;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import com.hbm.util.Tuple;
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

import java.util.List;

/**
 * CE {@code TileEntityFusionBreeder}. {@code receivesFusionPower=false} — flux only.
 * TODO(CE: TileEntityFusionBreeder.java:323): OpenComputers ntm_fusion_breeder.
 */
public class FusionBreederBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IFluidStandardTransceiverMK2, IFusionPowerReceiver, MenuProvider {

    public static final double CAPACITY = 10_000D;

    protected PlasmaNetwork.PlasmaNode plasmaNode;
    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    public double neutronEnergy;
    public double neutronEnergySync;
    public double progress;

    public FusionBreederBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, true, false);
        tanks[0] = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionBreeder");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        tanks[0].setType(0, inventory);
        if (!canProcessSolid() && !canProcessLiquid()) progress = 0;
        this.neutronEnergySync = this.neutronEnergy;
        for (DirPos pos : getConPos()) {
            if (tanks[0].getTankType() != Fluids.NONE) trySubscribe(tanks[0].getTankType(), level, pos);
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, pos);
        }
        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = FusionFacing.of(this).getOpposite();
            BlockPos nodePos = worldPosition.offset(dir.getStepX() * 2, 2, dir.getStepZ() * 2);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetwork.THE_PROVIDER);
            if (plasmaNode == null) {
                plasmaNode = (PlasmaNetwork.PlasmaNode) new PlasmaNetwork.PlasmaNode(PlasmaNetwork.THE_PROVIDER, nodePos)
                        .setConnections(new DirPos(worldPosition.getX() + dir.getStepX() * 3,
                                worldPosition.getY() + 2, worldPosition.getZ() + dir.getStepZ() * 3, dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }
        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);
        networkPackNT(25);
        this.neutronEnergy = 0;
    }

    public boolean canProcessSolid() {
        if (inventory.getStackInSlot(1).isEmpty()) return false;
        if (inventory.getStackInSlot(1).getItem() == WeaponMeleeItems.METEORITE_SWORD_IRRADIATED.get() 
                && inventory.getStackInSlot(2).isEmpty()) return true;
        OutgasserRecipes.OutgasserRecipe output = OutgasserRecipes.getRecipe(inventory.getStackInSlot(1));
        if (output == null) return false;
        FluidStack fluid = output.fluidOutput;
        if (fluid != null) {
            if (tanks[1].getTankType() != fluid.type && tanks[1].getFill() > 0) return false;
            tanks[1].setTankType(fluid.type);
            if (tanks[1].getFill() + fluid.fill > tanks[1].getMaxFill()) return false;
        }
        ItemStack out = output.solidOutput;
        if (inventory.getStackInSlot(2).isEmpty() || out == null || out.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(inventory.getStackInSlot(2), out)
                && inventory.getStackInSlot(2).getCount() + out.getCount() <= inventory.getStackInSlot(2).getMaxStackSize();
    }

    public boolean canProcessLiquid() {
        Tuple.Pair<Integer, FluidStack> output = FluidBreederRecipes.getOutput(tanks[0].getTankType());
        if (output == null) return false;
        if (tanks[0].getFill() < output.getKey()) return false;
        FluidStack fluid = output.getValue();
        if (tanks[1].getTankType() != fluid.type && tanks[1].getFill() > 0) return false;
        tanks[1].setTankType(fluid.type);
        return tanks[1].getFill() + fluid.fill <= tanks[1].getMaxFill();
    }

    private void processSolid() {
        if (inventory.getStackInSlot(1).getItem() == WeaponMeleeItems.METEORITE_SWORD_IRRADIATED.get()) {
            ItemStack sword = inventory.getStackInSlot(1).copy();
            sword.shrink(1);
            inventory.setStackInSlot(1, sword);
            inventory.setStackInSlot(2, new ItemStack(WeaponMeleeItems.METEORITE_SWORD_FUSED.get()));
            this.progress = 0;
            return;
        }
        OutgasserRecipes.OutgasserRecipe output = OutgasserRecipes.getRecipe(inventory.getStackInSlot(1));
        ItemStack stack = inventory.getStackInSlot(1);
        if (!stack.isEmpty()) {
            ItemStack copy = stack.copy();
            copy.shrink(1);
            inventory.setStackInSlot(1, copy);
        }
        this.progress = 0;
        if (output == null) return;
        if (output.fluidOutput != null) {
            tanks[1].setFill(tanks[1].getFill() + output.fluidOutput.fill);
        }
        ItemStack out = output.solidOutput;
        if (out != null && !out.isEmpty()) {
            if (inventory.getStackInSlot(2).isEmpty()) {
                inventory.setStackInSlot(2, out.copy());
            } else {
                ItemStack dest = inventory.getStackInSlot(2).copy();
                dest.grow(out.getCount());
                inventory.setStackInSlot(2, dest);
            }
        }
    }

    private void processLiquid() {
        Tuple.Pair<Integer, FluidStack> output = FluidBreederRecipes.getOutput(tanks[0].getTankType());
        tanks[0].setFill(tanks[0].getFill() - output.getKey());
        tanks[1].setFill(tanks[1].getFill() + output.getValue().fill);
    }

    public void doProgress() {
        if (canProcessSolid()) {
            this.progress += this.neutronEnergy;
            if (progress > CAPACITY) {
                processSolid();
                progress = 0;
                setChanged();
            }
        } else if (canProcessLiquid()) {
            this.progress += this.neutronEnergy;
            if (progress > CAPACITY) {
                processLiquid();
                progress = 0;
                setChanged();
            }
        } else {
            progress = 0;
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 1) return OutgasserRecipes.getRecipe(stack) != null;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == 2;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{1, 2};
    }

    public DirPos[] getConPos() {
        Direction dir = FusionFacing.of(this);
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + dir.getStepX() * 3, p.getY() + 2, p.getZ() + dir.getStepZ() * 3, dir),
                new DirPos(p.getX() + rot.getStepX() * 2, p.getY(), p.getZ() + rot.getStepZ() * 2, rot),
                new DirPos(p.getX() - rot.getStepX() * 2, p.getY(), p.getZ() - rot.getStepZ() * 2, rot.getOpposite()),
                new DirPos(p.getX() + dir.getStepX() + rot.getStepX() * 2, p.getY(), p.getZ() + dir.getStepZ() + rot.getStepZ() * 2, rot),
                new DirPos(p.getX() + dir.getStepX() - rot.getStepX() * 2, p.getY(), p.getZ() + dir.getStepZ() - rot.getStepZ() * 2, rot.getOpposite())
        };
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && plasmaNode != null) {
            UniNodespace.destroyNode(level, plasmaNode);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(neutronEnergySync);
        buf.writeDouble(progress);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        neutronEnergy = buf.readDouble();
        progress = buf.readDouble();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("progress", progress);
        tanks[0].writeToNBT(tag, "t0");
        tanks[1].writeToNBT(tag, "t1");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        progress = tag.getDouble("progress");
        tanks[0].readFromNBT(tag, "t0");
        tanks[1].readFromNBT(tag, "t1");
    }

    @Override
    public boolean receivesFusionPower() {
        return false;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        this.neutronEnergy = neutronPower;
        doProgress();
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FusionBreederMenu(id, inv, this);
    }
}
