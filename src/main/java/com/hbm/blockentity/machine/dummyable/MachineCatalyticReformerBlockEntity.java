package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.CatalyticReformerMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ReformingRecipes;
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
 * CE {@code TileEntityMachineCatalyticReformer}: 20k HE / 100 mB + catalyst. Canister load/unload skipped.
 */
public class MachineCatalyticReformerBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000;

    public final FluidTankNTM input;
    public final FluidTankNTM out1;
    public final FluidTankNTM out2;
    public final FluidTankNTM out3;
    public long power;

    public MachineCatalyticReformerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 11, true, true);
        this.input = new FluidTankNTM(Fluids.NAPHTHA, 64_000).withOwner(this);
        this.out1 = new FluidTankNTM(Fluids.REFORMATE, 24_000).withOwner(this);
        this.out2 = new FluidTankNTM(Fluids.PETROLEUM, 24_000).withOwner(this);
        this.out3 = new FluidTankNTM(Fluids.HYDROGEN, 24_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.catalyticReformer");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 9) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 10) return stack.getItem() == catalyst();
        return slot == 1 || slot == 3 || slot == 5 || slot == 7;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 2 || slot == 4 || slot == 6 || slot == 8;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(level, pos);
                trySubscribe(input.getTankType(), level, pos);
            }
        }
        power = Library.chargeTEFromItems(inventory, 0, power, MAX_POWER);
        ItemStack id = inventory.getStackInSlot(9);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            input.setTankType(ident.getType(level, worldPosition, id));
        }
        reform();
        for (DirPos pos : getConPos()) {
            if (out1.getFill() > 0) tryProvide(out1, level, pos);
            if (out2.getFill() > 0) tryProvide(out2, level, pos);
            if (out3.getFill() > 0) tryProvide(out3, level, pos);
        }
        dataChanged();
        networkPackMK2(150);
    }

    private void reform() {
        Triplet<FluidStack, FluidStack, FluidStack> out = ReformingRecipes.getOutput(input.getTankType());
        if (out == null) {
            out1.setTankType(Fluids.NONE);
            out2.setTankType(Fluids.NONE);
            out3.setTankType(Fluids.NONE);
            return;
        }
        out1.setTankType(out.getX().type);
        out2.setTankType(out.getY().type);
        out3.setTankType(out.getZ().type);
        if (power < 20_000) return;
        if (input.getFill() < 100) return;
        ItemStack cat = inventory.getStackInSlot(10);
        if (cat.isEmpty() || cat.getItem() != catalyst()) return;
        if (out1.getFill() + out.getX().fill > out1.getMaxFill()) return;
        if (out2.getFill() + out.getY().fill > out2.getMaxFill()) return;
        if (out3.getFill() + out.getZ().fill > out3.getMaxFill()) return;
        input.setFill(input.getFill() - 100);
        out1.setFill(out1.getFill() + out.getX().fill);
        out2.setFill(out2.getFill() + out.getY().fill);
        out3.setFill(out3.getFill() + out.getZ().fill);
        power -= 20_000;
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 2).relative(rot), dir),
                new DirPos(worldPosition.relative(dir, 2).relative(rot.getOpposite()), dir),
                new DirPos(worldPosition.relative(dir.getOpposite(), 2).relative(rot), dir.getOpposite()),
                new DirPos(worldPosition.relative(dir.getOpposite(), 2).relative(rot.getOpposite()), dir.getOpposite()),
                new DirPos(worldPosition.relative(rot, 3), rot),
                new DirPos(worldPosition.relative(rot.getOpposite(), 3), rot.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
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
        return List.of(input);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(out1, out2, out3);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(input, out1, out2, out3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        input.writeToNBT(tag, "input");
        out1.writeToNBT(tag, "o1");
        out2.writeToNBT(tag, "o2");
        out3.writeToNBT(tag, "o3");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        input.readFromNBT(tag, "input");
        out1.readFromNBT(tag, "o1");
        out2.readFromNBT(tag, "o2");
        out3.readFromNBT(tag, "o3");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        input.serialize(buf);
        out1.serialize(buf);
        out2.serialize(buf);
        out3.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        input.deserialize(buf);
        out1.deserialize(buf);
        out2.deserialize(buf);
        out3.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CatalyticReformerMenu(id, inv, this);
    }
}
