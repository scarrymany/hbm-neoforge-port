package com.hbm.blockentity.machine.fusion;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.fusion.IcfPressMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.IcfPressItems;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.items.machine.ItemICFPellet.EnumICFFuel;
import com.hbm.main.MainRegistry;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ported from CE's {@code TileEntityICFPress} - a small, non-multiblock machine that crafts
 * {@link ItemICFPellet} stacks from two independent fuel inputs (each either a fluid or a
 * single-ingot material) plus an optional muon-catalyst item consumed 1:1 for the muon-catalyzed
 * bonus ({@link ItemICFPellet#getFusingDifficulty}).
 * <p>
 * <b>Simplification versus CE</b>: CE's tank slots (6/7) double as item-canister-fill slots via
 * {@code FluidTankNTM.setType(slot, inventory)}, a subsystem this port's {@code FluidTankNTM}
 * explicitly does not carry over yet (see that class's own javadoc, "item-canister loading
 * subsystem ... left out"). This press's tanks fill only through the {@code fluidmk2} network
 * ({@link IFluidStandardReceiverMK2}) instead - a real, working fluid path, just without the
 * "insert a filled canister directly" convenience. Restoring that convenience is a follow-up once
 * the item-canister subsystem lands.
 */
public class IcfPressBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IFluidStandardReceiverMK2, IPersistentNBT, MenuProvider {

    public static final int MAX_MUON = 16;
    private static final int SLOT_EMPTY_PELLET = 0;
    private static final int SLOT_OUTPUT = 1;
    private static final int SLOT_MUON = 2;
    private static final int SLOT_MUON_CONTAINER_OUT = 3;
    private static final int SLOT_FUEL1 = 4;
    private static final int SLOT_FUEL2 = 5;
    private static final int[] SLOTS_TOP_BOTTOM = new int[]{0, 1, 2, 3, 4};
    private static final int[] SLOTS_SIDES = new int[]{0, 1, 2, 3, 5};

    public final FluidTankNTM[] tanks = new FluidTankNTM[2];
    public int muon;
    private final boolean[] usedFluid = new boolean[2];

    public IcfPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, false);
        tanks[0] = new FluidTankNTM(Fluids.DEUTERIUM, 16_000).withOwner(this);
        tanks[1] = new FluidTankNTM(Fluids.TRITIUM, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineICFPress");
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(tanks[0].getTankType(), level, worldPosition.relative(dir), dir);
                trySubscribe(tanks[1].getTankType(), level, worldPosition.relative(dir), dir);
            }
        }

        ItemStack muonStack = inventory.getStackInSlot(SLOT_MUON);
        if (muon <= 0 && !muonStack.isEmpty() && muonStack.getItem() == IcfPressItems.PARTICLE_MUON.get()) {
            muonStack.shrink(1);
            this.muon = MAX_MUON;
            setChanged();
        }

        press();
        dataChanged();
        networkPackMK2(15);
    }

    public void press() {
        ItemStack emptyPelletSlot = inventory.getStackInSlot(SLOT_EMPTY_PELLET);
        ItemStack outputSlot = inventory.getStackInSlot(SLOT_OUTPUT);

        if (emptyPelletSlot.isEmpty() || emptyPelletSlot.getItem() != IcfPressItems.ICF_PELLET_EMPTY.get()) return;
        if (!outputSlot.isEmpty()) return;

        EnumICFFuel fuel1 = getFuel(tanks[0], inventory.getStackInSlot(SLOT_FUEL1), 0);
        EnumICFFuel fuel2 = getFuel(tanks[1], inventory.getStackInSlot(SLOT_FUEL2), 1);

        if (fuel1 == null || fuel2 == null || fuel1 == fuel2) return;

        Item pelletItem = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "icf_pellet"));
        ItemStack newPellet = ItemICFPellet.setup(new ItemStack(pelletItem), fuel1, fuel2, muon > 0);
        inventory.setStackInSlot(SLOT_OUTPUT, newPellet);

        if (muon > 0) muon--;

        emptyPelletSlot.shrink(1);

        if (usedFluid[0]) {
            tanks[0].setFill(tanks[0].getFill() - 1000);
        } else {
            inventory.getStackInSlot(SLOT_FUEL1).shrink(1);
        }
        if (usedFluid[1]) {
            tanks[1].setFill(tanks[1].getFill() - 1000);
        } else {
            inventory.getStackInSlot(SLOT_FUEL2).shrink(1);
        }

        setChanged();
    }

    @Nullable
    private EnumICFFuel getFuel(FluidTankNTM tank, ItemStack slot, int index) {
        usedFluid[index] = false;
        if (tank.getFill() >= 1000 && ItemICFPellet.FLUID_MAP.containsKey(tank.getTankType())) {
            usedFluid[index] = true;
            return ItemICFPellet.FLUID_MAP.get(tank.getTankType());
        }
        if (slot.isEmpty()) return null;
        List<Mats.MaterialStack> mats = Mats.getMaterialsFromItem(slot);
        if (mats.size() != 1) return null;

        Mats.MaterialStack mat = mats.get(0);
        if (mat.amount != MaterialShapes.INGOT.q(1)) return null;
        return ItemICFPellet.MATERIAL_MAP.get(mat.material);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.getItem() == IcfPressItems.ICF_PELLET_EMPTY.get()) return slot == SLOT_EMPTY_PELLET;
        if (stack.getItem() == IcfPressItems.PARTICLE_MUON.get()) return slot == SLOT_MUON;
        return slot == SLOT_FUEL1 || slot == SLOT_FUEL2;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return side == Direction.UP || side == Direction.DOWN ? SLOTS_TOP_BOTTOM : SLOTS_SIDES;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == SLOT_OUTPUT || slot == SLOT_MUON_CONTAINER_OUT;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tanks[0].writeToNBT(tag, "t0");
        tanks[1].writeToNBT(tag, "t1");
        tag.putInt("muon", muon);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks[0].readFromNBT(tag, "t0");
        tanks[1].readFromNBT(tag, "t1");
        this.muon = tag.getInt("muon");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte((byte) muon);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.muon = buf.readByte();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        tanks[0].writeToNBT(nbt, "t0");
        tanks[1].writeToNBT(nbt, "t1");
        nbt.putInt("muon", muon);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tanks[0].readFromNBT(nbt, "t0");
        tanks[1].readFromNBT(nbt, "t1");
        this.muon = nbt.getInt("muon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IcfPressMenu(containerId, playerInventory, this);
    }
}
