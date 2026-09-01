package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HydrotreaterMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple.Triplet;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineHydrotreater}: 20k HE / 100 mB + H₂@P1 + catalyst. Canister load skipped.
 */
public class MachineHydrotreaterBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;

    public final FluidTankNTM oil;
    public final FluidTankNTM hydrogen;
    public final FluidTankNTM out1;
    public final FluidTankNTM out2;
    public long power;

    public MachineHydrotreaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 9, true, true);
        this.oil = new FluidTankNTM(Fluids.OIL, 64_000).withOwner(this);
        this.hydrogen = new FluidTankNTM(Fluids.HYDROGEN, 64_000).withOwner(this).withPressure(1);
        this.out1 = new FluidTankNTM(Fluids.OIL_DS, 24_000).withOwner(this);
        this.out2 = new FluidTankNTM(Fluids.SOURGAS, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hydrotreater");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 7) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 8) return stack.getItem() == catalyst();
        return slot == 1 || slot == 3 || slot == 5;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 2 || slot == 4 || slot == 6;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                trySubscribe(oil.getTankType(), level, pos);
                trySubscribe(hydrogen.getTankType(), level, pos);
            }
        }
        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);
        ItemStack id = inventory.getStackInSlot(7);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            oil.setTankType(ident.getType(level, worldPosition, id));
        }
        if (level.getGameTime() % 2 == 0) reform();
        for (DirPos pos : getConPos()) {
            if (out1.getFill() > 0) tryProvide(out1, level, pos);
            if (out2.getFill() > 0) tryProvide(out2, level, pos);
        }
        dataChanged();
        networkPackMK2(25);
    }

    private void reform() {
        Triplet<FluidStack, FluidStack, FluidStack> out = HydrotreatingRecipes.getOutput(oil.getTankType());
        if (out == null) {
            out1.setTankType(Fluids.NONE);
            out2.setTankType(Fluids.NONE);
            return;
        }
        hydrogen.withPressure(out.getX().pressure).setTankType(out.getX().type);
        out1.setTankType(out.getY().type);
        out2.setTankType(out.getZ().type);
        if (power < 20_000) return;
        if (oil.getFill() < 100) return;
        if (hydrogen.getFill() < out.getX().fill) return;
        ItemStack cat = inventory.getStackInSlot(8);
        if (cat.isEmpty() || cat.getItem() != catalyst()) return;
        if (out1.getFill() + out.getY().fill > out1.getMaxFill()) return;
        if (out2.getFill() + out.getZ().fill > out2.getMaxFill()) return;
        oil.setFill(oil.getFill() - 100);
        hydrogen.setFill(hydrogen.getFill() - out.getX().fill);
        out1.setFill(out1.getFill() + out.getY().fill);
        out2.setFill(out2.getFill() + out.getZ().fill);
        power -= 20_000;
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() + 1, Direction.EAST),
                new DirPos(worldPosition.getX() + 2, worldPosition.getY(), worldPosition.getZ() - 1, Direction.EAST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() + 1, Direction.WEST),
                new DirPos(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 1, Direction.WEST),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() + 2, Direction.SOUTH),
                new DirPos(worldPosition.getX() + 1, worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
                new DirPos(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 2, Direction.NORTH),
        };
    }

    private static Item catalyst() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "catalytic_converter"));
    }

    public static boolean isCatalyst(ItemStack stack) {
        Item item = catalyst();
        return item != Items.AIR && stack.getItem() == item;
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
        return List.of(oil, hydrogen);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(out1, out2);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(oil, hydrogen, out1, out2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        oil.writeToNBT(tag, "t0");
        hydrogen.writeToNBT(tag, "t1");
        out1.writeToNBT(tag, "t2");
        out2.writeToNBT(tag, "t3");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        oil.readFromNBT(tag, "t0");
        hydrogen.readFromNBT(tag, "t1");
        out1.readFromNBT(tag, "t2");
        out2.readFromNBT(tag, "t3");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        oil.serialize(buf);
        hydrogen.serialize(buf);
        out1.serialize(buf);
        out2.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        oil.deserialize(buf);
        hydrogen.deserialize(buf);
        out1.deserialize(buf);
        out2.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HydrotreaterMenu(id, inv, this);
    }
}
